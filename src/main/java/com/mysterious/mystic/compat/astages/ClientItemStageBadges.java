package com.mysterious.mystic.compat.astages;

import com.mysterious.mystic.MysteriousMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

@EventBusSubscriber(modid = MysteriousMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientItemStageBadges {
    private static final IItemDecorator BADGE = (graphics, font, stack, x, y) -> {
        if (!StageBadgeRenderer.itemBadgesSuppressed() && ClientRecipeStageTooltips.isItemLocked(stack)) {
            StageBadgeRenderer.draw(graphics, x, y);
        }
        return false;
    };

    private ClientItemStageBadges() {
    }

    @SubscribeEvent
    public static void onRegisterItemDecorations(RegisterItemDecorationsEvent event) {
        BuiltInRegistries.ITEM.forEach(item -> event.register(item, BADGE));
    }
}
