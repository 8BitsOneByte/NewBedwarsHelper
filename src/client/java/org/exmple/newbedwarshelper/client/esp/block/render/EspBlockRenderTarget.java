package org.exmple.newbedwarshelper.client.esp.block.render;

import net.minecraft.world.level.block.state.BlockState;

public interface EspBlockRenderTarget {
    String id();

    int colorFor(BlockState state);
}
