package com.mysterious.mystic.client;

import com.mysterious.mystic.ModEntities;
import com.mysterious.mystic.MysteriousMod;
import com.mysterious.mystic.flower.ModFlowers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import vazkii.botania.api.block.WandHUD;
import vazkii.botania.api.neoforge.BotaniaNeoForgeCapabilities;
import vazkii.botania.api.block_entity.BindableSpecialFlowerBlockEntity.BindableFlowerWandHud;

@EventBusSubscriber(modid = MysteriousMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerFlowerHud(RegisterCapabilitiesEvent event) {
        BlockCapability<WandHUD, Void> hud = BotaniaNeoForgeCapabilities.getBlockApiLookupById(WandHUD.BLOCK_LOOKUP);
        ModFlowers.TYPES_BY_KIND.values().forEach(type -> event.registerBlockEntity(
                hud, type.get(), (flower, context) -> new BindableFlowerWandHud<>(flower)));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.ELECTRIC_EEL.get(), ElectricEelRenderer::new);
        ModFlowers.TYPES_BY_KIND.values().forEach(type -> event.registerBlockEntityRenderer(
                type.get(), vazkii.botania.client.render.block_entity.SpecialFlowerBlockEntityRenderer::new));
    }
}
