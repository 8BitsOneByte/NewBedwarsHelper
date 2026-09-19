package org.exmple.newbedwarshelper.client.waterclutchhelper;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.HayBlock;
import net.minecraft.world.level.block.HoneyBlock;
import net.minecraft.world.level.block.PointedDripstoneBlock;
import net.minecraft.world.level.block.PowderSnowBlock;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.exmple.newbedwarshelper.client.z_debug.waterclutch.WaterClutchDebugger;

public final class WaterClutchFallPredictor {
    private static final double MAX_SCAN_DISTANCE = 128.0;
    private static final double EPSILON = 1.0E-6;
    private static final int MAX_PREDICTION_TICKS = 200;
    private static final double HORIZONTAL_AIR_DRAG = 0.91;
    private static final double NATURAL_FALL_GRAVITY = 0.08;
    private static final double NATURAL_FALL_DRAG = 0.98;

    private WaterClutchFallPredictor() {
    }

    public static Optional<WaterClutchLandingAssessment> assess(Player player, Level level) {
        if (player.getDeltaMovement().y >= 0.0
                || player.onGround()
                || player.hasEffect(MobEffects.SLOW_FALLING)
                || player.hasEffect(MobEffects.LEVITATION)) {
            WaterClutchDebugger.event("predictor-skip vertical=%.3f onGround=%s slowFalling=%s levitation=%s",
                    player.getDeltaMovement().y, player.onGround(), player.hasEffect(MobEffects.SLOW_FALLING),
                    player.hasEffect(MobEffects.LEVITATION));
            return Optional.empty();
        }

        PredictedLanding predictedLanding = findPredictedLanding(player, level);
        if (predictedLanding == null) {
            WaterClutchDebugger.event("predictor-skip no-collision-within=%.1f", MAX_SCAN_DISTANCE);
            return Optional.empty();
        }

        double landingY = predictedLanding.box().minY;
        double distance = player.getBoundingBox().minY - landingY;
        LandingBlock landing = predictedLanding.block();
        WaterClutchDebugger.event(
                "predictor-path ticks=%d landingCenter=(%.3f,%.3f) horizontalDelta=(%.3f,%.3f)",
                predictedLanding.ticks(),
                predictedLanding.box().getCenter().x,
                predictedLanding.box().getCenter().z,
                predictedLanding.box().getCenter().x - player.getBoundingBox().getCenter().x,
                predictedLanding.box().getCenter().z - player.getBoundingBox().getCenter().z);
        traceSnowNearLanding(player, level, landingY, landing, predictedLanding.box());

        double projectedFallDistance = predictedLanding.projectedFallDistance();

        BlockState state = landing.state();
        if (state.getBlock() instanceof SlimeBlock || state.getBlock() instanceof PowderSnowBlock) {
            WaterClutchDebugger.event("predictor-safe landing-block=%s", state.getBlock());
            return Optional.of(new WaterClutchLandingAssessment(landing.pos(), state, predictedLanding.box(), distance, landingY, 0));
        }

        if (state.getBlock() instanceof BedBlock) {
            projectedFallDistance *= 0.5;
        } else if (state.getBlock() instanceof PointedDripstoneBlock
                && state.getValue(PointedDripstoneBlock.TIP_DIRECTION) == Direction.UP) {
            projectedFallDistance += 2.5;
        }

        if (player.isIgnoringFallDamageFromCurrentImpulse() && player.currentImpulseImpactPos != null) {
            projectedFallDistance = Math.min(projectedFallDistance, player.currentImpulseImpactPos.y - landingY);
        }

        double blockModifier = 1.0;
        if (state.getBlock() instanceof HayBlock || state.getBlock() instanceof HoneyBlock) {
            blockModifier = 0.2;
        } else if (state.getBlock() instanceof PointedDripstoneBlock
                && state.getValue(PointedDripstoneBlock.TIP_DIRECTION) == Direction.UP) {
            blockModifier = 2.0;
        }

        double safeDistance = player.getAttributeValue(Attributes.SAFE_FALL_DISTANCE);
        double playerModifier = player.getAttributeValue(Attributes.FALL_DAMAGE_MULTIPLIER);
        int damage = (int)Math.floor((projectedFallDistance + EPSILON - safeDistance) * blockModifier * playerModifier);
        damage = Math.max(0, damage);
        WaterClutchDebugger.event("predictor-result projectedFall=%.3f safeDistance=%.3f blockModifier=%.3f playerModifier=%.3f damage=%d",
                projectedFallDistance, safeDistance, blockModifier, playerModifier, damage);
        return Optional.of(new WaterClutchLandingAssessment(landing.pos(), state, predictedLanding.box(), distance, landingY, damage));
    }

    private static PredictedLanding findPredictedLanding(Player player, Level level) {
        AABB initialBox = player.getBoundingBox();
        AABB box = initialBox;
        Vec3 velocity = player.getDeltaMovement();
        double projectedFallDistance = player.fallDistance;

        for (int tick = 1; tick <= MAX_PREDICTION_TICKS; tick++) {
            Vec3 movement = Entity.collideBoundingBox(player, velocity, box, level, List.of());
            AABB movedBox = box.move(movement);
            if (movement.y < 0.0) {
                projectedFallDistance += -movement.y;
            }
            if (intersectsFallDistanceReset(level, movedBox)) {
                projectedFallDistance = 0.0;
                WaterClutchDebugger.event("predictor-path fall-reset tick=%d minY=%.3f", tick, movedBox.minY);
            }
            boolean landed = velocity.y < 0.0 && movement.y > velocity.y + EPSILON;
            if (landed) {
                LandingBlock block = findLandingBlock(player, level, movedBox, movedBox.minY);
                return block == null ? null : new PredictedLanding(movedBox, block, tick, projectedFallDistance);
            }

            box = movedBox;
            if (initialBox.minY - box.minY >= MAX_SCAN_DISTANCE) {
                return null;
            }

            double nextX = Math.abs(movement.x - velocity.x) <= EPSILON ? velocity.x * HORIZONTAL_AIR_DRAG : 0.0;
            double nextZ = Math.abs(movement.z - velocity.z) <= EPSILON ? velocity.z * HORIZONTAL_AIR_DRAG : 0.0;
            double nextY = (velocity.y - NATURAL_FALL_GRAVITY) * NATURAL_FALL_DRAG;
            velocity = new Vec3(nextX, nextY, nextZ);
        }
        return null;
    }

