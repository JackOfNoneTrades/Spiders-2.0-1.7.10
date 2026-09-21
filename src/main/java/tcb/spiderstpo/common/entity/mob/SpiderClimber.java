// Adapted from TheCyberBrick's Spiders 2.0 for Minecraft 1.7.10, 2026-09-21.
package tcb.spiderstpo.common.entity.mob;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.commons.lang3.tuple.Pair;

import tcb.spiderstpo.common.CollisionSmoothingUtil;
import tcb.spiderstpo.common.Config;
import tcb.spiderstpo.common.Matrix4f;
import tcb.spiderstpo.common.SpiderDebug;
import tcb.spiderstpo.common.SurfaceAttachment;
import tcb.spiderstpo.common.SurfaceFrame;
import tcb.spiderstpo.common.Vec3d;
import tcb.spiderstpo.common.entity.movement.AdvancedClimberPathNavigator;

public final class SpiderClimber {

    public final EntityCreature entity;
    public final SpiderDebug debug;
    private final float movementSpeedScale;
    private final double defaultMovementSpeed;
    private final boolean retainPlayerRidingControls;
    private Vec3d remoteNormal = new Vec3d(0, 1, 0);
    private double remoteOffsetX, remoteOffsetY, remoteOffsetZ;
    private Vec3d remoteForward = new Vec3d(0, 0, 1);
    private Vec3d renderForward = remoteForward, prevRenderForward = remoteForward;
    private float remoteHeadYaw, remotePitch, renderHeadYaw, renderPitch;
    private boolean receivedRenderState;

    public double prevStickingOffsetX, prevStickingOffsetY, prevStickingOffsetZ;
    public double stickingOffsetX, stickingOffsetY, stickingOffsetZ;

    public Vec3d orientationNormal = new Vec3d(0, 1, 0);
    public Vec3d prevOrientationNormal = new Vec3d(0, 1, 0);

    public float prevOrientationYawDelta;
    public float orientationYawDelta;

    protected double attachedStickingOffsetX, attachedStickingOffsetY, attachedStickingOffsetZ;
    protected Vec3d attachedOrientationNormal = new Vec3d(0, 1, 0);

    protected int attachedTicks = 5;

    protected Vec3d attachedSides = new Vec3d(0, 0, 0);
    protected Vec3d prevAttachedSides = new Vec3d(0, 0, 0);

    protected boolean isTravelingInFluid = false;
    private int droppingTicks;
    private boolean rightingWithRider;

    protected float collisionsInclusionRange = 2.0f;
    protected float collisionsSmoothingRange = 1.25f;

    public SpiderClimber(EntityCreature entity) {
        this(entity, 0.375F, false, 0.8);
    }

    public SpiderClimber(EntityCreature entity, float movementSpeedScale, boolean retainPlayerRidingControls,
        double defaultMovementSpeed) {
        this.entity = entity;
        this.movementSpeedScale = movementSpeedScale;
        this.defaultMovementSpeed = defaultMovementSpeed;
        this.retainPlayerRidingControls = retainPlayerRidingControls;
        debug = new SpiderDebug(entity);
        stickingOffsetY = prevStickingOffsetY = remoteOffsetY = entity.height / 2.0;
    }

    public static SpiderClimber get(Entity entity) {
        return entity instanceof ClimberAccess ? ((ClimberAccess) entity).spiderstpo$getClimber() : null;
    }

    public boolean isActive() {
        return !retainPlayerRidingControls || !(entity.riddenByEntity instanceof EntityPlayer);
    }

    public void receive(Vec3d normal, Vec3d forward, float headYaw, float pitch, double x, double y, double z) {
        remoteNormal = normal;
        remoteForward = forward;
        remoteHeadYaw = headYaw;
        remotePitch = pitch;
        receivedRenderState = true;
        remoteOffsetX = x;
        remoteOffsetY = y;
        remoteOffsetZ = z;
    }

    public SurfaceFrame getRenderFrame(float partialTicks) {
        if (!receivedRenderState) {
            Vec3d forward = getOrientation(1).getDirection(entity.renderYawOffset, 0);
            return SurfaceFrame.interpolate(prevOrientationNormal, orientationNormal, forward, forward, partialTicks);
        }
        return SurfaceFrame
            .interpolate(prevOrientationNormal, orientationNormal, prevRenderForward, renderForward, partialTicks);
    }

