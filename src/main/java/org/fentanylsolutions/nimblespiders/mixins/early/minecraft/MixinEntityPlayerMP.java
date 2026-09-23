package org.fentanylsolutions.nimblespiders.mixins.early.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;

import org.fentanylsolutions.nimblespiders.common.entity.mob.SpiderClimber;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMP {

    @Inject(method = "setEntityActionState", at = @At("TAIL"))
    private void nimblespiders$mountInput(float strafe, float forward, boolean jump, boolean sneak, CallbackInfo ci) {
        EntityPlayerMP rider = (EntityPlayerMP) (Object) this;
        SpiderClimber mount = SpiderClimber.get(rider.ridingEntity);
        if (mount != null) mount.riderInput(rider, strafe, forward, jump);
    }
}
