package org.exmple.newbedwarshelper.client.esp.block.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.exmple.newbedwarshelper.client.esp.EspStorageManager;

public final class EspBlockScanScheduler {
    private static final int RESCAN_LOADED_CHUNKS_INTERVAL_TICKS = 20 * 30;
    private static final int TRIM_INTERVAL_TICKS = 20;

    private final EspBlockChunkCache cache;
    private final EspBlockScanWorker worker;
    private ClientLevel lastLevel;
    private int ticksUntilLoadedChunkRescan;
    private int ticksUntilTrim;
    private boolean wasActive;
    private boolean rescanRequested;

    public EspBlockScanScheduler(EspBlockChunkCache cache) {
        this.cache = cache;
        this.worker = new EspBlockScanWorker(cache);
    }

    public void tick(Minecraft client) {
        if (client.level != this.lastLevel) {
            this.clear();
            this.lastLevel = client.level;
        }

        if (!shouldMaintainCache(client)) {
            this.wasActive = false;
            return;
        }

        if (this.rescanRequested || !this.wasActive || this.ticksUntilLoadedChunkRescan-- <= 0) {
            this.submitLoadedChunks(client);
            this.ticksUntilLoadedChunkRescan = RESCAN_LOADED_CHUNKS_INTERVAL_TICKS;
            this.rescanRequested = false;
        }

        if (this.ticksUntilTrim-- <= 0) {
            this.trimDistantChunks(client);
            this.ticksUntilTrim = TRIM_INTERVAL_TICKS;
        }

        this.wasActive = true;
    }

    public boolean shouldMaintainCache(Minecraft client) {
        return client.level != null
                && client.player != null
                && EspStorageManager.isGlobalEspEnabled()
                && EspBlockRenderTargetResolver.hasAnyEnabledTarget();
    }

    public void clear() {
        this.worker.invalidatePendingResults();
        this.lastLevel = null;
        this.ticksUntilLoadedChunkRescan = 0;
        this.ticksUntilTrim = 0;
        this.wasActive = false;
        this.rescanRequested = false;
        this.cache.clear();
    }

    public void requestRescan() {
        this.worker.invalidatePendingResults();
        this.cache.clear();
        this.rescanRequested = true;
    }

    public void submitChunk(Minecraft client, LevelChunk chunk) {
        if (chunk == null || !shouldMaintainCache(client)) {
            return;
        }

        this.worker.submit(chunk);
    }

    private void submitLoadedChunks(Minecraft client) {
        int centerChunkX = client.player.blockPosition().getX() >> 4;
        int centerChunkZ = client.player.blockPosition().getZ() >> 4;
        int radius = chunkRadius(client);

        for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
            for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
                LevelChunk chunk = client.level.getChunkSource().getChunk(chunkX, chunkZ, false);
                if (chunk != null) {
                    this.worker.submit(chunk);
                }
            }
        }

    }

    private void trimDistantChunks(Minecraft client) {
        BlockPos playerPos = client.player.blockPosition();
        this.cache.removeOutside(playerPos.getX() >> 4, playerPos.getZ() >> 4, chunkRadius(client) + 1);
    }

    private static int chunkRadius(Minecraft client) {
        return Math.max(2, client.options.getEffectiveRenderDistance()) + 1;
    }
}
