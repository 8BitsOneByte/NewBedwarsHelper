package org.exmple.newbedwarshelper.client.waterclutchhelper;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BeaconRenderer;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public final class WaterClutchLandingBeamRenderer {
    private static final int INVALID_AIM_COLOR = 0xFFFF2020;
    private static final int CONDITIONAL_AIM_COLOR = 0xFFFFD020;
    private static final int VALID_AIM_COLOR = 0xFF20FF40;
    private static BlockPos preparedLanding;
    private static float preparedAnimationTime;
    private static int preparedColor;

    private WaterClutchLandingBeamRenderer() {
    }

    public static void prepare(
            BlockPos landing,
            float animationTime,
            WaterClutchTargetResolver.AimStatus aimStatus
    ) {
        preparedLanding = landing.immutable();
        preparedAnimationTime = animationTime;
        preparedColor = switch (aimStatus) {
            case INVALID -> INVALID_AIM_COLOR;
            case CONDITIONAL -> CONDITIONAL_AIM_COLOR;
            case DIRECT -> VALID_AIM_COLOR;
        };
    }

    public static void clear() {
        preparedLanding = null;
    }

    public static void submit(Minecraft client, LevelRenderState levelState, SubmitNodeCollector output) {
        if (client.level == null || preparedLanding == null) {
            return;
        }

        Vec3 camera = levelState.cameraRenderState.pos;
        int startY = preparedLanding.getY() + 1;
        int height = Math.max(1, client.level.getMaxY() - startY);
        PoseStack poseStack = new PoseStack();
        poseStack.translate(
                preparedLanding.getX() - camera.x,
                startY - camera.y,
                preparedLanding.getZ() - camera.z
        );
        BeaconRenderer.submitBeaconBeam(
                poseStack,
                output,
                BeaconRenderer.BEAM_LOCATION,
                1.0F,
                preparedAnimationTime,
                0,
                height,
                preparedColor,
                BeaconRenderer.SOLID_BEAM_RADIUS,
                BeaconRenderer.BEAM_GLOW_RADIUS
        );
    }
}
