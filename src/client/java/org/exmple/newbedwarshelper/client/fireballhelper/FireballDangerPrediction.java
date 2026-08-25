package org.exmple.newbedwarshelper.client.fireballhelper;

import net.minecraft.world.phys.Vec3;

public record FireballDangerPrediction(Vec3 center, double radius, FireballDangerLevel level) {
}
