package org.exmple.newbedwarshelper.client.toolswitcher;

import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.exmple.newbedwarshelper.client.z_config.ModConfig;

public final class ToolSwitcherManager {
    private static final int MANUAL_SWITCH_SUPPRESSION_TICKS = 10;
    private static final int TOOL_MODE_ARM_TICKS = 10;
    private static final int TOOL_MODE_ACTIVE_TICKS = 140;
    private static final int COMBAT_STATE_TICKS = 100;
    private static final int MIN_SWORD_REACTION_TICKS = 2;
    private static final int MAX_SWORD_REACTION_TICKS = 4;

    private static ToolSwitcherPendingSwitch pendingSwitch;
    private static int manualSwitchSuppressionTicks;
    private static int attackHeldTicks;
    private static int toolModeTicksRemaining;
    private static int combatTicksRemaining;
    private static int pendingSwordSwitchTicks;
    private static int lastObservedSelectedSlot = -1;
    private static int lastAutomaticToolSlot = -1;
    private static ItemStack lastAutomaticTool = ItemStack.EMPTY;
    private static boolean requireAttackRelease;
    private static boolean initialized;

    private ToolSwitcherManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        ClientTickEvents.START_CLIENT_TICK.register(ToolSwitcherManager::onClientTick);
        initialized = true;
    }

    public static boolean isConfiguredEnabled() {
        return ModConfig.getInstance().toolSwitcher.enabled;
    }

    public static void setConfiguredEnabled(boolean enabled) {
        ModConfig config = ModConfig.getInstance();
        config.toolSwitcher.enabled = enabled;
        config.save();
        if (!enabled) {
            resetFeatureState();
        } else {
            clearPendingSwitch();
        }
    }

    public static boolean toggleEnabled() {
        boolean enabled = !isConfiguredEnabled();
        setConfiguredEnabled(enabled);
        return enabled;
    }

    public static int getMinDelayTicks() {
        return ModConfig.getInstance().toolSwitcher.minDelayTicks;
    }

    public static int getMaxDelayTicks() {
        return ModConfig.getInstance().toolSwitcher.maxDelayTicks;
    }

    public static void setDelayRange(int minDelayTicks, int maxDelayTicks) {
        int normalizedMin = Math.max(1, minDelayTicks);
        int normalizedMax = Math.max(1, maxDelayTicks);
        if (normalizedMin > normalizedMax) {
            int previousMin = normalizedMin;
            normalizedMin = normalizedMax;
            normalizedMax = previousMin;
        }

        ModConfig config = ModConfig.getInstance();
        if (config.toolSwitcher.minDelayTicks == normalizedMin
                && config.toolSwitcher.maxDelayTicks == normalizedMax) {
            return;
        }
        config.toolSwitcher.minDelayTicks = normalizedMin;
        config.toolSwitcher.maxDelayTicks = normalizedMax;
        config.save();
        clearPendingSwitch();
    }

    public static void onPlayerDamage() {
        if (!isConfiguredEnabled()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        deactivateToolMode(true);
        combatTicksRemaining = COMBAT_STATE_TICKS;
        clearPendingSwitch();

        Inventory inventory = client.player.getInventory();
        scheduleSwordSwitch(inventory);
    }

    private static void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            resetRuntimeState();
            return;
        }

        advanceTimedStates();

        Inventory inventory = client.player.getInventory();
        int selectedSlot = inventory.getSelectedSlot();
        if (lastObservedSelectedSlot >= 0 && selectedSlot != lastObservedSelectedSlot) {
            lastObservedSelectedSlot = selectedSlot;
            manualSwitchSuppressionTicks = MANUAL_SWITCH_SUPPRESSION_TICKS;
            clearPendingSwitch();
            cancelPendingSwordSwitch();
            clearAutomaticToolTracking();
            attackHeldTicks = 0;
            requireAttackRelease = true;
            return;
        }
        lastObservedSelectedSlot = selectedSlot;

        if (!isConfiguredEnabled()) {
            resetFeatureState();
            return;
        }

        boolean attackDown = client.options.keyAttack.isDown();
        if (!attackDown) {
            attackHeldTicks = 0;
            requireAttackRelease = false;
        }

        if (client.gui.screen() != null || client.player.isSpectator() || client.player.hasInfiniteMaterials()) {
            deactivateToolMode(true);
            cancelPendingSwordSwitch();
            return;
        }

        if (combatTicksRemaining > 0) {
            deactivateToolMode(true);
            processPendingSwordSwitch(inventory);
            return;
        }

        if (manualSwitchSuppressionTicks > 0) {
            manualSwitchSuppressionTicks--;
            clearPendingSwitch();
            return;
        }

        if (toolModeTicksRemaining > 0) {
            processToolSwitch(client, inventory, false);
            return;
        }

        if (requireAttackRelease || !attackDown) {
            attackHeldTicks = 0;
            clearPendingSwitch();
            return;
        }

        if (!hasValidBlockTarget(client)) {
            attackHeldTicks = 0;
            clearPendingSwitch();
            return;
        }

        attackHeldTicks++;
        if (attackHeldTicks < TOOL_MODE_ARM_TICKS) {
            return;
        }

        attackHeldTicks = 0;
        toolModeTicksRemaining = TOOL_MODE_ACTIVE_TICKS;
        requireAttackRelease = true;
        processToolSwitch(client, inventory, true);
    }

    private static void processToolSwitch(Minecraft client, Inventory inventory, boolean immediate) {
        if (!(client.hitResult instanceof BlockHitResult blockHit)) {
            clearPendingSwitch();
            return;
        }

        BlockPos blockPos = blockHit.getBlockPos();
        BlockState blockState = client.level.getBlockState(blockPos);
        if (blockState.isAir()) {
            clearPendingSwitch();
            return;
        }

        ToolSwitcherCandidate candidate = ToolSwitcherResolver.findBest(inventory, blockState);
        if (candidate == null) {
            clearPendingSwitch();
            return;
        }

        if (immediate) {
            selectAutomaticTool(inventory, candidate);
            return;
        }

        if (!matchesPendingTarget(blockPos, blockState, candidate)) {
            int delay = (int)ThreadLocalRandom.current().nextLong(minDelayTicks(), (long)maxDelayTicks() + 1L);
            pendingSwitch = new ToolSwitcherPendingSwitch(blockPos.immutable(), blockState, candidate, delay);
            return;
        }

        if (!isCandidateStillValid(inventory, pendingSwitch.candidate())) {
            clearPendingSwitch();
            return;
        }

        if (pendingSwitch.ticksRemaining() > 1) {
            pendingSwitch = pendingSwitch.tickDown();
            return;
        }

        selectAutomaticTool(inventory, pendingSwitch.candidate());
    }

    private static void processPendingSwordSwitch(Inventory inventory) {
        clearPendingSwitch();
        if (pendingSwordSwitchTicks <= 0) {
            return;
        }

        if (!isStillHoldingAutomaticTool(inventory)) {
            cancelPendingSwordSwitch();
            clearAutomaticToolTracking();
            return;
        }

        if (pendingSwordSwitchTicks > 1) {
            pendingSwordSwitchTicks--;
            return;
        }

        int swordSlot = ToolSwitcherWeaponResolver.findBestSwordSlot(inventory);
        if (swordSlot >= 0 && swordSlot != inventory.getSelectedSlot()) {
            inventory.setSelectedSlot(swordSlot);
            lastObservedSelectedSlot = swordSlot;
        }
        cancelPendingSwordSwitch();
        clearAutomaticToolTracking();
    }

    private static void selectAutomaticTool(Inventory inventory, ToolSwitcherCandidate candidate) {
        inventory.setSelectedSlot(candidate.slot());
        lastObservedSelectedSlot = candidate.slot();
        lastAutomaticToolSlot = candidate.slot();
        lastAutomaticTool = candidate.stackSnapshot().copy();
        clearPendingSwitch();
    }

    private static boolean hasValidBlockTarget(Minecraft client) {
        if (!(client.hitResult instanceof BlockHitResult blockHit)) {
            return false;
        }
        return !client.level.getBlockState(blockHit.getBlockPos()).isAir();
    }

    private static boolean isStillHoldingAutomaticTool(Inventory inventory) {
        return lastAutomaticToolSlot >= 0
                && inventory.getSelectedSlot() == lastAutomaticToolSlot
                && ItemStack.isSameItem(lastAutomaticTool, inventory.getItem(lastAutomaticToolSlot));
    }

    private static void advanceTimedStates() {
        if (toolModeTicksRemaining > 0) {
            toolModeTicksRemaining--;
            if (toolModeTicksRemaining == 0) {
                clearPendingSwitch();
                requireAttackRelease = true;
            }
        }

        if (combatTicksRemaining > 0) {
            combatTicksRemaining--;
        }
    }

    private static void scheduleSwordSwitch(Inventory inventory) {
        if (isStillHoldingAutomaticTool(inventory)) {
            pendingSwordSwitchTicks = ThreadLocalRandom.current().nextInt(
                    MIN_SWORD_REACTION_TICKS,
                    MAX_SWORD_REACTION_TICKS + 1
            );
        } else {
            cancelPendingSwordSwitch();
        }
    }

    private static void deactivateToolMode(boolean waitForAttackRelease) {
        attackHeldTicks = 0;
        toolModeTicksRemaining = 0;
        requireAttackRelease = waitForAttackRelease;
        clearPendingSwitch();
    }

    private static void cancelPendingSwordSwitch() {
        pendingSwordSwitchTicks = 0;
    }

    private static void clearAutomaticToolTracking() {
        lastAutomaticToolSlot = -1;
        lastAutomaticTool = ItemStack.EMPTY;
    }

    private static boolean matchesPendingTarget(BlockPos blockPos, BlockState state, ToolSwitcherCandidate candidate) {
        return pendingSwitch != null
                && pendingSwitch.blockPos().equals(blockPos)
                && pendingSwitch.blockState().equals(state)
                && pendingSwitch.candidate().slot() == candidate.slot()
                && ItemStack.isSameItemSameComponents(pendingSwitch.candidate().stackSnapshot(), candidate.stackSnapshot());
    }

    private static boolean isCandidateStillValid(Inventory inventory, ToolSwitcherCandidate candidate) {
        ItemStack currentStack = inventory.getItem(candidate.slot());
        return ItemStack.isSameItemSameComponents(candidate.stackSnapshot(), currentStack);
    }

    private static int minDelayTicks() {
        return ModConfig.getInstance().toolSwitcher.minDelayTicks;
    }

    private static int maxDelayTicks() {
        return ModConfig.getInstance().toolSwitcher.maxDelayTicks;
    }

    private static void clearPendingSwitch() {
        pendingSwitch = null;
    }

    private static void resetRuntimeState() {
        resetFeatureState();
        manualSwitchSuppressionTicks = 0;
        lastObservedSelectedSlot = -1;
    }

    private static void resetFeatureState() {
        clearPendingSwitch();
        cancelPendingSwordSwitch();
        clearAutomaticToolTracking();
        attackHeldTicks = 0;
        toolModeTicksRemaining = 0;
        combatTicksRemaining = 0;
        requireAttackRelease = false;
    }
}
