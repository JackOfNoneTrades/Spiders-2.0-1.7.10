package org.fentanylsolutions.nimblespiders.mixins.early.minecraft;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.world.World;

import org.fentanylsolutions.nimblespiders.common.Vec3d;
import org.fentanylsolutions.nimblespiders.common.entity.mob.ClimberRider;
import org.fentanylsolutions.nimblespiders.common.entity.mob.SpiderClimber;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityArrow.class)
public abstract class MixinEntityArrow {

    @Inject(
        method = "<init>(Lnet/minecraft/world/World;Lnet/minecraft/entity/EntityLivingBase;Lnet/minecraft/entity/EntityLivingBase;FF)V",
        at = @At("RETURN"))
    private void nimblespiders$riderArrow(World world, EntityLivingBase shooter, EntityLivingBase target, float speed,
        float inaccuracy, CallbackInfo ci) {
        SpiderClimber mount = ClimberRider.getMount(shooter);
        if (mount == null) return;
        // Skeletons and LOTR archers otherwise create their arrows above the upside-down
        // rider, inside the ceiling. Keep vanilla's aim/arc, measured from the actual head.
        Vec3d origin = ClimberRider.getEyePosition(shooter)
            .subtract(mount.getRenderFrame(1).up.scale(0.1));
        double dx = target.posX - origin.x, dz = target.posZ - origin.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        double dy = target.boundingBox.minY + target.height / 3.0 - origin.y;
        EntityArrow arrow = (EntityArrow) (Object) this;
        arrow.yOffset = 0;
        arrow.setPosition(
            origin.x + (horizontal > 1e-7 ? dx / horizontal : 0),
            origin.y,
            origin.z + (horizontal > 1e-7 ? dz / horizontal : 0));
        arrow.setThrowableHeading(dx, dy + horizontal * 0.2, dz, speed, inaccuracy);
    }
}
