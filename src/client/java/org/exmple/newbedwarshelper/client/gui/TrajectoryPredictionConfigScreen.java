package org.exmple.newbedwarshelper.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.exmple.newbedwarshelper.client.z_config.ModConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

public class TrajectoryPredictionConfigScreen extends Screen {
    private static final Component TITLE = Component.translatable("screen.newbedwarshelper.trajectory_prediction.title");
    private static final Component PROJECTILE_ON = Component.translatable("screen.newbedwarshelper.trajectory_prediction.projectile.on");
    private static final Component PROJECTILE_OFF = Component.translatable("screen.newbedwarshelper.trajectory_prediction.projectile.off");
    private static final Component ARROW_ON = Component.translatable("screen.newbedwarshelper.trajectory_prediction.arrow.on");
    private static final Component ARROW_OFF = Component.translatable("screen.newbedwarshelper.trajectory_prediction.arrow.off");
    private static final Component DONE_TEXT = Component.translatable("screen.newbedwarshelper.trajectory_prediction.done");
    private static final List<Integer> COLOR_PRESETS = List.of(0x57FFE1, 0xF4C95D, 0xFF8A72, 0x9EE493, 0xE8EEF2);
    private static final List<Float> LINE_WIDTHS = List.of(1.0F, 1.5F, 2.0F, 2.5F);
    private static final List<Float> TRANSPARENCIES = List.of(0.40F, 0.56F, 0.72F, 0.88F);
    private static final List<Double> MAX_LENGTHS = List.of(48.0D, 96.0D, 144.0D, 192.0D);
    private static final List<Integer> PRECISIONS = List.of(1, 2, 3, 4);
    private static final int BUTTON_WIDTH = 150;

    private final Screen parent;

    public TrajectoryPredictionConfigScreen(Minecraft minecraft, Screen parent) {
        super(minecraft, minecraft.font, TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        GridLayout gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().padding(4, 4, 4, 0);
        GridLayout.RowHelper helper = gridLayout.createRowHelper(2);
        ModConfig.TrajectoryPredictionConfig config = ModConfig.getInstance().trajectoryPrediction;

        helper.addChild(createToggleButton(TrajectoryPredictionConfigScreen::projectileToggleText, config.projectileEnabled,
                enabled -> updateConfig(cfg -> cfg.projectileEnabled = enabled)), gridLayout.newCellSettings().paddingTop(50));
        helper.addChild(createToggleButton(TrajectoryPredictionConfigScreen::arrowToggleText, config.arrowEnabled,
                enabled -> updateConfig(cfg -> cfg.arrowEnabled = enabled)), gridLayout.newCellSettings().paddingTop(50));
        helper.addChild(createCycleButton(TrajectoryPredictionConfigScreen::projectileColorText, config.projectileColor,
                colorValues(config.projectileColor), value -> updateConfig(cfg -> cfg.projectileColor = value)));
        helper.addChild(createCycleButton(TrajectoryPredictionConfigScreen::arrowColorText, config.arrowColor,
                colorValues(config.arrowColor), value -> updateConfig(cfg -> cfg.arrowColor = value)));
        helper.addChild(createCycleButton(TrajectoryPredictionConfigScreen::lineWidthText, config.lineWidth,
                valuesIncluding(LINE_WIDTHS, config.lineWidth), value -> updateConfig(cfg -> cfg.lineWidth = value)));
        helper.addChild(createCycleButton(TrajectoryPredictionConfigScreen::transparencyText, config.transparency,
                valuesIncluding(TRANSPARENCIES, config.transparency), value -> updateConfig(cfg -> cfg.transparency = value)));
        helper.addChild(createCycleButton(TrajectoryPredictionConfigScreen::maxLengthText, config.maxLength,
                valuesIncluding(MAX_LENGTHS, config.maxLength), value -> updateConfig(cfg -> cfg.maxLength = value)));
        helper.addChild(createCycleButton(TrajectoryPredictionConfigScreen::precisionText, config.samplingPrecision,
                valuesIncluding(PRECISIONS, config.samplingPrecision), value -> updateConfig(cfg -> cfg.samplingPrecision = value)));
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

    private static CycleButton<Boolean> createToggleButton(Function<Boolean, Component> text, boolean initialValue, Consumer<Boolean> onChange) {
        return createCycleButton(text, initialValue, List.of(Boolean.TRUE, Boolean.FALSE), onChange);
    }

    private static <T> CycleButton<T> createCycleButton(Function<T, Component> text, T initialValue, List<T> values, Consumer<T> onChange) {
        CycleButton<T> button = CycleButton.builder(text, initialValue)
                .withValues(values)
                .displayOnlyValue()
                .create(Component.empty(), (cycleButton, value) -> onChange.accept(value));
        button.setWidth(BUTTON_WIDTH);
        return button;
    }

    private static void updateConfig(Consumer<ModConfig.TrajectoryPredictionConfig> updater) {
        ModConfig config = ModConfig.getInstance();
        updater.accept(config.trajectoryPrediction);
        config.save();
    }

    private static List<Integer> colorValues(int current) {
        return valuesIncluding(COLOR_PRESETS, current & 0xFFFFFF);
    }

    private static <T> List<T> valuesIncluding(List<T> presets, T current) {
        if (presets.contains(current)) {
            return presets;
        }

        List<T> values = new ArrayList<>(presets.size() + 1);
        values.add(current);
        values.addAll(presets);
        return values;
    }

    private static Component projectileToggleText(boolean enabled) {
        return enabled ? PROJECTILE_ON : PROJECTILE_OFF;
    }

    private static Component arrowToggleText(boolean enabled) {
        return enabled ? ARROW_ON : ARROW_OFF;
    }

    private static Component projectileColorText(int color) {
        return Component.translatable("screen.newbedwarshelper.trajectory_prediction.projectile_color", colorText(color));
    }

    private static Component arrowColorText(int color) {
        return Component.translatable("screen.newbedwarshelper.trajectory_prediction.arrow_color", colorText(color));
    }

    private static Component colorText(int color) {
        return switch (color & 0xFFFFFF) {
            case 0x57FFE1 -> Component.translatable("screen.newbedwarshelper.trajectory_prediction.color.aqua");
            case 0xF4C95D -> Component.translatable("screen.newbedwarshelper.trajectory_prediction.color.gold");
            case 0xFF8A72 -> Component.translatable("screen.newbedwarshelper.trajectory_prediction.color.coral");
            case 0x9EE493 -> Component.translatable("screen.newbedwarshelper.trajectory_prediction.color.mint");
            case 0xE8EEF2 -> Component.translatable("screen.newbedwarshelper.trajectory_prediction.color.silver");
            default -> Component.literal(String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF));
        };
    }

    private static Component lineWidthText(float width) {
        return Component.translatable("screen.newbedwarshelper.trajectory_prediction.line_width", String.format(Locale.ROOT, "%.1f", width));
    }

    private static Component transparencyText(float transparency) {
        return Component.translatable("screen.newbedwarshelper.trajectory_prediction.transparency", Math.round(transparency * 100.0F));
    }

    private static Component maxLengthText(double maxLength) {
        return Component.translatable("screen.newbedwarshelper.trajectory_prediction.max_length", Math.round(maxLength));
    }

    private static Component precisionText(int precision) {
        return Component.translatable("screen.newbedwarshelper.trajectory_prediction.precision", precision);
    }
}
