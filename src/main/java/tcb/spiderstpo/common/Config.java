package tcb.spiderstpo.common;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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
    private static Set<String> excludedClasses = new HashSet<>();

    public static void init(File directory) {
        configuration = new Configuration(new File(directory, "spiderstpo/spiderstpo.cfg"));
        sync();
    }

    public static void sync() {
        enabled = configuration
            .get(
                "climbing",
                "enabled",
                true,
                "Enable improved spider climbing and AI. Reload the world after changing.")
            .setLanguageKey("spiderstpo.config.enabled")
            .setRequiresWorldRestart(true)
            .getBoolean();
        excludedClasses = new HashSet<>(
            Arrays.asList(
                configuration.get(
                    "climbing",
                    "excluded_classes",
                    new String[0],
                    "Fully qualified spider class names to leave unchanged, including their subclasses. Reload the world after changing.")
                    .setLanguageKey("spiderstpo.config.excluded_classes")
                    .setRequiresWorldRestart(true)
                    .getStringList()));
        maxDropHeight = configuration.getInt(
            "max_drop_height",
            "falling",
            8,
            0,
            32,
            "Maximum voluntary drop in blocks. Zero disables planned drops. Default: 8.",
            "spiderstpo.config.max_drop_height");
        fallDamage = configuration.getBoolean(
            "fall_damage",
            "falling",
            true,
            "Allow improved spiders to take damage from falls beyond their safe distance. Default: true.",
            "spiderstpo.config.fall_damage");
        safeFallDistance = configuration.getInt(
            "safe_fall_distance",
            "falling",
            -1,
            -1,
            256,
            "Blocks spiders can fall without damage. -1 follows max_drop_height; otherwise overrides it. Each extra block deals one damage point (half a heart), rounded up, with normal potion and damage modifiers.",
            "spiderstpo.config.safe_fall_distance");
        pathSearchBudget = configuration.getInt(
            "path_search_budget",
            "pathfinding",
            2048,
            128,
            16384,
            "Maximum nodes examined per path search. Higher values find longer routes but use more server time.",
            "spiderstpo.config.path_search_budget");
        debugMode = configuration.getBoolean(
            "debug_logging",
            "pathfinding",
            false,
            "Log nearby spiders, targets, paths and drop decisions once per second. Diagnostic only; writes to the Forge FML log.",
            "spiderstpo.config.debug_logging") || System.getenv("MCMODDING_DEBUG_MODE") != null;
        if (SpiderMod.logger != null) SpiderMod.logger.info(
            "[SpiderDebug] settings debug={} enabled={} pathBudget={} excluded={}",
            debugMode,
            enabled,
            pathSearchBudget,
            excludedClasses);
        configuration.setCategoryLanguageKey("climbing", "spiderstpo.config.climbing");
        configuration.setCategoryLanguageKey("pathfinding", "spiderstpo.config.pathfinding");
        configuration.setCategoryLanguageKey("falling", "spiderstpo.config.falling");
        if (configuration.hasChanged()) configuration.save();
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
