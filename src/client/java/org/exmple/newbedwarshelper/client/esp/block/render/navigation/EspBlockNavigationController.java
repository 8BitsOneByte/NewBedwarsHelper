package org.exmple.newbedwarshelper.client.esp.block.render.navigation;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.exmple.newbedwarshelper.client.esp.block.render.EspBlockCacheEntry;
import org.exmple.newbedwarshelper.client.esp.block.render.EspBlockEspController;

import java.util.concurrent.atomic.AtomicBoolean;

public final class EspBlockNavigationController {
    private static final EspBlockNavigationIndex INDEX = new EspBlockNavigationIndex();
    private static final AtomicBoolean DIRTY = new AtomicBoolean(true);
    private static EspBlockNavigationGroup nearestGroup;
    private static BlockPos suppressedPos;

    private EspBlockNavigationController() {
    }

    public static void markDirty() {
        DIRTY.set(true);
    }

    public static void update(Minecraft client) {
        nearestGroup = null;
        suppressedPos = null;

        if (!EspBlockNavigationConstants.ENABLED || client.level == null || client.player == null) {
            return;
        }

        if (DIRTY.getAndSet(false)) {
            INDEX.rebuild(EspBlockEspController.snapshot());
        }

        Vec3 origin = client.gameRenderer.mainCamera().position();
        nearestGroup = INDEX.nearestNavigableGroup(origin);
        if (nearestGroup == null) {
            return;
        }

        HitResult hitResult = client.hitResult;
        if (hitResult instanceof BlockHitResult blockHitResult) {
            BlockPos hitPos = blockHitResult.getBlockPos();
            EspBlockNavigationGroup hitGroup = INDEX.groupAt(hitPos);
            if (hitGroup != null && hitGroup.id() == nearestGroup.id()) {
                suppressedPos = hitPos.immutable();
            }
        }
    }

    public static boolean shouldUseNavigationColor(EspBlockCacheEntry entry) {
        return nearestGroup != null
                && nearestGroup.contains(entry.pos());
    }

    public static boolean shouldUseAimedNavigationColor(EspBlockCacheEntry entry) {
        return suppressedPos != null && entry.pos().equals(suppressedPos);
    }

    public static EspBlockNavigationGroup nearestGroup() {
        return nearestGroup;
    }

}
