package test;

import com.breakinblocks.neovitae.common.block.NVBlocks;
import com.breakinblocks.neovitae.common.blockentity.SpiritCacheBlockEntity;
import com.breakinblocks.neovitae.common.menu.SpiritCacheMenu;
import com.mysterious.mystic.compat.lootr.SpiritCacheLootrAccess;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import noobanidus.mods.lootr.common.api.LootrAPI;
import noobanidus.mods.lootr.common.api.data.inventory.ILootrInventory;
import noobanidus.mods.lootr.common.data.LootrSavedData;

@Mod("eicore_spirit_test")
@net.neoforged.neoforge.gametest.PrefixGameTestTemplate(false)
public class SpiritCacheTests {
    public SpiritCacheTests(IEventBus bus) {
        bus.addListener((RegisterGameTestsEvent event) -> event.register(SpiritCacheTests.class));
    }

    @GameTest(template = "empty", templateNamespace = "neoforge", timeoutTicks = 100)
    public static void personalLoot(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        level.setBlockAndUpdate(pos, NVBlocks.SPIRIT_CACHE.block().get().defaultBlockState());
        var cache = (SpiritCacheBlockEntity) level.getBlockEntity(pos);
        ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse("eicore_spirit_test:fixture"));
        cache.setLootTable(table, 123L);
        var provider = ((SpiritCacheLootrAccess)cache).eicore$getLootrProvider();
        h.assertTrue(provider != null && LootrAPI.resolveBlockEntity(cache) == provider, "official converter resolves cache");
        ServerPlayer a = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "Alice"), ClientInformation.createDefault());
        ServerPlayer b = new ServerPlayer(level.getServer(), level, new GameProfile(UUID.randomUUID(), "Bob"), ClientInformation.createDefault());
        for (ServerPlayer player : java.util.List.of(a, b)) {
            player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
                    new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player,
                    net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false));
        }
        a.setPos(pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5);
        b.setPos(a.position());
        var playerField = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
        playerField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var players = (java.util.Map<UUID, ServerPlayer>)playerField.get(level.getServer().getPlayerList());
        players.put(a.getUUID(), a);
        players.put(b.getUUID(), b);
        try {
            ILootrInventory ia = provider.getPersonalInventory(a);
            ILootrInventory ib = provider.getPersonalInventory(b);
            h.assertTrue(ia != null && ib != null, "initial inventory exists; valid=" + cache.stillValid(a) + ", fake=" + LootrAPI.isFakePlayer(a));
            h.assertTrue(ia != ib && count(ia) == 8 && count(ib) == 8, "two independent fixture rewards: " + count(ia) + "/" + count(ib) + ", same=" + (ia == ib));
            h.assertTrue(ia.createMenu(1, a.getInventory(), a) instanceof SpiritCacheMenu, "original menu");
            for (int slot = 0; slot < ia.getContainerSize(); slot++) {
                if (!ia.getItem(slot).isEmpty()) {
                    ia.removeItem(slot, 1);
                    break;
                }
            }
            h.assertTrue(count(provider.getPersonalInventory(a)) == 7 && count(ib) == 8, "partial claims persist independently");
            provider.open(a);
            h.assertTrue(a.containerMenu instanceof SpiritCacheMenu && a.containerMenu.stillValid(a), "original menu opened and usable");
            a.closeContainer();
            ia.clearContent();
            h.assertTrue(count(provider.getPersonalInventory(a)) == 0 && count(ib) == 8, "no reroll or cross-player removal");
            h.assertTrue(cache.getLootTable() == table && cache.getItem(0).isEmpty(), "global unpack suppressed");
            cache.unpackLootTable(a);
            h.assertTrue(cache.getLootTable() == table && cache.getItem(0).isEmpty(), "external global unpack suppressed");
            CompoundTag savedCache = cache.saveWithFullMetadata(level.registryAccess());
            var data = (LootrSavedData)LootrAPI.getData(provider);
            CompoundTag savedData = data.save(new CompoundTag(), level.registryAccess());
            var reloadedData = LootrSavedData.load(savedData, level.registryAccess());
            h.assertTrue(count(reloadedData.getInventory(a.getUUID())) == 0 && count(reloadedData.getInventory(b.getUUID())) == 8, "Lootr NBT round trip");
            var reloadedCache = (SpiritCacheBlockEntity)BlockEntity.loadStatic(pos, cache.getBlockState(), savedCache, level.registryAccess());
            reloadedCache.setLevel(level);
            level.setBlockEntity(reloadedCache);
            var reloadedProvider = ((SpiritCacheLootrAccess)reloadedCache).eicore$getLootrProvider();
            h.assertTrue(provider.getInfoUUID().equals(reloadedProvider.getInfoUUID()), "identity survives reload");
            h.assertTrue(count(reloadedProvider.getPersonalInventory(a)) == 0, "cache reload does not reroll");
            BlockPos otherPos = pos.east(2);
            level.setBlockAndUpdate(otherPos, cache.getBlockState());
            var other = (SpiritCacheBlockEntity)level.getBlockEntity(otherPos);
            other.setLootTable(table, 123L);
            a.setPos(otherPos.getX(), otherPos.getY(), otherPos.getZ());
            var otherProvider = ((SpiritCacheLootrAccess)other).eicore$getLootrProvider();
            h.assertTrue(!otherProvider.getInfoUUID().equals(provider.getInfoUUID()) && count(otherProvider.getPersonalInventory(a)) == 8, "distinct cache");
            var moved = (SpiritCacheBlockEntity)BlockEntity.loadStatic(otherPos.above(), cache.getBlockState(), savedCache, level.registryAccess());
            moved.setLevel(level);
            h.assertTrue(!((SpiritCacheLootrAccess)moved).eicore$getLootrProvider().getInfoUUID().equals(provider.getInfoUUID()), "copied NBT cannot share identity across positions");
            ((SpiritCacheLootrAccess)other).eicore$markPlayerPlaced();
            h.assertTrue(((SpiritCacheLootrAccess)other).eicore$getLootrProvider() == null, "player placed excluded");
            var placedReload = (SpiritCacheBlockEntity)BlockEntity.loadStatic(otherPos, other.getBlockState(), other.saveWithFullMetadata(level.registryAccess()), level.registryAccess());
            placedReload.setLevel(level);
            h.assertTrue(((SpiritCacheLootrAccess)placedReload).eicore$getLootrProvider() == null, "placed flag persists");
            cache.setLootTable(null);
            h.assertTrue(((SpiritCacheLootrAccess)cache).eicore$getLootrProvider() == null, "already globally looted cache excluded");
            h.succeed();
        } finally {
            players.remove(a.getUUID());
            players.remove(b.getUUID());
        }
    }

    /** Seed and verify run in separate JVMs against the same isolated server world. */
    @GameTest(template = "empty", templateNamespace = "neoforge", timeoutTicks = 100)
    public static void processRestart(GameTestHelper h) throws Exception {
        var level = h.getLevel();
        var marker = java.nio.file.Path.of("restart-fixture.nbt");
        boolean verify = System.getProperty("eicore.restartStage", "seed").equals("verify");
        CompoundTag manifest = verify ? net.minecraft.nbt.NbtIo.readCompressed(marker, net.minecraft.nbt.NbtAccounter.unlimitedHeap()) : new CompoundTag();
        UUID alice = verify ? manifest.getUUID("Alice") : UUID.randomUUID();
        UUID bob = verify ? manifest.getUUID("Bob") : UUID.randomUUID();
        BlockPos pos = new BlockPos(32, 100, 32);
        BlockPos otherPos = pos.east(2);
        level.getChunk(pos);
        if (!verify) {
            var table = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse("eicore_spirit_test:fixture"));
            for (BlockPos target : java.util.List.of(pos, otherPos)) {
                level.removeBlock(target, false);
                level.setBlockAndUpdate(target, NVBlocks.SPIRIT_CACHE.block().get().defaultBlockState());
                ((SpiritCacheBlockEntity)level.getBlockEntity(target)).setLootTable(table, 123L);
            }
        }
        var cache = (SpiritCacheBlockEntity)level.getBlockEntity(pos);
        var other = (SpiritCacheBlockEntity)level.getBlockEntity(otherPos);
        h.assertTrue(cache != null && other != null, "physical caches survive server process restart");
        var provider = ((SpiritCacheLootrAccess)cache).eicore$getLootrProvider();
        var otherProvider = ((SpiritCacheLootrAccess)other).eicore$getLootrProvider();
        var playerField = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");
        playerField.setAccessible(true);
        @SuppressWarnings("unchecked")
        var players = (java.util.Map<UUID, ServerPlayer>)playerField.get(level.getServer().getPlayerList());
        ServerPlayer a = connectedPlayer(level, alice, "RestartAlice", pos);
        ServerPlayer b = connectedPlayer(level, bob, "RestartBob", pos);
        players.put(alice, a);
        players.put(bob, b);
        try {
            if (verify) {
                h.assertTrue(provider.getInfoUUID().equals(manifest.getUUID("Cache"))
                        && otherProvider.getInfoUUID().equals(manifest.getUUID("Other")), "cache identities loaded from real chunks");
                var data = LootrAPI.getData(provider);
                h.assertTrue(data.getInventory(alice) != null && count(data.getInventory(alice)) == 0
                        && data.getInventory(bob) != null && count(data.getInventory(bob)) == 7, "Lootr reads empty and partial inventories from disk without regeneration");
                h.assertTrue(count(provider.getPersonalInventory(a)) == 0 && count(provider.getPersonalInventory(b)) == 7, "reopening after restart preserves state");
                h.assertTrue(count(LootrAPI.getData(otherProvider).getInventory(alice)) == 6, "second cache retains independent remaining loot");
                provider.open(a);
                h.assertTrue(a.containerMenu instanceof SpiritCacheMenu && a.containerMenu.stillValid(a), "original menu remains usable after restart");
                a.closeContainer();
            } else {
                provider.open(a);
                a.closeContainer();
                provider.getPersonalInventory(a).clearContent();
                remove(provider.getPersonalInventory(b), 1);
                a.setPos(otherPos.getX() + .5, otherPos.getY() + .5, otherPos.getZ() + .5);
                remove(otherProvider.getPersonalInventory(a), 2);
                // A new ServerPlayer object with the same UUID models a later login.
                ServerPlayer relog = connectedPlayer(level, alice, "RestartAlice", pos);
                players.put(alice, relog);
                h.assertTrue(count(provider.getPersonalInventory(relog)) == 0, "same UUID after login does not reroll");
                manifest.putUUID("Alice", alice);
                manifest.putUUID("Bob", bob);
                manifest.putUUID("Cache", provider.getInfoUUID());
                manifest.putUUID("Other", otherProvider.getInfoUUID());
                net.minecraft.nbt.NbtIo.writeCompressed(manifest, marker);
            }
            System.out.println("EICORE_RESTART_" + (verify ? "VERIFY" : "SEED") + "_PASS cache=" + provider.getInfoUUID());
            h.succeed();
        } finally {
            players.remove(alice);
            players.remove(bob);
        }
    }

    private static ServerPlayer connectedPlayer(net.minecraft.server.level.ServerLevel level, UUID uuid, String name, BlockPos pos) {
        ServerPlayer player = new ServerPlayer(level.getServer(), level, new GameProfile(uuid, name), ClientInformation.createDefault());
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(),
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), player,
                net.minecraft.server.network.CommonListenerCookie.createInitial(player.getGameProfile(), false));
        player.setPos(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        return player;
    }

    private static void remove(ILootrInventory inventory, int amount) {
        for (int slot = 0; slot < inventory.getContainerSize() && amount > 0; slot++) {
            amount -= inventory.removeItem(slot, amount).getCount();
        }
    }

    private static int count(ILootrInventory inventory) {
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) total += inventory.getItem(i).getCount();
        return total;
    }
}
