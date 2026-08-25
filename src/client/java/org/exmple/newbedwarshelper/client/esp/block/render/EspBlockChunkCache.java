package org.exmple.newbedwarshelper.client.esp.block.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import org.exmple.newbedwarshelper.client.esp.block.render.navigation.EspBlockNavigationController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class EspBlockChunkCache {
    private final Map<Long, EspBlockChunk> chunks = new HashMap<>();
    private long revision;
    private EspBlockSpatialSnapshot cachedSpatialSnapshot;

    public synchronized void replaceChunk(ChunkPos pos, List<EspBlockCacheEntry> entries) {
        long key = ChunkPos.pack(pos.x(), pos.z());
        if (entries.isEmpty()) {
            this.chunks.remove(key);
            this.markChanged();
            EspBlockNavigationController.markDirty();
            return;
        }

        EspBlockChunk chunk = new EspBlockChunk(pos);
        for (EspBlockCacheEntry entry : entries) {
            chunk.put(entry);
        }

        this.chunks.put(key, chunk);
        for (EspBlockCacheEntry entry : entries) {
            this.updateNeighboursAround(entry.pos());
        }
        this.markChanged();
        EspBlockNavigationController.markDirty();
    }

    public synchronized void updateBlock(BlockPos pos, EspBlockRenderTarget target, Block block, net.minecraft.world.phys.AABB bounds, int color) {
        ChunkPos chunkPos = new ChunkPos(pos.getX() >> 4, pos.getZ() >> 4);
        long key = ChunkPos.pack(chunkPos.x(), chunkPos.z());
        EspBlockChunk chunk = this.chunks.get(key);
        if (chunk == null) {
            chunk = new EspBlockChunk(chunkPos);
            this.chunks.put(key, chunk);
        }

        chunk.put(new EspBlockCacheEntry(pos.immutable(), target, block, bounds, color));
        this.updateNeighboursAround(pos);
        this.markChanged();
        EspBlockNavigationController.markDirty();
    }

    public synchronized void removeBlock(BlockPos pos) {
        long key = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        EspBlockChunk chunk = this.chunks.get(key);
        if (chunk == null) {
            return;
        }

        chunk.remove(pos);
        this.updateNeighboursAround(pos);
        if (chunk.isEmpty()) {
            this.chunks.remove(key);
        }
        this.markChanged();
        EspBlockNavigationController.markDirty();
    }

    public synchronized void removeChunk(ChunkPos pos) {
        if (this.chunks.remove(ChunkPos.pack(pos.x(), pos.z())) != null) {
            this.markChanged();
            EspBlockNavigationController.markDirty();
        }
    }

    public synchronized int removeOutside(int centerChunkX, int centerChunkZ, int chunkRadius) {
        int removed = 0;
        Iterator<EspBlockChunk> iterator = this.chunks.values().iterator();
        while (iterator.hasNext()) {
            ChunkPos pos = iterator.next().pos();
            if (pos.x() < centerChunkX - chunkRadius
                    || pos.x() > centerChunkX + chunkRadius
                    || pos.z() < centerChunkZ - chunkRadius
                    || pos.z() > centerChunkZ + chunkRadius) {
                iterator.remove();
                removed++;
            }
        }

        if (removed > 0) {
            this.markChanged();
            EspBlockNavigationController.markDirty();
        }
        return removed;
    }

    public synchronized void clear() {
        if (this.chunks.isEmpty()) {
            return;
        }
        this.chunks.clear();
        this.markChanged();
        EspBlockNavigationController.markDirty();
    }

    public synchronized int chunkCount() {
        return this.chunks.size();
    }

    public synchronized int blockCount() {
        int count = 0;
        for (EspBlockChunk chunk : this.chunks.values()) {
            count += chunk.size();
        }
        return count;
    }

    public synchronized List<EspBlockCacheEntry> snapshot() {
        List<EspBlockCacheEntry> snapshot = new ArrayList<>();
        for (EspBlockChunk chunk : this.chunks.values()) {
            snapshot.addAll(chunk.snapshot());
        }
        return snapshot;
    }

    public synchronized EspBlockSpatialSnapshot spatialSnapshot() {
        if (this.cachedSpatialSnapshot != null) {
            return this.cachedSpatialSnapshot;
        }

        List<EspBlockSpatialSnapshot.ChunkEntries> chunkSnapshots = new ArrayList<>(this.chunks.size());
        for (EspBlockChunk chunk : this.chunks.values()) {
            chunkSnapshots.add(new EspBlockSpatialSnapshot.ChunkEntries(chunk.pos(), List.copyOf(chunk.snapshot())));
        }
        this.cachedSpatialSnapshot = new EspBlockSpatialSnapshot(this.revision, List.copyOf(chunkSnapshots));
        return this.cachedSpatialSnapshot;
    }

    public synchronized List<EspBlockCacheEntry> entriesAt(Iterable<Long> packedPositions) {
        List<EspBlockCacheEntry> entries = new ArrayList<>();
        for (Long packedPos : packedPositions) {
            BlockPos pos = BlockPos.of(packedPos);
            EspBlockCacheEntry entry = this.get(pos);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    public synchronized void forEachEntry(Consumer<EspBlockCacheEntry> consumer) {
        for (EspBlockChunk chunk : this.chunks.values()) {
            chunk.forEachEntry(consumer);
        }
    }

    private void markChanged() {
        this.revision++;
        this.cachedSpatialSnapshot = null;
    }

    private EspBlockCacheEntry get(BlockPos pos) {
        EspBlockChunk chunk = this.chunks.get(ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4));
        return chunk == null ? null : chunk.get(pos);
    }

    private void updateNeighboursAround(BlockPos pos) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    mutable.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    EspBlockCacheEntry entry = this.get(mutable);
                    if (entry != null) {
                        entry.setNeighbours(this.computeNeighbours(entry));
                    }
                }
            }
        }
    }

    private int computeNeighbours(EspBlockCacheEntry entry) {
        int neighbours = 0;
        if (this.isNeighbour(entry, 0, 0, 1, Axis.Z, true)) neighbours |= EspBlockNeighbourFlags.FO;
        if (this.isNeighbourDiagonal(entry, 1, 0, 1)) neighbours |= EspBlockNeighbourFlags.FO_RI;
        if (this.isNeighbour(entry, 1, 0, 0, Axis.X, true)) neighbours |= EspBlockNeighbourFlags.RI;
        if (this.isNeighbourDiagonal(entry, 1, 0, -1)) neighbours |= EspBlockNeighbourFlags.BA_RI;
        if (this.isNeighbour(entry, 0, 0, -1, Axis.Z, false)) neighbours |= EspBlockNeighbourFlags.BA;
        if (this.isNeighbourDiagonal(entry, -1, 0, -1)) neighbours |= EspBlockNeighbourFlags.BA_LE;
        if (this.isNeighbour(entry, -1, 0, 0, Axis.X, false)) neighbours |= EspBlockNeighbourFlags.LE;
        if (this.isNeighbourDiagonal(entry, -1, 0, 1)) neighbours |= EspBlockNeighbourFlags.FO_LE;

        if (this.isNeighbour(entry, 0, 1, 0, Axis.Y, true)) neighbours |= EspBlockNeighbourFlags.TO;
        if (this.isNeighbourDiagonal(entry, 0, 1, 1)) neighbours |= EspBlockNeighbourFlags.TO_FO;
        if (this.isNeighbourDiagonal(entry, 0, 1, -1)) neighbours |= EspBlockNeighbourFlags.TO_BA;
        if (this.isNeighbourDiagonal(entry, 1, 1, 0)) neighbours |= EspBlockNeighbourFlags.TO_RI;
        if (this.isNeighbourDiagonal(entry, -1, 1, 0)) neighbours |= EspBlockNeighbourFlags.TO_LE;
        if (this.isNeighbour(entry, 0, -1, 0, Axis.Y, false)) neighbours |= EspBlockNeighbourFlags.BO;
        if (this.isNeighbourDiagonal(entry, 0, -1, 1)) neighbours |= EspBlockNeighbourFlags.BO_FO;
        if (this.isNeighbourDiagonal(entry, 0, -1, -1)) neighbours |= EspBlockNeighbourFlags.BO_BA;
        if (this.isNeighbourDiagonal(entry, 1, -1, 0)) neighbours |= EspBlockNeighbourFlags.BO_RI;
        if (this.isNeighbourDiagonal(entry, -1, -1, 0)) neighbours |= EspBlockNeighbourFlags.BO_LE;
        return neighbours;
    }

    private boolean isNeighbourDiagonal(EspBlockCacheEntry entry, int dx, int dy, int dz) {
        EspBlockCacheEntry neighbour = this.relative(entry.pos(), dx, dy, dz);
        return neighbour != null && neighbour.target().equals(entry.target());
    }

    private boolean isNeighbour(EspBlockCacheEntry entry, int dx, int dy, int dz, Axis axis, boolean positive) {
        EspBlockCacheEntry neighbour = this.relative(entry.pos(), dx, dy, dz);
        if (neighbour == null || !neighbour.target().equals(entry.target())) {
            return false;
        }

        double currentEdge = positive ? axis.max(entry) : axis.min(entry);
        double neighbourEdge = positive ? axis.min(neighbour) : axis.max(neighbour);
        return positive ? currentEdge >= 0.999D && neighbourEdge <= 0.001D : currentEdge <= 0.001D && neighbourEdge >= 0.999D;
    }

    private EspBlockCacheEntry relative(BlockPos pos, int dx, int dy, int dz) {
        return this.get(new BlockPos(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz));
    }

    private enum Axis {
        X {
            @Override
            double min(EspBlockCacheEntry entry) {
                return entry.bounds().minX;
            }

            @Override
            double max(EspBlockCacheEntry entry) {
                return entry.bounds().maxX;
            }
        },
        Y {
            @Override
            double min(EspBlockCacheEntry entry) {
                return entry.bounds().minY;
            }

            @Override
            double max(EspBlockCacheEntry entry) {
                return entry.bounds().maxY;
            }
        },
        Z {
            @Override
            double min(EspBlockCacheEntry entry) {
                return entry.bounds().minZ;
            }

            @Override
            double max(EspBlockCacheEntry entry) {
                return entry.bounds().maxZ;
            }
        };

        abstract double min(EspBlockCacheEntry entry);

        abstract double max(EspBlockCacheEntry entry);
    }
}
