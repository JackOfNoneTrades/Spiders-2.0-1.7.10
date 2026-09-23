package org.fentanylsolutions.nimblespiders.integration;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.entity.passive.EntityPig;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.FakePlayer;

import org.fentanylsolutions.nimblespiders.common.Config;
import org.fentanylsolutions.nimblespiders.common.SurfaceFrame;
import org.fentanylsolutions.nimblespiders.common.Vec3d;
import org.fentanylsolutions.nimblespiders.common.entity.mob.SpiderClimber;
import org.fentanylsolutions.nimblespiders.common.entity.movement.AdvancedClimberPathNavigator;
import org.fentanylsolutions.nimblespiders.common.entity.movement.AdvancedWalkNodeProcessor;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;

/** Runs against actual transformed Minecraft classes, in the disposable development world. */
@Mod(
    modid = "nimble-spiders-integration",
    name = "Spiders integration tests",
    version = "1",
    dependencies = "required-after:nimble-spiders")
public class SpiderIntegration {

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("nimble-spiders.integration")) return;
        String result;
        try {
            WorldServer world = MinecraftServer.getServer()
                .worldServerForDimension(0);
            world.difficultySetting = EnumDifficulty.NORMAL;
            world.getGameRules()
                .setOrCreateGameRule("doMobSpawning", "false");
            for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) world.getChunkFromChunkCoords(x, z);
            for (int x = -3; x <= 12; x++) for (int z = -4; z <= 4; z++) for (int y = 79; y <= 89; y++) {
                world.setBlock(
                    x,
                    y,
                    z,
                    y == 79 || x == 8 && y <= 87 || y == 87 && x <= 8 ? Blocks.stone : Blocks.air,
                    0,
                    2);
            }
            EntitySpider spider = new EntitySpider(world);
            check(spider.getClass() == EntitySpider.class, "vanilla spider identity");
            check(spider.getNavigator() instanceof AdvancedClimberPathNavigator, "mixin installs navigator");
            spider.setCustomNameTag("Persistent spider");
            spider.setHealth(9);
            NBTTagCompound nbt = new NBTTagCompound();
            check(spider.writeToNBTOptional(nbt), "vanilla save succeeds");
            check("Spider".equals(nbt.getString("id")), "vanilla save ID retained");
            Entity loaded = EntityList.createEntityFromNBT(nbt, world);
            check(
                loaded.getClass() == EntitySpider.class && ((EntitySpider) loaded).getHealth() == 9,
                "vanilla NBT round trip");
            check("Persistent spider".equals(((EntitySpider) loaded).getCustomNameTag()), "name retained");
            climb(spider, "spider");
            EntityCaveSpider cave = new EntityCaveSpider(world);
            climb(cave, "cave spider");
            EntityPig pig = new EntityPig(world);
            check(cave.attackEntityAsMob(pig) && pig.isPotionActive(Potion.poison), "cave spider poison retained");
            ModdedSpider subclass = new ModdedSpider(world);
            check(
                subclass.getDataWatcher()
                    .getWatchableObjectInt(20) == 42,
                "subclass watcher slot retained");
            climb(subclass, "subclass");
            climb(new LargeSpider(world), "large subclass");
            EntitySpider jockeyMount = new EntitySpider(world);
            net.minecraft.entity.monster.EntitySkeleton skeleton = new net.minecraft.entity.monster.EntitySkeleton(
                world);
            skeleton.mountEntity(jockeyMount);
            climb(jockeyMount, "vanilla skeleton jockey");
            riderObstruction(jockeyMount, "vanilla skeleton");
            riderArchery(jockeyMount);
            riderDrop(jockeyMount, "vanilla skeleton");
            skeleton.mountEntity(null);
            SpeedIntegration.run(world);
            if (Loader.isModLoaded("lotr")) LOTRIntegration.run(world);
            if (Loader.isModLoaded("abyssalcraft")) AbyssalCraftIntegration.run(world);
            EntitySpider falling = new EntitySpider(world);
            falling.setPosition(30, 110, 0);
            falling.tasks.taskEntries.clear();
            falling.targetTasks.taskEntries.clear();
            for (int i = 0; i < 20; i++) tick(falling);
            check(falling.posY < 105, "unsupported spiders fall");
            for (int x = 19; x <= 31; x++) for (int z = 6; z <= 14; z++) {
                world.setBlock(x, 79, z, Blocks.stone, 0, 2);
                if (x >= 23) world.setBlock(x, 80, z, Blocks.stone_slab, 0, 2);
            }
            EntitySpider slabWalker = new EntitySpider(world);
            slabWalker.tasks.taskEntries.clear();
            slabWalker.targetTasks.taskEntries.clear();
            slabWalker.setPosition(21.5, 80, 10.5);
            PathEntity slabPath = slabWalker.getNavigator()
                .getPathToXYZ(28.5, 81, 10.5);
            check(slabPath != null && slabPath.getFinalPathPoint().xCoord == 28, "route across bottom slabs");
            for (int i = 0; i < 240; i++) {
                if (i % 20 == 0) slabWalker.getNavigator()
                    .tryMoveToXYZ(28.5, 81, 10.5, 1);
                tick(slabWalker);
            }
            check(slabWalker.posX > 27, "walk across bottom slabs; x=" + slabWalker.posX);
            platform(world);
            platformAttachment(world);
            overhang(world, false, 85, 90);
            overhang(world, false, 84, 92);
            overhang(world, true, 85, 90);
            userPlatform(world);
            rotation(world);
            fallDamage(world);
            replanning(world);
            floatingBlock(world);
            Config.enabled = false;
            EntitySpider disabled = new EntitySpider(world);
            check(
                SpiderClimber.get(disabled) == null
                    && !(disabled.getNavigator() instanceof AdvancedClimberPathNavigator),
                "disabled config preserves vanilla");
            Config.enabled = true;
            result = "PASS: vanilla identity/NBT, wall and ceiling navigation for spiders/cave spiders/subclasses, snow layers, platform pursuit and ceiling drops, 18 reported-layout pursuit cases, 48 platform attachment cases, rotation synchronization, vanilla jockey health, ceiling drops, real head obstructions and ceiling archery, poison, watcher compatibility, config disable";
        } catch (Throwable failure) {
            failure.printStackTrace();
            result = "FAIL: " + failure;
        }
        System.out.println("SPIDERS_INTEGRATION " + result);
        Files.write(Paths.get("spiders-integration-result.txt"), result.getBytes(StandardCharsets.UTF_8));
        MinecraftServer.getServer()
            .initiateShutdown();
    }

    static void climb(EntityCreature entity, String label) {
        entity.tasks.taskEntries.clear();
        entity.targetTasks.taskEntries.clear();
        entity.setPosition(2.5, 80, 0.5);
        EntityLivingBase passenger = (EntityLivingBase) entity.riddenByEntity;
        float riderHealth = passenger == null ? 0 : passenger.getHealth();
        if (passenger instanceof EntityCreature) {
            ((EntityCreature) passenger).tasks.taskEntries.clear();
            ((EntityCreature) passenger).targetTasks.taskEntries.clear();
        }
        int targetY = 87 - (int) Math.ceil(entity.height);
        boolean ceiling = false;
        double maxY = 0;
        for (int tick = 0; tick < 600; tick++) {
            if (tick % 20 == 0) {
                PathEntity path = entity.getNavigator()
                    .getPathToXYZ(3.5, targetY, 0.5);
                if (tick == 0)
                    check(path != null && path.getFinalPathPoint().yCoord == targetY, label + " ceiling path");
                entity.getNavigator()
                    .setPath(path, 1);
            }
            tick(entity);
            if (passenger != null) check(
                passenger.getHealth() == riderHealth,
                label + " rider takes no damage at tick="
                    + tick
                    + " pos="
                    + entity.posX
                    + ","
                    + entity.posY
                    + " health="
                    + passenger.getHealth());
            SpiderClimber state = SpiderClimber.get(entity);
            maxY = Math.max(maxY, entity.posY);
            if (tick % 100 == 0) System.out.println(
                "SPIDER_TRACE " + label
                    + " t="
                    + tick
                    + " pos="
                    + entity.posX
                    + ","
                    + entity.posY
                    + ","
                    + entity.posZ
                    + " normal="
                    + state.orientationNormal.x
                    + ","
                    + state.orientationNormal.y
                    + ","
                    + state.orientationNormal.z);
            if (state.orientationNormal.y < -0.8 && entity.posY > 87 - entity.height - 0.2 && entity.posX < 6) {
                ceiling = true;
                break;
            }
        }
        check(ceiling, label + " reaches and walks on ceiling; maxY=" + maxY);
    }

    static void riderDrop(EntityCreature mount, String label) {
        riderDrop(mount, label, false);
        climb(mount, label + " return to ceiling");
        riderDrop(mount, label, true);
    }

    private static void riderDrop(EntityCreature mount, String label, boolean slowFall) {
        mount.getNavigator()
            .clearPathEntity();
        mount.setMoveForward(0);
        // Settle completely against the flat ceiling before beginning the departure.
        for (int i = 0; i < 12; i++) tick(mount);
        EntityLivingBase rider = (EntityLivingBase) mount.riddenByEntity;
        float health = rider.getHealth();
        SpiderClimber state = SpiderClimber.get(mount);
        check(state.orientationNormal.y < -0.9, label + " drop starts upside down");
        state.dropFromSurface();
        // A slow fall must wait for space rather than turn the passenger into the roof.
        for (int i = 0; slowFall && i < 12; i++) {
            state.afterVanillaTravel();
            mount.updateRiderPosition();
            check(
                mount.worldObj.func_147461_a(rider.boundingBox.contract(0.01, 0.01, 0.01))
                    .isEmpty(),
                label + " waits for clearance before righting");
        }
        check(state.orientationNormal.y < 0.9, label + " cannot stand rider upright against ceiling");
        boolean landed = false;
        for (int t = 0; t < 60; t++) {
            Vec3d previous = state.orientationNormal;
            tick(mount);
            // Landing damage still follows the rider's normal fall rules.
            if (mount.posY > 80.1)
                check(rider.getHealth() == health, label + " ceiling drop rider takes no airborne damage tick=" + t);
            check(
                mount.worldObj.func_147461_a(rider.boundingBox.contract(0.01, 0.01, 0.01))
                    .isEmpty(),
                label + " ceiling drop rider clears blocks tick=" + t);
            check(previous.dotProduct(state.orientationNormal) > 0.8, label + " ceiling drop turns smoothly tick=" + t);
            if (mount.posY < 80.1 && mount.onGround && state.orientationNormal.y > 0.99) {
                landed = true;
                break;
            }
        }
        check(landed, label + " ceiling drop lands upright");
    }

    static void riderArchery(EntityCreature mount) {
        EntityLivingBase rider = (EntityLivingBase) mount.riddenByEntity;
        EntityPig target = new EntityPig(mount.worldObj);
        target.setPosition(2.5, 80, 2.5);
        mount.worldObj.spawnEntityInWorld(target);
        net.minecraft.entity.projectile.EntityArrow arrow = new net.minecraft.entity.projectile.EntityArrow(
            mount.worldObj,
            rider,
            target,
            1.6F,
            0);
        try {
            check(rider.canEntityBeSeen(target), "ceiling jockey sees its target from the rotated head");
            check(arrow.posY < 87 && arrow.posY > 80, "ceiling jockey arrow starts outside roof");
            float health = target.getHealth();
            for (int t = 0; t < 40 && !arrow.isDead; t++) arrow.onUpdate();
            check(target.getHealth() < health, "ceiling jockey arrow hits its target");
        } finally {
            mount.worldObj.removeEntity(target);
            arrow.setDead();
        }
    }

    static void riderObstruction(EntityCreature mount, String label) {
        Entity rider = mount.riddenByEntity;
        mount.updateRiderPosition();
        check(!rider.isEntityInsideOpaqueBlock(), label + " head clears ceiling");
        SurfaceFrame frame = SpiderClimber.get(mount)
            .getRenderFrame(1);
        Vec3d head = frame.up.scale(rider.getEyeHeight())
            .addVector(rider.posX, rider.posY, rider.posZ);
        int x = net.minecraft.util.MathHelper.floor_double(head.x);
        int y = net.minecraft.util.MathHelper.floor_double(head.y);
        int z = net.minecraft.util.MathHelper.floor_double(head.z);
        net.minecraft.block.Block previous = mount.worldObj.getBlock(x, y, z);
        int metadata = mount.worldObj.getBlockMetadata(x, y, z);
        try {
            mount.worldObj.setBlock(x, y, z, Blocks.stone, 0, 2);
            check(rider.isEntityInsideOpaqueBlock(), label + " real head obstruction still suffocates");
        } finally {
            mount.worldObj.setBlock(x, y, z, previous, metadata, 2);
        }
    }

    private static void platform(WorldServer world) {
        // Connected platform: round the outside edge of a one-block-thick ledge above snow.
        for (int x = 17; x <= 31; x++) for (int z = -23; z <= -9; z++) for (int y = 79; y <= 90; y++) {
            boolean post = x == 24 && z == -16 && y <= 87;
            boolean platform = x >= 21 && x <= 27 && z >= -19 && z <= -13 && y == 83;
            world.setBlock(
                x,
                y,
                z,
                y == 79 || post || platform ? Blocks.planks : y == 80 ? Blocks.snow_layer : Blocks.air,
                0,
                2);
        }
        EntitySpider spider = new EntitySpider(world);
        spider.tasks.taskEntries.clear();
        spider.targetTasks.taskEntries.clear();
        spider.setPosition(22.5, 80, -15.5);
        boolean reached = false;
        for (int tick = 0; tick < 800; tick++) {
            if (tick % 12 == 0) {
                PathEntity path = spider.getNavigator()
                    .getPathToXYZ(22.5, 84, -15.5);
                if (tick == 0) {
                    check(path != null && path.getFinalPathPoint().yCoord == 84, "route onto platform");
                }
                spider.getNavigator()
                    .setPath(path, 1);
            }
            tick(spider);
            if (tick % 40 == 0) {
                SpiderClimber state = SpiderClimber.get(spider);
                PathEntity path = spider.getNavigator()
                    .getPath();
                System.out.println(
                    "PLATFORM_TRACE t=" + tick
                        + " pos="
                        + spider.posX
                        + ","
                        + spider.posY
                        + ","
                        + spider.posZ
                        + " normal="
                        + state.orientationNormal.x
                        + ","
                        + state.orientationNormal.y
                        + ","
                        + state.orientationNormal.z
                        + " point="
                        + (path == null || path.isFinished() ? "none"
                            : path.getPathPointFromIndex(path.getCurrentPathIndex())));
            }
            if (spider.posY >= 84 && spider.getDistanceSq(22.5, 84, -15.5) < 1) {
                reached = true;
                break;
            }
        }
        check(reached, "spider climbs narrow post and rounds platform edge above snow");
        world.setWorldTime(18000);
        world.skylightSubtracted = 11;
        FakePlayer player = new TestPlayer(world);
        player.theItemInWorldManager.setGameType(WorldSettings.GameType.SURVIVAL);
        player.capabilities.disableDamage = false;
        player.setPosition(22.5, 84, -15.5);
        player.setHealth(20);
        EntitySpider angry = new EntitySpider(world);
        angry.getRNG()
            .setSeed(12345);
        angry.setPosition(22.5, 80, -15.5);
        angry.ticksExisted = 1;
        angry.attackEntityFrom(DamageSource.causePlayerDamage(player), 1);
        for (int tick = 0; tick < 600 && player.getHealth() == 20; tick++) {
            tick(angry);
            if (tick % 40 == 0) System.out.println(
                "PURSUIT_TRACE t=" + tick
                    + " pos="
                    + angry.posX
                    + ","
                    + angry.posY
                    + ","
                    + angry.posZ
                    + " target="
                    + (angry.getAttackTarget() == player));
        }
        check(player.getHealth() < 20, "retaliating spider reaches and attacks player on platform with full AI");
    }

    private static void platformAttachment(WorldServer world) {
        // A one-block-thick floating ledge, with snow inside the attachment query below it.
        for (int snow = 0; snow < 2; snow++) {
            for (int x = 17; x <= 31; x++) for (int z = -23; z <= -9; z++) for (int y = 79; y <= 90; y++) {
                boolean ledge = x >= 21 && x <= 27 && z >= -19 && z <= -13 && y == 83;
                world.setBlock(
                    x,
                    y,
                    z,
                    ledge ? Blocks.planks
                        : y == 81 ? Blocks.grass : y == 82 && snow == 1 ? Blocks.snow_layer : Blocks.air,
                    0,
                    2);
            }
            for (int variant = 0; variant < 2; variant++)
                for (int side = 0; side < 4; side++) for (int spot = 0; spot < 3; spot++) {
                    EntitySpider spider = variant == 0 ? new EntitySpider(world) : new EntityCaveSpider(world);
                    spider.tasks.taskEntries.clear();
                    spider.targetTasks.taskEntries.clear();
                    double halfWidth = spider.width / 2.0;
                    double x = side == 0 ? 21 - halfWidth : side == 1 ? 28 + halfWidth : 21.15 + spot * 3.35;
                    double z = side == 2 ? -19 - halfWidth : side == 3 ? -12 + halfWidth : -18.85 + spot * 3.35;
                    spider.setPosition(x, 83.5 - spider.height / 2.0, z);
                    Vec3d outward = new Vec3d(
                        side == 0 ? -1 : side == 1 ? 1 : 0,
                        0,
                        side == 2 ? -1 : side == 3 ? 1 : 0);
                    for (int ticks = 0; ticks < 40; ticks++) {
                        tick(spider);
                        if (ticks < 10) continue;
                        SpiderClimber state = SpiderClimber.get(spider);
                        SurfaceFrame frame = state.getRenderFrame(1);
                        String label = "platform attachment snow=" + snow
                            + " variant="
                            + variant
                            + " side="
                            + side
                            + " spot="
                            + spot;
                        check(
                            state.orientationNormal.dotProduct(outward) > (spot == 1 ? 0.9 : 0.6),
                            label + " belly faces ledge, normal=" + state.orientationNormal);
                        check(
                            frame.up.dotProduct(outward) > (spot == 1 ? 0.9 : 0.6),
                            label + " rendered feet face ledge");
                        check(Math.abs(state.orientationNormal.y) < 0.45, label + " body does not remain upright");
                        check(
                            Math.abs(frame.forward.dotProduct(state.orientationNormal)) < 0.001,
                            label + " body runs along ledge");
                        check(
                            spider.boundingBox.maxY > 83 && spider.boundingBox.minY < 84,
                            label + " remains beside the ledge rather than falling");
                    }
                }
        }
    }

    private static void overhang(WorldServer world, boolean cave, int platformY, int roofY) {
        // The player's island has no connection to the pillar or roof.
        for (int x = 17; x <= 31; x++) for (int z = -23; z <= -9; z++) for (int y = 79; y <= 95; y++) {
            boolean post = x == 27 && z == -16 && y <= roofY;
            boolean island = x >= 21 && x <= 23 && z >= -17 && z <= -15 && y == platformY;
            boolean roof = x >= 20 && x <= 27 && z >= -19 && z <= -13 && y == roofY;
            world.setBlock(
                x,
                y,
                z,
                y == 79 || post || island || roof ? Blocks.planks : y == 80 ? Blocks.snow_layer : Blocks.air,
                0,
                2);
        }
        FakePlayer player = new TestPlayer(world);
        player.setPosition(22.5, platformY + 1, -15.5);
        player.setHealth(20);
        player.hurtResistantTime = 0;
        EntitySpider angry = cave ? new EntityCaveSpider(world) : new EntitySpider(world);
        AdvancedWalkNodeProcessor processor = new AdvancedWalkNodeProcessor(angry);
        check(!processor.canMove(26, 80, -15, 26, 81, -15), "corner-only contact cannot support climbing");
        double dropY = roofY - (double) angry.height;
        check(processor.canDropTo(22.5, dropY, -15.5, platformY + 1), "clear descent to island");
        world.setBlock(22, platformY + 3, -16, Blocks.planks, 0, 2);
        check(!processor.canDropTo(22.5, dropY, -15.5, platformY + 1), "no drop through solid blocks");
        world.setBlock(22, platformY + 3, -16, Blocks.air, 0, 2);
        check(!processor.canDropTo(22.5, platformY + 10, -15.5, platformY + 1), "drop height is bounded");
        angry.getRNG()
            .setSeed(67890);
        angry.setPosition(22.5, 80, -15.5);
        angry.ticksExisted = 1;
        angry.attackEntityFrom(DamageSource.causePlayerDamage(player), 1);
        boolean ceiling = false, dropped = false;
        for (int tick = 0; tick < 600 && player.getHealth() == 20; tick++) {
            tick(angry);
            SpiderClimber state = SpiderClimber.get(angry);
            ceiling |= state.orientationNormal.y < -0.8 && angry.posY > roofY - 2;
            dropped |= ceiling && angry.motionY < -0.1;
            if (tick % 40 == 0) System.out.println(
                "OVERHANG_TRACE cave=" + cave
                    + " t="
                    + tick
                    + " pos="
                    + angry.posX
                    + ","
                    + angry.posY
                    + ","
                    + angry.posZ
                    + " target="
                    + (angry.getAttackTarget() == player));
        }
        check(
            ceiling && dropped && player.getHealth() < 20,
            "spider crosses overhang and drops to attack player on disconnected island");
    }

    private static void userPlatform(WorldServer world) throws Exception {
        // Read only block geometry from the reported test build, isolated from other fixtures.
        for (int x = -1; x <= 0; x++) for (int z = 4; z <= 5; z++) world.getChunkFromChunkCoords(x, z);
        try (java.io.BufferedReader reader = new java.io.BufferedReader(
            new java.io.InputStreamReader(
                SpiderIntegration.class.getResourceAsStream("/platform-repro.txt"),
                StandardCharsets.UTF_8))) {
            reader.readLine();
            for (int y = 67; y <= 78; y++) for (int z = 6; z <= 16; z++) {
                String row = reader.readLine();
                for (int x = -10; x <= 3; x++) {
                    char c = row.charAt(x + 10);
                    net.minecraft.block.Block block = c == 'g' ? Blocks.grass
                        : c == 'd' ? Blocks.dirt
                            : c == 'P' ? Blocks.planks
                                : c == 's' ? Blocks.snow_layer
                                    : c == 't' ? Blocks.tallgrass
                                        : c == 'i' ? Blocks.ice : c == 'w' ? Blocks.water : Blocks.air;
                    world.setBlock(x, y, z + 64, block, c == 't' ? 1 : 0, 2);
                }
            }
        }
        double[][] starts = { { -7.197, 70, 10.522 }, { -0.35, 70.6, 10.448 }, { -1.475, 72.942, 12.117 } };
        AdvancedWalkNodeProcessor processor = new AdvancedWalkNodeProcessor(new EntitySpider(world));
        check(!processor.canMove(-2, 72, 76, -3, 72, 76), "cannot walk between facing walls across a gap");
        for (double targetZ : new double[] { 73.5, 75.5, 77.5 })
            for (int variant = 0; variant < 2; variant++) for (double[] start : starts) {
                FakePlayer player = new TestPlayer(world);
                player.setPosition(-5.5, 73, targetZ);
                player.setHealth(20);
                EntitySpider spider = variant == 0 ? new EntitySpider(world) : new EntityCaveSpider(world);
                spider.setPosition(start[0], start[1], start[2] + 64);
                spider.ticksExisted = 1;
                spider.getRNG()
                    .setSeed(12345);
                spider.attackEntityFrom(DamageSource.causePlayerDamage(player), 1);
                boolean ceiling = false;
                for (int ticks = 0; ticks < 800 && player.getHealth() == 20; ticks++) {
                    tick(spider);
                    ceiling |= SpiderClimber.get(spider).orientationNormal.y < -0.7;
                    if (ticks % 100 == 0) System.out.println(
                        "USER_PLATFORM variant=" + variant
                            + " targetZ="
                            + targetZ
                            + " start="
                            + start[0]
                            + ","
                            + start[1]
                            + ","
                            + start[2]
                            + " tick="
                            + ticks
                            + " pos="
                            + spider.posX
                            + ","
                            + spider.posY
                            + ","
                            + spider.posZ);
                }
                check(
                    player.getHealth() < 20,
                    "reported platform pursuit variant=" + variant
                        + " start="
                        + java.util.Arrays.toString(start)
                        + " targetZ="
                        + targetZ
                        + " reachedCeiling="
                        + ceiling);
            }
    }

    private static void rotation(WorldServer world) throws Exception {
        EntitySpider spider = new EntitySpider(world);
        spider.setPosition(7.525, 84, 0.5);
        spider.prevPosY = 83.9;
        spider.rotationYaw = 20;
        spider.renderYawOffset = spider.rotationYawHead = 170;
        java.lang.reflect.Method body = EntitySpider.class.getDeclaredMethod("func_110146_f", float.class, float.class);
        body.setAccessible(true);
        body.invoke(spider, 170F, 0F);
        check(spider.renderYawOffset == 20, "vertical motion keeps body aligned with movement");
        SpiderClimber state = SpiderClimber.get(spider);
        for (int degrees = 110; degrees <= 120; degrees++) {
            state.orientationNormal = new org.fentanylsolutions.nimblespiders.common.Vec3d(
                Math.sin(Math.toRadians(degrees)),
                Math.cos(Math.toRadians(degrees)),
                0);
            org.fentanylsolutions.nimblespiders.common.Vec3d forward = new org.fentanylsolutions.nimblespiders.common.Vec3d(
                0,
                0,
                1);
            spider.renderYawOffset = state.getOrientation(1)
                .getRotation(forward)
                .getLeft();
            spider.rotationYawHead = spider.renderYawOffset + 30;
            org.fentanylsolutions.nimblespiders.common.network.ClimberStateMessage sent = new org.fentanylsolutions.nimblespiders.common.network.ClimberStateMessage(
                state);
            io.netty.buffer.ByteBuf bytes = io.netty.buffer.Unpooled.buffer();
            try {
                sent.toBytes(bytes);
                org.fentanylsolutions.nimblespiders.common.network.ClimberStateMessage received = new org.fentanylsolutions.nimblespiders.common.network.ClimberStateMessage();
                received.fromBytes(bytes);
                check(received.forward.dotProduct(forward) > 0.999, "packet keeps world heading across frame seam");
                check(Math.abs(received.headYaw - 30) < 0.001, "head angle is paired with body frame");
                check(
                    received.normal.dotProduct(state.orientationNormal) > 0.999,
                    "packet pairs orientation with heading");
            } finally {
                bytes.release();
            }
        }
    }

    private static void fallDamage(WorldServer world) throws Exception {
        int previousDrop = Config.maxDropHeight, previousSafe = Config.safeFallDistance;
        boolean previousDamage = Config.fallDamage;
        java.lang.reflect.Method fall = net.minecraft.entity.EntityLivingBase.class
            .getDeclaredMethod("fall", float.class);
        fall.setAccessible(true);
        try {
            Config.maxDropHeight = 8;
            Config.safeFallDistance = -1;
            Config.fallDamage = true;
            for (EntitySpider spider : new EntitySpider[] { new EntitySpider(world), new EntityCaveSpider(world),
                new ModdedSpider(world) }) {
                float health = spider.getHealth();
                fall.invoke(spider, 8F);
                check(spider.getHealth() == health, "eight-block fall is safe for " + spider.getClass());
                fall.invoke(spider, 9F);
                check(
                    spider.getHealth() == health - 1,
                    "one damage point beyond safe distance for " + spider.getClass());
            }
            EntitySpider custom = new EntitySpider(world);
            Config.maxDropHeight = 12;
            fall.invoke(custom, 12F);
            check(custom.getHealth() == custom.getMaxHealth(), "safe distance follows changed drop height");
            Config.safeFallDistance = 3;
            fall.invoke(custom, 4F);
            check(custom.getHealth() == custom.getMaxHealth() - 1, "explicit safe distance overrides drop height");
            Config.fallDamage = false;
            custom.hurtResistantTime = 0;
            fall.invoke(custom, 100F);
            check(custom.getHealth() == custom.getMaxHealth() - 1, "fall damage can be disabled");
            Config.fallDamage = true;
            Config.enabled = false;
            EntitySpider disabled = new EntitySpider(world);
            Config.enabled = true;
            fall.invoke(disabled, 4F);
            check(disabled.getHealth() == disabled.getMaxHealth() - 1, "unmodified spiders retain vanilla fall damage");
            Config.safeFallDistance = -1;
            Config.maxDropHeight = 8;
            EntityPig pig = new EntityPig(world);
            fall.invoke(pig, 4F);
            check(pig.getHealth() == pig.getMaxHealth() - 1, "other mobs retain vanilla safe distance");
            for (int x = 40; x <= 44; x++) for (int z = 0; z <= 4; z++)
                for (int y = 79; y <= 120; y++) world.setBlock(x, y, z, y == 79 ? Blocks.stone : Blocks.air, 0, 2);
            AdvancedWalkNodeProcessor processor = new AdvancedWalkNodeProcessor(new EntitySpider(world));
            check(!processor.canDropTo(42.5, 89, 2.5, 80), "default voluntary drop limit");
            Config.maxDropHeight = 12;
            check(processor.canDropTo(42.5, 89, 2.5, 80), "configured voluntary drop limit");
            Config.maxDropHeight = 0;
            check(!processor.canDropTo(42.5, 81, 2.5, 80), "zero disables voluntary drops");
            Config.maxDropHeight = 8;
            for (int height : new int[] { 8, 12 }) {
                EntitySpider dropped = new EntitySpider(world);
                dropped.tasks.taskEntries.clear();
                dropped.targetTasks.taskEntries.clear();
                dropped.setPosition(42.5, 80 + height, 2.5);
                SpiderClimber.get(dropped)
                    .dropFromSurface();
                for (int ticks = 0; ticks < 100 && !dropped.onGround; ticks++) tick(dropped);
                check(dropped.onGround && dropped.posY == 80, "actual fall lands on the ground");
                // Vanilla accumulates fall distance before the landing tick, so the final partial
                // block may not count. Check safety at the limit and damage beyond it.
                float damage = dropped.getMaxHealth() - dropped.getHealth();
                check(
                    height == 8 ? damage == 0 : damage > 0 && damage <= height - 8,
                    "actual fall applies configured damage: " + height + " health=" + dropped.getHealth());
            }
            world.setBlock(42, 89, 2, Blocks.planks, 0, 2);
            check(processor.getDropY(42, 88, 2) == 88, "ceiling offset cannot put planned fall over the limit");
            world.setBlockToAir(42, 89, 2);
            world.setBlock(42, 88, 2, Blocks.planks, 0, 2);
            processor = new AdvancedWalkNodeProcessor(new EntitySpider(world));
            check(processor.getDropY(42, 87, 2) == 80, "ceiling drop within limit is available");
        } finally {
            Config.enabled = true;
            Config.maxDropHeight = previousDrop;
            Config.safeFallDistance = previousSafe;
            Config.fallDamage = previousDamage;
        }
    }

    private static void replanning(WorldServer world) {
        for (int x = 100; x <= 116; x++) for (int z = 0; z <= 4; z++)
            for (int y = 79; y <= 85; y++) world.setBlock(x, y, z, y == 79 ? Blocks.stone : Blocks.air, 0, 2);
        EntitySpider spider = new EntitySpider(world);
        spider.tasks.taskEntries.clear();
        spider.targetTasks.taskEntries.clear();
        spider.setPosition(100.9, 80, 2.5);
        spider.rotationYaw = spider.renderYawOffset = -90;
        double backwards = 0;
        for (int ticks = 0; ticks < 200 && spider.posX < 110; ticks++) {
            if (ticks % 5 == 0) spider.getNavigator()
                .tryMoveToXYZ(112.5, 80, ticks % 10 == 0 ? 2.5 : 3.5, 1);
            double previousX = spider.posX;
            tick(spider);
            backwards += Math.max(0, previousX - spider.posX);
        }
        check(spider.posX > 110, "replanning pursuit makes forward progress");
        check(backwards < 0.05, "replanning does not send spider back to cell centers: " + backwards);
    }

    private static void floatingBlock(WorldServer world) {
        for (int x = 62; x <= 78; x++) for (int z = 0; z <= 8; z++) for (int y = 79; y <= 91; y++) {
            boolean floating = x == 66 && z == 4 && y == 85;
            boolean platform = x >= 72 && x <= 74 && z >= 3 && z <= 5 && y == 83;
            boolean pillar = x == 74 && z == 4 && y <= 83;
            world.setBlock(x, y, z, y == 79 || floating || platform || pillar ? Blocks.planks : Blocks.air, 0, 2);
        }
        for (int variant = 0; variant < 2; variant++) {
            EntitySpider spider = variant == 0 ? new EntitySpider(world) : new EntityCaveSpider(world);
            FakePlayer player = new TestPlayer(world);
            player.setPosition(72.5, 84, 4.5);
            spider.setPosition(66.5, 86, 4.5);
            spider.ticksExisted = 1;
            int limit = Config.maxDropHeight;
            try {
                Config.maxDropHeight = 0;
                PathEntity blocked = spider.getNavigator()
                    .getPathToXYZ(72.5, 84, 4.5);
                check(
                    blocked == null || blocked.getFinalPathPoint().xCoord != 72,
                    "disabling drops does not invent a route off the isolated block");
                Config.maxDropHeight = 3;
                blocked = spider.getNavigator()
                    .getPathToXYZ(72.5, 84, 4.5);
                check(
                    blocked == null || blocked.getFinalPathPoint().xCoord != 72,
                    "isolated block descent respects the configured drop limit");
            } finally {
                Config.maxDropHeight = limit;
            }
            spider.getRNG()
                .setSeed(12345);
            spider.attackEntityFrom(DamageSource.causePlayerDamage(player), 1);
            spider.motionX = spider.motionY = spider.motionZ = 0;
            boolean dropped = false, landed = false;
            for (int ticks = 0; ticks < 800 && player.getHealth() == 20; ticks++) {
                tick(spider);
                dropped |= SpiderClimber.get(spider)
                    .isDropping();
                landed |= spider.onGround && spider.posY == 80;
                if (ticks % 100 == 0) System.out.println(
                    "FLOATING_BLOCK variant=" + variant
                        + " tick="
                        + ticks
                        + " pos="
                        + spider.posX
                        + ","
                        + spider.posY
                        + ","
                        + spider.posZ);
            }
            check(dropped && landed, "spider leaves isolated plank using a planned drop: " + variant);
            check(player.getHealth() < 20, "spider drops to ground and climbs back to target: " + variant);
            check(spider.getHealth() == spider.getMaxHealth() - 1, "planned drop stays within safe fall distance");
        }
    }

    static void tick(EntityCreature entity) {
        // World.updateEntityWithOptionalForce normally advances this before invoking onUpdate.
        // These isolated entities are ticked directly rather than inserted into the world's entity list.
        entity.ticksExisted++;
        entity.onUpdate();
        if (entity.riddenByEntity != null) {
            entity.riddenByEntity.ticksExisted++;
            entity.riddenByEntity.updateRidden();
        }
    }

    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static class TestPlayer extends FakePlayer {

        TestPlayer(WorldServer world) {
            super(world, new GameProfile(UUID.fromString("3c01b339-fdea-4b7f-bc99-3926a47b130b"), "ClimbingTest"));
            theItemInWorldManager.setGameType(WorldSettings.GameType.SURVIVAL);
        }

        @Override
        public void addPotionEffect(PotionEffect effect) {
            // This player has no network connection. Cave-spider poison is tested separately on a pig.
        }

        @Override
        public boolean attackEntityFrom(DamageSource source, float amount) {
            setHealth(getHealth() - amount);
            return true;
        }
    }

    public static class LargeSpider extends EntitySpider {

        public LargeSpider(World world) {
            super(world);
            setSize(1.4F, 1.2F);
        }
    }

    public static class ModdedSpider extends EntitySpider {

        public ModdedSpider(World world) {
            super(world);
        }

        @Override
        protected void entityInit() {
            super.entityInit();
            dataWatcher.addObject(20, 42);
        }
    }
}
