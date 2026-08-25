package org.exmple.newbedwarshelper.client.esp.block.render;

import net.minecraft.world.level.block.Block;
import org.exmple.newbedwarshelper.client.esp.block.EspBlockStorage;
import org.exmple.newbedwarshelper.client.esp.block.EspBlockTarget;
import org.exmple.newbedwarshelper.client.esp.blockentity.EspBlockEntityStorage;

public final class EspBlockRenderTargetResolver {
    private EspBlockRenderTargetResolver() {
    }

    public static EspBlockRenderTarget targetForBlock(Block block) {
        EspBlockTarget blockTarget = EspBlockStorage.targetForBlock(block);
        return blockTarget != null ? blockTarget : EspBlockEntityStorage.targetForBlock(block);
    }

    public static boolean hasAnyEnabledTarget() {
        return EspBlockStorage.hasAnyEnabledBlockTarget()
                || EspBlockEntityStorage.hasAnyEnabledBlockEntityTarget();
    }
}
