package org.exmple.newbedwarshelper.client.esp.blockentity;

import net.minecraft.world.level.block.Block;
import org.exmple.newbedwarshelper.client.esp.EspTempToggleMode;
import org.exmple.newbedwarshelper.client.esp.EspTargetWhitelist;
import org.exmple.newbedwarshelper.client.esp.EspToggleAction;
import org.exmple.newbedwarshelper.client.esp.block.render.EspBlockEspController;
import org.exmple.newbedwarshelper.client.z_config.ModConfig;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class EspBlockEntityStorage {
    private static final EspTargetWhitelist<EspBlockEntityTarget> BLOCK_ENTITY_TARGETS = new EspTargetWhitelist<>(EspBlockEntityStorage::createDefaultBlockEntityWhitelist);
    private static boolean initialized;

    private EspBlockEntityStorage() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        loadWhitelistFromDisk();
        initialized = true;
    }

    public static synchronized boolean isBlockEntityTargetEspEnabled(EspBlockEntityTarget target) {
        init();
        return BLOCK_ENTITY_TARGETS.isEnabled(target);
    }

    public static synchronized EspBlockEntityTarget targetForBlock(Block block) {
        init();
        EspBlockEntityTarget target = EspBlockEntityGroups.targetForBlock(block);
        return target != null && BLOCK_ENTITY_TARGETS.isEnabled(target) ? target : null;
    }

    public static synchronized boolean hasAnyEnabledBlockEntityTarget() {
        init();
        for (EspBlockEntityTarget target : BLOCK_ENTITY_TARGETS.persistentTargetKeys()) {
            if (BLOCK_ENTITY_TARGETS.isEnabled(target)) {
                return true;
            }
        }
        return false;
    }

    public static synchronized boolean isBlockEntityTargetPersistentlyEspEnabled(EspBlockEntityTarget target) {
        init();
        return BLOCK_ENTITY_TARGETS.isPersistentlyEnabled(target);
    }

    public static synchronized void setBlockEntityTargetEspEnabled(EspBlockEntityTarget target, boolean enabled) {
        init();
        BLOCK_ENTITY_TARGETS.setEnabled(target, enabled);
        saveWhitelistToDisk();
        EspBlockEspController.requestRescan();
    }

    public static synchronized void setBlockEntityTargetsEspEnabled(List<EspBlockEntityTarget> targets, boolean enabled) {
        init();
        BLOCK_ENTITY_TARGETS.setAllEnabled(targets, enabled);
        saveWhitelistToDisk();
        EspBlockEspController.requestRescan();
    }

    public static synchronized EspToggleAction getNextBlockEntityGroupToggleAction(List<EspBlockEntityTarget> targets) {
        init();
        return BLOCK_ENTITY_TARGETS.getNextGroupToggleAction(targets);
    }

    public static synchronized void applyNextBlockEntityGroupToggleAction(List<EspBlockEntityTarget> targets) {
        init();
        BLOCK_ENTITY_TARGETS.applyNextGroupToggleAction(targets);
        saveWhitelistToDisk();
        EspBlockEspController.requestRescan();
    }

    public static synchronized EspTempToggleMode getBlockEntityGroupTempToggleMode(List<EspBlockEntityTarget> targets) {
        init();
        return BLOCK_ENTITY_TARGETS.getGroupTempToggleMode(targets);
    }

    public static synchronized void cycleBlockEntityGroupTempToggleMode(List<EspBlockEntityTarget> targets) {
        init();
        BLOCK_ENTITY_TARGETS.cycleGroupTempToggleMode(targets);
        EspBlockEspController.requestRescan();
    }

    public static synchronized void setBlockEntityGroupTempToggleMode(List<EspBlockEntityTarget> targets, EspTempToggleMode mode) {
        init();
        BLOCK_ENTITY_TARGETS.setGroupTempToggleMode(targets, mode);
        EspBlockEspController.requestRescan();
    }

    public static synchronized void clearTemporaryOverrides() {
        BLOCK_ENTITY_TARGETS.clearTemporaryOverrides();
        EspBlockEspController.requestRescan();
    }

    public static synchronized void resetWhitelistToDefaults() {
        init();
        BLOCK_ENTITY_TARGETS.resetToDefaults();
        saveWhitelistToDisk();
        EspBlockEspController.requestRescan();
    }

    private static Map<EspBlockEntityTarget, Boolean> createDefaultBlockEntityWhitelist() {
        Map<EspBlockEntityTarget, Boolean> whitelist = new LinkedHashMap<>();
        for (EspBlockEntityTarget target : EspBlockEntityGroups.ALL_TARGETS) {
            whitelist.put(target, Boolean.FALSE);
        }

        return whitelist;
    }

    public static synchronized void loadWhitelistFromDisk() {
        ModConfig.EspConfig config = ModConfig.getInstance().esp;

        for (EspBlockEntityTarget target : BLOCK_ENTITY_TARGETS.persistentTargetKeys()) {
            Boolean value = config.blockEntityWhitelist.get(target.id());
            if (value != null) {
                BLOCK_ENTITY_TARGETS.setEnabled(target, value);
            }
        }

        saveWhitelistToDisk();
    }

    public static synchronized void saveWhitelistToDisk() {
        ModConfig config = ModConfig.getInstance();
        config.esp.blockEntityWhitelist.clear();

        for (Map.Entry<EspBlockEntityTarget, Boolean> entry : BLOCK_ENTITY_TARGETS.persistentTargets().entrySet()) {
            config.esp.blockEntityWhitelist.put(entry.getKey().id(), Boolean.TRUE.equals(entry.getValue()));
        }
        config.save();
    }
}
