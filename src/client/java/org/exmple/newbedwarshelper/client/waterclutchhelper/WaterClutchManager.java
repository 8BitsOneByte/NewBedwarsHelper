package org.exmple.newbedwarshelper.client.waterclutchhelper;

import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.exmple.newbedwarshelper.client.mixin.waterclutchhelper.MinecraftUseItemInvoker;
import org.exmple.newbedwarshelper.client.z_config.ModConfig;
import org.exmple.newbedwarshelper.client.z_debug.waterclutch.WaterClutchDebugger;

public final class WaterClutchManager {
    private static final int MAX_ATTEMPTS = 3;
    private static final double MIN_PREPARE_DISTANCE = 10.0;
    private static final int PREPARE_TIME_TICKS = 10;
    private static final int REACTION_SAFETY_TICKS = 3;
    private static final int MAX_HUMAN_REACTION_TICKS = 4;
    private static final double NATURAL_FALL_GRAVITY = 0.08;
    private static final double NATURAL_FALL_DRAG = 0.98;
    private static final double NATURAL_TERMINAL_FALL_SPEED =
            NATURAL_FALL_GRAVITY * NATURAL_FALL_DRAG / (1.0 - NATURAL_FALL_DRAG);
    private static final WaterClutchAttempt ATTEMPT = new WaterClutchAttempt();
    private static final WaterClutchSneakController SNEAK = new WaterClutchSneakController();

