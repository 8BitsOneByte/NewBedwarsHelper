package org.exmple.newbedwarshelper.client.waterclutchhelper;

import net.minecraft.world.InteractionHand;

final class WaterClutchAttempt {
    Stage stage = Stage.IDLE;
    InteractionHand hand;
    int ticksRemaining;
    int attempts;

    void reset() {
        this.stage = Stage.IDLE;
        this.hand = null;
        this.ticksRemaining = 0;
        this.attempts = 0;
    }

    enum Stage {
        IDLE,
        PREPARE_SNEAK,
        WAIT_REACTION,
        VERIFY,
        RETRY_WAIT,
        RELEASE_SNEAK
    }
}
