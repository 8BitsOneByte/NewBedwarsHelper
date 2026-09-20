package org.exmple.newbedwarshelper.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.exmple.newbedwarshelper.client.waterclutchhelper.WaterClutchManager;
import org.exmple.newbedwarshelper.client.waterclutchhelper.WaterClutchManager.MobBucketOption;

public class WaterClutchConfigScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.newbedwarshelper.water_clutch.title");
    private static final Component ENABLED_ON_TEXT = Component.translatable("screen.newbedwarshelper.water_clutch.enabled.on");
    private static final Component ENABLED_OFF_TEXT = Component.translatable("screen.newbedwarshelper.water_clutch.enabled.off");
    private static final Component BUCKET_ON_TEXT = Component.translatable("screen.newbedwarshelper.water_clutch.bucket.on");
    private static final Component BUCKET_OFF_TEXT = Component.translatable("screen.newbedwarshelper.water_clutch.bucket.off");
    private static final Component DONE_TEXT = Component.translatable("screen.newbedwarshelper.water_clutch.done");
    private static final int BUTTON_WIDTH = 150;

    private final Screen parent;

    public WaterClutchConfigScreen(Minecraft minecraft, Screen parent) {
        super(minecraft, minecraft.font, TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().padding(4, 4, 4, 0);
        GridLayout.RowHelper helper = gridLayout.createRowHelper(2);
        helper.addChild(createEnabledButton(), gridLayout.newCellSettings().paddingTop(50));
        helper.addChild(createPredictedLandingButton(), gridLayout.newCellSettings().paddingTop(50));
        helper.addChild(createAlwaysSneakButton());
        addBucketButton(helper, MobBucketOption.PUFFERFISH);
        addBucketButton(helper, MobBucketOption.SALMON);
        addBucketButton(helper, MobBucketOption.COD);
        addBucketButton(helper, MobBucketOption.TROPICAL_FISH);
        addBucketButton(helper, MobBucketOption.AXOLOTL);
        addBucketButton(helper, MobBucketOption.TADPOLE);
        helper.addChild(Button.builder(DONE_TEXT, button -> this.onClose())
                .width(BUTTON_WIDTH)
                .build(), 2, gridLayout.newCellSettings().paddingTop(5).alignHorizontallyCenter());
        gridLayout.arrangeElements();
        FrameLayout.alignInRectangle(gridLayout, 0, 0, this.width, this.height, 0.5F, 0.20F);
        gridLayout.visitWidgets(this::addRenderableWidget);

        int textWidth = this.font.width(this.title);
        this.addRenderableWidget(new StringWidget(this.width / 2 - textWidth / 2, 40, textWidth, 9, this.title, this.font));
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    private static CycleButton<Boolean> createEnabledButton() {
        CycleButton<Boolean> button = CycleButton.builder(WaterClutchConfigScreen::enabledText, WaterClutchManager.isEnabled())
                .withValues(Boolean.TRUE, Boolean.FALSE)
                .displayOnlyValue()
                .create(Component.empty(), (cycleButton, enabled) -> WaterClutchManager.setEnabled(enabled));
        button.setWidth(BUTTON_WIDTH);
        return button;
    }

    private static void addBucketButton(GridLayout.RowHelper helper, MobBucketOption option) {
        CycleButton<Boolean> button = CycleButton.builder(
                        enabled -> bucketText(option, enabled),
                        WaterClutchManager.isMobBucketEnabled(option))
                .withValues(Boolean.TRUE, Boolean.FALSE)
                .displayOnlyValue()
                .create(Component.empty(), (cycleButton, enabled) -> WaterClutchManager.setMobBucketEnabled(option, enabled));
        button.setWidth(BUTTON_WIDTH);
        helper.addChild(button);
    }

    private static CycleButton<Boolean> createPredictedLandingButton() {
        CycleButton<Boolean> button = CycleButton.builder(
                        enabled -> Component.translatable(enabled
                                ? "screen.newbedwarshelper.water_clutch.predicted_landing.on"
                                : "screen.newbedwarshelper.water_clutch.predicted_landing.off"),
                        WaterClutchManager.showsPredictedLandingBlock())
                .withValues(Boolean.TRUE, Boolean.FALSE)
                .displayOnlyValue()
                .create(Component.empty(), (cycleButton, enabled) -> WaterClutchManager.setShowPredictedLandingBlock(enabled));
        button.setWidth(BUTTON_WIDTH);
        return button;
    }

    private static CycleButton<Boolean> createAlwaysSneakButton() {
        CycleButton<Boolean> button = CycleButton.builder(
                        enabled -> Component.translatable(enabled
                                ? "screen.newbedwarshelper.water_clutch.always_sneak.on"
                                : "screen.newbedwarshelper.water_clutch.always_sneak.off"),
                        WaterClutchManager.alwaysSneaks())
                .withValues(Boolean.TRUE, Boolean.FALSE)
                .displayOnlyValue()
                .create(Component.empty(), (cycleButton, enabled) -> WaterClutchManager.setAlwaysSneak(enabled));
        button.setWidth(BUTTON_WIDTH);
        return button;
    }

    private static Component enabledText(boolean enabled) {
        return enabled ? ENABLED_ON_TEXT : ENABLED_OFF_TEXT;
    }

    private static Component bucketText(MobBucketOption option, boolean enabled) {
        String key = "screen.newbedwarshelper.water_clutch.bucket." + option.name().toLowerCase();
        return Component.translatable(
                enabled ? "screen.newbedwarshelper.water_clutch.bucket.enabled" : "screen.newbedwarshelper.water_clutch.bucket.disabled",
                Component.translatable(key),
                enabled ? BUCKET_ON_TEXT : BUCKET_OFF_TEXT
        );
    }
}
