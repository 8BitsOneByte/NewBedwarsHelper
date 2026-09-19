package org.exmple.newbedwarshelper.client.waterclutchhelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.client.Minecraft;
import org.exmple.newbedwarshelper.client.z_debug.waterclutch.WaterClutchDebugger;

public final class WaterClutchTargetResolver {
    private static final double EPSILON = 1.0E-6;
    private static final double AIM_GUIDE_DISTANCE = 160.0;
    private static final TagKey<Block> INTERACTABLES = TagKey.create(
            Registries.BLOCK,
            Identifier.fromNamespaceAndPath("newbedwarshelper", "water_clutch_interactables")
    );

    private WaterClutchTargetResolver() {
    }

    public static boolean predictedLandingNeedsSneak(Level level, WaterClutchLandingAssessment landing) {
        BlockPos landingPos = landing.landingBlockPos();
        BlockState landingState = level.getBlockState(landingPos);
        boolean waterloggable = landingState.getBlock() instanceof LiquidBlockContainer;
        boolean interactable = landingState.getMenuProvider(level, landingPos) != null || landingState.is(INTERACTABLES);
        WaterClutchDebugger.event("target-prepare landing=%s waterloggable=%s interactable=%s",
                landingPos, waterloggable, interactable);
        return waterloggable || interactable;
    }

    public static Target resolve(Minecraft client, Player player, WaterClutchLandingAssessment landing) {
        if (!(client.hitResult instanceof BlockHitResult hitResult)
                || hitResult.getType() != HitResult.Type.BLOCK) {
            WaterClutchDebugger.event("target-reject hit=%s",
                    client.hitResult == null ? "null" : client.hitResult.getType());
            return null;
        }
        return resolveHit(player, landing, hitResult, "target");
    }

    public static AimStatus classifyPredictedAim(Player player, WaterClutchLandingAssessment landing) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 view = player.getViewVector(1.0F);
        BlockHitResult hitResult = player.level().clip(new ClipContext(
                eye,
                eye.add(view.scale(AIM_GUIDE_DISTANCE)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            WaterClutchDebugger.event("aim-guide reject hit=%s", hitResult.getType());
            return AimStatus.INVALID;
        }
        Target target = resolveHit(player, landing, hitResult, "aim-guide");
        AimStatus status = target == null
                ? AimStatus.INVALID
                : hitResult.getDirection() == Direction.UP ? AimStatus.DIRECT : AimStatus.CONDITIONAL;
        WaterClutchDebugger.event("aim-guide status=%s pos=%s face=%s", status,
                hitResult.getBlockPos(), hitResult.getDirection());
        return status;
    }

    private static Target resolveHit(
            Player player,
            WaterClutchLandingAssessment landing,
            BlockHitResult hitResult,
            String tracePrefix
    ) {
        Direction hitDirection = hitResult.getDirection();
        if (hitDirection == Direction.DOWN) {
            WaterClutchDebugger.event("%s reject hit-block pos=%s face=%s unsupportedFace=down",
                    tracePrefix, hitResult.getBlockPos(), hitResult.getDirection());
            return null;
        }

        Level level = player.level();
        BlockPos clickedPos = hitResult.getBlockPos();
        BlockState clickedState = level.getBlockState(clickedPos);
        boolean waterloggable = clickedState.getBlock() instanceof LiquidBlockContainer;
        boolean interactable = clickedState.getMenuProvider(level, clickedPos) != null || clickedState.is(INTERACTABLES);
        boolean needsSneak = waterloggable || interactable;
        BlockPos placePos = needsSneak || !waterloggable ? clickedPos.relative(hitDirection) : clickedPos;
        BlockState placeState = level.getBlockState(placePos);
        traceSnowTarget(player, landing, hitResult, clickedPos, clickedState, placePos, placeState);

        if (!level.mayInteract(player, clickedPos)
                || !player.mayUseItemAt(placePos, hitDirection, player.getItemInHand(WaterClutchBucketPolicy.findUsableHand(player)))) {
            WaterClutchDebugger.event("%s reject permission clicked=%s place=%s", tracePrefix, clickedPos, placePos);
            return null;
        }

        boolean canReceiveWater = placeState.isAir()
                || placeState.canBeReplaced(Fluids.WATER)
                || placeState.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(player, level, placePos, placeState, Fluids.WATER);
        if (!canReceiveWater
                || !placeState.getFluidState().isEmpty()
                || level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, placePos)) {
            WaterClutchDebugger.event("%s reject place=%s canReceive=%s fluidEmpty=%s evaporates=%s",
                    tracePrefix, placePos, canReceiveWater, placeState.getFluidState().isEmpty(),
                    level.environmentAttributes().getValue(EnvironmentAttributes.WATER_EVAPORATES, placePos));
            return null;
        }

        double waterBottom = placePos.getY();
        double waterTop = waterBottom + 1.0;
        boolean reachesPlayerBeforeLanding = waterTop > landing.landingY() + EPSILON;
        boolean notAboveLandingPath = waterBottom <= landing.landingY() + 1.0 + EPSILON;
        boolean overlapsLandingFootprint = placePos.getX() + 1.0 > landing.landingBox().minX + EPSILON
                && placePos.getX() < landing.landingBox().maxX - EPSILON
                && placePos.getZ() + 1.0 > landing.landingBox().minZ + EPSILON
                && placePos.getZ() < landing.landingBox().maxZ - EPSILON;
        if (!reachesPlayerBeforeLanding || !notAboveLandingPath || !overlapsLandingFootprint) {
            WaterClutchDebugger.event(
                    "%s reject water-volume bottom=%.3f top=%.3f landingY=%.3f reachesBeforeLanding=%s notAbovePath=%s overlapsFootprint=%s",
                    tracePrefix, waterBottom, waterTop, landing.landingY(), reachesPlayerBeforeLanding, notAboveLandingPath,
                    overlapsLandingFootprint);
            return null;
        }
        WaterClutchDebugger.event("%s accept clicked=%s place=%s waterloggable=%s interactable=%s sneak=%s",
                tracePrefix, clickedPos, placePos, waterloggable, interactable, needsSneak);
        return new Target(clickedPos, placePos, needsSneak);
    }

