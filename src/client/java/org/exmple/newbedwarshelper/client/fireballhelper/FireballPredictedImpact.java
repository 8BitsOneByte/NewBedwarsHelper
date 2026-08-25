package org.exmple.newbedwarshelper.client.fireballhelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public record FireballPredictedImpact(Vec3 location, double beamX, double beamZ) {
    public static FireballPredictedImpact block(BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        return new FireballPredictedImpact(hit.getLocation(), pos.getX() + 0.5D, pos.getZ() + 0.5D);
    }

    public static FireballPredictedImpact player(Vec3 location) {
        return new FireballPredictedImpact(location, location.x, location.z);
    }
}
