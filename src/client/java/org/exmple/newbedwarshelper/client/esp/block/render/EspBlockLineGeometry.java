package org.exmple.newbedwarshelper.client.esp.block.render;

final class EspBlockLineGeometry {
    static final int X1_Z1_VERTICAL = 1 << 0;
    static final int X1_Z2_VERTICAL = 1 << 1;
    static final int X2_Z1_VERTICAL = 1 << 2;
    static final int X2_Z2_VERTICAL = 1 << 3;
    static final int BOTTOM_BACK_X = 1 << 4;
    static final int BOTTOM_FRONT_X = 1 << 5;
    static final int TOP_BACK_X = 1 << 6;
    static final int TOP_FRONT_X = 1 << 7;
    static final int BOTTOM_LEFT_Z = 1 << 8;
    static final int BOTTOM_RIGHT_Z = 1 << 9;
    static final int TOP_LEFT_Z = 1 << 10;
    static final int TOP_RIGHT_Z = 1 << 11;
    private static final int ALL_EDGES = (1 << 12) - 1;

    private EspBlockLineGeometry() {
    }

    static int edgeMask(int neighbours) {
        if (neighbours == 0) {
            return ALL_EDGES;
        }

        int edges = 0;
        if (((neighbours & EspBlockNeighbourFlags.LE) != EspBlockNeighbourFlags.LE && (neighbours & EspBlockNeighbourFlags.BA) != EspBlockNeighbourFlags.BA)
                || ((neighbours & EspBlockNeighbourFlags.LE) == EspBlockNeighbourFlags.LE && (neighbours & EspBlockNeighbourFlags.BA) == EspBlockNeighbourFlags.BA && (neighbours & EspBlockNeighbourFlags.BA_LE) != EspBlockNeighbourFlags.BA_LE)) {
            edges |= X1_Z1_VERTICAL;
        }
        if (((neighbours & EspBlockNeighbourFlags.LE) != EspBlockNeighbourFlags.LE && (neighbours & EspBlockNeighbourFlags.FO) != EspBlockNeighbourFlags.FO)
                || ((neighbours & EspBlockNeighbourFlags.LE) == EspBlockNeighbourFlags.LE && (neighbours & EspBlockNeighbourFlags.FO) == EspBlockNeighbourFlags.FO && (neighbours & EspBlockNeighbourFlags.FO_LE) != EspBlockNeighbourFlags.FO_LE)) {
            edges |= X1_Z2_VERTICAL;
        }
        if (((neighbours & EspBlockNeighbourFlags.RI) != EspBlockNeighbourFlags.RI && (neighbours & EspBlockNeighbourFlags.BA) != EspBlockNeighbourFlags.BA)
                || ((neighbours & EspBlockNeighbourFlags.RI) == EspBlockNeighbourFlags.RI && (neighbours & EspBlockNeighbourFlags.BA) == EspBlockNeighbourFlags.BA && (neighbours & EspBlockNeighbourFlags.BA_RI) != EspBlockNeighbourFlags.BA_RI)) {
            edges |= X2_Z1_VERTICAL;
        }
        if (((neighbours & EspBlockNeighbourFlags.RI) != EspBlockNeighbourFlags.RI && (neighbours & EspBlockNeighbourFlags.FO) != EspBlockNeighbourFlags.FO)
                || ((neighbours & EspBlockNeighbourFlags.RI) == EspBlockNeighbourFlags.RI && (neighbours & EspBlockNeighbourFlags.FO) == EspBlockNeighbourFlags.FO && (neighbours & EspBlockNeighbourFlags.FO_RI) != EspBlockNeighbourFlags.FO_RI)) {
            edges |= X2_Z2_VERTICAL;
        }
        if (((neighbours & EspBlockNeighbourFlags.BA) != EspBlockNeighbourFlags.BA && (neighbours & EspBlockNeighbourFlags.BO) != EspBlockNeighbourFlags.BO)
                || ((neighbours & EspBlockNeighbourFlags.BA) != EspBlockNeighbourFlags.BA && (neighbours & EspBlockNeighbourFlags.BO_BA) == EspBlockNeighbourFlags.BO_BA)) {
            edges |= BOTTOM_BACK_X;
        }
        if (((neighbours & EspBlockNeighbourFlags.FO) != EspBlockNeighbourFlags.FO && (neighbours & EspBlockNeighbourFlags.BO) != EspBlockNeighbourFlags.BO)
                || ((neighbours & EspBlockNeighbourFlags.FO) != EspBlockNeighbourFlags.FO && (neighbours & EspBlockNeighbourFlags.BO_FO) == EspBlockNeighbourFlags.BO_FO)) {
            edges |= BOTTOM_FRONT_X;
        }
        if (((neighbours & EspBlockNeighbourFlags.BA) != EspBlockNeighbourFlags.BA && (neighbours & EspBlockNeighbourFlags.TO) != EspBlockNeighbourFlags.TO)
                || ((neighbours & EspBlockNeighbourFlags.BA) != EspBlockNeighbourFlags.BA && (neighbours & EspBlockNeighbourFlags.TO_BA) == EspBlockNeighbourFlags.TO_BA)) {
            edges |= TOP_BACK_X;
        }
        if (((neighbours & EspBlockNeighbourFlags.FO) != EspBlockNeighbourFlags.FO && (neighbours & EspBlockNeighbourFlags.TO) != EspBlockNeighbourFlags.TO)
                || ((neighbours & EspBlockNeighbourFlags.FO) != EspBlockNeighbourFlags.FO && (neighbours & EspBlockNeighbourFlags.TO_FO) == EspBlockNeighbourFlags.TO_FO)) {
            edges |= TOP_FRONT_X;
        }
        if (((neighbours & EspBlockNeighbourFlags.LE) != EspBlockNeighbourFlags.LE && (neighbours & EspBlockNeighbourFlags.BO) != EspBlockNeighbourFlags.BO)
                || ((neighbours & EspBlockNeighbourFlags.LE) != EspBlockNeighbourFlags.LE && (neighbours & EspBlockNeighbourFlags.BO_LE) == EspBlockNeighbourFlags.BO_LE)) {
            edges |= BOTTOM_LEFT_Z;
        }
        if (((neighbours & EspBlockNeighbourFlags.RI) != EspBlockNeighbourFlags.RI && (neighbours & EspBlockNeighbourFlags.BO) != EspBlockNeighbourFlags.BO)
                || ((neighbours & EspBlockNeighbourFlags.RI) != EspBlockNeighbourFlags.RI && (neighbours & EspBlockNeighbourFlags.BO_RI) == EspBlockNeighbourFlags.BO_RI)) {
            edges |= BOTTOM_RIGHT_Z;
        }
        if (((neighbours & EspBlockNeighbourFlags.LE) != EspBlockNeighbourFlags.LE && (neighbours & EspBlockNeighbourFlags.TO) != EspBlockNeighbourFlags.TO)
                || ((neighbours & EspBlockNeighbourFlags.LE) != EspBlockNeighbourFlags.LE && (neighbours & EspBlockNeighbourFlags.TO_LE) == EspBlockNeighbourFlags.TO_LE)) {
            edges |= TOP_LEFT_Z;
        }
        if (((neighbours & EspBlockNeighbourFlags.RI) != EspBlockNeighbourFlags.RI && (neighbours & EspBlockNeighbourFlags.TO) != EspBlockNeighbourFlags.TO)
                || ((neighbours & EspBlockNeighbourFlags.RI) != EspBlockNeighbourFlags.RI && (neighbours & EspBlockNeighbourFlags.TO_RI) == EspBlockNeighbourFlags.TO_RI)) {
            edges |= TOP_RIGHT_Z;
        }
        return edges;
    }

    static int lineCount(int neighbours) {
        return Integer.bitCount(edgeMask(neighbours));
    }
}
