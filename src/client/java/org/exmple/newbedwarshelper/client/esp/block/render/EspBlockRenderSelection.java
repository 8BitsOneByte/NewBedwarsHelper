package org.exmple.newbedwarshelper.client.esp.block.render;

import java.util.List;

public record EspBlockRenderSelection(List<EspBlockSelectedRenderEntry> entries, int lineCount) {
    public static final EspBlockRenderSelection EMPTY = new EspBlockRenderSelection(List.of(), 0);
}
