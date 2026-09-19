package org.exmple.newbedwarshelper.client.mixin.waterclutchhelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.exmple.newbedwarshelper.client.waterclutchhelper.WaterClutchLandingBeamRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererWaterClutchLandingBeamMixin {
    @Inject(method = "submitFeatures", at = @At("RETURN"))
    private void newbedwarshelper$submitWaterClutchLandingBeam(
            LevelRenderState levelState,
            SubmitNodeCollector output,
            boolean renderBlockOutline,
            CallbackInfo ci
    ) {
        WaterClutchLandingBeamRenderer.submit(Minecraft.getInstance(), levelState, output);
    }
}