    public void updateClientAngles() {
        if (!receivedRenderState || !isActive()) return;
        // Vanilla's separate yaw/head packets describe a different surface frame until the next
        // climbing packet arrives. Render only the angles paired with our normal and body direction.
        entity.prevRenderYawOffset = entity.renderYawOffset = 0;
        entity.prevRotationYawHead = renderHeadYaw;
        renderHeadYaw += MathHelper.wrapAngleTo180_float(remoteHeadYaw - renderHeadYaw);
        entity.rotationYawHead = renderHeadYaw;
        entity.prevRotationPitch = renderPitch;
        entity.rotationPitch = renderPitch = remotePitch;
    }

    public float getMovementSpeed() {
        IAttributeInstance attribute = entity.getEntityAttribute(SharedMonsterAttributes.movementSpeed);
        // Apply the configured baseline relative to the native one: size variants, subclass
        // attributes and potion modifiers keep their existing relationships at default settings.
        return (float) ((attribute != null ? attribute.getAttributeValue() * movementSpeedScale : 1.0)
            * Config.getSpeedMultiplier(entity, defaultMovementSpeed));
    }

    private static double calculateXOffset(AxisAlignedBB aabb, AxisAlignedBB other, double offsetX) {
        if (other.maxY > aabb.minY && other.minY < aabb.maxY && other.maxZ > aabb.minZ && other.minZ < aabb.maxZ) {
            if (offsetX > 0.0D && other.maxX <= aabb.minX) {
                double dx = aabb.minX - other.maxX;

                if (dx < offsetX) {
                    offsetX = dx;
                }
            } else if (offsetX < 0.0D && other.minX >= aabb.maxX) {
                double dx = aabb.maxX - other.minX;

                if (dx > offsetX) {
                    offsetX = dx;
                }
            }

            return offsetX;
        } else {
            return offsetX;
        }
    }

    private static double calculateYOffset(AxisAlignedBB aabb, AxisAlignedBB other, double offsetY) {
        if (other.maxX > aabb.minX && other.minX < aabb.maxX && other.maxZ > aabb.minZ && other.minZ < aabb.maxZ) {
            if (offsetY > 0.0D && other.maxY <= aabb.minY) {
                double dy = aabb.minY - other.maxY;

                if (dy < offsetY) {
                    offsetY = dy;
                }
            } else if (offsetY < 0.0D && other.minY >= aabb.maxY) {
                double dy = aabb.maxY - other.minY;

                if (dy > offsetY) {
                    offsetY = dy;
                }
            }

            return offsetY;
        } else {
            return offsetY;
        }
    }

    private static double calculateZOffset(AxisAlignedBB aabb, AxisAlignedBB other, double offsetZ) {
        if (other.maxX > aabb.minX && other.minX < aabb.maxX && other.maxY > aabb.minY && other.minY < aabb.maxY) {
            if (offsetZ > 0.0D && other.maxZ <= aabb.minZ) {
                double dz = aabb.minZ - other.maxZ;

                if (dz < offsetZ) {
                    offsetZ = dz;
                }
            } else if (offsetZ < 0.0D && other.minZ >= aabb.maxZ) {
                double dz = aabb.maxZ - other.minZ;

                if (dz > offsetZ) {
                    offsetZ = dz;
                }
            }

            return offsetZ;
        } else {
            return offsetZ;
        }
    }

    public Pair<ForgeDirection, Vec3d> getWalkingSide() {
        ForgeDirection avoidPathingFacing = null;

        AxisAlignedBB entityBox = entity.boundingBox;

        double closestFacingDst = Double.MAX_VALUE;
        ForgeDirection closestFacing = null;

        Vec3d weighting = new Vec3d(0, 0, 0);

        float stickingDistance = entity.moveForward != 0 ? 1.5f : 0.1f;

        for (ForgeDirection facing : ForgeDirection.VALID_DIRECTIONS) {
            if (avoidPathingFacing == facing) {
                continue;
            }

            List<AxisAlignedBB> collisionBoxes = entity.worldObj.getCollidingBoundingBoxes(
                entity,
                entityBox.expand(0.2, 0.2, 0.2)
                    .addCoord(
                        facing.offsetX * stickingDistance,
                        facing.offsetY * stickingDistance,
                        facing.offsetZ * stickingDistance));

            double closestDst = Double.MAX_VALUE;

            for (AxisAlignedBB collisionBox : collisionBoxes) {
                switch (facing) {
                    case EAST:
                    case WEST:
                        closestDst = Math.min(
                            closestDst,
                            Math.abs(calculateXOffset(entityBox, collisionBox, -facing.offsetX * stickingDistance)));
                        break;
                    case UP:
                    case DOWN:
                        closestDst = Math.min(
                            closestDst,
                            Math.abs(calculateYOffset(entityBox, collisionBox, -facing.offsetY * stickingDistance)));
                        break;
                    case NORTH:
                    case SOUTH:
                        closestDst = Math.min(
                            closestDst,
                            Math.abs(calculateZOffset(entityBox, collisionBox, -facing.offsetZ * stickingDistance)));
                        break;
                }
            }

            if (closestDst < closestFacingDst) {
                closestFacingDst = closestDst;
                closestFacing = facing;
            }

            if (closestDst < Double.MAX_VALUE) {
                weighting = weighting.add(
                    new Vec3d(facing.offsetX, facing.offsetY, facing.offsetZ)
                        .scale(1 - Math.min(closestDst, stickingDistance) / stickingDistance));
            }
        }

        if (closestFacing == null) {
            return Pair.of(ForgeDirection.DOWN, new Vec3d(0, -1, 0));
        }

        return Pair.of(
            closestFacing,
            weighting.normalize()
                .addVector(0, -0.001f, 0)
                .normalize());
    }

