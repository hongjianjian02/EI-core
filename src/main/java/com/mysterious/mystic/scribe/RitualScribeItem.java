package com.mysterious.mystic.scribe;

import com.breakinblocks.neovitae.common.block.BlockRitualStone;
import com.breakinblocks.neovitae.common.item.ItemInscriptionTool;
import com.mysterious.mystic.BlockInteractionAccess;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;

public final class RitualScribeItem extends Item {
    public RitualScribeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown() || !looksAtAir(level, player)) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer serverPlayer && !(player.containerMenu instanceof RitualScribeMenu)) {
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, owner) -> new RitualScribeMenu(containerId, owner, hand, stack),
                    Component.translatable("screen.eicore.ritual_scribe.title")));
        }
        return InteractionResultHolder.consume(stack);
    }

    private static boolean looksAtAir(Level level, Player player) {
        if (player.pick(player.blockInteractionRange(), 1.0F, false).getType() != HitResult.Type.MISS) return false;
        Vec3 start = player.getEyePosition();
        Vec3 reach = player.getLookAngle().scale(player.entityInteractionRange());
        return ProjectileUtil.getEntityHitResult(level, player, start, start.add(reach),
                player.getBoundingBox().expandTowards(reach).inflate(1.0), Entity::isPickable) == null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!ModList.get().isLoaded("neovitae")) return InteractionResult.PASS;
        return NeoVitaeInteraction.inscribe(context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        ScribeState state = ScribeState.of(stack);
        ScribeMode selected = state.selected();
        Component none = Component.translatable("tooltip.eicore.ritual_scribe.none");
        List<Component> unlocked = ScribeMode.MODES.stream().filter(state::has).map(ScribeMode::label).toList();
        lines.add(Component.translatable("tooltip.eicore.ritual_scribe.selected", selected == null ? none : selected.label())
                .withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip.eicore.ritual_scribe.unlocked",
                unlocked.isEmpty() ? none : ComponentUtils.formatList(unlocked, Component.literal(" / "), label -> label))
                .withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.eicore.ritual_scribe.choose").withStyle(ChatFormatting.DARK_GRAY));
    }

    private static final class NeoVitaeInteraction {
        private static InteractionResult inscribe(UseOnContext context) {
            if (!(context.getLevel().getBlockState(context.getClickedPos()).getBlock() instanceof BlockRitualStone)) {
                return InteractionResult.PASS;
            }
            Player player = context.getPlayer();
            if (player instanceof ServerPlayer serverPlayer
                    && !BlockInteractionAccess.permitted(serverPlayer, context.getHand(), context.getClickedPos())) {
                return InteractionResult.FAIL;
            }
            ScribeMode mode = ScribeState.of(context.getItemInHand()).selected();
            if (mode == null) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.displayClientMessage(Component.translatable("message.eicore.ritual_scribe.locked"), true);
                }
                return InteractionResult.CONSUME;
            }
            return mode.tool().getItem() instanceof ItemInscriptionTool tool ? tool.useOn(context) : InteractionResult.PASS;
        }
    }
}
