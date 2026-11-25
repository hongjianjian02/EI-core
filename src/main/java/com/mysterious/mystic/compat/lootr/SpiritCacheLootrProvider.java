package com.mysterious.mystic.compat.lootr;

import com.breakinblocks.neovitae.common.blockentity.SpiritCacheBlockEntity;
import com.breakinblocks.neovitae.common.menu.SpiritCacheMenu;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.loot.LootTable;
import noobanidus.mods.lootr.common.api.BuiltInLootrTypes;
import noobanidus.mods.lootr.common.api.ILootrType;
import noobanidus.mods.lootr.common.api.LootrAPI;
import noobanidus.mods.lootr.common.api.MenuBuilder;
import noobanidus.mods.lootr.common.api.data.ILootrInfo;
import noobanidus.mods.lootr.common.api.data.ILootrSavedData;
import noobanidus.mods.lootr.common.api.data.LootrBlockType;
import noobanidus.mods.lootr.common.api.data.blockentity.ILootrBlockEntity;
import noobanidus.mods.lootr.common.api.data.inventory.ILootrInventory;
import noobanidus.mods.lootr.common.api.registry.LootrRegistry;
import org.jetbrains.annotations.Nullable;

/** Keeps the physical cache intact; all reward inventories and opener state belong to Lootr. */
public final class SpiritCacheLootrProvider implements ILootrBlockEntity {
    public static final MenuBuilder MENU = (id, inventory, container, rows) -> new SpiritCacheMenu(id, inventory, container);
    private final SpiritCacheBlockEntity cache;
    private final UUID id;
    private final BlockPos boundPosition;
    private final ResourceKey<Level> boundDimension;
    private final Set<UUID> clientOpeners = new HashSet<>();
    private final Set<UUID> clientDepleted = new HashSet<>();
    private boolean opened;
    private boolean clientOpened;

    public SpiritCacheLootrProvider(SpiritCacheBlockEntity cache) {
        this(cache, UUID.randomUUID(), cache.getBlockPos(), cache.getLevel().dimension());
    }

    private SpiritCacheLootrProvider(SpiritCacheBlockEntity cache, UUID id, BlockPos position, ResourceKey<Level> dimension) {
        this.cache = cache;
        this.id = id;
        this.boundPosition = position.immutable();
        this.boundDimension = dimension;
    }