    public static class Orientation {

        public final Vec3d normal, localZ, localY, localX;
        public final float componentZ, componentY, componentX, yaw, pitch;

        private Orientation(Vec3d normal, Vec3d localZ, Vec3d localY, Vec3d localX, float componentZ, float componentY,
            float componentX, float yaw, float pitch) {
            this.normal = normal;
            this.localZ = localZ;
            this.localY = localY;
            this.localX = localX;
            this.componentZ = componentZ;
            this.componentY = componentY;
            this.componentX = componentX;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public Vec3d getDirection(Vec3d local) {
            return this.localX.scale(local.x)
                .add(this.localY.scale(local.y))
                .add(this.localZ.scale(local.z));
        }

        public Vec3d getDirection(float yaw, float pitch) {
            float cy = MathHelper.cos(yaw * 0.017453292F);
            float sy = MathHelper.sin(yaw * 0.017453292F);
            float cp = -MathHelper.cos(-pitch * 0.017453292F);
            float sp = MathHelper.sin(-pitch * 0.017453292F);
            return this.localX.scale(sy * cp)
                .add(this.localY.scale(sp))
                .add(this.localZ.scale(cy * cp));
        }

        public Vec3d getLocal(Vec3d global) {
            return new Vec3d(
                this.localX.dotProduct(global),
                this.localY.dotProduct(global),
                this.localZ.dotProduct(global));
        }

        public Pair<Float, Float> getRotation(Vec3d global) {
            Vec3d local = this.getLocal(global);

            float yaw = (float) Math.toDegrees(Math.atan2(local.x, local.z)) + 180.0f;
            float pitch = (float) -Math
                .toDegrees(Math.atan2(local.y, MathHelper.sqrt_double(local.x * local.x + local.z * local.z)));

            return Pair.of(yaw, pitch);
        }
    }

    public Orientation getOrientation(float partialTicks) {
        Vec3d orientationNormal = this.prevOrientationNormal.add(
            this.orientationNormal.subtract(this.prevOrientationNormal)
                .scale(partialTicks));

        Vec3d localZ = new Vec3d(0, 0, 1);
        Vec3d localY = new Vec3d(0, 1, 0);
        Vec3d localX = new Vec3d(1, 0, 0);

        float componentZ = (float) localZ.dotProduct(orientationNormal);
        float componentY;
        float componentX = (float) localX.dotProduct(orientationNormal);

        float yaw = (float) Math.toDegrees(Math.atan2(componentX, componentZ));

        localZ = new Vec3d(Math.sin(Math.toRadians(yaw)), 0, Math.cos(Math.toRadians(yaw)));
        localY = new Vec3d(0, 1, 0);
        localX = new Vec3d(Math.sin(Math.toRadians(yaw - 90)), 0, Math.cos(Math.toRadians(yaw - 90)));

        componentZ = (float) localZ.dotProduct(orientationNormal);
        componentY = (float) localY.dotProduct(orientationNormal);
        componentX = (float) localX.dotProduct(orientationNormal);

        float pitch = (float) Math.toDegrees(
            Math.atan2(MathHelper.sqrt_double(componentX * componentX + componentZ * componentZ), componentY));

        Matrix4f m = new Matrix4f();

        m.multiply(new Matrix4f((float) Math.toRadians(yaw), 0, 1, 0));
        m.multiply(new Matrix4f((float) Math.toRadians(pitch), 1, 0, 0));
        m.multiply(
            new Matrix4f(
                (float) Math.toRadians((float) Math.signum(0.5f - componentY - componentZ - componentX) * yaw),
                0,
                1,
                0));

        localZ = m.multiply(new Vec3d(0, 0, -1));
        localY = m.multiply(new Vec3d(0, 1, 0));
        localX = m.multiply(new Vec3d(1, 0, 0));

        return new Orientation(
            orientationNormal,
            localZ,
            localY,
            localX,
            componentZ,
            componentY,
            componentX,
            yaw,
            pitch);
    }