    private WaterClutchManager() {
    }

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(WaterClutchManager::onClientTick);
    }

    public static boolean isEnabled() {
        return Boolean.TRUE.equals(ModConfig.getInstance().waterClutch.enabled);
    }

    public static void setEnabled(boolean enabled) {
        ModConfig config = ModConfig.getInstance();
        config.waterClutch.enabled = enabled;
        config.save();
        if (!enabled) {
            reset(Minecraft.getInstance());
        }
    }

    public static boolean showsPredictedLandingBlock() {
        return ModConfig.getInstance().waterClutch.showPredictedLandingBlock;
    }

    public static void setShowPredictedLandingBlock(boolean enabled) {
        ModConfig config = ModConfig.getInstance();
        config.waterClutch.showPredictedLandingBlock = enabled;
        config.save();
        if (!enabled) {
            WaterClutchLandingBeamRenderer.clear();
        }
    }

    public static boolean alwaysSneaks() {
        return ModConfig.getInstance().waterClutch.alwaysSneak;
    }

    public static void setAlwaysSneak(boolean enabled) {
        ModConfig config = ModConfig.getInstance();
        config.waterClutch.alwaysSneak = enabled;
        config.save();
    }

    public static boolean isMobBucketEnabled(MobBucketOption option) {
        ModConfig.WaterClutchConfig config = ModConfig.getInstance().waterClutch;
        return switch (option) {
            case PUFFERFISH -> config.pufferfishBucket;
            case SALMON -> config.salmonBucket;
            case COD -> config.codBucket;
            case TROPICAL_FISH -> config.tropicalFishBucket;
            case AXOLOTL -> config.axolotlBucket;
            case TADPOLE -> config.tadpoleBucket;
        };
    }

    public static void setMobBucketEnabled(MobBucketOption option, boolean enabled) {
        ModConfig config = ModConfig.getInstance();
        switch (option) {
            case PUFFERFISH -> config.waterClutch.pufferfishBucket = enabled;
            case SALMON -> config.waterClutch.salmonBucket = enabled;
            case COD -> config.waterClutch.codBucket = enabled;
            case TROPICAL_FISH -> config.waterClutch.tropicalFishBucket = enabled;
            case AXOLOTL -> config.waterClutch.axolotlBucket = enabled;
            case TADPOLE -> config.waterClutch.tadpoleBucket = enabled;
        }
        config.save();
    }

    private static void onClientTick(Minecraft client) {
        WaterClutchDebugger.beginTick(
                client,
                isEnabled(),
                ATTEMPT.stage.name(),
                ATTEMPT.ticksRemaining,
                ATTEMPT.attempts
        );
        try {
            runTick(client);
        } finally {
            WaterClutchDebugger.endTick(ATTEMPT.stage.name(), ATTEMPT.ticksRemaining, ATTEMPT.attempts);
        }
    }

    private static void runTick(Minecraft client) {
        WaterClutchLandingBeamRenderer.clear();
        Player player = client.player;
        if (ATTEMPT.stage == WaterClutchAttempt.Stage.RELEASE_SNEAK) {
            if (player == null || client.level == null || client.gui.screen() != null || !isEnabled()) {
                WaterClutchDebugger.event("release-aborted reset");
                reset(client);
            } else {
                WaterClutchDebugger.event("release-countdown");
                releaseSneak(client);
            }
            return;
        }
        String invalidReason = runtimeInvalidReason(client, player);
        if (invalidReason != null) {
            WaterClutchDebugger.event("runtime-invalid=%s reset", invalidReason);
            reset(client);
            return;
        }

        if (ATTEMPT.stage == WaterClutchAttempt.Stage.VERIFY
                && ATTEMPT.hand != null
                && !WaterClutchBucketPolicy.isSupportedFilledBucket(player.getItemInHand(ATTEMPT.hand))) {
            ATTEMPT.ticksRemaining = randomTicks(1, 3);
            ATTEMPT.stage = WaterClutchAttempt.Stage.RELEASE_SNEAK;
            WaterClutchDebugger.event("bucket-empty success releaseDelay=%d", ATTEMPT.ticksRemaining);
            return;
        }

        Optional<WaterClutchLandingAssessment> assessmentOptional = WaterClutchFallPredictor.assess(player, client.level);
        if (assessmentOptional.isEmpty()) {
            WaterClutchDebugger.event("assessment=empty reset");
            reset(client);
            return;
        }
        if (!assessmentOptional.get().needsProtection()) {
            WaterClutchDebugger.event("assessment=safe damage=%d reset", assessmentOptional.get().predictedFallDamage());
            reset(client);
            return;
        }

        WaterClutchLandingAssessment assessment = assessmentOptional.get();
        InteractionHand currentHand = WaterClutchBucketPolicy.findUsableHand(player);
        WaterClutchTargetResolver.AimStatus aimStatus = WaterClutchTargetResolver.AimStatus.INVALID;
        if (showsPredictedLandingBlock()) {
            if (currentHand != null) {
                aimStatus = WaterClutchTargetResolver.classifyPredictedAim(player, assessment);
            }
            WaterClutchLandingBeamRenderer.prepare(
                    assessment.landingBlockPos(),
                    Math.floorMod(client.level.getGameTime(), 40L),
                    aimStatus
            );
            WaterClutchDebugger.event("landing-beam prepared pos=%s aimStatus=%s", assessment.landingBlockPos(), aimStatus);
        }
        int impactTicks = ticksToImpact(player, assessment);
        WaterClutchDebugger.event(
                "assessment=protect distance=%.3f landingY=%.3f block=%s damage=%d ticksToImpact=%d terminalSpeed=%.3f",
                assessment.distanceToLanding(), assessment.landingY(), assessment.landingBlockPos(),
                assessment.predictedFallDamage(), impactTicks, NATURAL_TERMINAL_FALL_SPEED
        );
        if (currentHand == null) {
            WaterClutchDebugger.event("bucket-hand=none reset");
            reset(client);
            return;
        }

        if (ATTEMPT.stage == WaterClutchAttempt.Stage.IDLE) {
            if (assessment.distanceToLanding() > MIN_PREPARE_DISTANCE && impactTicks > PREPARE_TIME_TICKS) {
                WaterClutchDebugger.event("prepare-wait distance=%.3f minDistance=%.3f ticksToImpact=%d timeWindow=%d",
                        assessment.distanceToLanding(), MIN_PREPARE_DISTANCE, impactTicks, PREPARE_TIME_TICKS);
                return;
            }
            ATTEMPT.hand = currentHand;
            int maximumDelay = maximumReactionDelay(impactTicks);
            ATTEMPT.ticksRemaining = maximumDelay == 0 ? 0 : randomTicks(1, maximumDelay);
            boolean needsEarlySneak = alwaysSneaks()
                    || WaterClutchTargetResolver.predictedLandingNeedsSneak(client.level, assessment);
            WaterClutchDebugger.event(
                    "attempt-start hand=%s distance=%.3f ticksToImpact=%d allowedDelay=0..%d chosenDelay=%d earlySneak=%s",
                    currentHand, assessment.distanceToLanding(), impactTicks, maximumDelay,
                    ATTEMPT.ticksRemaining, needsEarlySneak
            );
            if (needsEarlySneak && !player.isShiftKeyDown()) {
                SNEAK.begin(client);
                ATTEMPT.stage = WaterClutchAttempt.Stage.PREPARE_SNEAK;
                WaterClutchDebugger.event("early-sneak-begin stage=PREPARE_SNEAK");
                return;
            }
            ATTEMPT.stage = WaterClutchAttempt.Stage.WAIT_REACTION;
        }

        if (currentHand != ATTEMPT.hand) {
            WaterClutchDebugger.event("bucket-hand-changed from=%s to=%s reset", ATTEMPT.hand, currentHand);
            reset(client);
            return;
        }

        if (ATTEMPT.stage == WaterClutchAttempt.Stage.WAIT_REACTION
                && !player.isShiftKeyDown()
                && (alwaysSneaks() || WaterClutchTargetResolver.predictedLandingNeedsSneak(client.level, assessment))) {
            SNEAK.begin(client);
            ATTEMPT.stage = WaterClutchAttempt.Stage.PREPARE_SNEAK;
            WaterClutchDebugger.event("updated-landing-sneak-begin stage=PREPARE_SNEAK");
            return;
        }

        switch (ATTEMPT.stage) {
            case PREPARE_SNEAK -> prepareSneak(client, player, assessment);
            case WAIT_REACTION -> waitReaction(client, player, assessment);
            case VERIFY -> verifyResult(client, player, assessment);
            case RETRY_WAIT -> waitRetry(client, player, assessment);
            case RELEASE_SNEAK -> releaseSneak(client);
            case IDLE -> {
            }
        }
    }

    private static void prepareSneak(Minecraft client, Player player, WaterClutchLandingAssessment assessment) {
        if (SNEAK.isReady(client)) {
            ATTEMPT.stage = WaterClutchAttempt.Stage.WAIT_REACTION;
            WaterClutchDebugger.event("sneak-ready stage=WAIT_REACTION remaining=%d", ATTEMPT.ticksRemaining);
            waitReaction(client, player, assessment);
        } else {
            WaterClutchDebugger.event("sneak-not-ready");
        }
    }

    private static void waitReaction(Minecraft client, Player player, WaterClutchLandingAssessment assessment) {
        if (ATTEMPT.ticksRemaining > 0) {
            if (ticksToImpact(player, assessment) <= 1) {
                WaterClutchDebugger.event("reaction-emergency-clamp from=%d to=0", ATTEMPT.ticksRemaining);
                ATTEMPT.ticksRemaining = 0;
            } else {
                ATTEMPT.ticksRemaining--;
                WaterClutchDebugger.event("reaction-countdown decrementedTo=%d", ATTEMPT.ticksRemaining);
            }
        }
        if (ATTEMPT.ticksRemaining > 0) {
            return;
        }

        WaterClutchTargetResolver.Target target = resolveCurrentTarget(client, player, assessment);
        if (target == null) {
            WaterClutchDebugger.event("reaction-armed target=none hold");
            return;
        }
        if (target.needsSneak() && !player.isShiftKeyDown()) {
            SNEAK.begin(client);
            ATTEMPT.stage = WaterClutchAttempt.Stage.PREPARE_SNEAK;
            WaterClutchDebugger.event("late-sneak-begin stage=PREPARE_SNEAK");
            return;
        }
        WaterClutchDebugger.event("reaction-armed target=%s->%s use-item-same-tick",
                target.clickedPos(), target.placePos());
        useItem(client);
    }

    private static void waitRetry(Minecraft client, Player player, WaterClutchLandingAssessment assessment) {
        if (resolveCurrentTarget(client, player, assessment) == null) {
            WaterClutchDebugger.event("retry target=none reset");
            reset(client);
            return;
        }
        if (ATTEMPT.ticksRemaining > 0) {
            ATTEMPT.ticksRemaining--;
            WaterClutchDebugger.event("retry-countdown decrementedTo=%d", ATTEMPT.ticksRemaining);
            return;
        }
        WaterClutchDebugger.event("retry-countdown elapsed use-item");
        useItem(client);
    }

    private static void useItem(Minecraft client) {
        ATTEMPT.attempts++;
        WaterClutchDebugger.event("use-item invoke attempt=%d hand=%s before=%s",
                ATTEMPT.attempts, ATTEMPT.hand, client.player == null ? "no-player" : client.player.getItemInHand(ATTEMPT.hand));
        ((MinecraftUseItemInvoker)(Object)client).newbedwarshelper$startUseItem();
        ATTEMPT.stage = WaterClutchAttempt.Stage.VERIFY;
        WaterClutchDebugger.event("use-item returned after=%s stage=VERIFY",
                client.player == null ? "no-player" : client.player.getItemInHand(ATTEMPT.hand));
    }

    private static void verifyResult(Minecraft client, Player player, WaterClutchLandingAssessment assessment) {
        if (ATTEMPT.attempts >= MAX_ATTEMPTS || resolveCurrentTarget(client, player, assessment) == null) {
            ATTEMPT.ticksRemaining = 0;
            ATTEMPT.stage = WaterClutchAttempt.Stage.RELEASE_SNEAK;
            WaterClutchDebugger.event("verify-stop attempts=%d stage=RELEASE_SNEAK", ATTEMPT.attempts);
            return;
        }
        ATTEMPT.ticksRemaining = randomTicks(2, 4);
        ATTEMPT.stage = WaterClutchAttempt.Stage.RETRY_WAIT;
        WaterClutchDebugger.event("verify-retry retryDelay=%d", ATTEMPT.ticksRemaining);
    }

    private static void releaseSneak(Minecraft client) {
        if (ATTEMPT.ticksRemaining > 0) {
            ATTEMPT.ticksRemaining--;
            WaterClutchDebugger.event("release-countdown decrementedTo=%d", ATTEMPT.ticksRemaining);
            return;
        }
        WaterClutchDebugger.event("release-countdown elapsed reset");
        reset(client);
    }

    private static String runtimeInvalidReason(Minecraft client, Player player) {
        if (!isEnabled()) return "disabled";
        if (player == null) return "no-player";
        if (client.level == null) return "no-level";
        if (client.gameMode == null) return "no-game-mode";
        if (client.gui.screen() != null) return "screen-open";
        if (!player.isAlive()) return "dead";
        if (player.isSpectator()) return "spectator";
        if (player.isCreative()) return "creative";
        if (player.isPassenger()) return "passenger";
        if (player.isFallFlying()) return "fall-flying";
        if (player.isUsingItem()) return "using-item";
        if (player.onGround()) return "on-ground";
        return null;
    }

    private static int ticksToImpact(Player player, WaterClutchLandingAssessment assessment) {
        double remainingDistance = assessment.distanceToLanding();
        double verticalVelocity = player.getDeltaMovement().y;
        for (int tick = 1; tick <= 200; tick++) {
            remainingDistance += verticalVelocity;
            if (remainingDistance <= 0.0) {
                return tick;
            }
            verticalVelocity = (verticalVelocity - NATURAL_FALL_GRAVITY) * NATURAL_FALL_DRAG;
        }
        return 200;
    }

    private static int maximumReactionDelay(int ticksToImpact) {
        int timingBudget = Math.max(0, ticksToImpact - REACTION_SAFETY_TICKS);
        return Math.min(MAX_HUMAN_REACTION_TICKS, timingBudget);
    }

    private static WaterClutchTargetResolver.Target resolveCurrentTarget(
            Minecraft client,
            Player player,
            WaterClutchLandingAssessment assessment
    ) {
        ((MinecraftUseItemInvoker)(Object)client).newbedwarshelper$pick(1.0F);
        WaterClutchDebugger.event("pick-refreshed partialTick=1.0");
        return WaterClutchTargetResolver.resolve(client, player, assessment);
    }

    private static int randomTicks(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private static void reset(Minecraft client) {
        WaterClutchDebugger.event("reset fromStage=%s sneakOwned=%s", ATTEMPT.stage, SNEAK.ownsSneak());
        SNEAK.release(client);
        ATTEMPT.reset();
    }

    public enum MobBucketOption {
        PUFFERFISH,
        SALMON,
        COD,
        TROPICAL_FISH,
        AXOLOTL,
        TADPOLE
    }
}
