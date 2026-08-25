package org.exmple.newbedwarshelper.client.fireballhelper;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.world.phys.AABB;
import org.lwjgl.BufferUtils;

import java.nio.ByteBuffer;

public final class FireballDangerBoxMesh {
    private static final int VERTEX_SIZE = DefaultVertexFormat.POSITION_COLOR.getVertexSize();
    private static final int INITIAL_VERTICES = 512;
    private static final int INITIAL_INDICES = 512;

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

    public void box(AABB box, int color, double cameraX, double cameraY, double cameraZ) {
        double x1 = box.minX - cameraX;
        double y1 = box.minY - cameraY;
        double z1 = box.minZ - cameraZ;
        double x2 = box.maxX - cameraX;
        double y2 = box.maxY - cameraY;
        double z2 = box.maxZ - cameraZ;

        quad(x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, color);
        quad(x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, color);
        quad(x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, color);
        quad(x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, color);
        quad(x1, y1, z2, x1, y1, z1, x2, y1, z1, x2, y1, z2, color);
        quad(x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, color);
    }

    public boolean isEmpty() {
        return this.indexCount == 0;
    }

    public GpuBuffer uploadVertices() {
        ByteBuffer buffer = this.vertices.duplicate();
        buffer.flip();
        buffer.limit(this.vertexCount * VERTEX_SIZE);
        this.vertexGpuBuffer = upload(
                this.vertexGpuBuffer,
                buffer,
                GpuBuffer.USAGE_VERTEX | GpuBuffer.USAGE_COPY_DST,
                "NewBedwarsHelper fireball danger vertices"
        );
        return this.vertexGpuBuffer;
    }

    public GpuBuffer uploadIndices() {
        ByteBuffer buffer = this.indices.duplicate();
        buffer.flip();
        buffer.limit(this.indexCount * Integer.BYTES);
        this.indexGpuBuffer = upload(
                this.indexGpuBuffer,
                buffer,
                GpuBuffer.USAGE_INDEX | GpuBuffer.USAGE_COPY_DST,
                "NewBedwarsHelper fireball danger indices"
        );
        return this.indexGpuBuffer;
    }

    public int indexCount() {
        return this.indexCount;
    }

    private void quad(
            double ax, double ay, double az,
            double bx, double by, double bz,
            double cx, double cy, double cz,
            double dx, double dy, double dz,
            int color
    ) {
        ensureCapacity(4, 6);
        int first = this.vertexCount;
        vertex(ax, ay, az, color);
        vertex(bx, by, bz, color);
        vertex(cx, cy, cz, color);
        vertex(dx, dy, dz, color);
        this.indices.putInt(first);
        this.indices.putInt(first + 1);
        this.indices.putInt(first + 2);
        this.indices.putInt(first);
        this.indices.putInt(first + 3);
        this.indices.putInt(first + 2);
        this.indexCount += 6;
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