    public float getVerticalOffset(float partialTicks) {
        return entity.height * 0.5F;
    }

    public Vec3d getRenderOffset(float partialTicks) {
        return new Vec3d(
            prevStickingOffsetX + (stickingOffsetX - prevStickingOffsetX) * partialTicks,
            prevStickingOffsetY + (stickingOffsetY - prevStickingOffsetY) * partialTicks,
            prevStickingOffsetZ + (stickingOffsetZ - prevStickingOffsetZ) * partialTicks)
                .subtract(getRenderFrame(partialTicks).up.scale(getVerticalOffset(partialTicks)));
    }

    public void resolveSpawnCollision() {
        if (entity.worldObj.isRemote || !isActive() || entity.noClip) return;
        AxisAlignedBB body = entity.boundingBox.contract(0.0001, 0.0001, 0.0001);
        List<AxisAlignedBB> collisions = new ArrayList<>(entity.worldObj.func_147461_a(body));
        if (collisions.isEmpty()) return;
        Vec3d escape = null;
        double shortest = Double.POSITIVE_INFINITY;
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            double distance = 0;
            for (AxisAlignedBB block : collisions) {
                double overlap = side.offsetX > 0 ? block.maxX - body.minX
                    : side.offsetX < 0 ? body.maxX - block.minX
                        : side.offsetY > 0 ? block.maxY - body.minY
                            : side.offsetY < 0 ? body.maxY - block.minY
                                : side.offsetZ > 0 ? block.maxZ - body.minZ : body.maxZ - block.minZ;
                distance = Math.max(distance, overlap + 0.0002);
            }
            if (distance >= shortest || distance > Math.max(entity.width, entity.height) + 0.01) continue;
            Vec3d offset = new Vec3d(side.offsetX, side.offsetY, side.offsetZ).scale(distance);
            if (!entity.worldObj.func_147461_a(body.getOffsetBoundingBox(offset.x, offset.y, offset.z))
                .isEmpty()) continue;
            escape = offset;
            shortest = distance;
        }
        if (escape != null) {
            entity.setPosition(entity.posX + escape.x, entity.posY + escape.y, entity.posZ + escape.z);
            debug.event("SPAWN_CLEARANCE offset=" + SpiderDebug.vector(escape));
        }
    }

    private Vec3d climbEntryNormal(Vec3d normal) {
        if (normal.y < 0.7 || !(entity.getNavigator() instanceof AdvancedClimberPathNavigator)) return normal;
        ForgeDirection side = ((AdvancedClimberPathNavigator) entity.getNavigator()).getClimbEntrySide();
        if (side == ForgeDirection.UNKNOWN) return normal;
        AxisAlignedBB body = entity.boundingBox;
        AxisAlignedBB probe = body.contract(0.001, 0.001, 0.001)
            .addCoord(side.offsetX * 0.15, 0, side.offsetZ * 0.15);
        if (entity.worldObj.func_147461_a(probe)
            .isEmpty()) return normal;
        Vec3d wallNormal = new Vec3d(-side.offsetX, 0, -side.offsetZ);
        // A narrow pillar contributes little to the smoothed field beside a broad floor,
        // especially for wide spiders. Once physically touching the route's wall, lean into
        // the floor/wall transition so an upward waypoint does not project to sideways noise.
        double lean = Math.max(0, normal.y - normal.dotProduct(wallNormal));
        return normal.add(wallNormal.scale(lean))
            .normalize();
    }

