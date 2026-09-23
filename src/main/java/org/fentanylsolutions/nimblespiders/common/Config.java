package org.fentanylsolutions.nimblespiders.common;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class Config {

    public static Configuration configuration;
    public static boolean enabled = true;
    public static boolean debugMode;
    public static int pathSearchBudget = 2048;
    public static int maxDropHeight = 8;
    public static boolean fallDamage = true;
    public static int safeFallDistance = -1;
    private static final String[] DEFAULT_SPIDER_SPEEDS = { "Spider:0.8", "CaveSpider:0.8", "lotr.MirkwoodSpider:0.35",
        "lotr.MordorSpider:0.35", "lotr.UtumnoIceSpider:0.35", "abyssalcraft.antispider:0.8" };
    private static volatile Map<String, Double> spiderSpeeds = parseSpeeds(DEFAULT_SPIDER_SPEEDS);
    private static Set<String> excludedClasses = new HashSet<>();

    public static void init(File directory) {
        configuration = new Configuration(new File(directory, "nimble-spiders/nimble-spiders.cfg"));
        sync();
    }

    public static void sync() {
        enabled = configuration
            .get(
                "climbing",
                "enabled",
                true,
                "Enable improved spider climbing and AI. Reload the world after changing.")
            .setLanguageKey("nimble-spiders.config.enabled")
            .setRequiresWorldRestart(true)
            .getBoolean();
        excludedClasses = new HashSet<>(
            Arrays.asList(
                configuration.get(
                    "climbing",
                    "excluded_classes",
                    new String[0],
                    "Fully qualified spider class names to leave unchanged, including their subclasses. Reload the world after changing.")
                    .setLanguageKey("nimble-spiders.config.excluded_classes")
                    .setRequiresWorldRestart(true)
                    .getStringList()));
        spiderSpeeds = parseSpeeds(
            configuration.get(
                "movement",
                "spider_speeds",
                DEFAULT_SPIDER_SPEEDS,
                "Baseline movement speeds as entity ID or Java class name:speed. Defaults: vanilla and AbyssalCraft 0.8, LOTR 0.35. Higher values make spiders faster. LOTR size differences, attribute modifiers and vanilla's climbing speed conversion are preserved. Accepts full or simple class names; the nearest matching class wins, with full class name before entity ID before simple class name. Subclasses inherit entries. Duplicate keys use the last value. Values must be finite, between 0 and 1024; invalid entries are ignored. Changes apply immediately to existing improved spiders. Server settings govern gameplay.")
                .setLanguageKey("nimble-spiders.config.spider_speeds")
                .getStringList());
        maxDropHeight = configuration.getInt(
            "max_drop_height",
            "falling",
            8,
            0,
            32,
            "Maximum voluntary drop in blocks. Zero disables planned drops. Default: 8.",
            "nimble-spiders.config.max_drop_height");
        fallDamage = configuration.getBoolean(
            "fall_damage",
            "falling",
            true,
            "Allow improved spiders and their mounted players to take damage from falls beyond their safe distance. Default: true.",
            "nimble-spiders.config.fall_damage");
        safeFallDistance = configuration.getInt(
            "safe_fall_distance",
            "falling",
            -1,
            -1,
            256,
            "Blocks spiders and their mounted players can fall without damage. -1 follows max_drop_height; otherwise overrides it. Each extra block deals one damage point (half a heart), rounded up, with normal potion and damage modifiers.",
            "nimble-spiders.config.safe_fall_distance");
        pathSearchBudget = configuration.getInt(
            "path_search_budget",
            "pathfinding",
            2048,
            128,
            16384,
            "Maximum nodes examined per path search. Higher values find longer routes but use more server time.",
            "nimble-spiders.config.path_search_budget");
        debugMode = configuration.getBoolean(
            "debug_logging",
            "pathfinding",
            false,
            "Log nearby spiders, targets, paths and drop decisions once per second. Diagnostic only; writes to the Forge FML log.",
            "nimble-spiders.config.debug_logging") || System.getenv("MCMODDING_DEBUG_MODE") != null;
        if (SpiderMod.logger != null) SpiderMod.logger.info(
            "[SpiderDebug] settings debug={} enabled={} pathBudget={} excluded={}",
            debugMode,
            enabled,
            pathSearchBudget,
            excludedClasses);
        configuration.setCategoryLanguageKey("climbing", "nimble-spiders.config.climbing");
        configuration.setCategoryLanguageKey("pathfinding", "nimble-spiders.config.pathfinding");
        configuration.setCategoryLanguageKey("falling", "nimble-spiders.config.falling");
        configuration.setCategoryLanguageKey("movement", "nimble-spiders.config.movement");
        if (configuration.hasChanged()) configuration.save();
    }

    static Map<String, Double> parseSpeeds(String[] entries) {
        Map<String, Double> parsed = new HashMap<>();
        for (String entry : entries) {
            try {
                int colon = entry.lastIndexOf(':');
                if (colon <= 0) throw new IllegalArgumentException();
                String key = entry.substring(0, colon)
                    .trim();
                double speed = Double.parseDouble(
                    entry.substring(colon + 1)
                        .trim());
                if (key.isEmpty() || !Double.isFinite(speed) || speed < 0 || speed > 1024)
                    throw new IllegalArgumentException();
                parsed.put(key, speed);
            } catch (IllegalArgumentException failure) {
                if (SpiderMod.logger != null) SpiderMod.logger.warn("Ignoring invalid spider speed entry: {}", entry);
            }
        }
        return Collections.unmodifiableMap(parsed);
    }

    public static double getSpeedMultiplier(Entity entity, double defaultSpeed) {
        Map<String, Double> speeds = spiderSpeeds;
        for (Class<?> type = entity.getClass(); type != null; type = type.getSuperclass()) {
            Double speed = speeds.get(type.getName());
            if (speed == null) speed = speeds.get(EntityList.classToStringMapping.get(type));
            if (speed == null) speed = speeds.get(type.getSimpleName());
            if (speed != null) return speed / defaultSpeed;
        }
        return 1;
    }

    public static boolean isEnabled(Class<?> type) {
        if (!enabled) return false;
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (excludedClasses.contains(current.getName())) return false;
        }
        return true;
    }

    public static int getSafeFallDistance() {
        return safeFallDistance < 0 ? maxDropHeight : safeFallDistance;
    }

    @SubscribeEvent
    public void changed(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (SpiderMod.MODID.equals(event.modID)) sync();
    }
}
