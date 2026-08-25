package org.exmple.newbedwarshelper.client.esp.block.render;

import net.minecraft.world.level.ChunkPos;

import java.util.List;

public record EspBlockSpatialSnapshot(long revision, List<ChunkEntries> chunks) {
    public record ChunkEntries(ChunkPos pos, List<EspBlockCacheEntry> entries) {
    }
}
