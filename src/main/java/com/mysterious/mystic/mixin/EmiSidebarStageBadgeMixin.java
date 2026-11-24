package com.mysterious.mystic.mixin;

import com.mysterious.mystic.compat.astages.EmiStageIndicators;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.emi.emi.screen.EmiScreenManager$ScreenSpace", remap = false)
public abstract class EmiSidebarStageBadgeMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void eicore$drawStageBadges(EmiDrawContext context, int mouseX, int mouseY,
                                        float delta, int start, CallbackInfo callback) {
        var space = (EmiScreenManager.ScreenSpace) (Object) this;
        var entries = space.getStacks();
        int visibleCount = Math.min(space.pageSize, entries.size() - start);
        for (int index = 0; index < visibleCount; index++) {
            if (!EmiStageIndicators.itemHints(entries.get(start + index)).isEmpty()) {
                EmiStageIndicators.drawBadge(context.raw(), space.getRawX(index) + 17, space.getRawY(index) + 1);
            }
        }
    }
}
