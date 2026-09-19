package org.exmple.newbedwarshelper.client.waterclutchhelper;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.exmple.newbedwarshelper.client.z_config.ModConfig;

public final class WaterClutchBucketPolicy {
    private WaterClutchBucketPolicy() {
    }

    public static InteractionHand findUsableHand(Player player) {
        if (isSupportedFilledBucket(player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (player.getMainHandItem().isEmpty() && isSupportedFilledBucket(player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    public static boolean isSupportedFilledBucket(ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET)) {
            return true;
        }

        ModConfig.WaterClutchConfig config = ModConfig.getInstance().waterClutch;
        Item item = stack.getItem();
        return item == Items.PUFFERFISH_BUCKET && config.pufferfishBucket
                || item == Items.SALMON_BUCKET && config.salmonBucket
                || item == Items.COD_BUCKET && config.codBucket
                || item == Items.TROPICAL_FISH_BUCKET && config.tropicalFishBucket
                || item == Items.AXOLOTL_BUCKET && config.axolotlBucket
                || item == Items.TADPOLE_BUCKET && config.tadpoleBucket;
    }
}
