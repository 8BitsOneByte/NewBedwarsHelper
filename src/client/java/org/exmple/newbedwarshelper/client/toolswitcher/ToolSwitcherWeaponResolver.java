package org.exmple.newbedwarshelper.client.toolswitcher;

import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

final class ToolSwitcherWeaponResolver {
    private static final int HOTBAR_SIZE = 9;
    private static final double SCORE_EPSILON = 0.0001D;

    private ToolSwitcherWeaponResolver() {
    }

    static int findBestSwordSlot(Inventory inventory) {
        int selectedSlot = inventory.getSelectedSlot();
        int bestSlot = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        double baseAttackDamage = inventory.player.getAttributeBaseValue(Attributes.ATTACK_DAMAGE);

        for (int slot = 0; slot < HOTBAR_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || !stack.is(ItemTags.SWORDS)) {
                continue;
            }

            ItemAttributeModifiers modifiers = stack.getOrDefault(
                    DataComponents.ATTRIBUTE_MODIFIERS,
                    ItemAttributeModifiers.EMPTY
            );
            double score = modifiers.compute(Attributes.ATTACK_DAMAGE, baseAttackDamage, EquipmentSlot.MAINHAND);
            if (bestSlot < 0
                    || score > bestScore + SCORE_EPSILON
                    || Math.abs(score - bestScore) <= SCORE_EPSILON
                    && isCloserToSelected(slot, bestSlot, selectedSlot)) {
                bestSlot = slot;
                bestScore = score;
            }
        }

        return bestSlot;
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
