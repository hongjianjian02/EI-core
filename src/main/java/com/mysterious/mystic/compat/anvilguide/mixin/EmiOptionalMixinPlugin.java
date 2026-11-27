package com.mysterious.mystic.compat.anvilguide.mixin;

import java.util.List;
import java.util.Set;

import com.mysterious.mystic.compat.anvilguide.emi.EmiFocusedQueryManager;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** Enables the narrow query hook only for the EMI build whose bytecode was checked. */
public final class EmiOptionalMixinPlugin implements IMixinConfigPlugin {
    private static final String MANAGER_CLASS = "dev.emi.emi.registry.EmiRecipes$Manager";
    private static final String QUERY_MIXIN =
            "com.mysterious.mystic.compat.anvilguide.mixin.EmiRecipeManagerMixin";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (FMLEnvironment.dist != Dist.CLIENT
                || !MANAGER_CLASS.equals(targetClassName)
                || !QUERY_MIXIN.equals(mixinClassName)) {
            return false;
        }
        // Mixin selection precedes ModList initialization. Metadata can be read here
        // without resolving any optional EMI classes, including on dedicated servers.
        LoadingModList loadingMods = LoadingModList.get();
        return loadingMods != null && loadingMods.getMods().stream()
                .anyMatch(mod -> "emi".equals(mod.getModId())
                        && EmiFocusedQueryManager.SUPPORTED_EMI_VERSION.equals(mod.getVersion().toString()));
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass,
            String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass,
            String mixinClassName, IMixinInfo mixinInfo) {
    }
}
