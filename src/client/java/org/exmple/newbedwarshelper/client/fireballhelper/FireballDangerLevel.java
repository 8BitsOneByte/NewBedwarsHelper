package org.exmple.newbedwarshelper.client.fireballhelper;

public enum FireballDangerLevel {
    SELF_AIM(0x66FFD21F, 0x805F6FFF),
    OTHER_AIM(0x78FF8C1A, 0x805F6FFF),
    IN_FLIGHT(0x82FF2E2E, 0x82FF28C8);

    private final int fillColor;
    private final int contrastFillColor;

    FireballDangerLevel(int fillColor, int contrastFillColor) {
        this.fillColor = fillColor;
        this.contrastFillColor = contrastFillColor;
    }

    public int fillColor() {
        return this.fillColor;
    }

    public int contrastFillColor() {
        return this.contrastFillColor;
    }
}
