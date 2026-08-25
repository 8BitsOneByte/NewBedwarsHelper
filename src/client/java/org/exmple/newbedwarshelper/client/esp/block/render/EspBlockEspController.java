package org.exmple.newbedwarshelper.client.esp.block.render;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.exmple.newbedwarshelper.client.esp.block.render.navigation.EspBlockNavigationController;
import org.exmple.newbedwarshelper.client.esp.block.render.navigation.EspBlockNavigationGroup;

import java.util.List;
import java.util.function.Consumer;

public final class EspBlockEspController {
    private static final EspBlockChunkCache CACHE = new EspBlockChunkCache();
    private static final EspBlockScanScheduler SCAN_SCHEDULER = new EspBlockScanScheduler(CACHE);
    private static final EspBlockRenderSelectionController RENDER_SELECTION = new EspBlockRenderSelectionController();
    private static boolean initialized;

    private EspBlockEspController() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        ClientTickEvents.END_CLIENT_TICK.register(EspBlockEspController::onClientTick);
        initialized = true;
    }

    public static List<EspBlockCacheEntry> snapshot() {
        return CACHE.snapshot();
    }

    public static void forEachEntry(Consumer<EspBlockCacheEntry> consumer) {
        CACHE.forEachEntry(consumer);
    }

    public static EspBlockRenderSelection renderSelection() {
        return RENDER_SELECTION.current();
    }

    public static boolean shouldRender(Minecraft client) {
        return SCAN_SCHEDULER.shouldMaintainCache(client);
    }

    public static void clear() {
        SCAN_SCHEDULER.clear();
        RENDER_SELECTION.clear();
    }

    public static void requestRescan() {
        SCAN_SCHEDULER.requestRescan();
        RENDER_SELECTION.clear();
    }

    public static void submitChunk(LevelChunk chunk) {
        SCAN_SCHEDULER.submitChunk(Minecraft.getInstance(), chunk);
    }

    public static void removeChunk(ChunkPos pos) {
        CACHE.removeChunk(pos);
    }

    public static void removePosition(BlockPos pos) {
        CACHE.removeBlock(pos);
    }

    public static void updatePosition(Minecraft client, BlockPos pos, BlockState state) {
        if (!SCAN_SCHEDULER.shouldMaintainCache(client)) {
            return;
        }

        EspBlockRenderTarget target = EspBlockRenderTargetResolver.targetForBlock(state.getBlock());
        if (target == null) {
            CACHE.removeBlock(pos);
            return;
        }

        CACHE.updateBlock(
                pos,
                target,
                state.getBlock(),
                EspBlockBoundsResolver.resolve(),
                EspBlockRenderColor.colorFor(target, state)
        );
    }

    private static void onClientTick(Minecraft client) {
        SCAN_SCHEDULER.tick(client);
        if (!SCAN_SCHEDULER.shouldMaintainCache(client)) {
            RENDER_SELECTION.clear();
            return;
        }

        EspBlockNavigationController.update(client);
        EspBlockNavigationGroup navigationGroup = EspBlockNavigationController.nearestGroup();
        int navigationGroupId = navigationGroup == null ? -1 : navigationGroup.id();
        List<EspBlockCacheEntry> navigationEntries = navigationGroup == null
                ? List.of()
                : CACHE.entriesAt(navigationGroup.positions());
        RENDER_SELECTION.update(client, CACHE.spatialSnapshot(), navigationGroupId, navigationEntries);
    }
}
