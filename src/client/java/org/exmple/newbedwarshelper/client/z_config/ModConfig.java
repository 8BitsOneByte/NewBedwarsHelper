package org.exmple.newbedwarshelper.client.z_config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = "newbedwarshelper.json";

    private static ModConfig instance;

    public AntiAfkConfig antiAfk = new AntiAfkConfig();
    public EspConfig esp = new EspConfig();
    public HitboxEnhanceConfig hitboxEnhance = new HitboxEnhanceConfig();
    public IspConfig isp = new IspConfig();
    public ItemModelEnhanceConfig itemModelEnhance = new ItemModelEnhanceConfig();
    public StatsFetcherConfig statsFetcher = new StatsFetcherConfig();
    public GammaOverrideConfig gammaOverride = new GammaOverrideConfig();
    public TrajectoryPredictionConfig trajectoryPrediction = new TrajectoryPredictionConfig();

    public static ModConfig getInstance() {
        if (instance == null) {
            instance = load();
        }

        return instance;
    }

    public static ModConfig load() {
        Path configPath = getConfigPath();
        if (Files.isRegularFile(configPath)) {
            try {
                String content = Files.readString(configPath, StandardCharsets.UTF_8);
                ModConfig config = GSON.fromJson(content, ModConfig.class);
                if (config != null) {
                    config.ensureDefaults();
                    config.save();
                    return config;
                }
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        ModConfig config = new ModConfig();
        config.save();
        return config;
    }

    public void save() {
        ensureDefaults();
        Path configPath = getConfigPath();
        try {
            Path parent = configPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(configPath, GSON.toJson(this), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    private void ensureDefaults() {
        if (antiAfk == null) {
            antiAfk = new AntiAfkConfig();
        }
        if (esp == null) {
            esp = new EspConfig();
        }
        if (hitboxEnhance == null) {
            hitboxEnhance = new HitboxEnhanceConfig();
        }
        if (isp == null) {
            isp = new IspConfig();
        }
        if (itemModelEnhance == null) {
            itemModelEnhance = new ItemModelEnhanceConfig();
        }
        if (statsFetcher == null) {
            statsFetcher = new StatsFetcherConfig();
        }
        if (gammaOverride == null) {
            gammaOverride = new GammaOverrideConfig();
        }
        if (trajectoryPrediction == null) {
            trajectoryPrediction = new TrajectoryPredictionConfig();
        }
        esp.ensureDefaults();
        hitboxEnhance.ensureDefaults();
        isp.ensureDefaults();
        itemModelEnhance.ensureDefaults();
        statsFetcher.ensureDefaults();
        gammaOverride.ensureDefaults();
        trajectoryPrediction.ensureDefaults();
    }

    private static Path getConfigPath() {
        return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE);
    }

    public static class AntiAfkConfig {
        public boolean featureEnabled = true;
        public boolean smallIcon = true;
    }

    public static class EspConfig {
        public Map<String, Boolean> entityWhitelist = new LinkedHashMap<>();
        public Map<String, Boolean> blockEntityWhitelist = new LinkedHashMap<>();
        public Map<String, Boolean> blockWhitelist = new LinkedHashMap<>();
        public boolean dangerousWitherSkullWhitelist = false;
        public Boolean showBlockEspTracer = true;
        public Boolean showPlayerModelsThroughWalls = false;

        private void ensureDefaults() {
            if (entityWhitelist == null) {
                entityWhitelist = new LinkedHashMap<>();
            }
            if (blockEntityWhitelist == null) {
                blockEntityWhitelist = new LinkedHashMap<>();
            }
            if (blockWhitelist == null) {
                blockWhitelist = new LinkedHashMap<>();
            }
            if (showBlockEspTracer == null) {
                showBlockEspTracer = true;
            }
            if (showPlayerModelsThroughWalls == null) {
                showPlayerModelsThroughWalls = false;
            }
        }
    }

    public static class HitboxEnhanceConfig {
        public Map<String, Boolean> entityWhitelist = new LinkedHashMap<>();
        public boolean dangerousWitherSkullWhitelist = true;

        private void ensureDefaults() {
            if (entityWhitelist == null) {
                entityWhitelist = new LinkedHashMap<>();
            }
        }
    }

    public static class IspConfig {
        public Map<String, Boolean> entityWhitelist = new LinkedHashMap<>();

        private void ensureDefaults() {
            if (entityWhitelist == null) {
                entityWhitelist = new LinkedHashMap<>();
            }
        }
    }

    public static class ItemModelEnhanceConfig {
        public Map<String, Float> itemScales = new LinkedHashMap<>();

        private void ensureDefaults() {
            if (itemScales == null) {
                itemScales = new LinkedHashMap<>();
            }
        }
    }

    public static class StatsFetcherConfig {
        public boolean showDangerousPlayers = true;
        public boolean showFinalKD = true;
        public boolean showDoublesFinalKD = true;
        public boolean showQuadsFinalKD = true;
        public boolean showTotalWins = true;
        public Boolean skipOwnTeamInGame = true;
        public Boolean autoWeballOnGameStart = false;
        public Boolean copyButtonsEnabled = true;
        public Boolean copyTextInEnglish = false;
        public double dangerousPlayersKDThreshold = 1.0;

        private void ensureDefaults() {
            if (skipOwnTeamInGame == null) {
                skipOwnTeamInGame = true;
            }
            if (autoWeballOnGameStart == null) {
                autoWeballOnGameStart = false;
            }
            if (copyButtonsEnabled == null) {
                copyButtonsEnabled = true;
            }
            if (copyTextInEnglish == null) {
                copyTextInEnglish = false;
            }
        }
    }

    public static class GammaOverrideConfig {
        public boolean enabled = false;
        public GammaOverrideMode mode = GammaOverrideMode.NIGHT_VISION;

        private void ensureDefaults() {
            if (mode == null) {
                mode = GammaOverrideMode.NIGHT_VISION;
            }
        }
    }

    public static class TrajectoryPredictionConfig {
        public Boolean enabled = false;
        public Boolean projectileEnabled = true;
        public Boolean arrowEnabled = true;
        public Boolean showTrajectory = true;
        public Boolean showLandingIndicator = true;
        public Integer projectileColor = 0x57FFE1;
        public Integer arrowColor = 0xF4C95D;
        public Float lineWidth = 1.5F;
        public Float transparency = 0.72F;
        public Double maxLength = 96.0D;
        public Integer samplingPrecision = 2;

        private void ensureDefaults() {
            if (enabled == null) {
                enabled = false;
            }
            if (projectileEnabled == null) {
                projectileEnabled = true;
            }
            if (arrowEnabled == null) {
                arrowEnabled = true;
            }
            if (showTrajectory == null) {
                showTrajectory = true;
            }
            if (showLandingIndicator == null) {
                showLandingIndicator = true;
            }
            if (projectileColor == null) {
                projectileColor = 0x57FFE1;
            }
            if (arrowColor == null) {
                arrowColor = 0xF4C95D;
            }
            if (lineWidth == null) {
                lineWidth = 1.5F;
            }
            if (transparency == null) {
                transparency = 0.72F;
            }
            if (maxLength == null) {
                maxLength = 96.0D;
            }
            if (samplingPrecision == null) {
                samplingPrecision = 2;
            }

            projectileColor &= 0xFFFFFF;
            arrowColor &= 0xFFFFFF;
            lineWidth = Math.max(0.5F, Math.min(4.0F, lineWidth));
            transparency = Math.max(0.1F, Math.min(1.0F, transparency));
            maxLength = Math.max(16.0D, Math.min(256.0D, maxLength));
            samplingPrecision = Math.max(1, Math.min(4, samplingPrecision));
        }
    }

    public enum GammaOverrideMode {
        NIGHT_VISION,
        INVALID_GAMMA
    }
}
