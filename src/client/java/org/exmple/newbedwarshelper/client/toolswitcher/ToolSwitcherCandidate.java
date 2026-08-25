package org.exmple.newbedwarshelper.client.toolswitcher;

import net.minecraft.world.item.ItemStack;

public record ToolSwitcherCandidate(int slot, ItemStack stackSnapshot, float destroySpeed) {
}
