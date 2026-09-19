package org.exmple.newbedwarshelper.client.waterclutchhelper;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.exmple.newbedwarshelper.client.mixin.waterclutchhelper.KeyMappingAccessor;
import org.lwjgl.glfw.GLFW;

final class WaterClutchSneakController {
    private boolean ownsSneak;

    boolean begin(Minecraft client) {
        if (client.options.keyShift.isDown()) {
            this.ownsSneak = false;
            return false;
        }
        this.ownsSneak = true;
        client.options.keyShift.setDown(true);
        return true;
    }

    boolean isReady(Minecraft client) {
        return client.player != null && client.player.isShiftKeyDown();
    }

    void release(Minecraft client) {
        if (!this.ownsSneak) {
            return;
        }
        client.options.keyShift.setDown(isPhysicallyDown(client, client.options.keyShift));
        this.ownsSneak = false;
    }

    boolean ownsSneak() {
        return this.ownsSneak;
    }

    private static boolean isPhysicallyDown(Minecraft client, KeyMapping mapping) {
        InputConstants.Key key = ((KeyMappingAccessor)(Object)mapping).newbedwarshelper$getKey();
        if (key.getType() == InputConstants.Type.KEYSYM) {
            return InputConstants.isKeyDown(client.getWindow(), key.getValue());
        }
        if (key.getType() == InputConstants.Type.MOUSE) {
            return GLFW.glfwGetMouseButton(client.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
        }
        return false;
    }
}
