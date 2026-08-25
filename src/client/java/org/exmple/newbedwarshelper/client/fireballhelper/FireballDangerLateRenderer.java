package org.exmple.newbedwarshelper.client.fireballhelper;

import com.mojang.blaze3d.IndexType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

public final class FireballDangerLateRenderer {
    private static final Matrix4f IDENTITY_TEXTURE_MATRIX = new Matrix4f();
    private static final FireballDangerBoxMesh MESH = new FireballDangerBoxMesh();
    private static List<DangerBox> preparedBoxes = List.of();

    private FireballDangerLateRenderer() {
    }

    public static void prepare(List<DangerBox> boxes) {
        preparedBoxes = List.copyOf(boxes);
    }

    public static void render(Minecraft client, Matrix4fc modelViewMatrix) {
        if (client.level == null || preparedBoxes.isEmpty()) {
            return;
        }

        Vec3 camera = client.gameRenderer.mainCamera().position();
        MESH.begin();
        for (DangerBox dangerBox : preparedBoxes) {
            MESH.box(dangerBox.box(), dangerBox.color(), camera.x, camera.y, camera.z);
        }
        if (MESH.isEmpty()) {
            return;
        }

        GpuBuffer vertexBuffer = MESH.uploadVertices();
        GpuBuffer indexBuffer = MESH.uploadIndices();
        GpuBufferSlice dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
                new Matrix4f(modelViewMatrix),
                new Vector4f(1.0F, 1.0F, 1.0F, 1.0F),
                new Vector3f(),
                IDENTITY_TEXTURE_MATRIX
        );
        RenderTarget mainRenderTarget = client.gameRenderer.mainRenderTarget();

        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "NewBedwarsHelper translucent fireball danger",
                mainRenderTarget.getColorTextureView(),
                Optional.empty(),
                mainRenderTarget.getDepthTextureView(),
                OptionalDouble.empty()
        )) {
            pass.setPipeline(FireballDangerRenderPipelines.TRANSLUCENT_BLOCK_FILL);
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", dynamicTransforms);
            pass.setVertexBuffer(0, vertexBuffer.slice());
            pass.setIndexBuffer(indexBuffer, IndexType.INT);
            pass.drawIndexed(MESH.indexCount(), 1, 0, 0, 0);
        }
    }

    public record DangerBox(AABB box, int color) {
    }
}
