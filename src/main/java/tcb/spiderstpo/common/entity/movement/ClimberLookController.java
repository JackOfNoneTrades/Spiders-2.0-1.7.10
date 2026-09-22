// Adapted from TheCyberBrick's Spiders 2.0 for Minecraft 1.7.10, 2026-09-21.
package tcb.spiderstpo.common.entity.movement;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.EntityLookHelper;
import net.minecraft.util.MathHelper;

import org.apache.commons.lang3.tuple.Pair;

import tcb.spiderstpo.common.Vec3d;
import tcb.spiderstpo.common.entity.mob.SpiderClimber;

public class ClimberLookController extends EntityLookHelper {

    private final EntityCreature entity;
    private float deltaLookYaw;
    private float deltaLookPitch;
    private boolean isLooking;
    private double posX;
    private double posY;
    private double posZ;

    public ClimberLookController(EntityCreature entity) {
        super(entity);
        this.entity = entity;
    }

    @Override
    public void setLookPositionWithEntity(Entity entityIn, float deltaYaw, float deltaPitch) {
        this.posX = entityIn.posX;

        if (entityIn instanceof EntityLivingBase) {
            this.posY = entityIn.posY + (double) entityIn.getEyeHeight();
        } else {
            this.posY = (entityIn.boundingBox.minY + entityIn.boundingBox.maxY) / 2.0D;
        }

        this.posZ = entityIn.posZ;
        this.deltaLookYaw = deltaYaw;
        this.deltaLookPitch = deltaPitch;
        this.isLooking = true;
    }

    @Override
    public void setLookPosition(double x, double y, double z, float deltaYaw, float deltaPitch) {
        this.posX = x;
        this.posY = y;
        this.posZ = z;
        this.deltaLookYaw = deltaYaw;
        this.deltaLookPitch = deltaPitch;
        this.isLooking = true;
    }

    @Override
    public void onUpdateLook() {
        if (SpiderClimber.get(entity)
            .isPlayerControlled()) {
            isLooking = false;
            return;
        }
        this.entity.rotationPitch = 0.0F;

        if (this.isLooking) {
            this.isLooking = false;

            Vec3d dir = new Vec3d(
                this.posX - this.entity.posX,
                this.posY - this.entity.posY - this.entity.getEyeHeight(),
                this.posZ - this.entity.posZ);
            Pair<Float, Float> rotation = SpiderClimber.get(this.entity)
                .getOrientation(1)
                .getRotation(dir);

            this.entity.rotationPitch = this
                .updateRotation(this.entity.rotationPitch, rotation.getRight(), this.deltaLookPitch);
            this.entity.rotationYawHead = this
                .updateRotation(this.entity.rotationYawHead, rotation.getLeft(), this.deltaLookYaw);
        } else {
            this.entity.rotationYawHead = this
                .updateRotation(this.entity.rotationYawHead, this.entity.renderYawOffset, 10.0F);
        }

        float yawOffset = MathHelper.wrapAngleTo180_float(this.entity.rotationYawHead - this.entity.renderYawOffset);

        if (!this.entity.getNavigator()
            .noPath()) {
            if (yawOffset < -75.0F) {
                this.entity.rotationYawHead = this.entity.renderYawOffset - 75.0F;
            }

            if (yawOffset > 75.0F) {
                this.entity.rotationYawHead = this.entity.renderYawOffset + 75.0F;
            }
        }
    }

    private float updateRotation(float rotation, float target, float maxIncrement) {
        float increment = MathHelper.wrapAngleTo180_float(target - rotation);

        if (increment > maxIncrement) {
            increment = maxIncrement;
        }

        if (increment < -maxIncrement) {
            increment = -maxIncrement;
        }

        return rotation + increment;
    }

    public boolean getIsLooking() {
        return this.isLooking;
    }

    public double getLookPosX() {
        return this.posX;
    }

    public double getLookPosY() {
        return this.posY;
    }

    public double getLookPosZ() {
        return this.posZ;
    }
}
