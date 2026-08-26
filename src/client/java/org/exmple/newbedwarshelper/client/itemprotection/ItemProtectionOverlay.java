package org.exmple.newbedwarshelper.client.itemprotection;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.exmple.newbedwarshelper.ModConstants;

public final class ItemProtectionOverlay {
    private static final Identifier ICON = Identifier.fromNamespaceAndPath(ModConstants.MOD_ID, "item_protection");
    private static final int ICON_SIZE = 8;

    private ItemProtectionOverlay() {
    }

    public static void draw(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                ICON,
                x,
                y,
                ICON_SIZE,
                ICON_SIZE
        );
    }
}
