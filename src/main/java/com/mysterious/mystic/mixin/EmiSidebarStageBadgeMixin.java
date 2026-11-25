package com.mysterious.mystic.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mysterious.mystic.compat.astages.EmiStageIndicators;
import com.mysterious.mystic.compat.astages.StageBadgeRenderer;
import dev.emi.emi.runtime.EmiDrawContext;
import dev.emi.emi.screen.EmiScreenManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "dev.emi.emi.screen.EmiScreenManager$ScreenSpace", remap = false)
public abstract class EmiSidebarStageBadgeMixin {
    @WrapMethod(method = "render(Ldev/emi/emi/runtime/EmiDrawContext;IIFI)V")
    private void eicore$drawStageBadges(EmiDrawContext context, int mouseX, int mouseY,
                                        float delta, int start, Operation<Void> original) {
        // The native decorator also runs inside EMI's direct rendering branch.
        // Suppression is scoped in a finally block and the sidebar draws one badge after its batch is flushed.
        StageBadgeRenderer.withoutItemBadges(() -> original.call(context, mouseX, mouseY, delta, start));
        var space = (EmiScreenManager.ScreenSpace) (Object) this;
        var entries = space.getStacks();
        int visibleCount = Math.min(space.pageSize, entries.size() - start);
        for (int index = 0; index < visibleCount; index++) {
            EmiStageIndicators.drawItemBadge(context.raw(), entries.get(start + index),
                    space.getRawX(index) + 1, space.getRawY(index) + 1);
        }
    }
}
