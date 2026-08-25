package org.exmple.newbedwarshelper.client.fireballhelper;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

public final class FireballRaycast {
    public static final double RANGE = 300.0D;
    private static final double FIREBALL_HORIZONTAL_RADIUS = EntityTypes.FIREBALL.getDimensions().width() * 0.5D;
    private static final double FIREBALL_VERTICAL_RADIUS = EntityTypes.FIREBALL.getDimensions().height() * 0.5D;

    private FireballRaycast() {
    }

    public static Optional<FireballTarget> findTargetBlock(Player player, float partialTicks) {
        HitResult hitResult = player.pick(RANGE, partialTicks, false);
        if (hitResult instanceof BlockHitResult blockHitResult && blockHitResult.getType() == HitResult.Type.BLOCK) {
            Level level = player.level();
            BlockState state = level.getBlockState(blockHitResult.getBlockPos());
            VoxelShape shape = state.getShape(level, blockHitResult.getBlockPos(), CollisionContext.of(player));
            if (!shape.isEmpty()) {
                return Optional.of(new FireballTarget(blockHitResult.getBlockPos(), shape));
            }
        }

        return Optional.empty();
    }

    public static Optional<FireballPredictedImpact> findAimImpact(Player player, float partialTicks) {
        Vec3 start = player.getEyePosition(partialTicks);
        Vec3 end = start.add(player.getViewVector(partialTicks).scale(RANGE));
        return findNearestImpact(player.level(), player, player, start, end, partialTicks);
    }

    public static Optional<FireballPredictedImpact> findProjectileImpact(Projectile projectile, float partialTicks) {
        Vec3 movement = projectile.getDeltaMovement();
        if (movement.lengthSqr() < 1.0E-8D) {
            return Optional.empty();
        }

        Vec3 start = projectile.getPosition(partialTicks);
        Vec3 end = start.add(movement.normalize().scale(RANGE));
        return findNearestImpact(projectile.level(), projectile, projectile.getOwner(), start, end, partialTicks);
    }

    private static Optional<FireballPredictedImpact> findNearestImpact(
            Level level,
            Entity source,
            Entity excludedPlayer,
            Vec3 start,
            Vec3 end,
            float partialTicks
    ) {
        Optional<BlockHitResult> blockHit = findBlockHit(level, source, start, end);
        Vec3 playerRayEnd = blockHit.map(BlockHitResult::getLocation).orElse(end);
        Optional<Vec3> playerHit = findFirstPlayerHit(
                level,
                excludedPlayer,
                start,
                playerRayEnd,
                partialTicks
        );
        if (playerHit.isPresent()) {
            return playerHit.map(FireballPredictedImpact::player);
        }
        return blockHit.map(FireballPredictedImpact::block);
    }

    private static Optional<Vec3> findFirstPlayerHit(
            Level level,
            Entity excludedPlayer,
            Vec3 start,
            Vec3 end,
            float partialTicks
    ) {
        Vec3 nearestHit = null;
        double nearestDistanceSquared = Double.MAX_VALUE;

        for (Player target : level.players()) {
            if (target == excludedPlayer || !target.isAlive() || target.isSpectator()) {
                continue;
            }

            Vec3 interpolatedOffset = target.getPosition(partialTicks).subtract(target.position());
            AABB targetBox = target.getBoundingBox()
                    .move(interpolatedOffset)
                    .inflate(FIREBALL_HORIZONTAL_RADIUS, FIREBALL_VERTICAL_RADIUS, FIREBALL_HORIZONTAL_RADIUS);
            Vec3 hit = targetBox.contains(start) ? start : targetBox.clip(start, end).orElse(null);
            if (hit == null) {
                continue;
            }

            double distanceSquared = start.distanceToSqr(hit);
            if (distanceSquared < nearestDistanceSquared) {
                nearestHit = hit;
                nearestDistanceSquared = distanceSquared;
            }
        }

        return Optional.ofNullable(nearestHit);
    }

    private static Optional<BlockHitResult> findBlockHit(Level level, Entity source, Vec3 start, Vec3 end) {
        BlockHitResult hitResult = level.clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                source
        ));
        return hitResult.getType() == HitResult.Type.BLOCK
                ? Optional.of(hitResult)
                : Optional.empty();
    }
}
