package org.exmple.newbedwarshelper.client.mixin.itemprotection;

import net.minecraft.client.player.LocalPlayer;
import org.exmple.newbedwarshelper.client.itemprotection.ItemProtectionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerItemProtectionMixin {
    @Inject(method = "drop(Z)Z", at = @At("HEAD"), cancellable = true)
    private void newbedwarshelper$preventProtectedItemDrop(
            boolean entireStack,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!ItemProtectionManager.isProtected(player.getMainHandItem())) {
            return;
        }

        callbackInfo.setReturnValue(false);
    }
}
