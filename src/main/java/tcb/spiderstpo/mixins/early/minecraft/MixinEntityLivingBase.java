package tcb.spiderstpo.mixins.early.minecraft;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tcb.spiderstpo.common.Config;
import tcb.spiderstpo.common.Vec3d;
import tcb.spiderstpo.common.entity.mob.ClimberRider;
import tcb.spiderstpo.common.entity.mob.SpiderClimber;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

    @Inject(method = "canEntityBeSeen", at = @At("HEAD"), cancellable = true)
    private void spiderstpo$riderSight(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Entity observer = (Entity) (Object) this;
        if (ClimberRider.getMount(observer) == null && ClimberRider.getMount(target) == null) return;
        Vec3d from = ClimberRider.getEyePosition(observer), to = ClimberRider.getEyePosition(target);
        cir.setReturnValue(
            observer.worldObj.rayTraceBlocks(
                Vec3.createVectorHelper(from.x, from.y, from.z),
                Vec3.createVectorHelper(to.x, to.y, to.z)) == null);
    }

    @ModifyConstant(method = "fall", constant = @Constant(floatValue = 3.0F))
    private float spiderstpo$safeFallDistance(float vanillaDistance) {
        if (SpiderClimber.get((EntityLivingBase) (Object) this) != null) {
            return Config.getSafeFallDistance();
        }
        return vanillaDistance;
    }
}
