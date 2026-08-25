package org.exmple.newbedwarshelper.client.esp.block.render.navigation;

import net.minecraft.core.BlockPos;
import org.exmple.newbedwarshelper.client.esp.block.render.EspBlockRenderTarget;

import java.util.HashSet;
import java.util.Set;

public final class EspBlockNavigationGroup {
    private final int id;
    private final EspBlockRenderTarget target;
    private final Set<Long> positions = new HashSet<>();
    private long sumX;
    private long sumY;
    private long sumZ;
    private int size;
    private boolean oversized;

    EspBlockNavigationGroup(int id, EspBlockRenderTarget target) {
        this.id = id;
        this.target = target;
    }

    void add(BlockPos pos) {
        this.size++;
        this.sumX += pos.getX();
        this.sumY += pos.getY();
        this.sumZ += pos.getZ();

        if (!this.oversized) {
            if (this.size <= EspBlockNavigationConstants.MAX_NAVIGABLE_GROUP_SIZE) {
                this.positions.add(pos.asLong());
            } else {
                this.oversized = true;
                this.positions.clear();
            }
        }
    }

    public int id() {
        return this.id;
    }

    public EspBlockRenderTarget target() {
        return this.target;
    }

    public int size() {
        return this.size;
    }

    public boolean isNavigable() {
        return !this.oversized && this.size > 0;
    }

    public boolean contains(BlockPos pos) {
        return this.positions.contains(pos.asLong());
    }

    public Iterable<Long> positions() {
        return this.positions;
    }

    public double centerX() {
        return (double) this.sumX / this.size + 0.5D;
    }

    public double centerY() {
        return (double) this.sumY / this.size + 0.5D;
    }

    public double centerZ() {
        return (double) this.sumZ / this.size + 0.5D;
    }
}