    public static SpiritCacheLootrProvider load(SpiritCacheBlockEntity cache, CompoundTag tag) {
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(tag.getString("Dimension")));
        SpiritCacheLootrProvider provider = new SpiritCacheLootrProvider(cache, tag.getUUID("Id"), BlockPos.of(tag.getLong("Position")), dimension);
        provider.opened = tag.getBoolean("Opened");
        ListTag openers = tag.getList("Openers", 11);
        for (int i = 0; i < openers.size(); i++) {
            provider.clientOpeners.add(NbtUtils.loadUUID(openers.get(i)));
        }
        ListTag depleted = tag.getList("Depleted", 11);
        for (int i = 0; i < depleted.size(); i++) {
            provider.clientDepleted.add(NbtUtils.loadUUID(depleted.get(i)));
        }
        return provider;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Id", id);
        tag.putLong("Position", boundPosition.asLong());
        tag.putString("Dimension", boundDimension.location().toString());
        tag.putBoolean("Opened", opened);
        Set<UUID> openers = cache.getLevel() != null && !cache.getLevel().isClientSide() ? getVisualOpeners() : clientOpeners;
        ListTag list = new ListTag();
        ListTag depleted = new ListTag();
        ILootrSavedData data = cache.getLevel() != null && !cache.getLevel().isClientSide() ? LootrAPI.getData(this) : null;
        if (openers != null) {
            for (UUID opener : openers) {
                list.add(NbtUtils.createUUID(opener));
                ILootrInventory inventory = data == null ? null : data.getInventory(opener);
                if (inventory != null ? inventory.isEmpty() : clientDepleted.contains(opener)) {
                    depleted.add(NbtUtils.createUUID(opener));
                }
            }
        }
        tag.put("Openers", list);
        tag.put("Depleted", depleted);
        return tag;
    }

    public boolean belongsToCache() {
        return boundPosition.equals(cache.getBlockPos()) && boundDimension.equals(cache.getLevel().dimension());
    }

    public void open(ServerPlayer player) {
        ILootrInventory inventory = getPersonalInventory(player);
        if (inventory == null) {
            return;
        }
        ILootrSavedData data = LootrAPI.getData(this);
        // Use the UUID API deliberately: global Lootr team mode must not merge these rewards.
        if (data.getActualOpeners().add(player.getUUID())) {
            data.getVisualOpeners().add(player.getUUID());
            player.awardStat(LootrRegistry.getLootedStat());
            LootrRegistry.getStatTrigger().trigger(player);
        }
        opened = true;
        data.markChanged();
        performUpdate(player);
        if (player.openMenu(inventory).isPresent()) {
            cache.getLevel().playSound(null, cache.getBlockPos(), SoundEvents.VAULT_INSERT_ITEM, SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    @Nullable
    public ILootrInventory getPersonalInventory(ServerPlayer player) {
        if (player.isSpectator() || LootrAPI.isFakePlayer(player) || !canPlayerOpen(player)) {
            return null;
        }
        ILootrSavedData data = LootrAPI.getData(this);
        if (data == null) {
            return null;
        }
        ILootrInventory inventory = data.getInventory(player.getUUID());
        if (inventory == null) {
            inventory = data.createInventory(this, player.getUUID(), getDefaultFiller());
        }
        inventory.setMenuBuilder(MENU);
        return inventory;
    }

    public boolean hasClientContents(Player player) {
        return !clientDepleted.contains(player.getUUID());
    }

    @Override public BlockEntity asBlockEntity() { return cache; }
    @Override public UUID getInfoUUID() { return id; }
    @Override public String getInfoKey() { return ILootrInfo.generateInfoKey(id); }
    @Override public BlockPos getInfoPos() { return cache.getBlockPos(); }
    @Override public ResourceKey<Level> getInfoDimension() { return boundDimension; }
    @Override public Level getInfoLevel() { return cache.getLevel(); }
    @Override public int getInfoContainerSize() { return cache.getContainerSize(); }
    @Override public Component getInfoDisplayName() { return cache.getDisplayName(); }
    @Override public ResourceKey<LootTable> getInfoLootTable() { return cache.getLootTable(); }
    @Override public long getInfoLootSeed() { return cache.getLootTableSeed(); }
    @Override public NonNullList<ItemStack> getInfoReferenceInventory() { return null; }
    @Override public boolean isInfoReferenceInventory() { return false; }
    @Override public ILootrType getInfoNewType() { return BuiltInLootrTypes.CHEST; }
    @Override public LootrBlockType getInfoBlockType() { return LootrBlockType.CHEST; }
    @Override public boolean hasBeenOpened() { return opened; }
    @Override public boolean isPhysicallyOpen() { return false; }
    @Override public boolean canRefresh() { return false; }
    @Override public boolean canDecay() { return false; }
    @Override public boolean canBeMarkedUnopened() { return false; }
    @Override public boolean canDropContentsWhenBroken() { return false; }
    @Override public boolean canPlayerOpen(ServerPlayer player) { return cache.stillValid(player); }
    @Override public void setHasBeenOpened(boolean value) { opened = value; }
    @Override public void performOpen(ServerPlayer player) { opened = true; }
    @Override public void performOpen() { opened = true; }
    @Override public void performClose(ServerPlayer player) { }
    @Override public void performClose() { }
    @Override public void performRefresh() { }
    @Override public void performDecay() { }
    @Override public void markChanged() { cache.setChangedNoSync(); markDataChanged(); }
    @Override public Set<UUID> getClientOpeners() { return clientOpeners; }
    @Override public boolean isClientOpened() { return clientOpened; }
    @Override public void setClientOpened(boolean value) { clientOpened = value; }
}
