package org.exmple.newbedwarshelper.client.mixin.waterclutchhelper;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftUseItemInvoker {
    @Invoker("pick")
    void newbedwarshelper$pick(float partialTick);

    @Invoker("startUseItem")
    void newbedwarshelper$startUseItem();
}
