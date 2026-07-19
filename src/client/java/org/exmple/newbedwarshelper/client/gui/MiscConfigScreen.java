package org.exmple.newbedwarshelper.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MiscConfigScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.newbedwarshelper.misc.title");
    private static final Component ANTI_AFK_CONFIG_TEXT = Component.translatable("screen.newbedwarshelper.misc.anti_afk");
    private static final Component GAMMA_OVERRIDE_CONFIG_TEXT = Component.translatable("screen.newbedwarshelper.misc.gamma_override");
    private static final Component BLOCK_ESP_CONFIG_TEXT = Component.translatable("screen.newbedwarshelper.misc.block_esp");
    private static final Component TRAJECTORY_PREDICTION_CONFIG_TEXT = Component.translatable("screen.newbedwarshelper.misc.trajectory_prediction");
    private static final Component DONE_TEXT = Component.translatable("screen.newbedwarshelper.misc.done");
    private static final int BUTTON_WIDTH = 150;

    private final Screen parent;

    public MiscConfigScreen(Minecraft minecraft, Screen parent) {
        super(minecraft, minecraft.font, TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().padding(4, 4, 4, 0);
        GridLayout.RowHelper helper = gridLayout.createRowHelper(2);
        helper.addChild(Button.builder(ANTI_AFK_CONFIG_TEXT, button -> this.minecraft.gui.setScreen(new AntiAfkConfigScreen(this.minecraft, this)))
                .width(BUTTON_WIDTH)
                .build(), gridLayout.newCellSettings().paddingTop(50));
        helper.addChild(Button.builder(GAMMA_OVERRIDE_CONFIG_TEXT, button -> this.minecraft.gui.setScreen(new GammaOverrideConfigScreen(this.minecraft, this)))
                .width(BUTTON_WIDTH)
                .build(), gridLayout.newCellSettings().paddingTop(50));
        helper.addChild(Button.builder(BLOCK_ESP_CONFIG_TEXT, button -> this.minecraft.gui.setScreen(new BlockEspConfigScreen(this.minecraft, this)))
                .width(BUTTON_WIDTH)
                .build(), gridLayout.newCellSettings().paddingTop(5));
        helper.addChild(Button.builder(TRAJECTORY_PREDICTION_CONFIG_TEXT, button -> this.minecraft.gui.setScreen(new TrajectoryPredictionConfigScreen(this.minecraft, this)))
                .width(BUTTON_WIDTH)
                .build(), gridLayout.newCellSettings().paddingTop(5));
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
}
