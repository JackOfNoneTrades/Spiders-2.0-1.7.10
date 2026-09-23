package org.fentanylsolutions.nimblespiders.mixins.late.invasion;

import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;

import org.fentanylsolutions.nimblespiders.common.Vec3d;
import org.fentanylsolutions.nimblespiders.common.entity.mob.SpiderClimber;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import invmod.common.entity.EntityIMLiving;
import invmod.common.entity.EntityIMSpider;
import invmod.common.entity.IMMoveHelper;
import invmod.common.entity.MoveState;

/** Follows Invasion's path targets in the spider's surface frame instead of ladder-style climbing. */
@Mixin(value = IMMoveHelper.class, remap = false)
public abstract class MixinIMMoveHelper {

    @Shadow
    protected EntityIMLiving a;
    @Shadow
    protected double b;
    @Shadow
    protected double c;
    @Shadow
    protected double d;
    @Shadow
    protected double setSpeed;
    @Shadow
    protected double targetSpeed;
    @Shadow
    protected boolean needsUpdate;

    @Inject(method = "doGroundMovement", at = @At("HEAD"), cancellable = true)
    private void nimblespiders$surfaceMovement(CallbackInfoReturnable<MoveState> cir) {
        SpiderClimber climber = SpiderClimber.get(a);
        if (climber == null || a.isInWater() || a.handleLavaMovement() || ((EntityIMSpider) a).getAirborneTime() > 0)
            return;
        needsUpdate = false;
        targetSpeed = setSpeed;
        SpiderClimber.Orientation orientation = climber.getOrientation(1);
        Vec3d up = orientation.localY;
        Vec3d offset = new Vec3d(b - a.posX, c - a.posY, d - a.posZ);
        Vec3d direction = offset.subtract(up.scale(offset.dotProduct(up)));
        // Invasion climbs straight up the column beside a wall. From the floor that target projects to
        // nothing, so walk into the wall and let surface travel carry the spider onto it.
        if (direction.lengthVector() < 0.1 && offset.dotProduct(up) > 0.5) {
            Vec3d wall = nimblespiders$adjacentWall(offset);
            if (wall != null) direction = wall;
        } else if (offset.x * up.x + offset.z * up.z < -0.1 && up.y < 0.9) {
            // Invasion routes over a wall by stepping into it and relying on ladder physics to rise.
            // Once on the wall that target lies behind the surface, so head up it instead of back down.
            direction = new Vec3d(0, 1, 0).subtract(up.scale(up.y));
        }
        if (direction.lengthVector() < 0.0001) {
            a.setMoveForward(0);
            cir.setReturnValue(MoveState.STANDING);
            return;
        }
        direction = direction.normalize();
        float yaw = 270 - (float) Math
            .toDegrees(Math.atan2(orientation.localZ.dotProduct(direction), orientation.localX.dotProduct(direction)));
        a.rotationYaw += MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(yaw - a.rotationYaw), -90, 90);
        double distanceSq = offset.dotProduct(offset);
        a.setAIMoveSpeed((float) (distanceSq < 0.064 && !a.isSprinting() ? targetSpeed * 0.5 : targetSpeed));
        cir.setReturnValue(climber.orientationNormal.y < 0.7 ? MoveState.CLIMBING : MoveState.RUNNING);
    }

    @Unique
    private Vec3d nimblespiders$adjacentWall(Vec3d offset) {
        Vec3d best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int side = 0; side < 4; side++) {
            int dx = side == 0 ? 1 : side == 1 ? -1 : 0, dz = side == 2 ? 1 : side == 3 ? -1 : 0;
            AxisAlignedBB probe = a.boundingBox.getOffsetBoundingBox(dx * 0.2, 0.01, dz * 0.2);
            if (a.worldObj.func_147461_a(probe)
                .isEmpty()) continue;
            double score = dx * offset.x + dz * offset.z;
            if (score > bestScore) {
                bestScore = score;
                best = new Vec3d(dx, 0, dz);
            }
        }
        return best;
    }
}
