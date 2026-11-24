package com.mysterious.mystic;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

final class AncientSeedTrowelItem extends Item {
    private static final int DIG_TICKS = 40;
    // Calibrated to the pack's current loot tables and mininProbability = 0.5.
    private static final double SINGLE_BIOME_EMPTY_DIG_CHANCE = 0.4424691894634759;
    private static final double TARGET_SINGLE_BIOME_EMPTY_DIG_CHANCE = 0.2225;
    private static final double EMPTY_DIG_RETRY_CHANCE =
            (SINGLE_BIOME_EMPTY_DIG_CHANCE - TARGET_SINGLE_BIOME_EMPTY_DIG_CHANCE)
                    / (SINGLE_BIOME_EMPTY_DIG_CHANCE * (1 - SINGLE_BIOME_EMPTY_DIG_CHANCE));
    private static final String MESSAGE = "item.eicore.ancient_seed_trowel.";
    private static final TagKey<Item> FINDS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(MysteriousMod.MODID, "ancient_seed_trowel_finds"));
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, MysteriousMod.MODID);
    // Keep the serialized attachment registered so old player saves can load and discard their cooldown.
    private static final Supplier<AttachmentType<Long>> LEGACY_READY_AT = ATTACHMENTS.register("trowel_ready_at",
            () -> AttachmentType.builder(() -> 0L).serialize(Codec.LONG).copyOnDeath().build());
    private static final Supplier<AttachmentType<Optional<Dig>>> ACTIVE_DIG = ATTACHMENTS.register("trowel_active_dig",
            () -> AttachmentType.builder(() -> Optional.<Dig>empty()).build());

    AncientSeedTrowelItem(Properties properties) {
        super(properties);
    }

    static void register(IEventBus bus) {
        ATTACHMENTS.register(bus);
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> clearLegacyCooldown(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> clearLegacyCooldown(event.getEntity()));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> clearLegacyCooldown(event.getEntity()));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player) || player.isSpectator() || player.isUsingItem()) {
            return InteractionResult.FAIL;
        }
        if (!ModList.get().isLoaded("immortalers_delight")) {
            return reject(player, "unavailable");
        }

        ServerLevel level = player.serverLevel();
        BlockPos pos = context.getClickedPos().immutable();
        if (!reachable(player, pos)) {
            return InteractionResult.FAIL;
        }
        BlockState state = level.getBlockState(pos);
        if (!diggable(level, pos, state)) {
            return reject(player, "invalid_ground");
        }
        InteractionHand hand = context.getHand();
        ItemStack stack = context.getItemInHand();
        Direction face = context.getClickedFace();
        if (!permitted(player, hand, pos, face, stack)) {
            return reject(player, "protected");
        }
        player.startUsingItem(hand);
        if (!player.isUsingItem() || player.getUseItem() != stack) {
            return InteractionResult.FAIL;
        }
        player.setData(ACTIVE_DIG, Optional.of(new Dig(level, pos, state, face, hand, stack,
                player.getInventory().selected, level.getGameTime())));
        playDigEffects(level, pos, state);
        return InteractionResult.CONSUME;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }
        Dig dig = activeDig(player);
        if (dig == null || !dig.valid(player, stack)) {
            player.stopUsingItem();
            return;
        }
        if (remaining < DIG_TICKS && remaining % 8 == 0) {
            playDigEffects(dig.level, dig.pos, dig.state);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) {
            return stack;
        }
        Dig dig = activeDig(player);
        player.removeData(ACTIVE_DIG);
        if (dig == null || !dig.valid(player, stack) || level.getGameTime() - dig.startedAt < DIG_TICKS - 1) {
            return stack;
        }
        if (!permitted(player, dig.hand, dig.pos, dig.face, stack)) {
            reject(player, "protected");
            return stack;
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        EquipmentSlot slot = dig.hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        stack.hurtAndBreak(1, player, slot);
        var params = new LootParams.Builder(dig.level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(dig.pos))
                .withParameter(LootContextParams.THIS_ENTITY, player)
                .create(LootContextParamSets.GIFT);
        var table = dig.level.getServer().reloadableRegistries().getLootTable(BuiltInLootTables.SNIFFER_DIGGING);
        List<ItemStack> finds = rollFinds(table, params);
        if (finds.isEmpty() && player.getRandom().nextDouble() < EMPTY_DIG_RETRY_CHANCE) {
            finds = rollFinds(table, params);
        }
        int found = 0;
        for (ItemStack drop : finds) {
            var item = new ItemEntity(dig.level, dig.pos.getX() + 0.5, dig.pos.getY() + 1.05,
                    dig.pos.getZ() + 0.5, drop);
            item.setDefaultPickUpDelay();
            if (dig.level.addFreshEntity(item)) {
                found += drop.getCount();
            }
        }
        if (found == 0) {
            player.displayClientMessage(Component.translatable(MESSAGE + "empty"), true);
        } else {
            player.displayClientMessage(Component.translatable(MESSAGE + "found", found), true);
        }
        return stack;
    }

    private static List<ItemStack> rollFinds(LootTable table, LootParams params) {
        return table.getRandomItems(params).stream()
                .filter(drop -> !drop.isEmpty() && drop.is(FINDS))
                .toList();
    }

    @Override
    public void onStopUsing(ItemStack stack, LivingEntity entity, int count) {
        if (entity instanceof ServerPlayer player) {
            player.removeData(ACTIVE_DIG);
        }
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return DIG_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BRUSH;
    }

    @Override
    public int getEnchantmentValue() {
        return Tiers.IRON.getEnchantmentValue();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(MESSAGE + "tooltip"));
        tooltip.add(Component.translatable(MESSAGE + "tooltip_durability"));
    }

    private static void clearLegacyCooldown(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.removeData(LEGACY_READY_AT);
            serverPlayer.getCooldowns().removeCooldown(ModItems.ANCIENT_SEED_TROWEL.get());
        }
    }

    private static Dig activeDig(ServerPlayer player) {
        return player.getExistingData(ACTIVE_DIG).flatMap(value -> value).orElse(null);
    }

    private static boolean reachable(ServerPlayer player, BlockPos pos) {
        ServerLevel level = player.serverLevel();
        return level.isInWorldBounds(pos) && level.getWorldBorder().isWithinBounds(pos) && level.hasChunkAt(pos)
                && player.canInteractWithBlock(pos, 0)
                && player.pick(player.blockInteractionRange(), 1, false) instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(pos);
    }

    private static boolean diggable(ServerLevel level, BlockPos pos, BlockState state) {
        BlockPos above = pos.above();
        return state.is(BlockTags.SNIFFER_DIGGABLE_BLOCK)
                && level.getBlockState(above).getCollisionShape(level, above).isEmpty();
    }

    private static boolean permitted(ServerPlayer player, InteractionHand hand, BlockPos pos, Direction face, ItemStack stack) {
        return player.mayUseItemAt(pos, face, stack) && BlockInteractionAccess.permitted(player, hand, pos);
    }

    private static InteractionResult reject(ServerPlayer player, String message, Object... arguments) {
        player.displayClientMessage(Component.translatable(MESSAGE + message, arguments), true);
        return InteractionResult.FAIL;
    }

    private static void playDigEffects(ServerLevel level, BlockPos pos, BlockState state) {
        level.playSound(null, pos, state.getSoundType(level, pos, null).getHitSound(), SoundSource.PLAYERS, 0.7F, 0.9F);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5, 9, 0.23, 0.08, 0.23, 0.04);
    }

    private record Dig(ServerLevel level, BlockPos pos, BlockState state, Direction face, InteractionHand hand,
                       ItemStack stack, int selectedSlot, long startedAt) {
        boolean valid(ServerPlayer player, ItemStack current) {
            return player.serverLevel() == level && player.isAlive() && !player.isSpectator()
                    && player.isUsingItem() && player.getUsedItemHand() == hand
                    && (hand != InteractionHand.MAIN_HAND || player.getInventory().selected == selectedSlot)
                    && current == stack && player.getItemInHand(hand) == stack && !stack.isEmpty()
                    && reachable(player, pos) && level.getBlockState(pos) == state && diggable(level, pos, state);
        }
    }
}
