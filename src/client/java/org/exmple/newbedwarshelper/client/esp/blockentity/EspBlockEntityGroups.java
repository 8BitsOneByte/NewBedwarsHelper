package org.exmple.newbedwarshelper.client.esp.blockentity;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EspBlockEntityGroups {
    public static final EspBlockEntityGroup STORAGE_BLOCKS = new EspBlockEntityGroup(
            "screen.newbedwarshelper.esp_whitelist.block_entity.group.storage_blocks",
            StorageTargets.create()
    );
    public static final EspBlockEntityGroup FUNCTIONAL_BLOCKS = new EspBlockEntityGroup(
            "screen.newbedwarshelper.esp_whitelist.block_entity.group.functional_blocks",
            FunctionalTargets.create()
    );
    public static final EspBlockEntityGroup REDSTONE_COMPONENTS = new EspBlockEntityGroup(
            "screen.newbedwarshelper.esp_whitelist.block_entity.group.redstone_components",
            RedstoneTargets.create()
    );
    public static final EspBlockEntityGroup SPAWNER_BLOCKS = new EspBlockEntityGroup(
            "screen.newbedwarshelper.esp_whitelist.block_entity.group.spawner_blocks",
            SpawnerTargets.create()
    );
    public static final EspBlockEntityGroup MISC = new EspBlockEntityGroup(
            "screen.newbedwarshelper.esp_whitelist.block_entity.group.misc",
            MiscTargets.create()
    );

    public static final List<EspBlockEntityGroup> ALL = List.of(
            STORAGE_BLOCKS,
            FUNCTIONAL_BLOCKS,
            REDSTONE_COMPONENTS,
            SPAWNER_BLOCKS,
            MISC
    );
    public static final List<EspBlockEntityTarget> ALL_TARGETS = createAllTargets();
    private static final Map<Block, EspBlockEntityTarget> TARGET_BY_BLOCK = createTargetByBlock();

    private EspBlockEntityGroups() {
    }

    public static EspBlockEntityTarget targetForBlock(Block block) {
        return TARGET_BY_BLOCK.get(block);
    }

    private static EspBlockEntityTarget target(String id, int color, String... blockIds) {
        List<Block> blocks = new ArrayList<>(blockIds.length);
        for (String blockId : blockIds) {
            blocks.add(BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("minecraft", blockId)));
        }
        return new EspBlockEntityTarget(id, "block_entity.newbedwarshelper." + id, blocks, color);
    }

    private static List<EspBlockEntityTarget> createAllTargets() {
        List<EspBlockEntityTarget> targets = new ArrayList<>();
        for (EspBlockEntityGroup group : ALL) {
            targets.addAll(group.targets());
        }
        return List.copyOf(targets);
    }

    private static Map<Block, EspBlockEntityTarget> createTargetByBlock() {
        Map<Block, EspBlockEntityTarget> targetsByBlock = new HashMap<>();
        for (EspBlockEntityTarget target : ALL_TARGETS) {
            for (Block block : target.blocks()) {
                EspBlockEntityTarget previous = targetsByBlock.put(block, target);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate block entity ESP target for " + BuiltInRegistries.BLOCK.getKey(block));
                }
            }
        }
        return Map.copyOf(targetsByBlock);
    }

    private static final class StorageTargets {
        private static List<EspBlockEntityTarget> create() {
            List<EspBlockEntityTarget> targets = new ArrayList<>();
            targets.add(target("chest", EspBlockEntityColors.CHEST, "chest"));
            targets.add(target("copper_chest", EspBlockEntityColors.CHEST, "copper_chest", "exposed_copper_chest", "weathered_copper_chest", "oxidized_copper_chest"));
            targets.add(target("waxed_copper_chest", EspBlockEntityColors.CHEST, "waxed_copper_chest", "waxed_exposed_copper_chest", "waxed_weathered_copper_chest", "waxed_oxidized_copper_chest"));
            targets.add(target("trapped_chest", EspBlockEntityColors.TRAPPED_CHEST, "trapped_chest"));
            targets.add(target("ender_chest", EspBlockEntityColors.ENDER_CHEST, "ender_chest"));
            targets.add(target("shulker_box", EspBlockEntityColors.SHULKER_BOX, "shulker_box", "white_shulker_box", "orange_shulker_box", "magenta_shulker_box", "light_blue_shulker_box", "yellow_shulker_box", "lime_shulker_box", "pink_shulker_box", "gray_shulker_box", "light_gray_shulker_box", "cyan_shulker_box", "purple_shulker_box", "blue_shulker_box", "brown_shulker_box", "green_shulker_box", "red_shulker_box", "black_shulker_box"));
            targets.add(target("barrel", EspBlockEntityColors.DEFAULT, "barrel"));
            targets.add(target("decorated_pot", EspBlockEntityColors.DEFAULT, "decorated_pot"));
            return List.copyOf(targets);
        }
    }

    private static final class FunctionalTargets {
        private static List<EspBlockEntityTarget> create() {
            List<EspBlockEntityTarget> targets = new ArrayList<>();
            targets.add(target("signs", EspBlockEntityColors.DEFAULT, "acacia_sign", "acacia_wall_sign", "bamboo_sign", "bamboo_wall_sign", "birch_sign", "birch_wall_sign", "cherry_sign", "cherry_wall_sign", "crimson_sign", "crimson_wall_sign", "dark_oak_sign", "dark_oak_wall_sign", "jungle_sign", "jungle_wall_sign", "mangrove_sign", "mangrove_wall_sign", "oak_sign", "oak_wall_sign", "pale_oak_sign", "pale_oak_wall_sign", "spruce_sign", "spruce_wall_sign", "warped_sign", "warped_wall_sign"));
            targets.add(target("hanging_signs", EspBlockEntityColors.DEFAULT, "acacia_hanging_sign", "acacia_wall_hanging_sign", "bamboo_hanging_sign", "bamboo_wall_hanging_sign", "birch_hanging_sign", "birch_wall_hanging_sign", "cherry_hanging_sign", "cherry_wall_hanging_sign", "crimson_hanging_sign", "crimson_wall_hanging_sign", "dark_oak_hanging_sign", "dark_oak_wall_hanging_sign", "jungle_hanging_sign", "jungle_wall_hanging_sign", "mangrove_hanging_sign", "mangrove_wall_hanging_sign", "oak_hanging_sign", "oak_wall_hanging_sign", "pale_oak_hanging_sign", "pale_oak_wall_hanging_sign", "spruce_hanging_sign", "spruce_wall_hanging_sign", "warped_hanging_sign", "warped_wall_hanging_sign"));
            targets.add(target("shelves", EspBlockEntityColors.DEFAULT, "acacia_shelf", "bamboo_shelf", "birch_shelf", "cherry_shelf", "crimson_shelf", "dark_oak_shelf", "jungle_shelf", "mangrove_shelf", "oak_shelf", "pale_oak_shelf", "spruce_shelf", "warped_shelf"));
            targets.add(target("banners", EspBlockEntityColors.DEFAULT, "white_banner", "white_wall_banner", "orange_banner", "orange_wall_banner", "magenta_banner", "magenta_wall_banner", "light_blue_banner", "light_blue_wall_banner", "yellow_banner", "yellow_wall_banner", "lime_banner", "lime_wall_banner", "pink_banner", "pink_wall_banner", "gray_banner", "gray_wall_banner", "light_gray_banner", "light_gray_wall_banner", "cyan_banner", "cyan_wall_banner", "purple_banner", "purple_wall_banner", "blue_banner", "blue_wall_banner", "brown_banner", "brown_wall_banner", "green_banner", "green_wall_banner", "red_banner", "red_wall_banner", "black_banner", "black_wall_banner"));
            targets.add(target("beacon", EspBlockEntityColors.DEFAULT, "beacon"));
            targets.add(target("bee_nest", EspBlockEntityColors.DEFAULT, "bee_nest"));
            targets.add(target("beehive", EspBlockEntityColors.DEFAULT, "beehive"));
            targets.add(target("bell", EspBlockEntityColors.DEFAULT, "bell"));
            targets.add(target("blast_furnace", EspBlockEntityColors.DEFAULT, "blast_furnace"));
            targets.add(target("brewing_stand", EspBlockEntityColors.DEFAULT, "brewing_stand"));
            targets.add(target("campfire", EspBlockEntityColors.CAMPFIRE, "campfire"));
            targets.add(target("chiseled_bookshelf", EspBlockEntityColors.DEFAULT, "chiseled_bookshelf"));
            targets.add(target("conduit", EspBlockEntityColors.DEFAULT, "conduit"));
            targets.add(target("enchanting_table", EspBlockEntityColors.DEFAULT, "enchanting_table"));
            targets.add(target("furnace", EspBlockEntityColors.DEFAULT, "furnace"));
            targets.add(target("jukebox", EspBlockEntityColors.DEFAULT, "jukebox"));
            targets.add(target("lectern", EspBlockEntityColors.DEFAULT, "lectern"));
            targets.add(target("potent_sulfur", EspBlockEntityColors.DEFAULT, "potent_sulfur"));
            targets.add(target("sculk_catalyst", EspBlockEntityColors.DEFAULT, "sculk_catalyst"));
            targets.add(target("sculk_shrieker", EspBlockEntityColors.DEFAULT, "sculk_shrieker"));
            targets.add(target("smoker", EspBlockEntityColors.DEFAULT, "smoker"));
            targets.add(target("soul_campfire", EspBlockEntityColors.SOUL_CAMPFIRE, "soul_campfire"));
            return List.copyOf(targets);
        }
    }

    private static final class RedstoneTargets {
        private static List<EspBlockEntityTarget> create() {
            List<EspBlockEntityTarget> targets = new ArrayList<>();
            targets.add(target("calibrated_sculk_sensor", EspBlockEntityColors.DEFAULT, "calibrated_sculk_sensor"));
            targets.add(target("comparator", EspBlockEntityColors.DEFAULT, "comparator"));
            targets.add(target("crafter", EspBlockEntityColors.DEFAULT, "crafter"));
            targets.add(target("daylight_detector", EspBlockEntityColors.DEFAULT, "daylight_detector"));
            targets.add(target("dispenser", EspBlockEntityColors.DEFAULT, "dispenser"));
            targets.add(target("dropper", EspBlockEntityColors.DEFAULT, "dropper"));
            targets.add(target("hopper", EspBlockEntityColors.DEFAULT, "hopper"));
            targets.add(target("sculk_sensor", EspBlockEntityColors.DEFAULT, "sculk_sensor"));
            return List.copyOf(targets);
        }
    }

    private static final class SpawnerTargets {
        private static List<EspBlockEntityTarget> create() {
            return List.of(
                    target("spawner", EspBlockEntityColors.DEFAULT, "spawner"),
                    target("trial_spawner", EspBlockEntityColors.DEFAULT, "trial_spawner"),
                    target("vault", EspBlockEntityColors.DEFAULT, "vault")
            );
        }
    }

    private static final class MiscTargets {
        private static List<EspBlockEntityTarget> create() {
            List<EspBlockEntityTarget> targets = new ArrayList<>();
            targets.add(target("creeper_head", EspBlockEntityColors.DEFAULT, "creeper_head", "creeper_wall_head"));
            targets.add(target("dragon_head", EspBlockEntityColors.DEFAULT, "dragon_head", "dragon_wall_head"));
            targets.add(target("piglin_head", EspBlockEntityColors.DEFAULT, "piglin_head", "piglin_wall_head"));
            targets.add(target("player_head", EspBlockEntityColors.DEFAULT, "player_head", "player_wall_head"));
            targets.add(target("skeleton_skull", EspBlockEntityColors.DEFAULT, "skeleton_skull", "skeleton_wall_skull"));
            targets.add(target("wither_skeleton_skull", EspBlockEntityColors.DEFAULT, "wither_skeleton_skull", "wither_skeleton_wall_skull"));
            targets.add(target("zombie_head", EspBlockEntityColors.DEFAULT, "zombie_head", "zombie_wall_head"));
            targets.add(target("copper_golem_statue", EspBlockEntityColors.DEFAULT, "copper_golem_statue", "exposed_copper_golem_statue", "weathered_copper_golem_statue", "oxidized_copper_golem_statue"));
            targets.add(target("waxed_copper_golem_statue", EspBlockEntityColors.DEFAULT, "waxed_copper_golem_statue", "waxed_exposed_copper_golem_statue", "waxed_weathered_copper_golem_statue", "waxed_oxidized_copper_golem_statue"));
            targets.add(target("chain_command_block", EspBlockEntityColors.CHAIN_COMMAND_BLOCK, "chain_command_block"));
            targets.add(target("command_block", EspBlockEntityColors.COMMAND_BLOCK, "command_block"));
            targets.add(target("creaking_heart", EspBlockEntityColors.DEFAULT, "creaking_heart"));
            targets.add(target("end_gateway", EspBlockEntityColors.END_GATEWAY, "end_gateway"));
            targets.add(target("end_portal", EspBlockEntityColors.END_PORTAL, "end_portal"));
            targets.add(target("jigsaw", EspBlockEntityColors.DEFAULT, "jigsaw"));
            targets.add(target("moving_piston", EspBlockEntityColors.DEFAULT, "moving_piston"));
            targets.add(target("repeating_command_block", EspBlockEntityColors.REPEATING_COMMAND_BLOCK, "repeating_command_block"));
            targets.add(target("structure_block", EspBlockEntityColors.DEFAULT, "structure_block"));
            targets.add(target("suspicious_gravel", EspBlockEntityColors.DEFAULT, "suspicious_gravel"));
            targets.add(target("suspicious_sand", EspBlockEntityColors.DEFAULT, "suspicious_sand"));
            targets.add(target("test_block", EspBlockEntityColors.DEFAULT, "test_block"));
            targets.add(target("test_instance_block", EspBlockEntityColors.DEFAULT, "test_instance_block"));
            return List.copyOf(targets);
        }
    }
}
