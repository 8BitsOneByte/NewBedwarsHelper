package org.exmple.newbedwarshelper.client.fireballhelper;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FireballHelper {
    private static final double SELF_DANGER_RADIUS = 0.9D;
    private static final double OTHER_DANGER_RADIUS = 2.0D;
    private static final double IN_FLIGHT_DANGER_RADIUS = 2.0D;
    private static final int TARGET_FILL_COLOR = ARGB.colorFromFloat(0.48F, 0.42F, 0.12F, 0.95F);
    private static final int TARGET_OUTLINE_COLOR = 0xFF57FFE1;
    private static final float TARGET_PADDING = 0.02F;

    private FireballHelper() {
    }

    public static void emitDangerPrediction(Minecraft client, float partialTicks) {
        if (client.level == null || client.player == null) {
            return;
        }

        Map<BlockPos, FireballDangerLevel> dangerBlocks = new HashMap<>();
        Set<FireballImpactBeamRenderer.BeamColumn> projectileImpactColumns = new HashSet<>();
        FireballTarget ownTarget = null;

        if (isHoldingFireCharge(client.player)) {
            ownTarget = FireballRaycast.findTargetBlock(client.player, partialTicks).orElse(null);
            FireballRaycast.findAimImpact(client.player, partialTicks)
                    .map(impact -> new FireballDangerPrediction(
                            impact.location(),
                            SELF_DANGER_RADIUS,
                            FireballDangerLevel.SELF_AIM
                    ))
                    .ifPresent(prediction -> FireballDangerVolume.mergeFullBlocks(
                            client.level,
                            prediction,
                            dangerBlocks
                    ));
        }

        for (Player player : client.level.players()) {
            if (player == client.player || !player.isAlive() || !isHoldingFireCharge(player)) {
                continue;
            }

            FireballRaycast.findAimImpact(player, partialTicks)
                    .map(impact -> new FireballDangerPrediction(
                            impact.location(),
                            OTHER_DANGER_RADIUS,
                            FireballDangerLevel.OTHER_AIM
                    ))
                    .ifPresent(prediction -> FireballDangerVolume.mergeFullBlocks(
                            client.level,
                            prediction,
                            dangerBlocks
                    ));
        }

        client.level.getEntities(
                EntityTypes.FIREBALL,
                client.player.getBoundingBox().inflate(FireballRaycast.RANGE),
                fireball -> fireball.isAlive()
        ).forEach(fireball -> FireballRaycast.findProjectileImpact(fireball, partialTicks)
                .ifPresent(impact -> {
                    projectileImpactColumns.add(new FireballImpactBeamRenderer.BeamColumn(
                            impact.beamX(),
                            impact.beamZ()
                    ));
                    FireballDangerVolume.mergeFullBlocks(
                            client.level,
                            new FireballDangerPrediction(
                                    impact.location(),
                                    IN_FLIGHT_DANGER_RADIUS,
                                    FireballDangerLevel.IN_FLIGHT
                            ),
                            dangerBlocks
                    );
                }));

        FireballImpactBeamRenderer.prepare(
                projectileImpactColumns,
                Math.floorMod(client.level.getGameTime(), 40L) + partialTicks
        );
        FireballDangerRenderer.emit(client.level, dangerBlocks);
        if (ownTarget != null) {
            emitTargetShape(ownTarget);
        }
    }

    private static void emitTargetShape(FireballTarget target) {
        BlockPos pos = target.pos();
        for (AABB box : target.shape().toAabbs()) {
            AABB worldBox = box.move(pos).inflate(TARGET_PADDING);
            Gizmos.cuboid(worldBox, GizmoStyle.fill(TARGET_FILL_COLOR));
            Gizmos.cuboid(worldBox, GizmoStyle.stroke(TARGET_OUTLINE_COLOR));
        }
    }

    private static boolean isHoldingFireCharge(Player player) {
        return player.getMainHandItem().is(Items.FIRE_CHARGE)
                || player.getOffhandItem().is(Items.FIRE_CHARGE);
    }
}
