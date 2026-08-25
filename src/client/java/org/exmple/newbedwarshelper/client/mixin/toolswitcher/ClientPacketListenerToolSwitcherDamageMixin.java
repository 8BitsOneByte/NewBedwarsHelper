package org.exmple.newbedwarshelper.client.mixin.toolswitcher;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import org.exmple.newbedwarshelper.client.toolswitcher.ToolSwitcherManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerToolSwitcherDamageMixin {
    @Inject(method = "handleDamageEvent", at = @At("TAIL"))
    private void newbedwarshelper$handleToolSwitcherDamage(
            ClientboundDamageEventPacket packet,
            CallbackInfo callbackInfo
    ) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || packet.entityId() != client.player.getId()) {
            return;
        }

        ToolSwitcherManager.onPlayerDamage();
    }
}
