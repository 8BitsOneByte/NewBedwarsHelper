package org.exmple.newbedwarshelper.client.esp.block.render;

import net.minecraft.world.level.block.state.BlockState;

public final class EspBlockRenderColor {
    private EspBlockRenderColor() {
    }

    public static int colorFor(EspBlockRenderTarget target, BlockState state) {
        return target.colorFor(state);
    }
}