    private static boolean intersectsFallDistanceReset(Level level, AABB box) {
        int minX = (int)Math.floor(box.minX + EPSILON);
        int maxX = (int)Math.floor(box.maxX - EPSILON);
        int minY = (int)Math.floor(box.minY + EPSILON);
        int maxY = (int)Math.floor(box.maxY - EPSILON);
        int minZ = (int)Math.floor(box.minZ + EPSILON);
        int maxZ = (int)Math.floor(box.maxZ - EPSILON);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (state.getBlock() instanceof WebBlock) {
                        return true;
                    }
                    if (state.getFluidState().is(FluidTags.WATER)) {
                        double fluidTop = y + state.getFluidState().getHeight(level, cursor);
                        if (box.minY < fluidTop - EPSILON && box.maxY > y + EPSILON) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static LandingBlock findLandingBlock(Player player, Level level, AABB box, double landingY) {
        int minX = (int)Math.floor(box.minX + EPSILON);
        int maxX = (int)Math.floor(box.maxX - EPSILON);
        int minZ = (int)Math.floor(box.minZ + EPSILON);
        int maxZ = (int)Math.floor(box.maxZ - EPSILON);
        int centerY = (int)Math.floor(landingY);
        CollisionContext context = CollisionContext.of(player);
        LandingBlock best = null;
        double bestTop = Double.NEGATIVE_INFINITY;
        double bestHorizontalDistance = Double.POSITIVE_INFINITY;
        double centerX = box.getCenter().x;
        double centerZ = box.getCenter().z;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int y = centerY - 1; y <= centerY + 1; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    VoxelShape shape = state.getCollisionShape(level, cursor, context).move(x, y, z);
                    for (AABB collision : shape.toAabbs()) {
                        boolean horizontalOverlap = collision.maxX > box.minX + EPSILON
                                && collision.minX < box.maxX - EPSILON
                                && collision.maxZ > box.minZ + EPSILON
                                && collision.minZ < box.maxZ - EPSILON;
                        if (horizontalOverlap
                                && collision.maxY <= box.minY + EPSILON
                                && Math.abs(collision.maxY - landingY) < 1.0E-4) {
                            double horizontalDistance = horizontalDistanceSquared(centerX, centerZ, collision);
                            if (collision.maxY > bestTop + EPSILON
                                    || Math.abs(collision.maxY - bestTop) <= EPSILON
                                    && horizontalDistance < bestHorizontalDistance - EPSILON) {
                                bestTop = collision.maxY;
                                bestHorizontalDistance = horizontalDistance;
                                best = new LandingBlock(cursor.immutable(), state);
                            }
                        }
                    }
                }
            }
        }
        return best;
    }

    private static double horizontalDistanceSquared(double x, double z, AABB collision) {
        double dx = x < collision.minX ? collision.minX - x : x > collision.maxX ? x - collision.maxX : 0.0;
        double dz = z < collision.minZ ? collision.minZ - z : z > collision.maxZ ? z - collision.maxZ : 0.0;
        return dx * dx + dz * dz;
    }

    private static void traceSnowNearLanding(
            Player player,
            Level level,
            double landingY,
            LandingBlock landing,
            AABB box
    ) {
        int minX = (int)Math.floor(box.minX + EPSILON);
        int maxX = (int)Math.floor(box.maxX - EPSILON);
        int minZ = (int)Math.floor(box.minZ + EPSILON);
        int maxZ = (int)Math.floor(box.maxZ - EPSILON);
        int centerY = (int)Math.floor(landingY);
        CollisionContext context = CollisionContext.of(player);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int y = centerY - 1; y <= centerY + 1; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    cursor.set(x, y, z);
                    BlockState state = level.getBlockState(cursor);
                    if (!(state.getBlock() instanceof SnowLayerBlock)) {
                        continue;
                    }

                    VoxelShape outline = state.getShape(level, cursor, context);
                    VoxelShape collision = state.getCollisionShape(level, cursor, context);
                    WaterClutchDebugger.event(
                            "snow-predict role=%s pos=%s layers=%d landingY=%.3f outlineTop=%.3f collisionTop=%s collisionEmpty=%s",
                            cursor.equals(landing.pos()) ? "landing" : "footprint",
                            cursor.immutable(),
                            state.getValue(SnowLayerBlock.LAYERS),
                            landingY,
                            cursor.getY() + shapeTop(outline),
                            collision.isEmpty() ? "none" : String.format(java.util.Locale.ROOT, "%.3f", cursor.getY() + shapeTop(collision)),
                            collision.isEmpty());
                }
            }
        }
    }

    private static double shapeTop(VoxelShape shape) {
        return shape.isEmpty() ? 0.0 : shape.max(Direction.Axis.Y);
    }

    private record LandingBlock(BlockPos pos, BlockState state) {
    }

    private record PredictedLanding(AABB box, LandingBlock block, int ticks, double projectedFallDistance) {
    }
}
