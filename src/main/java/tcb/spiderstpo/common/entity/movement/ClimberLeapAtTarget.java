package tcb.spiderstpo.common.entity.movement;

import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAILeapAtTarget;

import tcb.spiderstpo.common.entity.mob.SpiderClimber;

/** Jumping uses world-space gravity, so only leap from the ground. */
public final class ClimberLeapAtTarget extends EntityAILeapAtTarget {

    private final EntityLiving entity;

    public ClimberLeapAtTarget(EntityLiving entity) {
        super(entity, 0.4F);
        this.entity = entity;
    }

    @Override
    public boolean shouldExecute() {
        SpiderClimber climber = SpiderClimber.get(entity);
        return climber != null && climber.isActive() && climber.orientationNormal.y > 0.9 && super.shouldExecute();
    }
}
