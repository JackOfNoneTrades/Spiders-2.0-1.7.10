package org.fentanylsolutions.nimblespiders.integration;

import static org.fentanylsolutions.nimblespiders.integration.SpiderIntegration.check;
import static org.fentanylsolutions.nimblespiders.integration.SpiderIntegration.climb;

import net.minecraft.world.WorldServer;

import org.fentanylsolutions.nimblespiders.common.entity.mob.SpiderClimber;
import org.fentanylsolutions.nimblespiders.common.entity.movement.AdvancedClimberPathNavigator;

import com.shinoow.abyssalcraft.common.entity.anti.EntityAntiSpider;

/** Optional runtime checks, loaded only when AbyssalCraft is in the development runtime. */
public final class AbyssalCraftIntegration {

    static void run(WorldServer world) {
        EntityAntiSpider spider = new EntityAntiSpider(world);
        check(SpiderClimber.get(spider) != null, "AbyssalCraft anti-spider climber");
        check(spider.getNavigator() instanceof AdvancedClimberPathNavigator, "AbyssalCraft anti-spider navigator");
        climb(spider, "AbyssalCraft anti-spider");
    }
}
