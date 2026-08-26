package org.exmple.newbedwarshelper.client.mixin.itemprotection;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.exmple.newbedwarshelper.client.itemprotection.ItemProtectionManager;
import org.exmple.newbedwarshelper.client.itemprotection.ItemProtectionOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudItemProtectionMixin {
    @Inject(
            method = "extractItemHotbar",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/Hud;extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V",
                    ordinal = 0,
                    shift = At.Shift.AFTER
            )
    )
    private void newbedwarshelper$drawProtectedHotbarItemIcon(
            CallbackInfo callbackInfo,
            @Local(name = "graphics") GuiGraphicsExtractor graphics,
            @Local(name = "i") int index,
            @Local(name = "x") int x,
            @Local(name = "y") int y,
            @Local(name = "player") Player player
    ) {
        if (ItemProtectionManager.isProtected(player.getInventory().getNonEquipmentItems().get(index))) {
            ItemProtectionOverlay.draw(graphics, x, y);
        }
    }

    @Inject(method = "extractItemHotbar", at = @At("TAIL"))
    private void newbedwarshelper$drawProtectedOffhandItemIcon(
            GuiGraphicsExtractor graphics,
            DeltaTracker deltaTracker,
            CallbackInfo callbackInfo
    ) {
        if (!(Minecraft.getInstance().getCameraEntity() instanceof Player player)) {
            return;
        }

        ItemStack offhandStack = player.getOffhandItem();
        if (!ItemProtectionManager.isProtected(offhandStack)) {
            return;
        }

        int centerX = graphics.guiWidth() / 2;
        int x = player.getMainArm().getOpposite() == HumanoidArm.LEFT
                ? centerX - 91 - 26
                : centerX + 91 + 10;
        int y = graphics.guiHeight() - 16 - 3;
        ItemProtectionOverlay.draw(graphics, x, y);
    }
}