    private static void traceSnowTarget(
            Player player,
            WaterClutchLandingAssessment landing,
            BlockHitResult hitResult,
            BlockPos clickedPos,
            BlockState clickedState,
            BlockPos placePos,
            BlockState placeState
    ) {
        Level level = player.level();
        BlockPos landingPos = landing.landingBlockPos();
        BlockState landingState = level.getBlockState(landingPos);
        if (!(clickedState.getBlock() instanceof SnowLayerBlock)
                && !(placeState.getBlock() instanceof SnowLayerBlock)
                && !(landingState.getBlock() instanceof SnowLayerBlock)) {
            return;
        }

        WaterClutchDebugger.event(
                "snow-target hitPos=%s hitFace=%s hitLocation=(%.3f,%.3f,%.3f) clicked=%s place=%s predictedLanding=%s landingY=%.3f",
                clickedPos,
                hitResult.getDirection(),
                hitResult.getLocation().x,
                hitResult.getLocation().y,
                hitResult.getLocation().z,
                describeSnowState(player, level, clickedPos, clickedState),
                describeSnowState(player, level, placePos, placeState),
                describeSnowState(player, level, landingPos, landingState),
                landing.landingY());
    }

    private static String describeSnowState(Player player, Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof SnowLayerBlock)) {
            return pos + "/" + state.getBlock();
        }

        CollisionContext context = CollisionContext.of(player);
        VoxelShape outline = state.getShape(level, pos, context);
        VoxelShape collision = state.getCollisionShape(level, pos, context);
        String collisionTop = collision.isEmpty()
                ? "none"
                : String.format(java.util.Locale.ROOT, "%.3f", pos.getY() + collision.max(Direction.Axis.Y));
        return String.format(
                java.util.Locale.ROOT,
                "%s/snow[layers=%d,outlineTop=%.3f,collisionTop=%s,collisionEmpty=%s]",
                pos,
                state.getValue(SnowLayerBlock.LAYERS),
                pos.getY() + outline.max(Direction.Axis.Y),
                collisionTop,
                collision.isEmpty());
    }

    public record Target(BlockPos clickedPos, BlockPos placePos, boolean needsSneak) {
    }

    public enum AimStatus {
        INVALID,
        CONDITIONAL,
        DIRECT
    }
}