    protected void updateOffsetsAndOrientation() {
        Vec3d bodyForward = getRenderFrame(1).forward;
        Vec3d direction = this.getOrientation(1)
            .getDirection(entity.rotationYaw, entity.rotationPitch);

        boolean isAttached = false;

        double baseStickingOffsetX = 0.0f;
        double baseStickingOffsetY = this.getVerticalOffset(1);
        double baseStickingOffsetZ = 0.0f;
        Vec3d baseOrientationNormal = new Vec3d(0, 1, 0);

        if (isActive() && !this.isTravelingInFluid && !isDropping() && entity.onGround && entity.ridingEntity == null) {
            Vec3d p = new Vec3d(entity.posX, entity.posY, entity.posZ);

            Vec3d s = p.addVector(0, entity.height / 2, 0);
            // LOTR spiders can be nearly three blocks wide. Their body center is farther from
            // the wall, so the smoothing neighborhood must grow with them to round an inside corner.
            float collisionScale = Math.max(1, entity.width / 1.4F);
            double inclusionRange = this.collisionsInclusionRange * collisionScale;
            AxisAlignedBB inclusionBox = AxisAlignedBB.getBoundingBox(s.x, s.y, s.z, s.x, s.y, s.z)
                .expand(inclusionRange, inclusionRange, inclusionRange);

            // World reuses its collision list; the navigation/face probes below must not replace it.
            List<AxisAlignedBB> boxes = new ArrayList<>(
                entity.worldObj.getCollidingBoundingBoxes(entity, inclusionBox));

            Pair<Vec3d, Vec3d> attachmentPoint = CollisionSmoothingUtil
                .findClosestPoint(boxes, this.collisionsSmoothingRange * collisionScale, 1.0f, 0.005f, 20, 0.05f, s);

            debug.attachment(boxes.size(), attachmentPoint != null);
            if (attachmentPoint != null) {
                Vec3d normal = climbEntryNormal(attachmentPoint.getRight());
                Vec3d contact = SurfaceAttachment.findContact(boxes, s, normal, inclusionRange * 2);
                if (contact != null) {
                    isAttached = true;
                    Vec3d pivot = contact.subtract(p)
                        .add(normal.scale(getVerticalOffset(1)));
                    this.attachedStickingOffsetX = pivot.x;
                    this.attachedStickingOffsetY = pivot.y;
                    this.attachedStickingOffsetZ = pivot.z;
                    this.attachedOrientationNormal = normal;
                }
            }
        }

        this.prevStickingOffsetX = this.stickingOffsetX;
        this.prevStickingOffsetY = this.stickingOffsetY;
        this.prevStickingOffsetZ = this.stickingOffsetZ;
        this.prevOrientationNormal = this.orientationNormal;

        float attachmentBlend = this.attachedTicks * 0.2f;

        this.stickingOffsetX = baseStickingOffsetX
            + (this.attachedStickingOffsetX - baseStickingOffsetX) * attachmentBlend;
        this.stickingOffsetY = baseStickingOffsetY
            + (this.attachedStickingOffsetY - baseStickingOffsetY) * attachmentBlend;
        this.stickingOffsetZ = baseStickingOffsetZ
            + (this.attachedStickingOffsetZ - baseStickingOffsetZ) * attachmentBlend;
        this.orientationNormal = baseOrientationNormal
            .add(
                this.attachedOrientationNormal.subtract(baseOrientationNormal)
                    .scale(attachmentBlend))
            .normalize();

        if (entity.riddenByEntity != null && isActive()) {
            if (!isAttached && (isDropping() || attachedTicks <= 2) && prevOrientationNormal.y < 0.99)
                rightingWithRider = true;
            if (isAttached && orientationNormal.y < 0.7) rightingWithRider = false;
            if (rightingWithRider) {
                Vec3d target = isAttached ? orientationNormal : baseOrientationNormal;
                Vec3d forward = bodyForward;
                Vec3d next = SurfaceFrame.turnNormal(prevOrientationNormal, target, forward, Math.PI / 9);
                if (ClimberRider.canTurn(this, next, forward)) orientationNormal = next;
                else {
                    orientationNormal = prevOrientationNormal;
                    stickingOffsetX = prevStickingOffsetX;
                    stickingOffsetY = prevStickingOffsetY;
                    stickingOffsetZ = prevStickingOffsetZ;
                }
                if (orientationNormal.dotProduct(target) > 0.9999) rightingWithRider = false;
            }
        } else rightingWithRider = false;

        if (!isAttached) {
            this.attachedTicks = Math.max(0, this.attachedTicks - 1);
        } else {
            this.attachedTicks = Math.min(5, this.attachedTicks + 1);
        }

        Pair<Float, Float> newRotations = this.getOrientation(1)
            .getRotation(direction);

        float yawDelta = newRotations.getLeft() - entity.rotationYaw;
        float pitchDelta = newRotations.getRight() - entity.rotationPitch;

        this.prevOrientationYawDelta = this.orientationYawDelta;
        this.orientationYawDelta = yawDelta;

        entity.rotationYaw = MathHelper.wrapAngleTo180_float(entity.rotationYaw + yawDelta);
        entity.prevRotationYaw = this.wrapAngleInRange(entity.prevRotationYaw/* + yawDelta */, entity.rotationYaw);

        entity.renderYawOffset = MathHelper.wrapAngleTo180_float(entity.renderYawOffset + yawDelta);
        entity.prevRenderYawOffset = this
            .wrapAngleInRange(entity.prevRenderYawOffset/* + yawDelta */, entity.renderYawOffset);

        entity.rotationYawHead = MathHelper.wrapAngleTo180_float(entity.rotationYawHead + yawDelta);
        entity.prevRotationYawHead = this
            .wrapAngleInRange(entity.prevRotationYawHead/* + yawDelta */, entity.rotationYawHead);
        // this.interpTargetHeadYaw = MathHelper.wrapAngleTo180_float(this.interpTargetHeadYaw + yawDelta);

        entity.rotationPitch = MathHelper.wrapAngleTo180_float(entity.rotationPitch + pitchDelta);
        entity.prevRotationPitch = this
            .wrapAngleInRange(entity.prevRotationPitch/* + pitchDelta */, entity.rotationPitch);
    }

