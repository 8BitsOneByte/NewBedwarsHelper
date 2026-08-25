package org.exmple.newbedwarshelper.client.mixin.fireballhelper;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import org.exmple.newbedwarshelper.client.fireballhelper.FireballDangerLateRenderer;
import org.joml.Matrix4fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererFireballDangerMixin {
    @Inject(method = "renderLevel", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = "ldc=hand"))
    private void newbedwarshelper$renderTranslucentFireballDanger(
            DeltaTracker deltaTracker,
            CallbackInfo ci,
            @Local(name = "modelViewMatrix") Matrix4fc modelViewMatrix
    ) {
        FireballDangerLateRenderer.render(Minecraft.getInstance(), modelViewMatrix);
    }
}
