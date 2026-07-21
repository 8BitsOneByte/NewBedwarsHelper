package org.exmple.newbedwarshelper.client.gui;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.exmple.newbedwarshelper.ModConstants;
import org.exmple.newbedwarshelper.client.antiafk.AntiAFKManager;
import org.exmple.newbedwarshelper.client.esp.EspStorageManager;
import org.exmple.newbedwarshelper.client.gammaoverride.GammaOverrideManager;
import org.exmple.newbedwarshelper.client.z_config.ModConfig;
import org.lwjgl.glfw.GLFW;

public class KeyMappingManager {
    private static final String ESP_ON_KEY = "overlay.newbedwarshelper.esp.on";
    private static final String ESP_OFF_KEY = "overlay.newbedwarshelper.esp.off";
    private static final String GAMMA_OVERRIDE_ON_KEY = "overlay.newbedwarshelper.gamma_override.on";
    private static final String GAMMA_OVERRIDE_OFF_KEY = "overlay.newbedwarshelper.gamma_override.off";
    private static final String TRAJECTORY_ON_KEY = "overlay.newbedwarshelper.trajectory_path.on";
    private static final String TRAJECTORY_OFF_KEY = "overlay.newbedwarshelper.trajectory_path.off";
    private static final String LANDING_INDICATOR_ON_KEY = "overlay.newbedwarshelper.landing_indicator.on";
    private static final String LANDING_INDICATOR_OFF_KEY = "overlay.newbedwarshelper.landing_indicator.off";

    private static final KeyMapping.Category CUSTOM_KEY_CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "bed_wars_helper")
    );
    private static final KeyMapping OPEN_ESP_WHITELIST_CONFIG_KEY = new KeyMapping(
            "key.newbedwarshelper.open_esp_whitelist_config",
            GLFW.GLFW_KEY_F9,
            CUSTOM_KEY_CATEGORY
    );
    private static final KeyMapping TOGGLE_GLOBAL_ESP_KEY = new KeyMapping(
            "key.newbedwarshelper.toggle_global_esp",
            GLFW.GLFW_KEY_U,
            CUSTOM_KEY_CATEGORY
    );
    private static final KeyMapping TOGGLE_ANTI_AFK_KEY = new KeyMapping(
            "key.newbedwarshelper.toggle_anti_afk",
            GLFW.GLFW_KEY_F8,
            CUSTOM_KEY_CATEGORY
    );
    private static final KeyMapping TOGGLE_GAMMA_OVERRIDE_KEY = new KeyMapping(
            "key.newbedwarshelper.toggle_gamma_override",
            GLFW.GLFW_KEY_UNKNOWN,
            CUSTOM_KEY_CATEGORY
    );
    private static final KeyMapping TOGGLE_TRAJECTORY_PATH_KEY = new KeyMapping(
            "key.newbedwarshelper.toggle_trajectory_path",
            GLFW.GLFW_KEY_UNKNOWN,
            CUSTOM_KEY_CATEGORY
    );
    private static final KeyMapping TOGGLE_LANDING_INDICATOR_KEY = new KeyMapping(
            "key.newbedwarshelper.toggle_landing_indicator",
            GLFW.GLFW_KEY_UNKNOWN,
            CUSTOM_KEY_CATEGORY
    );
    private static boolean initialized;
    private static boolean resetOnNextJoin = true;

    public static void init() {
        if (initialized) {
            return;
        }

        KeyMappingHelper.registerKeyMapping(OPEN_ESP_WHITELIST_CONFIG_KEY);
        KeyMappingHelper.registerKeyMapping(TOGGLE_GLOBAL_ESP_KEY);
        KeyMappingHelper.registerKeyMapping(TOGGLE_ANTI_AFK_KEY);
        KeyMappingHelper.registerKeyMapping(TOGGLE_GAMMA_OVERRIDE_KEY);
        KeyMappingHelper.registerKeyMapping(TOGGLE_TRAJECTORY_PATH_KEY);
        KeyMappingHelper.registerKeyMapping(TOGGLE_LANDING_INDICATOR_KEY);
        ClientTickEvents.END_CLIENT_TICK.register(KeyMappingManager::onClientTick);
        initialized = true;
    }

    private static void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            resetOnNextJoin = true;
        } else if (resetOnNextJoin) {
            EspStorageManager.setGlobalEspEnabled(false);
            resetOnNextJoin = false;
        }

        while (OPEN_ESP_WHITELIST_CONFIG_KEY.consumeClick()) {
            client.gui.setScreen(new ModScreen(client, client.gui.screen()));
        }

        while (TOGGLE_GLOBAL_ESP_KEY.consumeClick()) {
            boolean enabled = !EspStorageManager.isGlobalEspEnabled();
            EspStorageManager.setGlobalEspEnabled(enabled);

            if (client.player != null) {
                Component text = Component.literal("ESP:")
                        .append(Component.translatable(enabled ? ESP_ON_KEY : ESP_OFF_KEY))
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
                client.player.sendOverlayMessage(text);
            }
        }

        while (TOGGLE_ANTI_AFK_KEY.consumeClick()) {
            AntiAFKManager.toggle();
        }

        while (TOGGLE_GAMMA_OVERRIDE_KEY.consumeClick()) {
            boolean enabled = GammaOverrideManager.toggleEnabled();

            if (client.player != null) {
                Component text = Component.translatable(enabled ? GAMMA_OVERRIDE_ON_KEY : GAMMA_OVERRIDE_OFF_KEY)
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
                client.player.sendOverlayMessage(text);
            }
        }

        while (TOGGLE_TRAJECTORY_PATH_KEY.consumeClick()) {
            ModConfig config = ModConfig.getInstance();
            boolean enabled = !Boolean.TRUE.equals(config.trajectoryPrediction.showTrajectory);
            config.trajectoryPrediction.showTrajectory = enabled;
            config.save();
            sendToggleOverlay(client, enabled, TRAJECTORY_ON_KEY, TRAJECTORY_OFF_KEY);
        }

        while (TOGGLE_LANDING_INDICATOR_KEY.consumeClick()) {
            ModConfig config = ModConfig.getInstance();
            boolean enabled = !Boolean.TRUE.equals(config.trajectoryPrediction.showLandingIndicator);
            config.trajectoryPrediction.showLandingIndicator = enabled;
            config.save();
            sendToggleOverlay(client, enabled, LANDING_INDICATOR_ON_KEY, LANDING_INDICATOR_OFF_KEY);
        }

        AntiAFKManager.update(client.player);
    }

    private static void sendToggleOverlay(Minecraft client, boolean enabled, String onKey, String offKey) {
        if (client.player != null) {
            Component text = Component.translatable(enabled ? onKey : offKey)
                    .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED);
            client.player.sendOverlayMessage(text);
        }
    }
}
