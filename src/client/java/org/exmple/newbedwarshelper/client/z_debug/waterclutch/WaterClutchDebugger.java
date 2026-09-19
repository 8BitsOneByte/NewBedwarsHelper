package org.exmple.newbedwarshelper.client.z_debug.waterclutch;

import com.mojang.logging.LogUtils;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;

/**
 * Temporary tick-accurate trace for the water-clutch state machine.
 * This debugger is active only when NewbedwarshelperClient explicitly calls init().
 */
public final class WaterClutchDebugger {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final StringBuilder EVENTS = new StringBuilder(512);
    private static boolean initialized;
    private static boolean capturing;
    private static long internalTick;

    private WaterClutchDebugger() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        LOGGER.info("[NBH Water Clutch Debug] enabled; tick traces are written to the normal Minecraft log");
    }

    public static void beginTick(Minecraft client, boolean featureEnabled, String stage, int ticksRemaining, int attempts) {
        capturing = initialized && (featureEnabled || !"IDLE".equals(stage));
        EVENTS.setLength(0);
        if (!capturing) {
            return;
        }

        internalTick++;
        long gameTick = client.level == null ? -1L : client.level.getGameTime();
        Player player = client.player;
        append("tick=%d gameTick=%d beginStage=%s remaining=%d attempts=%d",
                internalTick, gameTick, stage, ticksRemaining, attempts);
        if (player == null) {
            append("player=null");
            return;
        }

        append("pos=(%.3f,%.3f,%.3f) velocity=(%.3f,%.3f,%.3f) rotation=(%.3f,%.3f) fallDistance=%.3f onGround=%s",
                player.getX(), player.getY(), player.getZ(),
                player.getDeltaMovement().x, player.getDeltaMovement().y, player.getDeltaMovement().z,
                player.getYRot(), player.getXRot(), player.fallDistance, player.onGround());
        append("main=%s off=%s shift=%s using=%s screen=%s hit=%s",
                player.getMainHandItem(), player.getOffhandItem(), player.isShiftKeyDown(), player.isUsingItem(),
                client.gui.screen() == null ? "none" : client.gui.screen().getClass().getSimpleName(),
                describeHit(client));
    }

    public static void event(String format, Object... arguments) {
        if (!capturing) {
            return;
        }
        append(format, arguments);
    }

    public static void endTick(String stage, int ticksRemaining, int attempts) {
        if (!capturing) {
            return;
        }
        append("endStage=%s remaining=%d attempts=%d", stage, ticksRemaining, attempts);
        LOGGER.info("[NBH Water Clutch Tick] {}", EVENTS);
        capturing = false;
    }

    private static void append(String format, Object... arguments) {
        if (!EVENTS.isEmpty()) {
            EVENTS.append(" | ");
        }
        EVENTS.append(String.format(Locale.ROOT, format, arguments));
    }

    private static String describeHit(Minecraft client) {
        if (client.hitResult instanceof BlockHitResult blockHit) {
            return "BLOCK@" + blockHit.getBlockPos() + "/" + blockHit.getDirection();
        }
        return client.hitResult == null ? "null" : client.hitResult.getType().name();
    }
}
