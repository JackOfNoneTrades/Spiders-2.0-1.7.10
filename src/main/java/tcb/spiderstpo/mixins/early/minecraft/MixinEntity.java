package tcb.spiderstpo.mixins.early.minecraft;

import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tcb.spiderstpo.common.entity.mob.ClimberRider;
import tcb.spiderstpo.common.entity.mob.SpiderClimber;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Inject(method = "updateRiderPosition", at = @At("HEAD"), cancellable = true)
    private void spiderstpo$positionRider(CallbackInfo ci) {
        if (ClimberRider.updatePosition((Entity) (Object) this)) ci.cancel();
    }

    @Inject(method = "updateRidden", at = @At("HEAD"))
    private void spiderstpo$positionBeforeDamage(CallbackInfo ci) {
        Entity rider = (Entity) (Object) this;
        if (ClimberRider.getMount(rider) != null) ClimberRider.updatePosition(rider.ridingEntity);
    }

    @Inject(method = "isEntityInsideOpaqueBlock", at = @At("HEAD"), cancellable = true)
    private void spiderstpo$orientedSuffocation(CallbackInfoReturnable<Boolean> cir) {
        Entity rider = (Entity) (Object) this;
        SpiderClimber mount = ClimberRider.getMount(rider);
        if (mount != null) cir.setReturnValue(ClimberRider.isInsideOpaqueBlock(rider, mount));
    }
}
