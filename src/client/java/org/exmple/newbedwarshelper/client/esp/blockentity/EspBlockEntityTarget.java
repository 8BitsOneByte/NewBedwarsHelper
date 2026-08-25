package org.exmple.newbedwarshelper.client.esp.blockentity;

import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.exmple.newbedwarshelper.client.esp.block.render.EspBlockRenderTarget;

import java.util.List;

public record EspBlockEntityTarget(String id, String translationKey, List<Block> blocks, int color) implements EspBlockRenderTarget {
    public EspBlockEntityTarget {
        blocks = List.copyOf(blocks);
    }

    @Override
    public int colorFor(BlockState state) {
        return ARGB.opaque(this.color);
    }
}
