package org.exmple.newbedwarshelper.client.mixin.fireballhelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import org.exmple.newbedwarshelper.client.fireballhelper.FireballImpactBeamRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererFireballImpactBeamMixin {
    @Inject(method = "submitFeatures", at = @At("RETURN"))
    private void newbedwarshelper$submitFireballImpactBeams(
            LevelRenderState levelState,
            SubmitNodeCollector output,
            boolean renderBlockOutline,
            CallbackInfo ci
    ) {
        FireballImpactBeamRenderer.submit(Minecraft.getInstance(), levelState, output);
    }
}
