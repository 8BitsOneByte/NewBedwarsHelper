package org.exmple.newbedwarshelper.client.esp.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.util.ARGB;
import org.exmple.newbedwarshelper.client.esp.block.render.EspBlockRenderTarget;

import java.util.List;

public record EspBlockTarget(String id, String translationKey, List<Block> blocks, EspBlockTargetColorMode colorMode) implements EspBlockRenderTarget {
    public EspBlockTarget {
        blocks = List.copyOf(blocks);
    }

    @Override
    public int colorFor(BlockState state) {
        return ARGB.opaque(EspBlockColors.colorFor(this, state));
    }
}