    private float wrapAngleInRange(float angle, float target) {
        while (target - angle < -180.0F) {
            angle -= 360.0F;
        }

        while (target - angle >= 180.0F) {
            angle += 360.0F;
        }

        return angle;
    }

    public Vec3d getStickingForce(Pair<ForgeDirection, Vec3d> walkingSide) {
        return walkingSide.getRight()
            .scale(0.08f);
    }

    public boolean isDropping() {
        return droppingTicks > 0;
    }

    public void dropFromSurface() {
        droppingTicks = 40;
        entity.getNavigator()
            .clearPathEntity();
        entity.setMoveForward(0);
        entity.onGround = false;
        entity.motionX = entity.motionZ = 0;
        entity.motionY = -0.08;
    }

    public boolean travel(float strafe, float forward) {
        if (!isActive()) return false;
        isTravelingInFluid = entity.isInWater() || entity.handleLavaMovement();
        if (entity.worldObj.isRemote) {
            updateLimbSwing();
            prevOrientationNormal = orientationNormal;
            prevStickingOffsetX = stickingOffsetX;
            prevStickingOffsetY = stickingOffsetY;
            prevStickingOffsetZ = stickingOffsetZ;
            prevRenderForward = renderForward;
            renderForward = remoteForward;
            orientationNormal = remoteNormal;
            stickingOffsetX = remoteOffsetX;
            stickingOffsetY = remoteOffsetY;
            stickingOffsetZ = remoteOffsetZ;
            return true;
        }
        if (isDropping()) {
            droppingTicks--;
            if (entity.onGround || isTravelingInFluid || entity.ridingEntity != null) droppingTicks = 0;
            return false;
        }
        if (isTravelingInFluid || entity.ridingEntity != null) return false;
        travelOnGround(strafe, 0, forward);
        updateOffsetsAndOrientation();
        return true;
    }

    public void afterVanillaTravel() {
        if (!entity.worldObj.isRemote) updateOffsetsAndOrientation();
    }

