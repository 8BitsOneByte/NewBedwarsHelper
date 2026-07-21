package org.exmple.newbedwarshelper.client.esp.block.render.pipeline;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.world.phys.AABB;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

public final class EspBlockLineMesh {
    private static final int VERTEX_SIZE = DefaultVertexFormat.POSITION_COLOR.getVertexSize();
    private static final int INITIAL_VERTICES = 4096;
    private static final int INITIAL_INDICES = 8192;

    private ByteBuffer vertices;
    private ByteBuffer indices;
    private GpuBuffer vertexGpuBuffer;
    private GpuBuffer indexGpuBuffer;
    private int vertexCount;
    private int indexCount;

    public void begin() {
        this.vertexCount = 0;
        this.indexCount = 0;
        if (this.vertices == null || this.indices == null) {
            this.vertices = BufferUtils.createByteBuffer(INITIAL_VERTICES * VERTEX_SIZE);
            this.indices = BufferUtils.createByteBuffer(INITIAL_INDICES * Integer.BYTES);
        } else {
            this.vertices.clear();
            this.indices.clear();
        }
    }

    public void line(double x1, double y1, double z1, double x2, double y2, double z2, int argb, double cameraX, double cameraY, double cameraZ) {
        this.ensureCapacity(2, 2);
        int first = this.vertexCount;
        this.vertex(x1 - cameraX, y1 - cameraY, z1 - cameraZ, argb);
        this.vertex(x2 - cameraX, y2 - cameraY, z2 - cameraZ, argb);
        this.indices.putInt(first);
        this.indices.putInt(first + 1);
        this.indexCount += 2;
    }

    public void box(AABB box, int argb, double cameraX, double cameraY, double cameraZ) {
        this.line(box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, argb, cameraX, cameraY, cameraZ);
        this.line(box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, argb, cameraX, cameraY, cameraZ);
        this.line(box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, argb, cameraX, cameraY, cameraZ);
        this.line(box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, argb, cameraX, cameraY, cameraZ);
        this.line(box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, argb, cameraX, cameraY, cameraZ);
        this.line(box.minX, box.minY, box.maxZ, box.maxX, box.minY, box.maxZ, argb, cameraX, cameraY, cameraZ);
        this.line(box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, argb, cameraX, cameraY, cameraZ);
        this.line(box.minX, box.maxY, box.maxZ, box.maxX, box.maxY, box.maxZ, argb, cameraX, cameraY, cameraZ);
        this.line(box.minX, box.minY, box.minZ, box.minX, box.minY, box.maxZ, argb, cameraX, cameraY, cameraZ);
        this.line(box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, argb, cameraX, cameraY, cameraZ);
        this.line(box.minX, box.maxY, box.minZ, box.minX, box.maxY, box.maxZ, argb, cameraX, cameraY, cameraZ);
        this.line(box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, argb, cameraX, cameraY, cameraZ);
    }

    public boolean isEmpty() {
        return this.indexCount == 0;
    }

    public int lineCount() {
        return this.indexCount / 2;
    }

    public GpuBuffer uploadVertices() {
        ByteBuffer buffer = this.vertices.duplicate();
        buffer.flip();
        buffer.limit(this.vertexCount * VERTEX_SIZE);
        this.vertexGpuBuffer = upload(this.vertexGpuBuffer, buffer, GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST, "NewBedwarsHelper Block ESP vertices");
        return this.vertexGpuBuffer;
    }

    public GpuBuffer uploadIndices() {
        ByteBuffer buffer = this.indices.duplicate();
        buffer.flip();
        buffer.limit(this.indexCount * Integer.BYTES);
        this.indexGpuBuffer = upload(this.indexGpuBuffer, buffer, GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST, "NewBedwarsHelper Block ESP indices");
        return this.indexGpuBuffer;
    }

    public int indexCount() {
        return this.indexCount;
    }

    private void vertex(double x, double y, double z, int argb) {
        this.vertices.putFloat((float) x);
        this.vertices.putFloat((float) y);
        this.vertices.putFloat((float) z);
        this.vertices.put((byte) ((argb >> 16) & 0xFF));
        this.vertices.put((byte) ((argb >> 8) & 0xFF));
        this.vertices.put((byte) (argb & 0xFF));
        this.vertices.put((byte) ((argb >> 24) & 0xFF));
        this.vertexCount++;
    }

    private void ensureCapacity(int addedVertices, int addedIndices) {
        int requiredVertexBytes = (this.vertexCount + addedVertices) * VERTEX_SIZE;
        if (requiredVertexBytes > this.vertices.capacity()) {
            this.vertices = grow(this.vertices, requiredVertexBytes);
        }

        int requiredIndexBytes = (this.indexCount + addedIndices) * Integer.BYTES;
        if (requiredIndexBytes > this.indices.capacity()) {
            this.indices = grow(this.indices, requiredIndexBytes);
        }
    }

    private static ByteBuffer grow(ByteBuffer oldBuffer, int requiredBytes) {
        int oldPosition = oldBuffer.position();
        int newCapacity = oldBuffer.capacity();
        while (newCapacity < requiredBytes) {
            newCapacity *= 2;
        }

        ByteBuffer newBuffer = BufferUtils.createByteBuffer(newCapacity);
        oldBuffer.flip();
        newBuffer.put(oldBuffer);
        newBuffer.position(oldPosition);
        return newBuffer;
    }

    private static GpuBuffer upload(GpuBuffer target, ByteBuffer buffer, int usage, String label) {
        if (target == null || target.size() < buffer.remaining()) {
            if (target != null) {
                target.close();
            }
            return RenderSystem.getDevice().createBuffer(() -> label, usage, buffer);
        }

        RenderSystem.getDevice().createCommandEncoder().writeToBuffer(target.slice(0, buffer.remaining()), buffer);
        return target;
    }
}
