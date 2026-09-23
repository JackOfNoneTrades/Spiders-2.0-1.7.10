// Adapted from TheCyberBrick's Spiders 2.0 for Minecraft 1.7.10, 2026-09-21.
package org.fentanylsolutions.nimblespiders.common.entity.movement;

import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityMoveHelper;
import net.minecraft.util.MathHelper;

import org.fentanylsolutions.nimblespiders.common.Vec3d;
import org.fentanylsolutions.nimblespiders.common.entity.mob.SpiderClimber;

public class ClimberMoveController extends EntityMoveHelper {

    private final EntityCreature climber;
    private double x, y, z, speed;
    private boolean updating;

    public ClimberMoveController(EntityCreature climber) {
        super(climber);
        this.climber = climber;
    }

    @Override
    public void setMoveTo(double x, double y, double z, double speed) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.speed = speed;
        updating = true;
    }

    @Override
    public boolean isUpdating() {
        return updating;
    }

    @Override
    public double getSpeed() {
        return speed;
    }

    @Override
    public void onUpdateMoveHelper() {
        if (SpiderClimber.get(climber)
            .isPlayerControlled()) {
            updating = false;
            return;
        }
        climber.setMoveForward(0);
        if (!updating) return;
        updating = false;
        SpiderClimber.Orientation orientation = SpiderClimber.get(climber)
            .getOrientation(1);
        Vec3d up = orientation.localY;
        Vec3d offset = new Vec3d(x - climber.posX, y - climber.posY, z - climber.posZ);
        Vec3d direction = offset.subtract(up.scale(offset.dotProduct(up)));
        SpiderClimber.get(climber).debug.move(offset, direction);
        if (direction.lengthVector() < 0.0001) return;
        direction = direction.normalize();
        float yaw = 270 - (float) Math
            .toDegrees(Math.atan2(orientation.localZ.dotProduct(direction), orientation.localX.dotProduct(direction)));
        climber.rotationYaw += MathHelper
            .clamp_float(MathHelper.wrapAngleTo180_float(yaw - climber.rotationYaw), -90, 90);
        climber.setAIMoveSpeed(
            (float) (speed * SpiderClimber.get(climber)
                .getMovementSpeed()));
    }
}