    protected void travelOnGround(float strafe, float vertical, float forward) {
        Orientation orientation = this.getOrientation(1);

        Vec3d forwardVector = orientation.getDirection(entity.rotationYaw, 0);
        Vec3d upVector = orientation.getDirection(entity.rotationYaw, -90);

        Pair<ForgeDirection, Vec3d> walkingSide = this.getWalkingSide();

        Vec3d stickingForce = this.getStickingForce(walkingSide);

        if (forward != 0) {
            float slipperiness = 0.91f;

            if (entity.onGround) {
                slipperiness = this.getSurfaceSlipperiness(walkingSide.getLeft());
            }

            float friction = forward * 0.16277136F / (slipperiness * slipperiness * slipperiness);

            float f = forward * forward;
            if (f >= 1.0E-4F) {
                f = Math.max(MathHelper.sqrt_double(f), 1.0f);
                f = friction / f;

                Vec3d forwardOffset = new Vec3d(
                    forwardVector.x * forward * f,
                    forwardVector.y * forward * f,
                    forwardVector.z * forward * f);

                double px = entity.posX;
                double py = entity.posY;
                double pz = entity.posZ;
                Vec3d motion = new Vec3d(entity.motionX, entity.motionY, entity.motionZ);
                AxisAlignedBB aabb = entity.boundingBox.copy();

                // Probe actual movement vector
                this.moveEntity(forwardOffset.x, forwardOffset.y, forwardOffset.z);

                Vec3d movementDir = new Vec3d(entity.posX - px, entity.posY - py, entity.posZ - pz).normalize();

                entity.boundingBox.setBB(aabb);
                this.resetPositionToBox();
                entity.motionX = motion.x;
                entity.motionY = motion.y;
                entity.motionZ = motion.z;

                // Probe collision normal
                Vec3d probeVector = new Vec3d(
                    Math.abs(movementDir.x) < 0.001D ? -Math.signum(upVector.x) : 0,
                    Math.abs(movementDir.y) < 0.001D ? -Math.signum(upVector.y) : 0,
                    Math.abs(movementDir.z) < 0.001D ? -Math.signum(upVector.z) : 0).normalize()
                        .scale(0.0001D);
                this.moveEntity(probeVector.x, probeVector.y, probeVector.z);

                Vec3d collisionNormal = new Vec3d(
                    Math.abs(entity.posX - px - probeVector.x) > 0.000001D ? Math.signum(-probeVector.x) : 0,
                    Math.abs(entity.posY - py - probeVector.y) > 0.000001D ? Math.signum(-probeVector.y) : 0,
                    Math.abs(entity.posZ - pz - probeVector.z) > 0.000001D ? Math.signum(-probeVector.z) : 0)
                        .normalize();

                entity.boundingBox.setBB(aabb);
                this.resetPositionToBox();
                entity.motionX = motion.x;
                entity.motionY = motion.y;
                entity.motionZ = motion.z;

                // Movement vector projected to surface
                Vec3d surfaceMovementDir = movementDir
                    .subtract(collisionNormal.scale(collisionNormal.dotProduct(movementDir)))
                    .normalize();

                boolean isInnerCorner = Math.abs(collisionNormal.x) + Math.abs(collisionNormal.y)
                    + Math.abs(collisionNormal.z) > 1.0001f;

                // Only project movement vector to surface if not moving across inner corner, otherwise it'd get stuck
                // in the corner
                if (!isInnerCorner) {
                    movementDir = surfaceMovementDir;
                }

                // Nullify sticking force along movement vector projected to surface
                stickingForce = stickingForce.subtract(
                    surfaceMovementDir.scale(
                        surfaceMovementDir.normalize()
                            .dotProduct(stickingForce)));

                float moveSpeed = forward * f;
                entity.motionX += movementDir.x * moveSpeed;
                entity.motionY += movementDir.y * moveSpeed;
                entity.motionZ += movementDir.z * moveSpeed;
            }
        }

        double px = entity.posX;
        double py = entity.posY;
        double pz = entity.posZ;
        Vec3d motion = new Vec3d(entity.motionX, entity.motionY, entity.motionZ);

        this.moveEntity(motion.x, motion.y, motion.z);

        entity.motionX += stickingForce.x;
        entity.motionY += stickingForce.y;
        entity.motionZ += stickingForce.z;

        this.prevAttachedSides = this.attachedSides;
        this.attachedSides = new Vec3d(
            Math.abs(entity.posX - px - motion.x) > 0.001D ? -Math.signum(motion.x) : 0,
            Math.abs(entity.posY - py - motion.y) > 0.001D ? -Math.signum(motion.y) : 0,
            Math.abs(entity.posZ - pz - motion.z) > 0.001D ? -Math.signum(motion.z) : 0);

        float slipperiness = 0.91f;

        if (entity.onGround) {
            entity.fallDistance = 0;

            slipperiness = this.getSurfaceSlipperiness(walkingSide.getLeft());
        }

        motion = new Vec3d(entity.motionX, entity.motionY, entity.motionZ);
        Vec3d orthogonalMotion = upVector.scale(upVector.dotProduct(motion));
        Vec3d tangentialMotion = motion.subtract(orthogonalMotion);

        entity.motionX = tangentialMotion.x * slipperiness + orthogonalMotion.x * 0.98f;
        entity.motionY = tangentialMotion.y * slipperiness + orthogonalMotion.y * 0.98f;
        entity.motionZ = tangentialMotion.z * slipperiness + orthogonalMotion.z * 0.98f;

        boolean detachedX = this.attachedSides.x != this.prevAttachedSides.x && Math.abs(this.attachedSides.x) < 0.001D;
        boolean detachedY = this.attachedSides.y != this.prevAttachedSides.y && Math.abs(this.attachedSides.y) < 0.001D;
        boolean detachedZ = this.attachedSides.z != this.prevAttachedSides.z && Math.abs(this.attachedSides.z) < 0.001D;

        if (detachedX || detachedY || detachedZ) {
            float stepHeight = entity.stepHeight;
            entity.stepHeight = 0;

            boolean prevOnGround = entity.onGround;
            boolean prevCollidedHorizontally = entity.isCollidedHorizontally;
            boolean prevCollidedVertically = entity.isCollidedVertically;

            // Offset so that AABB is moved above the new surface
            this.moveEntity(
                detachedX ? -this.prevAttachedSides.x * 0.25f : 0,
                detachedY ? -this.prevAttachedSides.y * 0.25f : 0,
                detachedZ ? -this.prevAttachedSides.z * 0.25f : 0);

            Vec3d axis = this.prevAttachedSides.normalize();
            Vec3d attachVector = upVector.scale(-1);
            attachVector = attachVector.subtract(axis.scale(axis.dotProduct(attachVector)));

            if (Math.abs(attachVector.x) > Math.abs(attachVector.y)
                && Math.abs(attachVector.x) > Math.abs(attachVector.z)) {
                attachVector = new Vec3d(Math.signum(attachVector.x), 0, 0);
            } else if (Math.abs(attachVector.y) > Math.abs(attachVector.z)) {
                attachVector = new Vec3d(0, Math.signum(attachVector.y), 0);
            } else {
                attachVector = new Vec3d(0, 0, Math.signum(attachVector.z));
            }

            double attachDst = motion.lengthVector() + 0.1f;

            AxisAlignedBB aabb = entity.boundingBox.copy();
            motion = new Vec3d(entity.motionX, entity.motionY, entity.motionZ);

            // Offset AABB towards new surface until it touches
            for (int i = 0; i < 2 && !entity.onGround; i++) {
                this.moveEntity(attachVector.x * attachDst, attachVector.y * attachDst, attachVector.z * attachDst);
            }

            entity.stepHeight = stepHeight;

            // Attaching failed, fall back to previous position
            if (!entity.onGround) {
                entity.boundingBox.setBB(aabb);
                this.resetPositionToBox();
                entity.motionX = motion.x;
                entity.motionY = motion.y;
                entity.motionZ = motion.z;
                entity.onGround = prevOnGround;
                entity.isCollidedHorizontally = prevCollidedHorizontally;
                entity.isCollidedVertically = prevCollidedVertically;
                entity.isCollided = entity.isCollidedHorizontally || entity.isCollidedVertically;
            } else {
                entity.motionX = entity.motionY = entity.motionZ = 0;
            }
        }

        this.updateLimbSwing();
    }

