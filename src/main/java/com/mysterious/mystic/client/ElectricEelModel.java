package com.mysterious.mystic.client;

import com.mysterious.mystic.MysteriousMod;
import com.mysterious.mystic.entity.ElectricEelEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ElectricEelModel extends GeoModel<ElectricEelEntity> {
    private static final ResourceLocation MODEL =
            ResourceLocation.fromNamespaceAndPath(MysteriousMod.MODID, "geo/electric_eel.geo.json");
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MysteriousMod.MODID, "textures/entity/electric_eel.png");
    private static final ResourceLocation ANIMATION =
            ResourceLocation.fromNamespaceAndPath(MysteriousMod.MODID, "animations/electric_eel.animation.json");

    @Override
    public ResourceLocation getModelResource(ElectricEelEntity entity) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ElectricEelEntity entity) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ElectricEelEntity entity) {
        return ANIMATION;
    }
}
