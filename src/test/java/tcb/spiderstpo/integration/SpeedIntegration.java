package tcb.spiderstpo.integration;

import static tcb.spiderstpo.integration.SpiderIntegration.check;
import static tcb.spiderstpo.integration.SpiderIntegration.tick;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.config.Property;

import tcb.spiderstpo.common.Config;
import tcb.spiderstpo.common.entity.mob.SpiderClimber;

public final class SpeedIntegration {

    static Property property() {
        return Config.configuration.getCategory("movement")
            .get("spider_speeds");
    }

    static void speeds(String... entries) {
        property().set(entries);
        Config.sync();
    }

    static double speed(EntityCreature entity) {
        return SpiderClimber.get(entity)
            .getMovementSpeed();
    }

    static void near(double expected, double actual, String message) {
        check(Math.abs(expected - actual) < 1e-6, message + " expected=" + expected + " actual=" + actual);
    }

    static void run(WorldServer world) {
        String[] saved = property().getStringList()
            .clone();
        try {
            EntitySpider spider = new EntitySpider(world);
            EntityCaveSpider cave = new EntityCaveSpider(world);
            near(0.3, speed(spider), "default vanilla climbing speed retained");
            near(0.3, speed(cave), "default cave climbing speed retained");
            speeds("Spider:1.6", "CaveSpider:0.8");
            near(0.6, speed(spider), "entity ID changes existing spider speed");
            near(0.3, speed(cave), "specific cave entry overrides inherited spider entry");
            speeds("Spider:1.6");
            near(0.6, speed(cave), "subclasses inherit speed entries");
            speeds("Spider:1.6", "net.minecraft.entity.monster.EntitySpider:1.2");
            near(0.45, speed(spider), "full class overrides same-class entity ID");
            speeds("EntityCaveSpider:1.2", "EntitySpider:1.6");
            near(0.45, speed(cave), "simple class matching prefers nearest type");
            AttributeModifier boost = new AttributeModifier("speed integration boost", 0.5, 2);
            spider.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
                .applyModifier(boost);
            near(0.9, speed(spider), "attribute modifier still multiplies configured speed");
            spider.getEntityAttribute(SharedMonsterAttributes.movementSpeed)
                .removeModifier(boost);
            speeds("Spider:0");
            near(0, speed(spider), "zero speed disables voluntary movement");
            speeds("MissingOptionalSpider:1.5");
            near(0.3, speed(spider), "unmatched classes retain native speed");
            double normal = walk(world, 0.8), fast = walk(world, 1.0);
            check(fast > normal * 1.15, "higher speed entry actually moves farther: " + normal + " -> " + fast);
        } finally {
            speeds(saved);
        }
    }

    private static double walk(WorldServer world, double configured) {
        for (int x = 110; x <= 130; x++) for (int z = 8; z <= 12; z++) world.setBlock(x, 79, z, Blocks.stone, 0, 2);
        speeds("Spider:" + configured);
        EntitySpider spider = new EntitySpider(world);
        spider.tasks.taskEntries.clear();
        spider.targetTasks.taskEntries.clear();
        spider.setPosition(112.5, 80, 10.5);
        for (int i = 0; i < 30; i++) {
            spider.getMoveHelper()
                .setMoveTo(128.5, 80, 10.5, 1);
            tick(spider);
        }
        return spider.posX - 112.5;
    }
}