    public void updateLimbSwing() {
        entity.prevLimbSwingAmount = entity.limbSwingAmount;
        double dx = entity.posX - entity.prevPosX;
        double dy = entity.posY - entity.prevPosY;
        double dz = entity.posZ - entity.prevPosZ;
        float f = MathHelper.sqrt_double(dx * dx + dy * dy + dz * dz) * 4.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        entity.limbSwingAmount += (f - entity.limbSwingAmount) * 0.4F;
        entity.limbSwing += entity.limbSwingAmount;
    }

    private void moveEntity(double x, double y, double z) {
        double py = entity.posY;

        entity.moveEntity(x, y, z);

        if (Math.abs(entity.posY - py - y) > 0.000001D) {
            entity.motionY = 0;
        }

        entity.onGround |= entity.isCollidedHorizontally || entity.isCollidedVertically;
    }

    private float getSurfaceSlipperiness(ForgeDirection side) {
        return entity.worldObj.getBlock(
            MathHelper.floor_double(entity.posX + side.offsetX),
            MathHelper.floor_double(entity.posY + side.offsetY),
            MathHelper.floor_double(entity.posZ + side.offsetZ)).slipperiness * 0.91F;
    }

    private void resetPositionToBox() {
        entity.posX = (entity.boundingBox.minX + entity.boundingBox.maxX) / 2;
        entity.posY = entity.boundingBox.minY + entity.yOffset - entity.ySize;
        entity.posZ = (entity.boundingBox.minZ + entity.boundingBox.maxZ) / 2;
    }
}
