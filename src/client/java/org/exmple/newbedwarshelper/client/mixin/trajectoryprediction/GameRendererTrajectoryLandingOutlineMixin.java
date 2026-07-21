package org.exmple.newbedwarshelper.client.mixin.trajectoryprediction;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.exmple.newbedwarshelper.client.trajectoryprediction.TrajectoryLandingOutlineRenderer;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererTrajectoryLandingOutlineMixin {
    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=hand"))
    private void newbedwarshelper$renderTrajectoryLandingOutline(
            DeltaTracker deltaTracker,
            CallbackInfo ci,
            @Local(name = "modelViewMatrix") Matrix4fc modelViewMatrix
    ) {
        TrajectoryLandingOutlineRenderer.render(Minecraft.getInstance(), modelViewMatrix);
    }
}
