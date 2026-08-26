package org.exmple.newbedwarshelper.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import org.exmple.newbedwarshelper.client.antiafk.AntiAFKManager;
import org.exmple.newbedwarshelper.client.esp.EspStorageManager;
import org.exmple.newbedwarshelper.client.enemystatusviewer.BedwarsInvisibilityToastNotifier;
import org.exmple.newbedwarshelper.client.enemystatusviewer.BedwarsProtectionTracker;
import org.exmple.newbedwarshelper.client.gammaoverride.GammaOverrideManager;
import org.exmple.newbedwarshelper.client.gui.KeyMappingManager;
import org.exmple.newbedwarshelper.client.gui.MultilineSystemToast;
import org.exmple.newbedwarshelper.client.hitboxenhance.HitboxEnhanceTargetStorage;
import org.exmple.newbedwarshelper.client.isp.IspTargetStorage;
import org.exmple.newbedwarshelper.client.itemprotection.ItemProtectionManager;
import org.exmple.newbedwarshelper.client.itemmodelenhance.ItemScaleRegistry;
import org.exmple.newbedwarshelper.client.statsfetcher.StatsFetcherAutoWeballTrigger;
import org.exmple.newbedwarshelper.client.toolswitcher.ToolSwitcherManager;
import org.exmple.newbedwarshelper.client.utils.AsyncExecutor;
import org.exmple.newbedwarshelper.client.utils.bedwars.BedwarsGameDetector;
import org.exmple.newbedwarshelper.client.z_commands.itemmodelenhance.ImeCommand;
import org.exmple.newbedwarshelper.client.z_commands.statsfetcher.WebCommand;
import org.exmple.newbedwarshelper.client.z_commands.statsfetcher.WeballCommand;
import org.exmple.newbedwarshelper.client.z_debug.blockentityclassification.BlockEntityClassificationDebugger;

public class NewbedwarshelperClient implements ClientModInitializer {
    public static final String NAMESPACE = "newbedwarshelper";
    @Override
    public void onInitializeClient() {
        AntiAFKManager.init();
        GammaOverrideManager.init();
        KeyMappingManager.init();
        ToolSwitcherManager.init();
        MultilineSystemToast.init();
        EspStorageManager.init();
        HitboxEnhanceTargetStorage.init();
        IspTargetStorage.init();
        ItemProtectionManager.init();
        ItemScaleRegistry.init();
        BedwarsGameDetector.init();
        BedwarsInvisibilityToastNotifier.init();
        BedwarsProtectionTracker.init();
        StatsFetcherAutoWeballTrigger.init();
        WebCommand.register();
        WeballCommand.register();
        ImeCommand.register();
        // Debug-only classification entry point. Remove this init call before shipping if the helper is not needed.
        BlockEntityClassificationDebugger.init();
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            EspStorageManager.clearTemporaryOverrides();
            EspStorageManager.clearRuntimeCaches();
            HitboxEnhanceTargetStorage.clearTemporaryOverrides();
            IspTargetStorage.clearTemporaryOverrides();
            AsyncExecutor.shutdown();
        });
    }
}
