package tcb.spiderstpo.integration;

import static tcb.spiderstpo.integration.SpiderIntegration.check;
import static tcb.spiderstpo.integration.SpiderIntegration.tick;

import java.lang.reflect.Method;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.DamageSource;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.mojang.authlib.GameProfile;

import lotr.common.entity.ai.LOTREntityAIAttackOnCollide;
import lotr.common.entity.ai.LOTREntityAIFollowHiringPlayer;
import lotr.common.entity.ai.LOTREntityAIHiredRemainStill;
import lotr.common.entity.npc.LOTREntityMirkwoodSpider;
import lotr.common.entity.npc.LOTREntityMordorOrc;
import lotr.common.entity.npc.LOTREntityMordorSpider;
import lotr.common.entity.npc.LOTREntitySpiderBase;
import lotr.common.entity.npc.LOTREntityUtumnoIceSpider;
import tcb.spiderstpo.common.Config;
import tcb.spiderstpo.common.entity.mob.SpiderClimber;
import tcb.spiderstpo.common.entity.movement.AdvancedClimberPathNavigator;
import tcb.spiderstpo.common.entity.movement.AdvancedWalkNodeProcessor;

/** Optional runtime checks, loaded only when LOTR is in the development runtime. */
public final class LOTRIntegration {

    private static void speeds(WorldServer world) {
        String[] saved = SpeedIntegration.property()
            .getStringList()
            .clone();
        String[] names = { "lotr.MirkwoodSpider", "lotr.MordorSpider", "lotr.UtumnoIceSpider" };
        try {
            for (int variant = 0; variant < 3; variant++) {
                LOTREntitySpiderBase spider = create(world, variant);
                double nativeSpeed = spider
                    .getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed)
                    .getAttributeValue();
                SpeedIntegration.speeds(names[variant] + ":0.35");
                SpeedIntegration
                    .near(nativeSpeed, SpeedIntegration.speed(spider), "LOTR default preserves native size speed");
                SpeedIntegration.speeds(names[variant] + ":0.7");
                SpeedIntegration
                    .near(nativeSpeed * 2, SpeedIntegration.speed(spider), "LOTR named speed changes existing spider");
                SpeedIntegration.speeds(
                    spider.getClass()
                        .getName() + ":0.525");
                SpeedIntegration
                    .near(nativeSpeed * 1.5, SpeedIntegration.speed(spider), "LOTR fully qualified class speed");
            }
            SpeedIntegration.speeds("lotr.MordorSpider:0.4375");
            LOTREntityMordorSpider faster = new LOTREntityMordorSpider(world);
            faster.setPosition(2.5, 80, 0.5);
            tick(faster);
            SpiderIntegration.climb(faster, "faster LOTR spider");
        } finally {
            SpeedIntegration.speeds(saved);
        }
    }

    private static void reportedPillar(WorldServer world, boolean wallStart) {
        // Same pillar/platform dimensions and stalled position as the reported world.
        for (int x = 160; x <= 178; x++) for (int z = 334; z <= 355; z++) for (int y = 74; y <= 89; y++) {
            boolean solid = y <= 75 || x == 169 && z == 348 && y <= 85
                || x == 169 && z >= 344 && z <= 348 && y == 85
                || x >= 169 && x <= 171 && z >= 340 && z <= 344 && y == 80;
            world.setBlock(x, y, z, solid ? Blocks.dirt : Blocks.air, 0, 2);
        }
        LOTREntityMordorSpider spider = new LOTREntityMordorSpider(world);
        spider.setSpiderScale(3);
        spider.setPosition(wallStart ? 169.5 : 168.5, wallStart ? 80 : 76, 346.6);
        tick(spider);
        spider.targetTasks.taskEntries.clear();
        AdvancedWalkNodeProcessor processor = new AdvancedWalkNodeProcessor(spider);
        check(!processor.canMove(168, 81, 345, 3, 168, 81, 344, 0), "no unsupported wall to floor shortcut");
        check(
            !processor.canMove(168, 80, 345, 3, 168, 81, 344, 0),
            "opposing platform edge cannot substitute for pillar support");
        spider.getRNG()
            .setSeed(12345);
        EntityPig target = new EntityPig(world);
        target.setPosition(wallStart ? 169.998 : 169.103, 81, wallStart ? 341.026 : 344.661);
        spider.setAttackTarget(target);
        boolean reached = false, ceiling = false, dropped = false;
        for (int t = 0; t < 600; t++) {
            tick(spider);
            ceiling |= SpiderClimber.get(spider).orientationNormal.y < -0.7 && spider.posY > 82;
            dropped |= ceiling && spider.motionY < -0.1;
            if (t % 100 == 0) System.out.println(
                "REPORTED_PILLAR t=" + t
                    + " pos="
                    + spider.posX
                    + ","
                    + spider.posY
                    + ","
                    + spider.posZ
                    + " path="
                    + tcb.spiderstpo.common.SpiderDebug.path(
                        spider.getNavigator()
                            .getPath()));
            if (spider.posY >= 79 && target.getHealth() < target.getMaxHealth()) {
                reached = true;
                System.out.println(
                    "REPORTED_PILLAR reached t=" + t
                        + " ceiling="
                        + ceiling
                        + " dropped="
                        + dropped
                        + " pos="
                        + spider.posX
                        + ","
                        + spider.posY
                        + ","
                        + spider.posZ);
                break;
            }
        }
        check(reached, "large LOTR spider reaches reported platform, wallStart=" + wallStart);
        if (wallStart) {
            check(ceiling && dropped, "wall-spawned LOTR spider uses overhang and drops toward target");
            check(spider.posZ < 344.5, "spider leaves pillar and reaches platform");
        }
    }

    private static void wallSpawns(WorldServer world) {
        for (int x = 90; x <= 100; x++) for (int y = 79; y <= 91; y++) for (int z = -5; z <= 5; z++)
            world.setBlock(x, y, z, x >= 95 && y >= 84 && z >= 0 ? Blocks.stone : Blocks.air, 0, 2);
        for (int variant = 0; variant < 3; variant++)
            for (int scale = 0; scale <= 3; scale++) for (int side = 0; side < 6; side++) {
                LOTREntitySpiderBase spider = create(world, variant);
                spider.setSpiderScale(scale);
                double x = side < 2 ? (side == 0 ? 94.5 : 101.5) : 97.5;
                double y = side >= 4 ? (side == 4 ? 83 : 92) : 86;
                double z = side >= 2 && side < 4 ? (side == 2 ? -0.5 : 6.5) : 2.5;
                spider.setPosition(x, y, z);
                spider.tasks.taskEntries.clear();
                spider.targetTasks.taskEntries.clear();
                float health = spider.getHealth();
                for (int t = 0; t < 8; t++) tick(spider);
                check(
                    spider.getHealth() == health,
                    "LOTR wall spawn no damage species=" + variant + " scale=" + scale + " side=" + side);
                check(
                    world.func_147461_a(spider.boundingBox.contract(0.001, 0.001, 0.001))
                        .isEmpty(),
                    "LOTR wall spawn collision box clears blocks species=" + variant
                        + " scale="
                        + scale
                        + " side="
                        + side);
            }
    }

    public static void run(WorldServer world) throws Exception {
        speeds(world);
        reportedPillar(world, false);
        reportedPillar(world, true);
        wallSpawns(world);
        for (int variant = 0; variant < 3; variant++) {
            for (int scale = 0; scale <= 3; scale++) {
                LOTREntitySpiderBase spider = create(world, variant);
                check(SpiderClimber.get(spider) != null, "LOTR climbing state installed");
                check(spider.getNavigator() instanceof AdvancedClimberPathNavigator, "LOTR navigator installed");
                check(hasTask(spider, LOTREntityAIAttackOnCollide.class), "LOTR combat AI retained");
                check(hasTask(spider, LOTREntityAIFollowHiringPlayer.class), "LOTR hired follow AI retained");
                check(hasTask(spider, LOTREntityAIHiredRemainStill.class), "LOTR hired stay AI retained");
                check(!spider.targetTasks.taskEntries.isEmpty(), "LOTR faction target AI retained");
                spider.setSpiderScale(scale);
                spider.setPosition(2.5, 80, 0.5);
                tick(spider);
                check(
                    Math.abs(spider.width - 1.4F * spider.getSpiderScaleAmount()) < 0.001,
                    "LOTR body scale retained");
                SpiderIntegration.climb(spider, "LOTR variant=" + variant + " scale=" + scale);
                check(spider.getSpiderScale() == scale, "LOTR scale watcher retained");
            }
            LOTREntitySpiderBase spider = create(world, variant);
            spider.setSpiderScale(2);
            spider
                .setSpiderType(variant == 2 ? LOTREntitySpiderBase.VENOM_SLOWNESS : LOTREntitySpiderBase.VENOM_POISON);
            spider.setCustomNameTag("LOTR persistent spider");
            spider.setHealth(9);
            NBTTagCompound nbt = new NBTTagCompound();
            check(spider.writeToNBTOptional(nbt), "LOTR entity writes registered NBT");
            Entity loaded = EntityList.createEntityFromNBT(nbt, world);
            check(loaded != null && loaded.getClass() == spider.getClass(), "LOTR registered entity identity retained");
            LOTREntitySpiderBase restored = (LOTREntitySpiderBase) loaded;
            check(
                restored.getSpiderScale() == 2 && restored.getSpiderType() == spider.getSpiderType()
                    && restored.getHealth() == 9
                    && restored.getFaction() == spider.getFaction(),
                "LOTR saved data retained");
            EntityPig victim = new EntityPig(world);
            victim.setHealth(victim.getMaxHealth());
            check(spider.attackEntityAsMob(victim), "LOTR spider attack hits");
            check(victim.isPotionActive(variant == 2 ? Potion.moveSlowdown : Potion.poison), "LOTR venom retained");
            pursuit(world, variant);
        }
        LOTREntityMordorSpider mounted = new LOTREntityMordorSpider(world);
        mounted.setSpiderScale(2);
        mounted.setSpiderClimbing(true);
        check(!mounted.isOnLadder(), "unridden LOTR spider uses surface travel");
        FakePlayer rider = new FakePlayer(
            world,
            new GameProfile(UUID.fromString("b1d05361-f8ad-4eec-9de0-9f46ce99c7b5"), "LOTRRiderTest"));
        // FakePlayer has no connection for EntityPlayerMP.mountEntity's attach packet.
        mounted.riddenByEntity = rider;
        rider.ridingEntity = mounted;
        check(
            !SpiderClimber.get(mounted)
                .isActive() && mounted.isOnLadder(),
            "ridden LOTR spider keeps ladder controls");
        check(
            !SpiderClimber.get(mounted)
                .travel(0, 1),
            "ridden LOTR spider delegates to LOTR movement");
        mounted.riddenByEntity = null;
        rider.ridingEntity = null;
        check(
            SpiderClimber.get(mounted)
                .isActive() && !mounted.isOnLadder(),
            "dismounted LOTR spider resumes climbing");
        LOTREntityMordorOrc orcRider = new LOTREntityMordorOrc(world);
        orcRider.mountEntity(mounted);
        check(
            SpiderClimber.get(mounted)
                .isActive(),
            "LOTR orc riders retain climbing AI");
        mounted.setSpiderScale(1);
        mounted.setPosition(2.5, 80, 0.5);
        tick(mounted);
        SpiderIntegration.climb(mounted, "LOTR orc rider");
        SpiderIntegration.riderObstruction(mounted, "LOTR orc");
        SpiderIntegration.riderArchery(mounted);
        SpiderIntegration.riderDrop(mounted, "LOTR orc");
        orcRider.mountEntity(null);
        LOTREntityMordorSpider largeMount = new LOTREntityMordorSpider(world);
        largeMount.setSpiderScale(3);
        largeMount.setPosition(2.5, 80, 0.5);
        tick(largeMount);
        LOTREntityMordorOrc largeRider = new LOTREntityMordorOrc(world);
        largeRider.mountEntity(largeMount);
        SpiderIntegration.climb(largeMount, "large LOTR orc rider");
        SpiderIntegration.riderObstruction(largeMount, "large LOTR orc");
        SpiderIntegration.riderDrop(largeMount, "large LOTR orc");
        largeRider.mountEntity(null);
        check(SpiderClimber.get(new LOTREntityMordorOrc(world)) == null, "other LOTR NPCs are unchanged");
        fallDamage(world);
        Config.enabled = false;
        try {
            LOTREntityMordorSpider disabled = new LOTREntityMordorSpider(world);
            check(
                SpiderClimber.get(disabled) == null
                    && !(disabled.getNavigator() instanceof AdvancedClimberPathNavigator),
                "disabled config preserves LOTR");
            disabled.setSpiderClimbing(true);
            check(disabled.isOnLadder(), "disabled LOTR retains original climbing");
            check(!disabled.attackEntityFrom(DamageSource.fall, 4), "disabled LOTR retains original fall immunity");
        } finally {
            Config.enabled = true;
        }
        System.out.println(
            "LOTR_INTEGRATION PASS: 3 species, 12 size/climbing cases, native pursuit including narrow pillar, 72 wall-spawn cases, venom, NBT, mounts, fall damage and disable");
    }

    private static LOTREntitySpiderBase create(WorldServer world, int variant) {
        return variant == 0 ? new LOTREntityMirkwoodSpider(world)
            : variant == 1 ? new LOTREntityMordorSpider(world) : new LOTREntityUtumnoIceSpider(world);
    }

    private static boolean hasTask(LOTREntitySpiderBase spider, Class<?> type) {
        for (Object value : spider.tasks.taskEntries) {
            if (type.isInstance(((EntityAITasks.EntityAITaskEntry) value).action)) return true;
        }
        return false;
    }

    private static void pursuit(WorldServer world, int variant) {
        // A floating platform reached through a neighboring pillar and an overhead bridge.
        for (int x = 70; x <= 86; x++) for (int z = -5; z <= 5; z++) for (int y = 79; y <= 93; y++) {
            boolean floor = y == 79;
            boolean platform = y == 83 && x >= 71 && x <= 77 && Math.abs(z) <= 3;
            boolean pillar = x >= 83 && x <= 85 && Math.abs(z) <= 2 && y <= 91;
            boolean overhang = y == 91 && x >= 72 && x <= 85 && Math.abs(z) <= 3;
            world.setBlock(x, y, z, floor || platform || pillar || overhang ? Blocks.stone : Blocks.air, 0, 2);
        }
        EntityPig target = new EntityPig(world);
        target.setPosition(74.5, 84, 0.5);
        target.setHealth(target.getMaxHealth());
        LOTREntitySpiderBase spider = create(world, variant);
        spider.getRNG()
            .setSeed(12345);
        spider.setSpiderScale(1);
        spider.setPosition(80.5, 80, 0.5);
        spider.setAttackTarget(target);
        boolean climbed = false;
        for (int i = 0; i < 1200 && target.getHealth() == target.getMaxHealth(); i++) {
            tick(spider);
            climbed |= spider.posY > 87;
            if (i % 100 == 0) System.out.println(
                "LOTR_PURSUIT variant=" + variant
                    + " t="
                    + i
                    + " pos="
                    + spider.posX
                    + ","
                    + spider.posY
                    + ","
                    + spider.posZ);
        }
        check(
            climbed && target.getHealth() < target.getMaxHealth(),
            "LOTR native attack AI reaches floating platform: " + variant);
    }

    private static void fallDamage(WorldServer world) throws Exception {
        boolean original = Config.fallDamage;
        int originalSafe = Config.safeFallDistance;
        int originalDrop = Config.maxDropHeight;
        Method fall = EntityLivingBase.class.getDeclaredMethod("fall", float.class);
        fall.setAccessible(true);
        try {
            Config.safeFallDistance = -1;
            Config.maxDropHeight = 8;
            Config.fallDamage = true;
            LOTREntityMordorSpider spider = new LOTREntityMordorSpider(world);
            float health = spider.getHealth();
            check(
                spider.attackEntityFrom(DamageSource.fall, 2) && spider.getHealth() == health - 2,
                "LOTR respects enabled fall damage");
            for (int variant = 0; variant < 3; variant++) {
                LOTREntitySpiderBase falling = create(world, variant);
                float before = falling.getHealth();
                fall.invoke(falling, 8F);
                check(falling.getHealth() == before, "LOTR safe fall distance follows drop height");
                fall.invoke(falling, 9F);
                check(falling.getHealth() == before - 1, "LOTR damage beyond safe distance");
            }
            Config.safeFallDistance = 3;
            LOTREntityMordorSpider custom = new LOTREntityMordorSpider(world);
            float before = custom.getHealth();
            fall.invoke(custom, 4F);
            check(custom.getHealth() == before - 1, "LOTR explicit safe fall distance");
            Config.fallDamage = false;
            LOTREntityMordorSpider immune = new LOTREntityMordorSpider(world);
            check(!immune.attackEntityFrom(DamageSource.fall, 2), "LOTR respects disabled fall damage");
        } finally {
            Config.fallDamage = original;
            Config.safeFallDistance = originalSafe;
            Config.maxDropHeight = originalDrop;
        }
    }
}
