package org.exmple.newbedwarshelper.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

final class DigitsOnlyEditBox extends EditBox {
    DigitsOnlyEditBox(Font font, int width, int height, Component narration) {
        super(font, width, height, narration);
    }

    @Override
    public void insertText(String input) {
        if (containsOnlyAsciiDigits(input)) {
            super.insertText(input);
        }
    }

    private static boolean containsOnlyAsciiDigits(String input) {
        for (int index = 0; index < input.length(); index++) {
            char character = input.charAt(index);
            if (character < '0' || character > '9') {
                return false;
            }
        }
        return true;
    }
}
