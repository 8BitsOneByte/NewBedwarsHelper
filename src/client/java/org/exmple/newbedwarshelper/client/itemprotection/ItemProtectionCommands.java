package org.exmple.newbedwarshelper.client.itemprotection;

import com.mojang.brigadier.Command;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class ItemProtectionCommands {
    private static final String PROTECTED_KEY = "commands.newbedwarshelper.itemprotection.protected";
    private static final String ALREADY_PROTECTED_KEY = "commands.newbedwarshelper.itemprotection.already_protected";
    private static final String UNPROTECTED_KEY = "commands.newbedwarshelper.itemprotection.unprotected";
    private static final String NOT_PROTECTED_KEY = "commands.newbedwarshelper.itemprotection.not_protected";
    private static final String NO_ITEM_KEY = "commands.newbedwarshelper.itemprotection.no_item";
    private static final String SIGNATURE_FAILED_KEY = "commands.newbedwarshelper.itemprotection.signature_failed";

    private ItemProtectionCommands() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommands.literal("protecthanditem")
                    .executes(context -> protect(context.getSource())));
            dispatcher.register(ClientCommands.literal("unprotecthanditem")
                    .executes(context -> unprotect(context.getSource())));
        });
    }

    private static int protect(FabricClientCommandSource source) {
        ItemStack heldItem = source.getPlayer().getMainHandItem();
        ItemProtectionManager.ChangeResult result = ItemProtectionManager.protect(heldItem);
        switch (result) {
            case CHANGED -> source.sendFeedback(Component.translatable(PROTECTED_KEY, heldItem.getHoverName())
                    .withStyle(ChatFormatting.GREEN));
            case ALREADY_PRESENT -> source.sendFeedback(Component.translatable(ALREADY_PROTECTED_KEY, heldItem.getHoverName())
                    .withStyle(ChatFormatting.YELLOW));
            case NO_ITEM -> source.sendFeedback(Component.translatable(NO_ITEM_KEY).withStyle(ChatFormatting.RED));
            case SIGNATURE_FAILED -> source.sendFeedback(Component.translatable(SIGNATURE_FAILED_KEY).withStyle(ChatFormatting.RED));
            case NOT_PRESENT -> {
                return 0;
            }
        }
        return result == ItemProtectionManager.ChangeResult.CHANGED ? Command.SINGLE_SUCCESS : 0;
    }

    private static int unprotect(FabricClientCommandSource source) {
        ItemStack heldItem = source.getPlayer().getMainHandItem();
        ItemProtectionManager.ChangeResult result = ItemProtectionManager.unprotect(heldItem);
        switch (result) {
            case CHANGED -> source.sendFeedback(Component.translatable(UNPROTECTED_KEY, heldItem.getHoverName())
                    .withStyle(ChatFormatting.GREEN));
            case NOT_PRESENT -> source.sendFeedback(Component.translatable(NOT_PROTECTED_KEY, heldItem.getHoverName())
                    .withStyle(ChatFormatting.YELLOW));
            case NO_ITEM -> source.sendFeedback(Component.translatable(NO_ITEM_KEY).withStyle(ChatFormatting.RED));
            case SIGNATURE_FAILED -> source.sendFeedback(Component.translatable(SIGNATURE_FAILED_KEY).withStyle(ChatFormatting.RED));
            case ALREADY_PRESENT -> {
                return 0;
            }
        }
        return result == ItemProtectionManager.ChangeResult.CHANGED ? Command.SINGLE_SUCCESS : 0;
    }
}
