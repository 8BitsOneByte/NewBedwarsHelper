package org.exmple.newbedwarshelper.client.esp.entity.player;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.phase.FeatureRenderPhase;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class PlayerThroughWallRenderer {
    private PlayerThroughWallRenderer() {
    }

    public static void submitAlwaysOnTop(
            Minecraft minecraft,
            LevelRenderState levelState,
            SubmitNodeCollector output
    ) {
        int playerStateCount = countPlayerStates(levelState);

        if (!PlayerThroughWallEsp.shouldRender() || minecraft.level == null || playerStateCount == 0) {
            return;
        }

        if (!(output instanceof SubmitNodeStorage target)) {
            return;
        }

        SubmitNodeStorage playerSubmits = new SubmitNodeStorage();
        submitPlayers(minecraft.getEntityRenderDispatcher(), levelState, playerSubmits);

        routeToAlwaysOnTop(playerSubmits, target);
    }

    private static int countPlayerStates(LevelRenderState levelState) {
        int count = 0;
        for (EntityRenderState state : levelState.entityRenderStates) {
            if (isPlayerState(state)) {
                count++;
            }
        }
        return count;
    }

    private static boolean isPlayerState(EntityRenderState state) {
        return state instanceof AvatarRenderState && state.entityType == EntityTypes.PLAYER;
    }

    private static void submitPlayers(
            EntityRenderDispatcher dispatcher,
            LevelRenderState levelState,
            SubmitNodeStorage output
    ) {
        CameraRenderState camera = levelState.cameraRenderState;
        Vec3 cameraPosition = camera.pos;
        PoseStack poseStack = new PoseStack();

        for (EntityRenderState state : levelState.entityRenderStates) {
            if (isPlayerState(state)) {
                submitPlayerModel(dispatcher, state, camera, cameraPosition, poseStack, output);
            }
        }
    }

    private static void submitPlayerModel(
            EntityRenderDispatcher dispatcher,
            EntityRenderState state,
            CameraRenderState camera,
            Vec3 cameraPosition,
            PoseStack poseStack,
            SubmitNodeStorage output
    ) {
        int originalOutlineColor = state.outlineColor;
        Component originalNameTag = state.nameTag;
        Component originalScoreText = state.scoreText;
        boolean originalDisplayFireAnimation = state.displayFireAnimation;
        List<EntityRenderState.ShadowPiece> originalShadowPieces = List.copyOf(state.shadowPieces);
        List<EntityRenderState.LeashState> originalLeashStates = state.leashStates;

        state.outlineColor = EntityRenderState.NO_OUTLINE;
        state.nameTag = null;
        state.scoreText = null;
        state.displayFireAnimation = false;
        state.shadowPieces.clear();
        state.leashStates = null;

        try {
            dispatcher.submit(
                    state,
                    camera,
                    state.x - cameraPosition.x,
                    state.y - cameraPosition.y,
                    state.z - cameraPosition.z,
                    poseStack,
                    output
            );
        } finally {
            state.outlineColor = originalOutlineColor;
            state.nameTag = originalNameTag;
            state.scoreText = originalScoreText;
            state.displayFireAnimation = originalDisplayFireAnimation;
            state.shadowPieces.addAll(originalShadowPieces);
            state.leashStates = originalLeashStates;
        }
    }

    private static void routeToAlwaysOnTop(SubmitNodeStorage source, SubmitNodeStorage target) {
        for (int order : source.getSubmitsPerOrder().keySet()) {
            SubmitNodeCollection sourceCollection = source.getSubmitsPerOrder().get(order);
            SubmitNodeCollection targetCollection = target.order(order);

            for (FeatureRenderPhase<?> phase : sourceCollection.allPhases()) {
                phase.sortInto((submit, strictlyOrdered) -> targetCollection.alwaysOnTop.submit(submit));
            }
        }
    }
}
