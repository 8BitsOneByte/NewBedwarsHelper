package org.exmple.newbedwarshelper.client.waterclutchhelper;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public record WaterClutchLandingAssessment(
        BlockPos landingBlockPos,
        BlockState landingBlockState,
        AABB landingBox,
        double distanceToLanding,
        double landingY,
        int predictedFallDamage
) {
    public boolean needsProtection() {
        return this.predictedFallDamage > 0;
    }
}
