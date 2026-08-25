package org.exmple.newbedwarshelper.client.esp.block.render;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.ArrayList;
import java.util.List;

public final class EspBlockChunkScanner {
    private EspBlockChunkScanner() {
    }

    public static ScanResult scan(LevelChunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        List<EspBlockCacheEntry> entries = new ArrayList<>();
        LevelChunkSection[] sections = chunk.getSections();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section.hasOnlyAir()) {
                continue;
            }

            int minY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(sectionIndex));
            for (int localY = 0; localY < 16; localY++) {
                int y = minY + localY;
                for (int localZ = 0; localZ < 16; localZ++) {
                    int z = chunkPos.getMinBlockZ() + localZ;
                    for (int localX = 0; localX < 16; localX++) {
                        BlockState state = section.getBlockState(localX, localY, localZ);
                        EspBlockRenderTarget target = EspBlockRenderTargetResolver.targetForBlock(state.getBlock());
                        if (target != null) {
                            int x = chunkPos.getMinBlockX() + localX;
                            BlockPos pos = new BlockPos(x, y, z);
                            entries.add(new EspBlockCacheEntry(
                                    pos,
                                    target,
                                    state.getBlock(),
                                    EspBlockBoundsResolver.resolve(),
                                    EspBlockRenderColor.colorFor(target, state)
                            ));
                        }
                    }
                }
            }
        }

        return new ScanResult(chunkPos, entries);
    }

    public record ScanResult(ChunkPos pos, List<EspBlockCacheEntry> entries) {
    }
}
