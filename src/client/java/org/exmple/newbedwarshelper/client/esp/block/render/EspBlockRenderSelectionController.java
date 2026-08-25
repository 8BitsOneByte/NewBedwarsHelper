package org.exmple.newbedwarshelper.client.esp.block.render;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

final class EspBlockRenderSelectionController {
    private static final int ENTRY_LINE_BUDGET = 9_999;
    private static final double REBUILD_MOVEMENT_SQR = 0.25D * 0.25D;

    private volatile EspBlockRenderSelection current = EspBlockRenderSelection.EMPTY;
    private long lastRevision = Long.MIN_VALUE;
    private int lastNavigationGroupId = Integer.MIN_VALUE;
    private Vec3 lastOrigin;

    EspBlockRenderSelection current() {
        return this.current;
    }

    void clear() {
        this.current = EspBlockRenderSelection.EMPTY;
        this.lastRevision = Long.MIN_VALUE;
        this.lastNavigationGroupId = Integer.MIN_VALUE;
        this.lastOrigin = null;
    }

    void update(
            Minecraft client,
            EspBlockSpatialSnapshot snapshot,
            int navigationGroupId,
            List<EspBlockCacheEntry> navigationEntries
    ) {
        if (client.player == null || snapshot.chunks().isEmpty()) {
            this.clear();
            return;
        }

        Vec3 origin = client.player.getEyePosition();
        if (this.lastRevision == snapshot.revision()
                && this.lastNavigationGroupId == navigationGroupId
                && this.lastOrigin != null
                && this.lastOrigin.distanceToSqr(origin) < REBUILD_MOVEMENT_SQR) {
            return;
        }

        SelectionAccumulator accumulator = new SelectionAccumulator(ENTRY_LINE_BUDGET);
        navigationEntries.stream()
                .sorted(entryComparator(origin))
                .forEach(accumulator::tryAdd);

        PriorityQueue<ChunkCandidate> chunks = new PriorityQueue<>(CHUNK_COMPARATOR);
        for (EspBlockSpatialSnapshot.ChunkEntries chunk : snapshot.chunks()) {
            chunks.add(new ChunkCandidate(chunk, horizontalDistanceLowerBoundSqr(origin, chunk.pos())));
        }

        PriorityQueue<EntryCandidate> entries = new PriorityQueue<>(ENTRY_COMPARATOR);
        while (!chunks.isEmpty() || !entries.isEmpty()) {
            while (!chunks.isEmpty()
                    && (entries.isEmpty() || chunks.peek().lowerBoundDistanceSqr() <= entries.peek().distanceSqr())) {
                EspBlockSpatialSnapshot.ChunkEntries chunk = chunks.remove().chunk();
                for (EspBlockCacheEntry entry : chunk.entries()) {
                    entries.add(new EntryCandidate(entry, distanceToCenterSqr(origin, entry)));
                }
            }

            if (entries.isEmpty()) {
                continue;
            }

            EspBlockCacheEntry entry = entries.remove().entry();
            if (accumulator.contains(entry.pos().asLong())) {
                continue;
            }
            if (!accumulator.tryAdd(entry)) {
                break;
            }
        }

        this.current = accumulator.finish();
        this.lastRevision = snapshot.revision();
        this.lastNavigationGroupId = navigationGroupId;
        this.lastOrigin = origin;
    }

    private static Comparator<EspBlockCacheEntry> entryComparator(Vec3 origin) {
        return Comparator.comparingDouble((EspBlockCacheEntry entry) -> distanceToCenterSqr(origin, entry))
                .thenComparingLong(entry -> entry.pos().asLong());
    }

    private static double distanceToCenterSqr(Vec3 origin, EspBlockCacheEntry entry) {
        AABB bounds = entry.bounds();
        double x = entry.pos().getX() + (bounds.minX + bounds.maxX) * 0.5D;
        double y = entry.pos().getY() + (bounds.minY + bounds.maxY) * 0.5D;
        double z = entry.pos().getZ() + (bounds.minZ + bounds.maxZ) * 0.5D;
        return origin.distanceToSqr(x, y, z);
    }

