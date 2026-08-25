package org.exmple.newbedwarshelper.client.fireballhelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FireballDangerRenderer {
    private static final int SIMILAR_COLOR_DISTANCE_SQUARED = 110 * 110;
    private static final float BLOCK_PADDING = 0.006F;

    private FireballDangerRenderer() {
    }

    public static void emit(Level level, Map<BlockPos, FireballDangerLevel> dangerBlocks) {
        List<FireballDangerLateRenderer.DangerBox> translucentBoxes = new ArrayList<>();
        for (Map.Entry<BlockPos, FireballDangerLevel> entry : dangerBlocks.entrySet()) {
            BlockPos pos = entry.getKey();
            FireballDangerLevel dangerLevel = entry.getValue();
            int color = chooseFillColor(level, pos, dangerLevel);
            AABB box = new AABB(pos).inflate(BLOCK_PADDING);
            if (hasTranslucentGeometry(level.getBlockState(pos), pos)) {
                translucentBoxes.add(new FireballDangerLateRenderer.DangerBox(box, color));
            } else {
                Gizmos.cuboid(box, GizmoStyle.fill(color));
            }
        }
        FireballDangerLateRenderer.prepare(translucentBoxes);
    }

    private static int chooseFillColor(Level level, BlockPos pos, FireballDangerLevel dangerLevel) {
        BlockState state = level.getBlockState(pos);
        int blockColor = state.getMapColor(level, pos).col;
        int dangerColor = dangerLevel.fillColor();
        return colorDistanceSquared(blockColor, dangerColor) <= SIMILAR_COLOR_DISTANCE_SQUARED
                ? dangerLevel.contrastFillColor()
                : dangerColor;
    }

    private static int colorDistanceSquared(int first, int second) {
        int red = ((first >> 16) & 0xFF) - ((second >> 16) & 0xFF);
        int green = ((first >> 8) & 0xFF) - ((second >> 8) & 0xFF);
        int blue = (first & 0xFF) - (second & 0xFF);
        return red * red + green * green + blue * blue;
    }

    private static boolean hasTranslucentGeometry(BlockState state, BlockPos pos) {
        BlockStateModel model = Minecraft.getInstance()
                .getModelManager()
                .getBlockStateModelSet()
                .get(state);
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(state.getSeed(pos)), parts);

        for (BlockStateModelPart part : parts) {
            if (hasTranslucentQuad(part.getQuads(null))) {
                return true;
            }
            for (Direction direction : Direction.values()) {
                if (hasTranslucentQuad(part.getQuads(direction))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasTranslucentQuad(List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            if (quad.materialInfo().layer() == ChunkSectionLayer.TRANSLUCENT) {
                return true;
            }
        }
        return false;
    }
}
