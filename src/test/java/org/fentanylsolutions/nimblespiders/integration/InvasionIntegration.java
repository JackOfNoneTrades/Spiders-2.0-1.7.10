package org.fentanylsolutions.nimblespiders.integration;

import static org.fentanylsolutions.nimblespiders.integration.SpiderIntegration.check;
import static org.fentanylsolutions.nimblespiders.integration.SpiderIntegration.tick;

import java.lang.reflect.Field;

import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.init.Blocks;
import net.minecraft.world.WorldServer;

import org.fentanylsolutions.nimblespiders.common.entity.mob.SpiderClimber;

import invmod.common.entity.EntityIMLiving;
import invmod.common.entity.EntityIMSpider;
import invmod.common.entity.PathNavigateAdapter;
import invmod.common.entity.ai.EntityAIGoToNexus;
import invmod.common.entity.ai.EntityAIPounce;

/** Optional runtime checks, loaded only when Invasion Mod is in the development runtime. */
public final class InvasionIntegration {

    static void run(WorldServer world) {
        EntityIMSpider spider = new EntityIMSpider(world);
        SpiderClimber climber = SpiderClimber.get(spider);
        check(climber != null, "Invasion spider climber");
        check(spider.getNavigator() instanceof PathNavigateAdapter, "Invasion spider keeps its navigator");
        check(hasTask(tasks(spider, "tasks"), EntityAIGoToNexus.class), "Invasion spider keeps its nexus AI");
        spider.setTier(2);
        check(hasTask(tasks(spider, "tasks"), EntityAIPounce.class), "Invasion jumping spider keeps its pounce AI");

        // Pounces keep Invasion's airborne physics and low jumping-spider gravity.
        spider.setPosition(200.5, 120, 40.5);
        tasks(spider, "tasks").taskEntries.clear();
        tasks(spider, "targetTasks").taskEntries.clear();
        spider.setAirborneTime(1);
        spider.motionX = spider.motionY = spider.motionZ = 0.3;
        spider.moveEntityWithHeading(0, 0);
        check(Math.abs(spider.motionY - (0.3 - spider.getGravity())) < 1.0E-6, "Invasion pounce physics preserved");
        spider.setAirborneTime(0);

        // Invasion routes over walls by stepping into them; surface travel must follow those paths up and across.
        for (int height = 1; height <= 5; height += 2) {
            check(climbWall(world, height, true), "Invasion spider climbs onto a " + height + "-high wall");
            check(climbWall(world, height, false), "Invasion spider crosses a " + height + "-high wall");
        }
    }

    private static boolean climbWall(WorldServer world, int height, boolean top) {
        for (int x = 196; x <= 212; x++) for (int z = 36; z <= 44; z++) {
            for (int y = 80; y <= 90; y++) world.setBlock(x, y, z, Blocks.air, 0, 2);
            world.setBlock(x, 79, z, Blocks.stone, 0, 2);
            if (x == 204) for (int y = 80; y < 80 + height; y++) world.setBlock(x, y, z, Blocks.stone, 0, 2);
        }
        EntityIMSpider spider = new EntityIMSpider(world);
        spider.setPosition(199.5, 80, 40.5);
        tasks(spider, "tasks").taskEntries.clear();
        tasks(spider, "targetTasks").taskEntries.clear();
        double targetX = top ? 204.5 : 208.5, targetY = top ? 80 + height : 80;
        for (int t = 0; t < 600; t++) {
            if (t % 20 == 0) spider.getNavigatorNew()
                .tryMoveToXYZ(targetX, targetY, 40.5, 0, spider.getMoveSpeedStat());
            tick(spider);
            if (Math.abs(spider.posX - targetX) < 1 && Math.abs(spider.posY - targetY) < 0.5 && spider.onGround)
                return true;
        }
        return false;
    }

    /** Invasion hides EntityLiving's task lists behind its own fields of the same names. */
    private static EntityAITasks tasks(EntityIMSpider spider, String name) {
        try {
            Field field = EntityIMLiving.class.getDeclaredField(name);
            field.setAccessible(true);
            return (EntityAITasks) field.get(spider);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(e);
        }
    }

    private static boolean hasTask(EntityAITasks tasks, Class<?> type) {
        for (Object entry : tasks.taskEntries)
            if (type.isInstance(((EntityAITasks.EntityAITaskEntry) entry).action)) return true;
        return false;
    }
}
