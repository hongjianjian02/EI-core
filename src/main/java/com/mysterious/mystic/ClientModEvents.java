package com.mysterious.mystic;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = MysteriousMod.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientModEvents {
    private ClientModEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.VOID_HERRSCHER.get(), VoidHerrscherRenderer::new);
    }

    private static final class VoidHerrscherRenderer
            extends HumanoidMobRenderer<VoidHerrscher, HumanoidModel<VoidHerrscher>> {
        private VoidHerrscherRenderer(EntityRendererProvider.Context context) {
            super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.8F);
            this.addLayer(new ItemInHandLayer<>(this, context.getItemInHandRenderer()));
        }

        @Override
        public net.minecraft.resources.ResourceLocation getTextureLocation(VoidHerrscher entity) {
            return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                    MysteriousMod.MODID, "textures/entity/void_herrscher.png");
        }
    }
}
