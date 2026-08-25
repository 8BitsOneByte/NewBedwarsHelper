package org.exmple.newbedwarshelper.client.fireballhelper;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public final class FireballDangerVolume {
    private FireballDangerVolume() {
    }

    public static void mergeFullBlocks(
            Level level,
            FireballDangerPrediction prediction,
            Map<BlockPos, FireballDangerLevel> dangerBlocks
    ) {
        Vec3 center = prediction.center();
        double radius = prediction.radius();
        double radiusSquared = radius * radius;
        int minX = Mth.floor(center.x - radius);
        int minY = Mth.floor(center.y - radius);
        int minZ = Mth.floor(center.z - radius);
        int maxX = Mth.floor(center.x + radius);
        int maxY = Mth.floor(center.y + radius);
        int maxZ = Mth.floor(center.z + radius);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (!state.isCollisionShapeFullBlock(level, pos)) {
                        continue;
                    }

                    if (new AABB(pos).distanceToSqr(center) > radiusSquared) {
                        continue;
                    }

                    dangerBlocks.merge(pos, prediction.level(), FireballDangerVolume::higherPriority);
                }
            }
        }
    }

    private static FireballDangerLevel higherPriority(FireballDangerLevel first, FireballDangerLevel second) {
        return first.ordinal() >= second.ordinal() ? first : second;
    }
}
