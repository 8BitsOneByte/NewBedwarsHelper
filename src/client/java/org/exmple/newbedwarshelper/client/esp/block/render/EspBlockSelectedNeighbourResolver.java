package org.exmple.newbedwarshelper.client.esp.block.render;

import net.minecraft.core.BlockPos;

import java.util.Map;

final class EspBlockSelectedNeighbourResolver {
    private EspBlockSelectedNeighbourResolver() {
    }

    static int compute(EspBlockCacheEntry entry, Map<Long, EspBlockCacheEntry> selectedByPos) {
        int neighbours = 0;
        if (isNeighbour(entry, selectedByPos, 0, 0, 1, Axis.Z, true)) neighbours |= EspBlockNeighbourFlags.FO;
        if (isNeighbourDiagonal(entry, selectedByPos, 1, 0, 1)) neighbours |= EspBlockNeighbourFlags.FO_RI;
        if (isNeighbour(entry, selectedByPos, 1, 0, 0, Axis.X, true)) neighbours |= EspBlockNeighbourFlags.RI;
        if (isNeighbourDiagonal(entry, selectedByPos, 1, 0, -1)) neighbours |= EspBlockNeighbourFlags.BA_RI;
        if (isNeighbour(entry, selectedByPos, 0, 0, -1, Axis.Z, false)) neighbours |= EspBlockNeighbourFlags.BA;
        if (isNeighbourDiagonal(entry, selectedByPos, -1, 0, -1)) neighbours |= EspBlockNeighbourFlags.BA_LE;
        if (isNeighbour(entry, selectedByPos, -1, 0, 0, Axis.X, false)) neighbours |= EspBlockNeighbourFlags.LE;
        if (isNeighbourDiagonal(entry, selectedByPos, -1, 0, 1)) neighbours |= EspBlockNeighbourFlags.FO_LE;
        if (isNeighbour(entry, selectedByPos, 0, 1, 0, Axis.Y, true)) neighbours |= EspBlockNeighbourFlags.TO;
        if (isNeighbourDiagonal(entry, selectedByPos, 0, 1, 1)) neighbours |= EspBlockNeighbourFlags.TO_FO;
        if (isNeighbourDiagonal(entry, selectedByPos, 0, 1, -1)) neighbours |= EspBlockNeighbourFlags.TO_BA;
        if (isNeighbourDiagonal(entry, selectedByPos, 1, 1, 0)) neighbours |= EspBlockNeighbourFlags.TO_RI;
        if (isNeighbourDiagonal(entry, selectedByPos, -1, 1, 0)) neighbours |= EspBlockNeighbourFlags.TO_LE;
        if (isNeighbour(entry, selectedByPos, 0, -1, 0, Axis.Y, false)) neighbours |= EspBlockNeighbourFlags.BO;
        if (isNeighbourDiagonal(entry, selectedByPos, 0, -1, 1)) neighbours |= EspBlockNeighbourFlags.BO_FO;
        if (isNeighbourDiagonal(entry, selectedByPos, 0, -1, -1)) neighbours |= EspBlockNeighbourFlags.BO_BA;
        if (isNeighbourDiagonal(entry, selectedByPos, 1, -1, 0)) neighbours |= EspBlockNeighbourFlags.BO_RI;
        if (isNeighbourDiagonal(entry, selectedByPos, -1, -1, 0)) neighbours |= EspBlockNeighbourFlags.BO_LE;
        return neighbours;
    }

    private static boolean isNeighbourDiagonal(
            EspBlockCacheEntry entry,
            Map<Long, EspBlockCacheEntry> selectedByPos,
            int dx,
            int dy,
            int dz
    ) {
        EspBlockCacheEntry neighbour = relative(entry.pos(), selectedByPos, dx, dy, dz);
        return neighbour != null && neighbour.target().equals(entry.target());
    }

    private static boolean isNeighbour(
            EspBlockCacheEntry entry,
            Map<Long, EspBlockCacheEntry> selectedByPos,
            int dx,
            int dy,
            int dz,
            Axis axis,
            boolean positive
    ) {
        EspBlockCacheEntry neighbour = relative(entry.pos(), selectedByPos, dx, dy, dz);
        if (neighbour == null || !neighbour.target().equals(entry.target())) {
            return false;
        }

        double currentEdge = positive ? axis.max(entry) : axis.min(entry);
        double neighbourEdge = positive ? axis.min(neighbour) : axis.max(neighbour);
        return positive
                ? currentEdge >= 0.999D && neighbourEdge <= 0.001D
                : currentEdge <= 0.001D && neighbourEdge >= 0.999D;
    }

    private static EspBlockCacheEntry relative(
            BlockPos pos,
            Map<Long, EspBlockCacheEntry> selectedByPos,
            int dx,
            int dy,
            int dz
    ) {
        return selectedByPos.get(BlockPos.asLong(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz));
    }

    private enum Axis {
        X {
            @Override double min(EspBlockCacheEntry entry) { return entry.bounds().minX; }
            @Override double max(EspBlockCacheEntry entry) { return entry.bounds().maxX; }
        },
        Y {
            @Override double min(EspBlockCacheEntry entry) { return entry.bounds().minY; }
            @Override double max(EspBlockCacheEntry entry) { return entry.bounds().maxY; }
        },
        Z {
            @Override double min(EspBlockCacheEntry entry) { return entry.bounds().minZ; }
            @Override double max(EspBlockCacheEntry entry) { return entry.bounds().maxZ; }
        };

        abstract double min(EspBlockCacheEntry entry);
        abstract double max(EspBlockCacheEntry entry);
    }
}
