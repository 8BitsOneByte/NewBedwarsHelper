package org.exmple.newbedwarshelper.client.fireballhelper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

public final class FireballImpactBeamRenderer {
    private static final int BEAM_COLOR = 0xFFFF3030;
    private static final float SOLID_RADIUS = BeaconRenderer.SOLID_BEAM_RADIUS;
    private static final float GLOW_RADIUS = BeaconRenderer.BEAM_GLOW_RADIUS;

    private static Set<BeamColumn> preparedImpactColumns = Set.of();
    private static float preparedAnimationTime;

    private FireballImpactBeamRenderer() {
    }

    public static void prepare(Set<BeamColumn> impactColumns, float animationTime) {
        preparedImpactColumns = Set.copyOf(impactColumns);
        preparedAnimationTime = animationTime;
    }

    public static void submit(
            Minecraft client,
            LevelRenderState levelState,
            SubmitNodeCollector output
    ) {
        if (client.level == null || preparedImpactColumns.isEmpty()) {
            return;
        }

        int minY = client.level.getMinY();
        int height = client.level.getMaxY() - minY;
        Vec3 camera = levelState.cameraRenderState.pos;
        PoseStack poseStack = new PoseStack();

        for (BeamColumn impactColumn : preparedImpactColumns) {
            poseStack.pushPose();
            poseStack.translate(
                    impactColumn.x() - 0.5D - camera.x,
                    minY - camera.y,
                    impactColumn.z() - 0.5D - camera.z
            );
            BeaconRenderer.submitBeaconBeam(
                    poseStack,
                    output,
                    BeaconRenderer.BEAM_LOCATION,
                    1.0F,
                    preparedAnimationTime,
                    0,
                    height,
                    BEAM_COLOR,
                    SOLID_RADIUS,
                    GLOW_RADIUS
            );
            poseStack.popPose();
        }
    }

    public record BeamColumn(double x, double z) {
    }
}