    private static double horizontalDistanceLowerBoundSqr(Vec3 origin, ChunkPos pos) {
        double minX = pos.getMinBlockX();
        double maxX = minX + 16.0D;
        double minZ = pos.getMinBlockZ();
        double maxZ = minZ + 16.0D;
        double dx = origin.x < minX ? minX - origin.x : origin.x > maxX ? origin.x - maxX : 0.0D;
        double dz = origin.z < minZ ? minZ - origin.z : origin.z > maxZ ? origin.z - maxZ : 0.0D;
        return dx * dx + dz * dz;
    }

    private static final Comparator<ChunkCandidate> CHUNK_COMPARATOR = Comparator
            .comparingDouble(ChunkCandidate::lowerBoundDistanceSqr)
            .thenComparingInt(candidate -> candidate.chunk().pos().x())
            .thenComparingInt(candidate -> candidate.chunk().pos().z());

    private static final Comparator<EntryCandidate> ENTRY_COMPARATOR = Comparator
            .comparingDouble(EntryCandidate::distanceSqr)
            .thenComparingLong(candidate -> candidate.entry().pos().asLong());

    private record ChunkCandidate(EspBlockSpatialSnapshot.ChunkEntries chunk, double lowerBoundDistanceSqr) {
    }

    private record EntryCandidate(EspBlockCacheEntry entry, double distanceSqr) {
    }

    private static final class SelectionAccumulator {
        private final int budget;
        private final Map<Long, EspBlockCacheEntry> selected = new LinkedHashMap<>();
        private int lineCount;

        private SelectionAccumulator(int budget) {
            this.budget = budget;
        }

        private boolean contains(long packedPos) {
            return this.selected.containsKey(packedPos);
        }

        private boolean tryAdd(EspBlockCacheEntry candidate) {
            long key = candidate.pos().asLong();
            if (this.selected.containsKey(key)) {
                return true;
            }

            List<EspBlockCacheEntry> affected = this.affectedEntries(candidate);
            int before = 0;
            for (EspBlockCacheEntry entry : affected) {
                before += this.lineCount(entry);
            }

            this.selected.put(key, candidate);
            int after = this.lineCount(candidate);
            for (EspBlockCacheEntry entry : affected) {
                after += this.lineCount(entry);
            }

            int updatedLineCount = this.lineCount - before + after;
            if (updatedLineCount > this.budget) {
                this.selected.remove(key);
                return false;
            }

            this.lineCount = updatedLineCount;
            return true;
        }

        private List<EspBlockCacheEntry> affectedEntries(EspBlockCacheEntry candidate) {
            List<EspBlockCacheEntry> affected = new ArrayList<>(26);
            int x = candidate.pos().getX();
            int y = candidate.pos().getY();
            int z = candidate.pos().getZ();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        EspBlockCacheEntry entry = this.selected.get(net.minecraft.core.BlockPos.asLong(x + dx, y + dy, z + dz));
                        if (entry != null) {
                            affected.add(entry);
                        }
                    }
                }
            }
            return affected;
        }

        private int lineCount(EspBlockCacheEntry entry) {
            int neighbours = EspBlockSelectedNeighbourResolver.compute(entry, this.selected);
            return EspBlockLineGeometry.lineCount(neighbours);
        }

        private EspBlockRenderSelection finish() {
            List<EspBlockSelectedRenderEntry> entries = new ArrayList<>(this.selected.size());
            int verifiedLineCount = 0;
            for (EspBlockCacheEntry entry : this.selected.values()) {
                int neighbours = EspBlockSelectedNeighbourResolver.compute(entry, this.selected);
                entries.add(new EspBlockSelectedRenderEntry(entry, neighbours));
                verifiedLineCount += EspBlockLineGeometry.lineCount(neighbours);
            }
            return new EspBlockRenderSelection(List.copyOf(entries), verifiedLineCount);
        }
    }
}
