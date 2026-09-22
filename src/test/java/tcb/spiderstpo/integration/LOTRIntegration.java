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

    private static void playerRiding(WorldServer world, int scale) {
        FakePlayer rider = new FakePlayer(
            world,
            new GameProfile(UUID.fromString("b1d05361-f8ad-4eec-9de0-9f46ce99c7b6"), "SpiderControls")) {

            @Override
            public void updateRidden() {
                ridingEntity.updateRiderPosition();
            }
        };
        lotr.common.LOTRLevelData.getData(rider)
            .setAlignment(lotr.common.fac.LOTRFaction.MORDOR, 100);
        LOTREntityMordorSpider spider = new LOTREntityMordorSpider(world);
        spider.setSpiderScale(scale);
        spider.setPosition(2.5, 80, 0.5);
        tick(spider);
        spider.tameNPC(rider);
        spider.riddenByEntity = rider;
        rider.ridingEntity = spider;
        rider.rotationYaw = rider.prevRotationYaw = -90;
        tick(spider);
        SpiderClimber state = SpiderClimber.get(spider);
        check(state.isPlayerControlled() && state.isActive(), "tamed player mount uses climbing movement");
        check(
            !lotr.common.entity.LOTRMountFunctions.isPlayerControlledMount(spider),
            "LOTR position packets do not override surface movement");
        boolean wall = false, ceiling = false;
        double startX = spider.posX;
        for (int t = 0; t < 240; t++) {
            rider.setEntityActionState(0, 1, false, false);
            tick(spider);
            wall |= state.orientationNormal.x < -0.8;
            ceiling |= state.orientationNormal.y < -0.8;
            if (t % 20 == 0) System.out.println(
                "PLAYER_RIDING t=" + t
                    + " pos="
                    + spider.posX
                    + ","
                    + spider.posY
                    + ","
                    + spider.posZ
                    + " up="
                    + state.orientationNormal.x
                    + ","
                    + state.orientationNormal.y
                    + ","
                    + state.orientationNormal.z);
            if (ceiling && spider.posX < 6) break;
        }
        check(wall && ceiling, "W input climbs wall and crosses ceiling");
        check(Math.abs(spider.posX - startX) > 1, "mounted input changes position");
        check(
            rider.ridingEntity == spider && rider.getHealth() == rider.getMaxHealth(),
            "rider stays mounted and clear of blocks");
        tcb.spiderstpo.common.SurfaceFrame view = tcb.spiderstpo.common.entity.mob.ClimberRider.getViewFrame(rider, 1);
        check(view.up.y < -0.8, "player view turns upside down on ceiling");
        check(
            rider.getLook(1).yCoord * view.forward.y + rider.getLook(1).xCoord * view.forward.x
                + rider.getLook(1).zCoord * view.forward.z > 0.999,
            "aim matches rotated camera");
        check(
            spider.getSpiderClimbTime() > 0 && spider.shouldRenderClimbingMeter(),
            "LOTR climbing stamina and meter retained");
        cameraTurns(spider, rider, true);
        spider.setSpiderClimbTime(99);
        rider.setEntityActionState(0, 0, false, false);
        tick(spider);
        check(state.isDropping(), "exhausted player mount releases ceiling");
        // Resume the same ceiling attachment to exercise an independent Space press.
        for (int t = 0; t < 60; t++) {
            rider.setEntityActionState(0, 0, false, false);
            tick(spider);
        }
        check(spider.getSpiderClimbTime() == 0, "stamina recovers on floor");
        spider.setPosition(5.5, 87 - spider.height, 0.5);
        spider.motionX = spider.motionY = spider.motionZ = 0;
        for (int t = 0; t < 15; t++) {
            rider.setEntityActionState(0, 0, false, false);
            tick(spider);
        }
        double dropY = spider.posY;
        rider.setEntityActionState(0, 0, true, false);
        tick(spider);
        check(state.isDropping(), "Space starts ceiling drop");
        for (int t = 0; t < 8; t++) {
            rider.setEntityActionState(0, 0, true, false);
            tick(spider);
        }
        check(spider.posY < dropY - 0.5, "holding Space does not restart drop each tick");
        check(rider.getHealth() == rider.getMaxHealth(), "ceiling drop keeps player out of roof");
        spider.riddenByEntity = null;
        rider.ridingEntity = null;
        if (scale == 1) playerGroundControls(world, rider);
    }

    private static void playerGroundControls(WorldServer world, FakePlayer rider) {
        for (int x = 112; x <= 128; x++) for (int z = -8; z <= 8; z++)
            for (int y = 79; y <= 83; y++) world.setBlock(x, y, z, y == 79 ? Blocks.stone : Blocks.air, 0, 2);
        LOTREntityMordorSpider spider = new LOTREntityMordorSpider(world);
        spider.setSpiderScale(1);
        spider.tameNPC(rider);
        spider.riddenByEntity = rider;
        rider.ridingEntity = spider;
        rider.rotationYaw = rider.prevRotationYaw = -90;
        SpiderClimber state = SpiderClimber.get(spider);
        for (int direction = 0; direction < 4; direction++) {
            spider.setPosition(120.5, 80, 0.5);
            spider.motionX = spider.motionY = spider.motionZ = 0;
            tick(spider);
            for (int t = 0; t < 20; t++) {
                rider.setEntityActionState(
                    direction == 2 ? 1 : direction == 3 ? -1 : 0,
                    direction == 0 ? 1 : direction == 1 ? -1 : 0,
                    false,
                    false);
                tick(spider);
            }
            if (direction == 0) check(spider.posX > 121.5, "W moves forward");
            if (direction == 1) check(spider.posX < 120.2, "S moves backward");
            if (direction == 2) check(spider.posZ > 1, "A strafes left");
            if (direction == 3) check(spider.posZ < 0, "D strafes right");
        }
        rider.rotationYaw = rider.prevRotationYaw = 0;
        for (int t = 0; t < 15; t++) {
            rider.setEntityActionState(0, 1, false, false);
            tick(spider);
        }
        check(state.getRenderFrame(1).forward.z > 0.9, "mouse yaw steers mount");
        for (int t = 0; t < 10; t++) {
            rider.setEntityActionState(0, 0, false, false);
            tick(spider);
        }
        double x = spider.posX, z = spider.posZ;
        for (int t = 0; t < 10; t++) {
            rider.setEntityActionState(0, 0, false, false);
            tick(spider);
        }
        check(spider.getDistanceSq(x, spider.posY, z) < 0.02, "releasing WASD stops mount");
        tcb.spiderstpo.common.Vec3d serverEye = tcb.spiderstpo.common.entity.mob.ClimberRider.getEyePosition(rider);
        rider.yOffset = 1.62F;
        spider.updateRiderPosition();
        check(
            serverEye.subtract(tcb.spiderstpo.common.entity.mob.ClimberRider.getEyePosition(rider))
                .lengthVector() < 0.001,
            "client/server player eye offset agrees");
        FakePlayer clientRider = new FakePlayer(world, new GameProfile(UUID.randomUUID(), "ClientEye")) {

            @Override
            public float getDefaultEyeHeight() {
                return 0.12F;
            }
        };
        clientRider.yOffset = 1.62F;
        clientRider.ridingEntity = spider;
        spider.riddenByEntity = clientRider;
        spider.updateRiderPosition();
        check(
            serverEye.subtract(tcb.spiderstpo.common.entity.mob.ClimberRider.getEyePosition(clientRider))
                .lengthVector() < 0.001,
            "client default eye height matches server eye");
        clientRider.ridingEntity = null;
        spider.riddenByEntity = rider;
        rider.yOffset = 0;
        spider.setPosition(7.3, 83, 0.5);
        spider.motionX = spider.motionY = spider.motionZ = 0;
        for (int t = 0; t < 25; t++) {
            rider.setEntityActionState(0, 0, false, false);
            tick(spider);
        }
        check(state.orientationNormal.x < -0.8, "player mount attaches to wall");
        cameraTurns(spider, rider, false);
        double y = spider.posY;
        rider.setEntityActionState(0, 0, true, false);
        tick(spider);
        check(state.isDropping(), "Space starts wall drop");
        for (int t = 0; t < 8; t++) {
            rider.setEntityActionState(0, 0, true, false);
            tick(spider);
        }
        check(spider.posY < y - 0.5, "Space releases wall grip");
        for (int xw = 118; xw <= 124; xw++) for (int zw = -2; zw <= 3; zw++)
            for (int yw = 80; yw <= 81; yw++) world.setBlock(xw, yw, zw, Blocks.water, 0, 2);
        spider.setPosition(120.5, 80.5, -1.5);
        spider.motionX = spider.motionY = spider.motionZ = 0;
        rider.rotationYaw = rider.prevRotationYaw = 0;
        boolean inWater = false;
        for (int t = 0; t < 25; t++) {
            rider.setEntityActionState(0, 1, false, false);
            tick(spider);
            inWater |= spider.isInWater();
        }
        check(inWater && spider.posZ > -0.5, "mounted controls continue through water");
        spider.riddenByEntity = null;
        rider.ridingEntity = null;
    }

    private static void cameraTurns(LOTREntityMordorSpider spider, FakePlayer rider, boolean ceiling) {
        SpiderClimber state = SpiderClimber.get(spider);
        // Isolate camera movement from LOTR's intentional five-second stamina release.
        spider.setSpiderClimbTime(0);
        double y = spider.posY;
        for (int t = 0; t < 60; t++) {
            rider.rotationYaw += t % 2 == 0 ? 179 : -135;
            rider.rotationPitch = t % 2 == 0 ? 89 : -89;
            rider.setEntityActionState(0, 0, false, false);
            tick(spider);
            check(!state.isDropping(), "camera turns do not request a drop");
            check(
                ceiling ? state.orientationNormal.y < -0.8 : state.orientationNormal.x < -0.8,
                "sharp camera turns preserve surface attachment");
            check(Math.abs(spider.posY - y) < 0.3, "stationary camera turns do not make the mount fall");
        }
        check(rider.getHealth() == rider.getMaxHealth(), "turning player remains clear of blocks");
        rider.rotationPitch = 0;
    }

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
        for (int scale = 1; scale <= 3; scale++) playerRiding(world, scale);
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
            playerFallDamage(world, fall);
        } finally {
            Config.fallDamage = original;
            Config.safeFallDistance = originalSafe;
            Config.maxDropHeight = originalDrop;
        }
    }

    private static void playerFallDamage(WorldServer world, Method fall) throws Exception {
        FakePlayer rider = new FakePlayer(world, new GameProfile(UUID.randomUUID(), "SpiderFallRider")) {

            @Override
            public boolean isEntityInvulnerable() {
                return false;
            }
        };
        LOTREntityMordorSpider mount = new LOTREntityMordorSpider(world);
        mount.setSpiderScale(1);
        mount.tameNPC(rider);
        mount.riddenByEntity = rider;
        rider.ridingEntity = mount;
        SpiderClimber.get(mount)
            .setPlayerControlled(true);
        Config.fallDamage = true;
        Config.safeFallDistance = -1;
        Config.maxDropHeight = 8;
        fall.invoke(mount, 8F);
        check(rider.getHealth() == rider.getMaxHealth(), "mounted player shares spider safe distance");
        fall.invoke(mount, 9F);
        check(rider.getHealth() == rider.getMaxHealth() - 1, "mounted player takes damage beyond safe distance");
        rider.hurtResistantTime = 0;
        Config.safeFallDistance = 12;
        fall.invoke(rider, 12F);
        check(rider.getHealth() == rider.getMaxHealth() - 1, "mounted player respects explicit safe distance");
        Config.fallDamage = false;
        fall.invoke(mount, 100F);
        fall.invoke(rider, 100F);
        check(rider.getHealth() == rider.getMaxHealth() - 1, "mounted player shares disabled fall damage");
        mount.riddenByEntity = null;
        rider.ridingEntity = null;
        fall.invoke(rider, 4F);
        check(rider.getHealth() == rider.getMaxHealth() - 2, "dismounted player retains normal fall damage");
        rider.hurtResistantTime = 0;
        EntityPig pig = new EntityPig(world);
        pig.riddenByEntity = rider;
        rider.ridingEntity = pig;
        fall.invoke(pig, 4F);
        check(rider.getHealth() == rider.getMaxHealth() - 3, "other mounts retain normal player fall damage");
        pig.riddenByEntity = null;
        rider.ridingEntity = null;
    }
}
