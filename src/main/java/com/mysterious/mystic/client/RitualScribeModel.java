package com.mysterious.mystic.client;

import com.mysterious.mystic.ModItems;
import com.mysterious.mystic.scribe.ScribeMode;
import com.mysterious.mystic.scribe.ScribeState;
import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.model.BakedModelWrapper;

final class RitualScribeModel extends BakedModelWrapper<BakedModel> {
    private final ItemOverrides overrides;

    private RitualScribeModel(BakedModel fallback, Map<ScribeMode, BakedModel> variants) {
        super(fallback);
        this.overrides = new ItemOverrides() {
            @Override
            @Nullable
            public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level,
                                      @Nullable LivingEntity entity, int seed) {
                ScribeMode selected = ScribeState.of(stack).selected();
                BakedModel variant = selected == null ? null : variants.get(selected);
                return variant != null ? variant : fallback.getOverrides().resolve(fallback, stack, level, entity, seed);
            }
        };
    }

    static void install(Map<ModelResourceLocation, BakedModel> models) {
        ModelResourceLocation scribeId = ModelResourceLocation.inventory(ModItems.RITUAL_SCRIBE_TOOL.getId());
        BakedModel fallback = models.get(scribeId);
        if (fallback == null) return;

        BakedModel missing = models.get(ModelBakery.MISSING_MODEL_VARIANT);
        Map<ScribeMode, BakedModel> variants = new EnumMap<>(ScribeMode.class);
        for (ScribeMode mode : ScribeMode.MODES) {
            BakedModel model = models.get(ModelResourceLocation.inventory(mode.toolId()));
            if (model != null && model != missing) {
                variants.put(mode, model);
            }
        }
        if (!variants.isEmpty()) {
            models.put(scribeId, new RitualScribeModel(fallback, variants));
        }
    }

    @Override
    public ItemOverrides getOverrides() {
        return overrides;
    }
}
