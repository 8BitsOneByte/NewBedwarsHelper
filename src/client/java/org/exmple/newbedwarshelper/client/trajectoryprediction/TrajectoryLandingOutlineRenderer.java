package org.exmple.newbedwarshelper.client.trajectoryprediction;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.exmple.newbedwarshelper.client.esp.block.render.pipeline.EspBlockLineMesh;
import org.exmple.newbedwarshelper.client.esp.block.render.pipeline.EspBlockLineRenderer;
import org.exmple.newbedwarshelper.client.esp.block.render.pipeline.EspBlockRenderPipelines;
import org.joml.Matrix4fc;

public final class TrajectoryLandingOutlineRenderer {
    private static final int MAX_BOXES = 48;
    private static final AABB[] BOXES = new AABB[MAX_BOXES];
    private static final int[] COLORS = new int[MAX_BOXES];
    private static final EspBlockLineMesh MESH = new EspBlockLineMesh();

    private static int boxCount;

    private TrajectoryLandingOutlineRenderer() {
    }

    public static void beginFrame() {
        boxCount = 0;
    }

    public static void addBox(AABB box, int color) {
        if (boxCount >= MAX_BOXES) {
            return;
        }

        BOXES[boxCount] = box;
        COLORS[boxCount] = color;
        boxCount++;
    }

    public static void render(Minecraft client, Matrix4fc modelViewMatrix) {
        if (boxCount == 0 || client.level == null) {
            return;
        }

        Vec3 camera = client.gameRenderer.mainCamera().position();
        MESH.begin();
        for (int index = 0; index < boxCount; index++) {
            MESH.box(BOXES[index], COLORS[index], camera.x, camera.y, camera.z);
        }

        // Reuse the Block ESP pass: ALWAYS_PASS depth comparison plus disabled depth
        // writes makes the landing outline visible through blocks without leaking state.
        EspBlockLineRenderer.render(MESH, modelViewMatrix, EspBlockRenderPipelines.BLOCK_ESP_LINES);
    }
}
