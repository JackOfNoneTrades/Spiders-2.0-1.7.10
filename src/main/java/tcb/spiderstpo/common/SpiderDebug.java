package tcb.spiderstpo.common;

import java.util.Locale;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.pathfinding.PathEntity;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;

import tcb.spiderstpo.common.entity.mob.SpiderClimber;

/** Server-side, opt-in diagnostics. No random draws or changes to AI decisions. */
public final class SpiderDebug {

    private final EntityCreature entity;
    private int nextStateTick, nextSearchTick;
    private int lastTarget = Integer.MIN_VALUE;
    private double lastX, lastY, lastZ;
    private boolean reported;
    private Vec3d moveOffset, moveTangent;
    private String attachment = "not_sampled";

    public SpiderDebug(EntityCreature entity) {
        this.entity = entity;
    }

    public boolean enabled() {
        return Config.debugMode && SpiderMod.logger != null
            && !entity.worldObj.isRemote
            && (entity.getAttackTarget() != null || entity.worldObj.getClosestPlayerToEntity(entity, 32) != null);
    }

    public void event(String message) {
        if (enabled()) SpiderMod.logger.info(
            "[SpiderDebug] dim={} spider={} tick={} {}",
            entity.dimension,
            entity.getEntityId(),
            entity.ticksExisted,
            message);
    }

    public boolean beginSearch() {
        if (!enabled() || entity.ticksExisted < nextSearchTick) return false;
        nextSearchTick = entity.ticksExisted + 20;
        return true;
    }

    public void move(Vec3d offset, Vec3d tangent) {
        if (!Config.debugMode) return;
        moveOffset = offset;
        moveTangent = tangent;
    }

    public void attachment(int boxes, boolean found) {
        if (Config.debugMode) attachment = "boxes=" + boxes + ",found=" + found;
    }

    public void tick(SpiderClimber climber) {
        if (!enabled()) return;
        EntityLivingBase target = entity.getAttackTarget();
        int targetId = target == null ? -1 : target.getEntityId();
        if (!reported) event(
            "TRACK class=" + entity.getClass()
                .getName()
                + " uuid="
                + entity.getUniqueID()
                + " size="
                + vector(entity.width, entity.height, entity.stepHeight));
        if (targetId != lastTarget) {
            event("TARGET previous=" + lastTarget + " current=" + describe(target));
            lastTarget = targetId;
        }
        if (reported && entity.ticksExisted < nextStateTick) return;
        nextStateTick = entity.ticksExisted + 20;
        double progress = reported ? Math.sqrt(entity.getDistanceSq(lastX, lastY, lastZ)) : 0;
        lastX = entity.posX;
        lastY = entity.posY;
        lastZ = entity.posZ;
        reported = true;
        PathEntity path = entity.getNavigator()
            .getPath();
        event(
            "STATE pos=" + position(entity)
                + " motion="
                + vector(entity.motionX, entity.motionY, entity.motionZ)
                + " moved="
                + number(progress)
                + " normal="
                + vector(climber.orientationNormal)
                + " surface="
                + climber.getWalkingSide()
                    .getLeft()
                + " onGround="
                + entity.onGround
                + " collision="
                + entity.isCollidedHorizontally
                + "/"
                + entity.isCollidedVertically
                + " forward="
                + number(entity.moveForward)
                + " speed="
                + number(entity.getAIMoveSpeed())
                + " dropping="
                + climber.isDropping()
                + " fluid="
                + entity.isInWater()
                + " riding="
                + (entity.ridingEntity != null)
                + " brightness="
                + number(entity.getBrightness(1))
                + " attachment={"
                + attachment
                + "}"
                + " moveOffset="
                + vector(moveOffset)
                + " tangent="
                + vector(moveTangent)
                + " path="
                + path(path));
        Entity observed = target != null ? target : entity.worldObj.getClosestPlayerToEntity(entity, 32);
        event(
            "TARGET_STATE target=" + describe(target)
                + " nearestWhenIdle="
                + (target == null ? describe(observed) : "-")
                + " visible="
                + (observed != null && entity.canEntityBeSeen(observed))
                + " distance="
                + (observed == null ? "-" : number(Math.sqrt(entity.getDistanceSqToEntity(observed))))
                + " terrain="
                + terrain(path));
        moveOffset = moveTangent = null;
        attachment = "not_sampled";
    }

    private String terrain(PathEntity path) {
        int x = MathHelper.floor_double(entity.posX), y = MathHelper.floor_double(entity.boundingBox.minY),
            z = MathHelper.floor_double(entity.posZ);
        StringBuilder result = new StringBuilder("body[").append(block(x, y, z));
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) result.append(' ')
            .append(side)
            .append('=')
            .append(block(x + side.offsetX, y + side.offsetY, z + side.offsetZ));
        if (path != null && !path.isFinished()) {
            PathPoint next = path.getPathPointFromIndex(path.getCurrentPathIndex());
            result.append("] next[")
                .append(next)
                .append(' ')
                .append(block(next.xCoord, next.yCoord, next.zCoord));
            for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) result.append(' ')
                .append(side)
                .append('=')
                .append(block(next.xCoord + side.offsetX, next.yCoord + side.offsetY, next.zCoord + side.offsetZ));
        }
        return result.append(']')
            .toString();
    }

    private String block(int x, int y, int z) {
        if (!entity.worldObj.blockExists(x, y, z)) return "unloaded";
        return Block.blockRegistry.getNameForObject(entity.worldObj.getBlock(x, y, z)) + ":"
            + entity.worldObj.getBlockMetadata(x, y, z);
    }

    public static String path(PathEntity path) {
        if (path == null) return "null";
        StringBuilder result = new StringBuilder().append(path.getCurrentPathIndex())
            .append('/')
            .append(path.getCurrentPathLength())
            .append('[');
        int end = Math.min(path.getCurrentPathLength(), path.getCurrentPathIndex() + 24);
        for (int i = path.getCurrentPathIndex(); i < end; i++) {
            if (i != path.getCurrentPathIndex()) result.append(';');
            result.append(path.getPathPointFromIndex(i));
        }
        return result.append("] end=")
            .append(path.getFinalPathPoint())
            .toString();
    }

    public static String describe(Entity entity) {
        if (entity == null) return "none";
        return entity.getClass()
            .getSimpleName() + "#"
            + entity.getEntityId()
            + "@"
            + position(entity)
            + " alive="
            + entity.isEntityAlive()
            + (entity instanceof EntityPlayer ? " creative=" + ((EntityPlayer) entity).capabilities.isCreativeMode
                : "");
    }

    public static String position(Entity entity) {
        return vector(entity.posX, entity.boundingBox.minY, entity.posZ);
    }

    public static String vector(Vec3d value) {
        return value == null ? "none" : vector(value.x, value.y, value.z);
    }

    public static String vector(double x, double y, double z) {
        return String.format(Locale.ROOT, "(%.3f,%.3f,%.3f)", x, y, z);
    }

    private static String number(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }
}
