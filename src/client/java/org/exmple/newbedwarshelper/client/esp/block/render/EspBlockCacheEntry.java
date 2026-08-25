package org.exmple.newbedwarshelper.client.esp.block.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

public final class EspBlockCacheEntry {
    private final BlockPos pos;
    private final EspBlockRenderTarget target;
    private final Block block;
    private final AABB bounds;
    private final int color;
    private int neighbours;

    public EspBlockCacheEntry(BlockPos pos, EspBlockRenderTarget target, Block block, AABB bounds, int color) {
        this.pos = pos;
        this.target = target;
        this.block = block;
        this.bounds = bounds;
        this.color = color;
    }

    public BlockPos pos() {
        return this.pos;
    }

    public EspBlockRenderTarget target() {
        return this.target;
    }

    public Block block() {
        return this.block;
    }

    public AABB bounds() {
        return this.bounds;
    }

    public int color() {
        return this.color;
    }

    public int neighbours() {
        return this.neighbours;
    }

    public void setNeighbours(int neighbours) {
        this.neighbours = neighbours;
    }
}
