package org.exmple.newbedwarshelper.client.mixin.itemprotection;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.exmple.newbedwarshelper.client.itemprotection.ItemProtectionManager;
import org.exmple.newbedwarshelper.client.itemprotection.ItemProtectionOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenItemProtectionMixin<T extends AbstractContainerMenu> extends Screen {
    private static final int OUT_OF_BOUNDS_SLOT = -999;

    @Shadow
    @Final
    protected T menu;

    protected AbstractContainerScreenItemProtectionMixin(Component title) {
        super(title);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void newbedwarshelper$preventClosingWithProtectedCarriedItem(
            KeyEvent event,
            CallbackInfoReturnable<Boolean> callbackInfo
    ) {
        Minecraft client = Minecraft.getInstance();
        boolean manualClose = event.isEscape() || client.options.keyInventory.matches(event);
        ItemStack carried = this.menu.getCarried();
        if (!manualClose
                || !ItemProtectionManager.isProtected(carried)
                || ItemProtectionManager.canFullyReturnToInventory(client.player, carried)) {
            return;
        }

        callbackInfo.setReturnValue(true);
    }

    @Inject(
            method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void newbedwarshelper$preventProtectedItemGuiDrop(
            Slot slot,
            int slotId,
            int button,
            ContainerInput input,
        CallbackInfo callbackInfo
    ) {
        if (slotId == OUT_OF_BOUNDS_SLOT && ItemProtectionManager.isProtected(this.menu.getCarried())) {
            callbackInfo.cancel();
            return;
        }

        if (input == ContainerInput.THROW && slot != null && ItemProtectionManager.isProtected(slot.getItem())) {
            callbackInfo.cancel();
        }
    }

    @Inject(
            method = "extractSlot",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;item(Lnet/minecraft/world/item/ItemStack;III)V",
                    shift = At.Shift.AFTER
            )
    )
    private void newbedwarshelper$drawProtectedItemIcon(
            CallbackInfo callbackInfo,
            @Local(name = "graphics") GuiGraphicsExtractor graphics,
            @Local(name = "slot") Slot slot
    ) {
        if (ItemProtectionManager.isProtected(slot.getItem())) {
            ItemProtectionOverlay.draw(graphics, slot.x, slot.y);
        }
    }
}
