package com.mysterious.mystic.foodbox;

import com.mysterious.mystic.MysteriousMod;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid = MysteriousMod.MODID)
public final class FlavorBoxInteractionEvents {
    private FlavorBoxInteractionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void respectDeniedInteraction(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getEntity().isShiftKeyDown() || !(event.getItemStack().getItem() instanceof FlavorBoxItem)) {
            return;
        }
        if (event.getUseBlock() == TriState.FALSE || event.getUseItem() == TriState.FALSE) {
            event.setCancellationResult(InteractionResult.FAIL);
            event.setCanceled(true);
        }
    }
}
