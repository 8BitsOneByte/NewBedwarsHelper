package org.exmple.newbedwarshelper.client.esp.block;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class EspBlockColors {
    public static final int DEFAULT_COLOR = 0xFF55DD;

    private EspBlockColors() {
    }

    public static int colorFor(EspBlockTarget target, BlockState state) {
        if (target.colorMode() == EspBlockTargetColorMode.BED_COLOR && state.getBlock() instanceof BedBlock bedBlock) {
            return colorForDye(bedBlock.getColor());
        }

        return DEFAULT_COLOR;
    }

    private static int colorForDye(DyeColor color) {
        return color.getTextureDiffuseColor();
    }
}
