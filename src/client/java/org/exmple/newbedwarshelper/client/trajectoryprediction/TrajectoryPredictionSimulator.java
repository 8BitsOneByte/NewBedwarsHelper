package org.exmple.newbedwarshelper.client.trajectoryprediction;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEgg;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownExperienceBottle;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownLingeringPotion;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownSplashPotion;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BubbleColumnBlock;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.exmple.newbedwarshelper.client.z_config.ModConfig;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public final class TrajectoryPredictionSimulator {
    // Minecraft 26.2 sources: ThrowableProjectile#applyInertia, AbstractArrow#tick,
    // CrossbowItem#ARROW_POWER, Entity#updateFluidInteraction, and ProjectileUtil#computeMargin.
    // Gravity remains entity-driven through Projectile#getGravity because potions, experience
    // bottles, ordinary throwables, and arrows override different vanilla defaults.
    private static final float THROWABLE_AIR_DRAG = 0.99F;
    private static final float THROWABLE_WATER_DRAG = 0.8F;
    private static final float ARROW_AIR_DRAG = 0.99F;
    private static final float ARROW_WATER_DRAG = 0.6F;
    private static final float CROSSBOW_ARROW_POWER = 3.15F;
    private static final float VANILLA_MULTISHOT_SPREAD = 10.0F;
    private static final double WATER_CURRENT_SCALE = 0.014D;
    private static final double FAST_LAVA_CURRENT_SCALE = 0.007D;
    private static final double LAVA_CURRENT_SCALE = 0.0023333333333333335D;
    private static final int MAX_PREDICTION_TICKS = 200;
    private static final int MAX_SAMPLING_PRECISION = 4;
    private static final int MAX_PATHS = 3;
    private static final int MAX_POINTS_PER_PATH = MAX_PREDICTION_TICKS * MAX_SAMPLING_PRECISION + 1;
    private static final PredictionFrame FRAME = new PredictionFrame();

    private TrajectoryPredictionSimulator() {
    }

    public static PredictionFrame predict(Minecraft client, float partialTicks, ModConfig.TrajectoryPredictionConfig config) {
        FRAME.reset();
        if (client.level == null || client.player == null) {
            return FRAME;
        }

        boolean projectileEnabled = Boolean.TRUE.equals(config.projectileEnabled);
        boolean arrowEnabled = Boolean.TRUE.equals(config.arrowEnabled);
        if (!projectileEnabled && !arrowEnabled) {
            return FRAME;
        }

        Player player = client.player;
        if (player.isUsingItem()) {
            ItemStack useItem = player.getUseItem();
            if (useItem.getItem() instanceof BowItem) {
                if (arrowEnabled) {
                    addBowPrediction(client.level, player, useItem, partialTicks, config);
                }
                return FRAME;
            }
            if (useItem.getItem() instanceof CrossbowItem) {
                if (arrowEnabled) {
                    addCrossbowPredictions(client.level, player, useItem, partialTicks, config);
                }
                return FRAME;
            }
        }

        ItemStack mainHand = player.getMainHandItem();
        if (isSupportedThrowable(mainHand)) {
            if (projectileEnabled) {
                addThrowablePrediction(client.level, player, mainHand, partialTicks, config);
            }
            return FRAME;
        }
        if (mainHand.getItem() instanceof CrossbowItem) {
            if (arrowEnabled) {
                addCrossbowPredictions(client.level, player, mainHand, partialTicks, config);
            }
            if (canConsumeCrossbowUse(player, mainHand)) {
                return FRAME;
            }
        }
        if (mainHand.getItem() instanceof BowItem) {
            if (player.hasInfiniteMaterials() || !player.getProjectile(mainHand).isEmpty()) {
                return FRAME;
            }
        }

        ItemStack offHand = player.getOffhandItem();
        if (isSupportedThrowable(offHand)) {
            if (projectileEnabled) {
                addThrowablePrediction(client.level, player, offHand, partialTicks, config);
            }
        } else if (offHand.getItem() instanceof CrossbowItem && arrowEnabled) {
            addCrossbowPredictions(client.level, player, offHand, partialTicks, config);
        }

        return FRAME;
    }

    private static void addThrowablePrediction(
            Level level,
            Player player,
            ItemStack itemStack,
            float partialTicks,
            ModConfig.TrajectoryPredictionConfig config
    ) {
        Projectile projectile = createThrowable(level, player, itemStack);
        if (projectile == null) {
            return;
        }

        float power;
        float yOffset;
        if (itemStack.is(Items.SPLASH_POTION) || itemStack.is(Items.LINGERING_POTION)) {
            power = 0.5F;
            yOffset = -20.0F;
        } else if (itemStack.is(Items.EXPERIENCE_BOTTLE)) {
            power = 0.7F;
            yOffset = -20.0F;
        } else {
            power = 1.5F;
            yOffset = 0.0F;
        }

        Vec3 start = launchPosition(player, partialTicks);
        projectile.setPos(start);
        Vec3 movement = movementFromRotation(projectile, player, partialTicks, 0.0F, yOffset, power, true);
        Path path = FRAME.nextPath(false);
        if (path != null) {
            simulate(level, player, projectile, movement, false, start, config, path);
        }
    }

    private static void addBowPrediction(
            Level level,
            Player player,
            ItemStack bow,
            float partialTicks,
            ModConfig.TrajectoryPredictionConfig config
    ) {
        float charge = BowItem.getPowerForTime(player.getTicksUsingItem());
        if (charge < 0.1F) {
            return;
        }

        ItemStack ammunition = player.getProjectile(bow);
        if (ammunition.isEmpty()) {
            return;
        }

        AbstractArrow arrow = createArrow(level, player, bow, ammunition);
        Vec3 start = launchPosition(player, partialTicks);
        arrow.setPos(start);
        Vec3 movement = movementFromRotation(arrow, player, partialTicks, 0.0F, 0.0F, charge * 3.0F, true);
        Path path = FRAME.nextPath(true);
        if (path != null) {
            simulate(level, player, arrow, movement, true, start, config, path);
        }
    }

    private static void addCrossbowPredictions(
            Level level,
            Player player,
            ItemStack crossbow,
            float partialTicks,
            ModConfig.TrajectoryPredictionConfig config
    ) {
        ChargedProjectiles charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
        if (charged == null || charged.isEmpty()) {
            return;
        }

        List<ItemStack> projectiles = charged.itemCopies();
        int projectileCount = projectiles.size();
        float maxAngle = projectileCount > 1 ? VANILLA_MULTISHOT_SPREAD : 0.0F;
        float angleStep = projectileCount == 1 ? 0.0F : 2.0F * maxAngle / (projectileCount - 1);
        float angleOffset = (projectileCount - 1) % 2 * angleStep / 2.0F;
        float direction = 1.0F;
        Vec3 start = launchPosition(player, partialTicks);

        // The 10 degree value is Minecraft 26.2's data/minecraft/enchantment/multishot.json
        // projectile_spread effect. ChargedProjectiles already contains the resulting three shots.
        for (int index = 0; index < projectileCount && FRAME.pathCount() < MAX_PATHS; index++) {
            ItemStack ammunition = projectiles.get(index);
            float angle = angleOffset + direction * ((index + 1) / 2) * angleStep;
            direction = -direction;
            if (!(ammunition.getItem() instanceof ArrowItem)) {
                continue;
            }

            AbstractArrow arrow = createArrow(level, player, crossbow, ammunition);
            arrow.setPos(start);
            Vec3 shotDirection = crossbowDirection(player, partialTicks, angle);
            Vec3 movement = centerMovementToShoot(arrow, shotDirection, CROSSBOW_ARROW_POWER);
            Path path = FRAME.nextPath(true);
            if (path != null) {
                simulate(level, player, arrow, movement, true, start, config, path);
            }
        }
    }

    private static void simulate(
            Level level,
            Player player,
            Projectile projectile,
            Vec3 initialMovement,
            boolean arrow,
            Vec3 start,
            ModConfig.TrajectoryPredictionConfig config,
            Path path
    ) {
        path.reset(arrow);
        path.add(start);
        projectile.setPos(start);
        projectile.setDeltaMovement(initialMovement);
        EntityFluidInteraction fluidInteraction = new EntityFluidInteraction(Set.of(FluidTags.WATER, FluidTags.LAVA));
        CollisionFilter collisionFilter = new CollisionFilter(player, arrow);
        SpecialBlockEffects specialBlockEffects = new SpecialBlockEffects(level, projectile);
        Step step = new Step();
        boolean inWater = false;
        boolean leftOwner = false;
        double travelled = 0.0D;
        double maxLength = config.maxLength;
        int precision = config.samplingPrecision;

        for (int tick = 1; tick <= MAX_PREDICTION_TICKS; tick++) {
            Vec3 from = projectile.position();
            if (!isFinite(from) || !isFinite(projectile.getDeltaMovement())
                    || from.y < level.getMinY() - 64.0D
                    || !level.hasChunk(SectionPos.blockToSectionCoord(from.x), SectionPos.blockToSectionCoord(from.z))) {
                return;
            }

            if (arrow) {
                if (isArrowInsideCollisionShape(level, projectile)) {
                    return;
                }

                // AbstractArrow#tick snapshots movement first. Water drag changes the stored
                // velocity, but the current tick's sweep still uses that pre-drag snapshot.
                // After moving: block effects, air drag only when dry, gravity, then baseTick
                // updates fluid contact/current for the following tick.
                Vec3 movement = projectile.getDeltaMovement();
                if (inWater) {
                    projectile.setDeltaMovement(movement.scale(ARROW_WATER_DRAG));
                }

                leftOwner = updateLeftOwner(player, projectile, leftOwner);
                collisionFilter.leftOwner = leftOwner;
                prepareStep(step, from, movement, travelled, maxLength);
                HitResult hitResult = findArrowHit(level, projectile, from, step.to, collisionFilter, tick);
                Vec3 to = hitResult.getType() == HitResult.Type.MISS ? step.to : hitResult.getLocation();
                double tickFraction = movement.lengthSqr() == 0.0D ? 0.0D : Math.min(1.0D, from.distanceTo(to) / movement.length());
                if (!path.appendSegment(from, to, precision)) {
                    return;
                }
                travelled += from.distanceTo(to);
                projectile.setPos(to);

                if (!specialBlockEffects.apply(from, to)) {
                    return;
                }
                if (hitResult instanceof BlockHitResult blockHitResult && blockHitResult.isWorldBorderHit()) {
                    projectile.setDeltaMovement(projectile.getDeltaMovement().scale(-0.1D));
                } else if (hitResult.getType() != HitResult.Type.MISS) {
                    path.setHit(level, projectile, hitResult, tick - 1.0F + (float)tickFraction);
                    return;
                } else if (step.reachedLimit) {
                    return;
                }

                if (!inWater) {
                    projectile.setDeltaMovement(projectile.getDeltaMovement().scale(ARROW_AIR_DRAG));
                }
                projectile.setDeltaMovement(projectile.getDeltaMovement().add(0.0D, -projectile.getGravity(), 0.0D));
                inWater = updateFluidInteraction(level, projectile, fluidInteraction);
            } else {
                // ThrowableProjectile#tick order in 26.2 is first-tick bubble handling,
                // gravity, inertia, swept block/entity collision, position, block effects,
                // then Projectile/Entity baseTick (owner departure and fluid current).
                if (tick == 1) {
                    applyFirstTickBubbleColumns(level, projectile);
                }

                Vec3 movement = projectile.getDeltaMovement()
                        .add(0.0D, -projectile.getGravity(), 0.0D)
                        .scale(inWater ? THROWABLE_WATER_DRAG : THROWABLE_AIR_DRAG);
                projectile.setDeltaMovement(movement);
                collisionFilter.leftOwner = leftOwner;
                prepareStep(step, from, movement, travelled, maxLength);
                HitResult hitResult = findThrowableHit(level, projectile, from, step.to, collisionFilter, tick);
                Vec3 to = hitResult.getType() == HitResult.Type.MISS ? step.to : hitResult.getLocation();
                double tickFraction = movement.lengthSqr() == 0.0D ? 0.0D : Math.min(1.0D, from.distanceTo(to) / movement.length());
                if (!path.appendSegment(from, to, precision)) {
                    return;
                }
                travelled += from.distanceTo(to);
                projectile.setPos(to);

                if (!specialBlockEffects.apply(from, to)) {
                    return;
                }
                if (hitResult.getType() != HitResult.Type.MISS) {
                    path.setHit(level, projectile, hitResult, tick - 1.0F + (float)tickFraction);
                    return;
                }
                if (step.reachedLimit) {
                    return;
                }

                leftOwner = updateLeftOwner(player, projectile, leftOwner);
                inWater = updateFluidInteraction(level, projectile, fluidInteraction);
            }
        }
    }

    private static HitResult findThrowableHit(
            Level level,
            Projectile projectile,
            Vec3 from,
            Vec3 to,
            Predicate<Entity> collisionFilter,
            int tick
    ) {
        // Vanilla clips the full tick segment against COLLIDER shapes and the world border,
        // then limits the entity sweep to the portion before that first block result.
        BlockHitResult blockHit = level.clipIncludingBorder(
                new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile)
        );
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? to : blockHit.getLocation();
        AABB searchBox = projectile.getBoundingBox().expandTowards(projectile.getDeltaMovement()).inflate(1.0D);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                level, projectile, from, entityEnd, searchBox, collisionFilter, entityHitMargin(tick)
        );
        return entityHit == null ? blockHit : entityHit;
    }

    private static HitResult findArrowHit(
            Level level,
            Projectile projectile,
            Vec3 from,
            Vec3 to,
            Predicate<Entity> collisionFilter,
            int tick
    ) {
        BlockHitResult blockHit = level.clipIncludingBorder(
                new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, projectile)
        );
        Vec3 entityEnd = blockHit.getType() == HitResult.Type.MISS ? to : blockHit.getLocation();
        AABB searchBox = projectile.getBoundingBox().expandTowards(projectile.getDeltaMovement()).inflate(1.0D);
        Collection<EntityHitResult> entityHits = ProjectileUtil.getManyEntityHitResult(
                level,
                projectile,
                from,
                entityEnd,
                searchBox,
                collisionFilter,
                entityHitMargin(tick),
                ClipContext.Block.COLLIDER,
                false
        );
        if (entityHits.isEmpty()) {
            return blockHit;
        }

        List<EntityHitResult> sortedHits = new ArrayList<>(entityHits);
        sortedHits.sort(Comparator.comparingDouble(hit -> from.distanceToSqr(hit.getEntity().position())));
        return sortedHits.getFirst();
    }

    private static boolean updateFluidInteraction(Level level, Projectile projectile, EntityFluidInteraction fluidInteraction) {
        fluidInteraction.update(projectile, false);
        boolean inWater = fluidInteraction.isInFluid(FluidTags.WATER);
        boolean inLava = fluidInteraction.isInFluid(FluidTags.LAVA);
        if (inWater) {
            fluidInteraction.applyCurrentTo(FluidTags.WATER, projectile, WATER_CURRENT_SCALE);
        }
        if (inLava) {
            double scale = level.environmentAttributes().getDimensionValue(EnvironmentAttributes.FAST_LAVA)
                    ? FAST_LAVA_CURRENT_SCALE
                    : LAVA_CURRENT_SCALE;
            fluidInteraction.applyCurrentTo(FluidTags.LAVA, projectile, scale);
        }
        return inWater;
    }

    private static void applyFirstTickBubbleColumns(Level level, Projectile projectile) {
        // ThrowableProjectile#handleFirstTickBubbleColumn checks only the spawn bounding
        // box. Portals and all other inside-block effects remain after the first move.
        for (BlockPos pos : BlockPos.betweenClosed(projectile.getBoundingBox())) {
            BlockState state = level.getBlockState(pos);
            if (state.is(Blocks.BUBBLE_COLUMN)) {
                applyBubbleColumnMovement(level, projectile, pos, state);
            }
        }
    }

    private static void applyBubbleColumnMovement(Level level, Projectile projectile, BlockPos pos, BlockState state) {
        boolean dragDown = state.getValue(BubbleColumnBlock.DRAG_DOWN);
        BlockState stateAbove = level.getBlockState(pos.above());
        boolean aboveColumn = stateAbove.getCollisionShape(level, pos).isEmpty() && stateAbove.getFluidState().isEmpty();
        Vec3 movement = projectile.getDeltaMovement();
        double y;

        // Projectile adds directly; ThrownEnderpearl deliberately delegates to Entity's
        // clamped implementation. Reproducing both branches avoids invoking live block
        // callbacks from a render-time dummy entity.
        if (projectile instanceof ThrownEnderpearl) {
            if (aboveColumn) {
                y = dragDown ? Math.max(-0.9D, movement.y - 0.03D) : Math.min(1.8D, movement.y + 0.1D);
            } else {
                y = dragDown ? Math.max(-0.3D, movement.y - 0.03D) : Math.min(0.7D, movement.y + 0.06D);
            }
        } else if (aboveColumn) {
            y = movement.y + (dragDown ? -0.03D : 0.1D);
        } else {
            y = movement.y + (dragDown ? -0.03D : 0.06D);
        }
        projectile.setDeltaMovement(movement.x, y, movement.z);
    }

    private static void applyHoneyBlockMovement(Projectile projectile, BlockPos pos) {
        Vec3 movement = projectile.getDeltaMovement();
        double oldDeltaY = movement.y / 0.98F + 0.08D;
        if (projectile.onGround()
                || projectile.getY() > pos.getY() + 0.9375D - 1.0E-7D
                || oldDeltaY >= -0.08D) {
            return;
        }

        double overlapDistance = 0.4375D + projectile.getBbWidth() / 2.0F;
        double dx = Math.abs(pos.getX() + 0.5D - projectile.getX());
        double dz = Math.abs(pos.getZ() + 0.5D - projectile.getZ());
        if (dx + 1.0E-7D <= overlapDistance && dz + 1.0E-7D <= overlapDistance) {
            return;
        }

        double y = (-0.05D - 0.08D) * 0.98F;
        if (oldDeltaY < -0.13D) {
            double horizontalReduction = -0.05D / oldDeltaY;
            projectile.setDeltaMovement(movement.x * horizontalReduction, y, movement.z * horizontalReduction);
        } else {
            projectile.setDeltaMovement(movement.x, y, movement.z);
        }
    }

    private static boolean intersectsBlock(
            Level level,
            Projectile projectile,
            Vec3 from,
            Vec3 to,
            BlockPos pos,
            BlockState state
    ) {
        VoxelShape insideShape = state.getEntityInsideCollisionShape(level, pos, projectile);
        if (insideShape.isEmpty()) {
            return false;
        }

        double halfWidth = projectile.getBbWidth() * 0.5D;
        double height = projectile.getBbHeight();
        for (AABB box : insideShape.toAabbs()) {
            AABB positionRange = new AABB(
                    pos.getX() + box.minX - halfWidth,
                    pos.getY() + box.minY - height,
                    pos.getZ() + box.minZ - halfWidth,
                    pos.getX() + box.maxX + halfWidth,
                    pos.getY() + box.maxY,
                    pos.getZ() + box.maxZ + halfWidth
            );
            if (positionRange.contains(from) || positionRange.clip(from, to).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private static boolean isArrowInsideCollisionShape(Level level, Projectile projectile) {
        BlockPos pos = projectile.blockPosition();
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return false;
        }

        Vec3 position = projectile.position();
        for (AABB box : state.getCollisionShape(level, pos).toAabbs()) {
            if (box.move(pos).contains(position)) {
                return true;
            }
        }
        return false;
    }

    private static boolean updateLeftOwner(Player player, Projectile projectile, boolean leftOwner) {
        if (leftOwner) {
            return true;
        }

        AABB collisionRange = projectile.getBoundingBox().expandTowards(projectile.getDeltaMovement()).inflate(1.0D);
        return player.getRootVehicle()
                .getSelfAndPassengers()
                .filter(EntitySelector.CAN_BE_PICKED)
                .noneMatch(entity -> collisionRange.intersects(entity.getBoundingBox()));
    }

    private static float entityHitMargin(int tick) {
        return Mth.clamp((tick - 2) / 20.0F, 0.0F, 0.3F);
    }

    private static void prepareStep(Step step, Vec3 from, Vec3 movement, double travelled, double maxLength) {
        double movementLength = movement.length();
        double remaining = Math.max(0.0D, maxLength - travelled);
        if (movementLength > remaining && movementLength > 0.0D) {
            step.to = from.add(movement.scale(remaining / movementLength));
            step.reachedLimit = true;
        } else {
            step.to = from.add(movement);
            step.reachedLimit = false;
        }
    }

    private static Vec3 movementFromRotation(
            Projectile projectile,
            Player player,
            float partialTicks,
            float yawOffset,
            float pitchOffset,
            float power,
            boolean inheritShooterMovement
    ) {
        // Camera rendering uses the same partial-tick pose. At partialTicks == 1 this is
        // vanilla's logical launch state; interpolation keeps the preview attached to the
        // player's rendered hands/camera between ticks without changing later tick physics.
        float xRot = Mth.lerp(partialTicks, player.xRotO, player.getXRot());
        float yRot = Mth.rotLerp(partialTicks, player.yRotO, player.getYRot()) + yawOffset;
        double radians = Math.PI / 180.0D;
        double x = -Mth.sin(yRot * radians) * Mth.cos(xRot * radians);
        double y = -Mth.sin((xRot + pitchOffset) * radians);
        double z = Mth.cos(yRot * radians) * Mth.cos(xRot * radians);
        Vec3 movement = centerMovementToShoot(projectile, new Vec3(x, y, z), power);
        if (inheritShooterMovement) {
            Vec3 playerMovement = player.getKnownMovement();
            movement = movement.add(playerMovement.x, player.onGround() ? 0.0D : playerMovement.y, playerMovement.z);
        }
        return movement;
    }

    private static Vec3 crossbowDirection(Player player, float partialTicks, float angle) {
        Vec3 up = player.getUpVector(partialTicks);
        Quaternionf rotation = new Quaternionf().setAngleAxis(
                angle * (float)(Math.PI / 180.0D),
                (float)up.x,
                (float)up.y,
                (float)up.z
        );
        Vector3f direction = player.getViewVector(partialTicks).toVector3f().rotate(rotation);
        return new Vec3(direction.x(), direction.y(), direction.z());
    }

    private static Vec3 centerMovementToShoot(Projectile projectile, Vec3 direction, float power) {
        // Vanilla launch uncertainty is 1.0 and adds independent triangular noise with a
        // 0.0172275 spread per axis. That RNG is created only on the server projectile and
        // is not knowable before firing, so prediction uses the exact zero-noise centerline
        // instead of inventing or re-sampling a visibly unstable client-side random path.
        return projectile.getMovementToShoot(direction.x, direction.y, direction.z, power, 0.0F);
    }

    private static Vec3 launchPosition(Player player, float partialTicks) {
        Vec3 playerPosition = player.getPosition(partialTicks);
        return playerPosition.add(0.0D, player.getEyeHeight() - 0.1F, 0.0D);
    }

    private static AbstractArrow createArrow(Level level, Player player, ItemStack weapon, ItemStack ammunition) {
        ArrowItem arrowItem = ammunition.getItem() instanceof ArrowItem item ? item : (ArrowItem)Items.ARROW;
        return arrowItem.createArrow(level, ammunition.copyWithCount(1), player, weapon);
    }

    private static Projectile createThrowable(Level level, Player player, ItemStack itemStack) {
        if (itemStack.is(Items.SNOWBALL)) {
            return new Snowball(level, player, itemStack);
        }
        if (itemStack.is(Items.EGG)) {
            return new ThrownEgg(level, player, itemStack);
        }
        if (itemStack.is(Items.ENDER_PEARL)) {
            return new ThrownEnderpearl(level, player, itemStack);
        }
        if (itemStack.is(Items.SPLASH_POTION)) {
            return new ThrownSplashPotion(level, player, itemStack);
        }
        if (itemStack.is(Items.LINGERING_POTION)) {
            return new ThrownLingeringPotion(level, player, itemStack);
        }
        if (itemStack.is(Items.EXPERIENCE_BOTTLE)) {
            return new ThrownExperienceBottle(level, player, itemStack);
        }
        return null;
    }

    private static boolean isSupportedThrowable(ItemStack itemStack) {
        return itemStack.is(Items.SNOWBALL)
                || itemStack.is(Items.EGG)
                || itemStack.is(Items.ENDER_PEARL)
                || itemStack.is(Items.SPLASH_POTION)
                || itemStack.is(Items.LINGERING_POTION)
                || itemStack.is(Items.EXPERIENCE_BOTTLE);
    }

    private static boolean canConsumeCrossbowUse(Player player, ItemStack crossbow) {
        ChargedProjectiles charged = crossbow.get(DataComponents.CHARGED_PROJECTILES);
        return (charged != null && !charged.isEmpty()) || !player.getProjectile(crossbow).isEmpty();
    }

    private static boolean isFinite(Vec3 vector) {
        return Double.isFinite(vector.x) && Double.isFinite(vector.y) && Double.isFinite(vector.z);
    }

    private static final class CollisionFilter implements Predicate<Entity> {
        private final Player owner;
        private final boolean arrow;
        private boolean leftOwner;

        private CollisionFilter(Player owner, boolean arrow) {
            this.owner = owner;
            this.arrow = arrow;
        }

        @Override
        public boolean test(Entity entity) {
            if (!entity.canBeHitByProjectile()) {
                return false;
            }
            if (!this.leftOwner && this.owner.isPassengerOfSameVehicle(entity)) {
                return false;
            }
            return !this.arrow || !(entity instanceof Player target) || this.owner.canHarmPlayer(target);
        }
    }

    private static final class Step {
        private Vec3 to;
        private boolean reachedLimit;
    }

    private static final class SpecialBlockEffects implements BlockGetter.BlockStepVisitor {
        private final Level level;
        private final Projectile projectile;
        private Vec3 from;
        private Vec3 to;
        private AABB deflatedEndBox;
        private boolean movedFar;
        private boolean canContinue;

        private SpecialBlockEffects(Level level, Projectile projectile) {
            this.level = level;
            this.projectile = projectile;
        }

        private boolean apply(Vec3 from, Vec3 to) {
            this.from = from;
            this.to = to;
            this.deflatedEndBox = this.projectile.getBoundingBox().deflate(1.0E-5F);
            this.movedFar = from.distanceToSqr(to) > Mth.square(0.9999900000002526D);
            this.canContinue = true;
            BlockGetter.forEachBlockIntersectedBetween(from, to, this.deflatedEndBox, this);
            return this.canContinue;
        }

        @Override
        public boolean visit(BlockPos pos, int iteration) {
            // Entity#checkInsideBlocks caps a movement at sixteen entry steps. Projectile
            // speeds are well below that, but preserving the guard avoids unbounded scans
            // if another mod supplies an extreme initial velocity.
            if (iteration >= 16) {
                return false;
            }

            BlockState state = this.level.getBlockState(pos);
            if (state.isAir() || !intersectsBlock(this.level, this.projectile, this.from, this.to, pos, state)) {
                return true;
            }
            if (state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL)) {
                this.canContinue = false;
                return false;
            }
            if (state.is(Blocks.END_GATEWAY)
                    && this.level.getBlockEntity(pos) instanceof TheEndGatewayBlockEntity gateway
                    && !gateway.isCoolingDown()) {
                this.canContinue = false;
                return false;
            }
            if (state.is(Blocks.HONEY_BLOCK)) {
                applyHoneyBlockMovement(this.projectile, pos);
            } else if (state.is(Blocks.BUBBLE_COLUMN)
                    && (this.movedFar || this.deflatedEndBox.intersects(new AABB(pos)))) {
                applyBubbleColumnMovement(this.level, this.projectile, pos, state);
            }
            return true;
        }
    }

    public enum HitType {
        NONE,
        BLOCK,
        ENTITY
    }

    public static final class PredictionFrame {
        private final Path[] paths = new Path[MAX_PATHS];
        private int pathCount;

        private PredictionFrame() {
            for (int i = 0; i < this.paths.length; i++) {
                this.paths[i] = new Path();
            }
        }

        private void reset() {
            this.pathCount = 0;
        }

        private Path nextPath(boolean arrow) {
            if (this.pathCount >= this.paths.length) {
                return null;
            }

            Path path = this.paths[this.pathCount++];
            path.reset(arrow);
            return path;
        }

        public int pathCount() {
            return this.pathCount;
        }

        public Path path(int index) {
            return this.paths[index];
        }
    }

    public static final class Path {
        private final Vec3[] points = new Vec3[MAX_POINTS_PER_PATH];
        private int pointCount;
        private boolean arrow;
        private HitType hitType = HitType.NONE;
        private Vec3 hitPosition;
        private Direction hitFace;
        private BlockPos hitBlockPos;
        private Entity hitEntity;
        private VoxelShape hitShape;
        private boolean worldBorderHit;
        private float flightTicks;

        private void reset(boolean arrow) {
            this.pointCount = 0;
            this.arrow = arrow;
            this.hitType = HitType.NONE;
            this.hitPosition = null;
            this.hitFace = null;
            this.hitBlockPos = null;
            this.hitEntity = null;
            this.hitShape = null;
            this.worldBorderHit = false;
            this.flightTicks = 0.0F;
        }

        private boolean add(Vec3 point) {
            if (this.pointCount >= this.points.length) {
                return false;
            }
            if (this.pointCount > 0 && this.points[this.pointCount - 1].distanceToSqr(point) < 1.0E-12D) {
                return true;
            }

            this.points[this.pointCount++] = point;
            return true;
        }

        private boolean appendSegment(Vec3 from, Vec3 to, int precision) {
            for (int sample = 1; sample <= precision; sample++) {
                if (!this.add(from.lerp(to, sample / (double)precision))) {
                    return false;
                }
            }
            return true;
        }

        private void setHit(Level level, Projectile projectile, HitResult hitResult, float flightTicks) {
            this.hitPosition = hitResult.getLocation();
            this.flightTicks = flightTicks;
            if (hitResult instanceof BlockHitResult blockHitResult) {
                this.hitType = HitType.BLOCK;
                this.hitFace = blockHitResult.getDirection();
                this.hitBlockPos = blockHitResult.getBlockPos();
                this.worldBorderHit = blockHitResult.isWorldBorderHit();
                if (!this.worldBorderHit) {
                    BlockState state = level.getBlockState(this.hitBlockPos);
                    this.hitShape = state.getCollisionShape(level, this.hitBlockPos, CollisionContext.of(projectile));
                }
            } else if (hitResult instanceof EntityHitResult entityHitResult) {
                this.hitType = HitType.ENTITY;
                this.hitEntity = entityHitResult.getEntity();
            }
        }

        public int pointCount() {
            return this.pointCount;
        }

        public Vec3 point(int index) {
            return this.points[index];
        }

        public boolean isArrow() {
            return this.arrow;
        }

        public HitType hitType() {
            return this.hitType;
        }

        public Vec3 hitPosition() {
            return this.hitPosition;
        }

        public Direction hitFace() {
            return this.hitFace;
        }

        public BlockPos hitBlockPos() {
            return this.hitBlockPos;
        }

        public Entity hitEntity() {
            return this.hitEntity;
        }

        public VoxelShape hitShape() {
            return this.hitShape;
        }

        public boolean worldBorderHit() {
            return this.worldBorderHit;
        }

        public float flightTicks() {
            return this.flightTicks;
        }
    }
}
