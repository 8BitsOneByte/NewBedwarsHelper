package org.exmple.newbedwarshelper.client.mixin.trajectoryprediction;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.exmple.newbedwarshelper.client.trajectoryprediction.TrajectoryPredictionRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class LevelExtractorTrajectoryPredictionMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(
            method = "extract",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/extract/LevelExtractor;extractGizmos()V"
            )
    )
    private void newbedwarshelper$emitTrajectoryPrediction(
            DeltaTracker deltaTracker,
            Camera camera,
            float deltaPartialTick,
            CallbackInfo ci
    ) {
        TrajectoryPredictionRenderer.emit(this.minecraft, deltaPartialTick);
    }
}
