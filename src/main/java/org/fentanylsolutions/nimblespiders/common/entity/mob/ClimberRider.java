package org.fentanylsolutions.nimblespiders.common.entity.mob;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

import org.fentanylsolutions.nimblespiders.common.SurfaceFrame;
import org.fentanylsolutions.nimblespiders.common.Vec3d;

/** The seat, rider collision box and suffocation samples share the mount's surface frame. */
public final class ClimberRider {

    public static SpiderClimber getMount(Entity rider) {
        SpiderClimber mount = SpiderClimber.get(rider.ridingEntity);
        return mount != null && mount.isActive() && mount.entity.riddenByEntity == rider ? mount : null;
    }

    public static SurfaceFrame getViewFrame(Entity player, float partialTicks) {
        SpiderClimber mount = getMount(player);
        if (mount == null || !(player instanceof EntityPlayer)) return null;
        float yaw = player.prevRotationYaw
            + MathHelper.wrapAngleTo180_float(player.rotationYaw - player.prevRotationYaw) * partialTicks;
        float pitch = player.prevRotationPitch + (player.rotationPitch - player.prevRotationPitch) * partialTicks;
        return mount.getRenderFrame(partialTicks)
            .look(MathHelper.wrapAngleTo180_float(yaw - mount.getRiderYaw(partialTicks)), pitch);
    }

    private static double eyeOffset(Entity entity) {
        if (!(entity instanceof EntityPlayer)) return entity.getEyeHeight();
        EntityPlayer player = (EntityPlayer) entity;
        // Forge's default eye height is 0.12 on client players and 1.62 on server players.
        // Match EntityRenderer/getPosition while accounting for the rotated posY offset.
        return 1.62F - player.yOffset + player.getEyeHeight() - player.getDefaultEyeHeight();
    }

    public static Vec3d getEyePosition(Entity entity) {
        return getEyePosition(entity, 1);
    }

    public static Vec3d getEyePosition(Entity entity, float partialTicks) {
        SpiderClimber mount = getMount(entity);
        Vec3d up = mount == null ? new Vec3d(0, 1, 0) : mount.getRenderFrame(partialTicks).up;
        // Client players store their eye-height offset in posY; server players store their feet.
        double eye = eyeOffset(entity);
        return up.scale(eye)
            .addVector(
                entity.prevPosX + (entity.posX - entity.prevPosX) * partialTicks,
                entity.prevPosY + (entity.posY - entity.prevPosY) * partialTicks,
                entity.prevPosZ + (entity.posZ - entity.prevPosZ) * partialTicks);
    }

    public static boolean updatePosition(Entity mount) {
        SpiderClimber climber = SpiderClimber.get(mount);
        if (climber == null || !climber.isActive() || mount.riddenByEntity == null) return false;
        Entity rider = mount.riddenByEntity;
        SurfaceFrame frame = climber.getRenderFrame(1);
        Vec3d seat = seatPosition(mount, frame, climber.getRenderOffset(1));
        rider.setPosition(seat.x, seat.y, seat.z);
        rider.fallDistance = 0;
        // Vanilla's box remains upright even when the rendered rider lies along a wall. Use
        // the axis-aligned enclosure of the rotated body for collision queries and hit detection.
        rider.boundingBox.setBB(bodyBox(rider, frame, seat));
        return true;
    }

    private static Vec3d seatPosition(Entity mount, SurfaceFrame frame, Vec3d renderOffset) {
        return new Vec3d(mount.posX, mount.posY, mount.posZ).add(renderOffset)
            .add(frame.up.scale(mount.getMountedYOffset() + mount.riddenByEntity.getYOffset()));
    }

    private static AxisAlignedBB bodyBox(Entity rider, SurfaceFrame frame, Vec3d seat) {
        Vec3d center = seat.add(frame.up.scale(rider.height / 2.0 - rider.yOffset));
        double half = rider.width / 2.0, halfHeight = rider.height / 2.0;
        double x = (Math.abs(frame.right.x) + Math.abs(frame.forward.x)) * half + Math.abs(frame.up.x) * halfHeight;
        double y = (Math.abs(frame.right.y) + Math.abs(frame.forward.y)) * half + Math.abs(frame.up.y) * halfHeight;
        double z = (Math.abs(frame.right.z) + Math.abs(frame.forward.z)) * half + Math.abs(frame.up.z) * halfHeight;
        return AxisAlignedBB
            .getBoundingBox(center.x - x, center.y - y, center.z - z, center.x + x, center.y + y, center.z + z);
    }

    public static boolean canTurn(SpiderClimber mount, Vec3d normal, Vec3d forward) {
        // Check the swept rider body, including intermediate orientations. Changing the
        // attachment frame must not move a passenger through the roof during a drop.
        for (int i = 1; i <= 4; i++) {
            double t = i / 4.0;
            SurfaceFrame frame = SurfaceFrame.interpolate(mount.prevOrientationNormal, normal, forward, forward, t);
            Vec3d pivot = new Vec3d(
                mount.prevStickingOffsetX + (mount.stickingOffsetX - mount.prevStickingOffsetX) * t,
                mount.prevStickingOffsetY + (mount.stickingOffsetY - mount.prevStickingOffsetY) * t,
                mount.prevStickingOffsetZ + (mount.stickingOffsetZ - mount.prevStickingOffsetZ) * t);
            Vec3d offset = pivot.subtract(frame.up.scale(mount.entity.height / 2.0));
            AxisAlignedBB box = bodyBox(mount.entity.riddenByEntity, frame, seatPosition(mount.entity, frame, offset));
            if (!mount.entity.worldObj.func_147461_a(box.contract(0.001, 0.001, 0.001))
                .isEmpty()) return false;
        }
        return true;
    }

    public static boolean isInsideOpaqueBlock(Entity rider, SpiderClimber mount) {
        SurfaceFrame frame = mount.getRenderFrame(1);
        for (int i = 0; i < 8; i++) {
            double x = ((i & 1) - 0.5) * rider.width * 0.8;
            double y = eyeOffset(rider) + (((i >> 1) & 1) - 0.5) * 0.1;
            double z = (((i >> 2) & 1) - 0.5) * rider.width * 0.8;
            Vec3d sample = frame.toWorld(x, y, z)
                .addVector(rider.posX, rider.posY, rider.posZ);
            if (rider.worldObj
                .getBlock(
                    MathHelper.floor_double(sample.x),
                    MathHelper.floor_double(sample.y),
                    MathHelper.floor_double(sample.z))
                .isNormalCube()) return true;
        }
        return false;
    }
}
