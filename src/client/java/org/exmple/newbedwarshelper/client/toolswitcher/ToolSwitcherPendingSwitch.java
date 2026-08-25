package org.exmple.newbedwarshelper.client.toolswitcher;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

record ToolSwitcherPendingSwitch(
        BlockPos blockPos,
        BlockState blockState,
        ToolSwitcherCandidate candidate,
        int ticksRemaining
) {
    ToolSwitcherPendingSwitch tickDown() {
        return new ToolSwitcherPendingSwitch(blockPos, blockState, candidate, ticksRemaining - 1);
    }
}
