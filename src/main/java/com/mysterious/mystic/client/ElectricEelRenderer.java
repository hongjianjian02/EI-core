package com.mysterious.mystic.client;

import com.mysterious.mystic.entity.ElectricEelEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ElectricEelRenderer extends GeoEntityRenderer<ElectricEelEntity> {
    public ElectricEelRenderer(EntityRendererProvider.Context context) {
        super(context, new ElectricEelModel());
        this.withScale(0.5F);
        this.shadowRadius = 0.3F;
    }
}
