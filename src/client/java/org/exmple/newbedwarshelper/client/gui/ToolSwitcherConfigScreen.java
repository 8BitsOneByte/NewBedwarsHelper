package org.exmple.newbedwarshelper.client.gui;

import java.math.BigInteger;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.exmple.newbedwarshelper.client.toolswitcher.ToolSwitcherManager;
import org.lwjgl.glfw.GLFW;

public class ToolSwitcherConfigScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.newbedwarshelper.tool_switcher.title");
    private static final Component MIN_DELAY_TEXT = Component.translatable("screen.newbedwarshelper.tool_switcher.min_delay");
    private static final Component MAX_DELAY_TEXT = Component.translatable("screen.newbedwarshelper.tool_switcher.max_delay");
    private static final Component RESET_TEXT = Component.translatable("screen.newbedwarshelper.tool_switcher.reset");
    private static final Component DONE_TEXT = Component.translatable("screen.newbedwarshelper.tool_switcher.done");
    private static final BigInteger MAX_INTEGER = BigInteger.valueOf(Integer.MAX_VALUE);
    private static final int FULL_WIDTH = 250;
    private static final int LABEL_WIDTH = 105;
    private static final int INPUT_WIDTH = 70;
    private static final int RESET_WIDTH = 65;

    private final Screen parent;
    private DigitsOnlyEditBox minDelayBox;
    private DigitsOnlyEditBox maxDelayBox;
    private int lastValidMinDelay;
    private int lastValidMaxDelay;

    public ToolSwitcherConfigScreen(Minecraft minecraft, Screen parent) {
        super(minecraft, minecraft.font, TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.lastValidMinDelay = ToolSwitcherManager.getMinDelayTicks();
        this.lastValidMaxDelay = ToolSwitcherManager.getMaxDelayTicks();
        this.minDelayBox = createDelayBox(MIN_DELAY_TEXT, this.lastValidMinDelay);
        this.maxDelayBox = createDelayBox(MAX_DELAY_TEXT, this.lastValidMaxDelay);

        GridLayout gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().padding(4, 4, 4, 0);
        GridLayout.RowHelper helper = gridLayout.createRowHelper(3);
        helper.addChild(Button.builder(enabledText(), button -> {
                    ToolSwitcherManager.setConfiguredEnabled(!ToolSwitcherManager.isConfiguredEnabled());
                    button.setMessage(enabledText());
                })
                .width(FULL_WIDTH)
                .build(), 3, gridLayout.newCellSettings().paddingTop(50).alignHorizontallyCenter());
        helper.addChild(new StringWidget(LABEL_WIDTH, 20, MIN_DELAY_TEXT, this.font), gridLayout.newCellSettings().paddingTop(5));
        helper.addChild(this.minDelayBox, gridLayout.newCellSettings().paddingTop(5));
        helper.addChild(Button.builder(RESET_TEXT, button -> {
                    this.minDelayBox.setValue("3");
                    commitDelayInputs();
                })
                .width(RESET_WIDTH)
                .build(), gridLayout.newCellSettings().paddingTop(5));
        helper.addChild(new StringWidget(LABEL_WIDTH, 20, MAX_DELAY_TEXT, this.font), gridLayout.newCellSettings().paddingTop(5));
        helper.addChild(this.maxDelayBox, gridLayout.newCellSettings().paddingTop(5));
        helper.addChild(Button.builder(RESET_TEXT, button -> {
                    this.maxDelayBox.setValue("7");
                    commitDelayInputs();
                })
                .width(RESET_WIDTH)
                .build(), gridLayout.newCellSettings().paddingTop(5));
        helper.addChild(Button.builder(DONE_TEXT, button -> this.onClose())
                .width(FULL_WIDTH)
                .build(), 3, gridLayout.newCellSettings().paddingTop(5).alignHorizontallyCenter());
        gridLayout.arrangeElements();
        FrameLayout.alignInRectangle(gridLayout, 0, 0, this.width, this.height, 0.5F, 0.25F);
        gridLayout.visitWidgets(this::addRenderableWidget);

        int textWidth = this.font.width(this.title);
        this.addRenderableWidget(new StringWidget(this.width / 2 - textWidth / 2, 40, textWidth, 9, this.title, this.font));
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) {
            commitDelayInputs();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean focused) {
        boolean delayInputWasFocused = this.minDelayBox != null && this.minDelayBox.isFocused()
                || this.maxDelayBox != null && this.maxDelayBox.isFocused();
        boolean handled = super.mouseClicked(event, focused);
        boolean delayInputIsFocused = this.minDelayBox != null && this.minDelayBox.isFocused()
                || this.maxDelayBox != null && this.maxDelayBox.isFocused();
        if (delayInputWasFocused && !delayInputIsFocused) {
            commitDelayInputs();
        }
        return handled;
    }

    @Override
    public void onClose() {
        commitDelayInputs();
        this.minecraft.gui.setScreen(this.parent);
    }

    private DigitsOnlyEditBox createDelayBox(Component narration, int value) {
        DigitsOnlyEditBox box = new DigitsOnlyEditBox(this.font, INPUT_WIDTH, 20, narration);
        box.setMaxLength(10);
        box.setValue(Integer.toString(value));
        return box;
    }

    private void commitDelayInputs() {
        if (this.minDelayBox == null || this.maxDelayBox == null) {
            return;
        }

        int minDelay = parseDelay(this.minDelayBox.getValue(), this.lastValidMinDelay);
        int maxDelay = parseDelay(this.maxDelayBox.getValue(), this.lastValidMaxDelay);
        ToolSwitcherManager.setDelayRange(minDelay, maxDelay);
        this.lastValidMinDelay = ToolSwitcherManager.getMinDelayTicks();
        this.lastValidMaxDelay = ToolSwitcherManager.getMaxDelayTicks();
        this.minDelayBox.setValue(Integer.toString(this.lastValidMinDelay));
        this.maxDelayBox.setValue(Integer.toString(this.lastValidMaxDelay));
    }

    private static int parseDelay(String value, int fallback) {
        if (value.isEmpty()) {
            return fallback;
        }

        BigInteger parsed = new BigInteger(value);
        if (parsed.compareTo(MAX_INTEGER) > 0) {
            return Integer.MAX_VALUE;
        }
        return Math.max(1, parsed.intValue());
    }

    private static Component enabledText() {
        return Component.translatable(ToolSwitcherManager.isConfiguredEnabled()
                ? "screen.newbedwarshelper.tool_switcher.enabled.on"
                : "screen.newbedwarshelper.tool_switcher.enabled.off");
    }
}
