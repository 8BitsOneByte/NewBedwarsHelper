package org.exmple.newbedwarshelper.client.mixin.fireballhelper;

import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.extract.LevelExtractor;
import org.exmple.newbedwarshelper.client.fireballhelper.FireballHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelExtractor.class)
public class LevelRendererFireballHelperMixin {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "extract", at = @At("TAIL"))
    private void newbedwarshelper$emitFireballDangerPrediction(DeltaTracker deltaTracker, Camera camera, float deltaPartialTick, CallbackInfo ci) {
        FireballHelper.emitDangerPrediction(this.minecraft, deltaPartialTick);
    }
}
