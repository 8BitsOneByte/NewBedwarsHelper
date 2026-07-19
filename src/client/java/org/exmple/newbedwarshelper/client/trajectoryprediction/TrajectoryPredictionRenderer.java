package org.exmple.newbedwarshelper.client.trajectoryprediction;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.ARGB;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.exmple.newbedwarshelper.client.trajectoryprediction.TrajectoryPredictionSimulator.HitType;
import org.exmple.newbedwarshelper.client.trajectoryprediction.TrajectoryPredictionSimulator.Path;
import org.exmple.newbedwarshelper.client.trajectoryprediction.TrajectoryPredictionSimulator.PredictionFrame;
import org.exmple.newbedwarshelper.client.z_config.ModConfig;

public final class TrajectoryPredictionRenderer {
    private static final float TARGET_PADDING = 0.0125F;

    private TrajectoryPredictionRenderer() {
    }

    public static void emit(Minecraft client, float partialTicks) {
        ModConfig.TrajectoryPredictionConfig config = ModConfig.getInstance().trajectoryPrediction;
        // This is intentionally before item classification and dummy-entity construction:
        // with both toggles off there is no simulation, collision query, or gizmo emission.
        if (!Boolean.TRUE.equals(config.projectileEnabled) && !Boolean.TRUE.equals(config.arrowEnabled)) {
            return;
        }
        if (client.level == null || client.player == null) {
            return;
        }

        PredictionFrame frame = TrajectoryPredictionSimulator.predict(client, partialTicks, config);
        for (int pathIndex = 0; pathIndex < frame.pathCount(); pathIndex++) {
            Path path = frame.path(pathIndex);
            renderPath(path, config);
            renderImpact(client, path, config);
        }
    }

    private static void renderPath(Path path, ModConfig.TrajectoryPredictionConfig config) {
        int pointCount = path.pointCount();
        if (pointCount < 2) {
            return;
        }

        int rgb = path.isArrow() ? config.arrowColor : config.projectileColor;
        float width = path.isArrow() ? config.lineWidth * 0.9F : config.lineWidth;
        // LevelExtractor consumes these depth-tested Gizmos in its normal world pipeline.
        // Gizmos own their blend/shader buffers, so this module does not leak render state.
        for (int pointIndex = 1; pointIndex < pointCount; pointIndex++) {
            float progress = pointIndex / (float)(pointCount - 1);
            float alpha = config.transparency * (0.38F + 0.62F * (float)Math.sqrt(progress));
            Gizmos.line(path.point(pointIndex - 1), path.point(pointIndex), ARGB.color(alpha, rgb), width);
        }
    }

    private static void renderImpact(Minecraft client, Path path, ModConfig.TrajectoryPredictionConfig config) {
        if (path.hitType() == HitType.NONE || path.hitPosition() == null) {
            return;
        }

        int rgb = path.isArrow() ? config.arrowColor : config.projectileColor;
        int impactColor = ARGB.color(config.transparency, rgb);
        float width = Math.max(1.0F, config.lineWidth);
        Gizmos.point(path.hitPosition(), impactColor, width * 2.4F);

        if (path.hitType() == HitType.ENTITY) {
            return;
        }
        if (path.worldBorderHit() || path.hitBlockPos() == null) {
            return;
        }

        BlockPos pos = path.hitBlockPos();
        BlockState state = client.level.getBlockState(pos);
        VoxelShape shape = path.hitShape();
        if (shape == null || shape.isEmpty()) {
            shape = state.getShape(client.level, pos);
        }

        int fillColor = ARGB.color(config.transparency * 0.14F, rgb);
        GizmoStyle style = GizmoStyle.strokeAndFill(impactColor, width, fillColor);
        if (shape.isEmpty()) {
            Gizmos.cuboid(new AABB(pos).inflate(TARGET_PADDING), style);
        } else {
            for (AABB box : shape.toAabbs()) {
                Gizmos.cuboid(box.move(pos).inflate(TARGET_PADDING), style);
            }
        }

        if (path.hitFace() != null) {
            Vec3 normal = path.hitFace().getUnitVec3();
            Gizmos.line(path.hitPosition(), path.hitPosition().add(normal.scale(0.16D)), impactColor, width);
        }
    }
}
