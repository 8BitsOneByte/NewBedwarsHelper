package org.exmple.newbedwarshelper.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.exmple.newbedwarshelper.client.gammaoverride.GammaOverrideIrisCompat;
import org.exmple.newbedwarshelper.client.gammaoverride.GammaOverrideManager;
import org.exmple.newbedwarshelper.client.z_config.ModConfig;

public class GammaOverrideConfigScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.newbedwarshelper.gamma_override.title");
    private static final Component ENABLED_ON_TEXT = Component.translatable("screen.newbedwarshelper.gamma_override.enabled.on");
    private static final Component ENABLED_OFF_TEXT = Component.translatable("screen.newbedwarshelper.gamma_override.enabled.off");
    private static final Component MODE_NIGHT_VISION_TEXT = Component.translatable("screen.newbedwarshelper.gamma_override.mode.night_vision");
    private static final Component MODE_INVALID_GAMMA_TEXT = Component.translatable("screen.newbedwarshelper.gamma_override.mode.invalid_gamma");
    private static final Component SHADER_INCOMPATIBLE_TOOLTIP = Component.translatable("screen.newbedwarshelper.gamma_override.mode.invalid_gamma.shader_incompatible").withStyle(ChatFormatting.RED);
    private static final Component DONE_TEXT = Component.translatable("screen.newbedwarshelper.gamma_override.done");
    private static final int BUTTON_WIDTH = 150;

    private final Screen parent;

    public GammaOverrideConfigScreen(Minecraft minecraft, Screen parent) {
        super(minecraft, minecraft.font, TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().padding(4, 4, 4, 0);
        GridLayout.RowHelper helper = gridLayout.createRowHelper(2);
        helper.addChild(createEnabledButton(), gridLayout.newCellSettings().paddingTop(50));
        helper.addChild(createModeButton(), gridLayout.newCellSettings().paddingTop(50));
        helper.addChild(Button.builder(DONE_TEXT, button -> this.onClose())
                .width(BUTTON_WIDTH)
                .build(), 2, gridLayout.newCellSettings().alignHorizontallyCenter());
        gridLayout.arrangeElements();
        FrameLayout.alignInRectangle(gridLayout, 0, 0, this.width, this.height, 0.5F, 0.25F);
        gridLayout.visitWidgets(this::addRenderableWidget);

        int textWidth = this.font.width(this.title);
        this.addRenderableWidget(new StringWidget(this.width / 2 - textWidth / 2, 40, textWidth, 9, this.title, this.font));
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    private static Component enabledText(boolean enabled) {
        return enabled ? ENABLED_ON_TEXT : ENABLED_OFF_TEXT;
    }

    private static CycleButton<Boolean> createEnabledButton() {
        CycleButton<Boolean> button = CycleButton.builder(GammaOverrideConfigScreen::enabledText, GammaOverrideManager.isEnabled())
                .withValues(Boolean.TRUE, Boolean.FALSE)
                .displayOnlyValue()
                .create(Component.empty(), (cycleButton, enabled) -> GammaOverrideManager.setEnabled(enabled));
        button.setWidth(BUTTON_WIDTH);
        return button;
    }

    private static CycleButton<ModConfig.GammaOverrideMode> createModeButton() {
        CycleButton<ModConfig.GammaOverrideMode> button = CycleButton.builder(GammaOverrideConfigScreen::modeText, GammaOverrideManager.getMode())
                .withValues(ModConfig.GammaOverrideMode.NIGHT_VISION, ModConfig.GammaOverrideMode.INVALID_GAMMA)
                .displayOnlyValue()
                .create(Component.empty(), (cycleButton, mode) -> {
                    GammaOverrideManager.setMode(mode);
                    updateModeButtonTooltip(cycleButton, mode);
                });
        button.setWidth(BUTTON_WIDTH);
        updateModeButtonTooltip(button, GammaOverrideManager.getMode());
        return button;
    }

    private static Component modeText(ModConfig.GammaOverrideMode mode) {
        return mode == ModConfig.GammaOverrideMode.INVALID_GAMMA ? MODE_INVALID_GAMMA_TEXT : MODE_NIGHT_VISION_TEXT;
    }

    private static void updateModeButtonTooltip(CycleButton<ModConfig.GammaOverrideMode> button, ModConfig.GammaOverrideMode mode) {
        if (mode == ModConfig.GammaOverrideMode.INVALID_GAMMA && GammaOverrideIrisCompat.isShaderPackInUse()) {
            button.setTooltip(Tooltip.create(SHADER_INCOMPATIBLE_TOOLTIP));
            return;
        }

        button.setTooltip(null);
    }
}
