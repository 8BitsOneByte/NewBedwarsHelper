package org.exmple.newbedwarshelper.client.toolswitcher;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;

final class ToolSwitcherResolver {
    private static final int HOTBAR_SIZE = 9;
    private static final float SPEED_EPSILON = 0.0001F;

    private ToolSwitcherResolver() {
    }

    static ToolSwitcherCandidate findBest(Inventory inventory, BlockState state) {
        int selectedSlot = inventory.getSelectedSlot();
        Holder<Enchantment> efficiency = inventory.player.level().registryAccess().getOrThrow(Enchantments.EFFICIENCY);
        float selectedSpeed = getEffectiveDestroySpeed(inventory.getItem(selectedSlot), state, efficiency);
        ToolSwitcherCandidate best = null;

        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            if (slot == selectedSlot) {
                continue;
            }

            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !hasMatchingToolRule(stack, state)) {
                continue;
            }

            float speed = getEffectiveDestroySpeed(stack, state, efficiency);
            if (speed <= selectedSpeed + SPEED_EPSILON) {
                continue;
            }

            if (best == null
                    || speed > best.destroySpeed() + SPEED_EPSILON
                    || Math.abs(speed - best.destroySpeed()) <= SPEED_EPSILON
                    && isCloserToSelected(slot, best.slot(), selectedSlot)) {
                best = new ToolSwitcherCandidate(slot, stack.copy(), speed);
            }
        }

        return best;
    }

    private static boolean hasMatchingToolRule(ItemStack stack, BlockState state) {
        Tool tool = stack.get(DataComponents.TOOL);
        if (tool == null) {
            return false;
        }

        return tool.rules().stream().anyMatch(rule -> rule.speed().isPresent() && state.is(rule.blocks()));
    }

    private static float getEffectiveDestroySpeed(ItemStack stack, BlockState state, Holder<Enchantment> efficiency) {
        float speed = stack.getDestroySpeed(state);
        if (speed > 1.0F) {
            int efficiencyLevel = EnchantmentHelper.getItemEnchantmentLevel(efficiency, stack);
            speed += efficiencyLevel * efficiencyLevel;
        }
        return speed;
    }

    private static boolean isCloserToSelected(int candidateSlot, int existingSlot, int selectedSlot) {
        int candidateDistance = hotbarDistance(candidateSlot, selectedSlot);
        int existingDistance = hotbarDistance(existingSlot, selectedSlot);
        return candidateDistance < existingDistance
                || candidateDistance == existingDistance && candidateSlot < existingSlot;
    }

    private static int hotbarDistance(int first, int second) {
        int direct = Math.abs(first - second);
        return Math.min(direct, HOTBAR_SIZE - direct);
    }
}
