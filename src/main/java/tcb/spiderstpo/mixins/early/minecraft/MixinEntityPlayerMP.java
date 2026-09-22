package tcb.spiderstpo.mixins.early.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tcb.spiderstpo.common.entity.mob.SpiderClimber;

@Mixin(EntityPlayerMP.class)
public abstract class MixinEntityPlayerMP {

    @Inject(method = "setEntityActionState", at = @At("TAIL"))
    private void spiderstpo$mountInput(float strafe, float forward, boolean jump, boolean sneak, CallbackInfo ci) {
        EntityPlayerMP rider = (EntityPlayerMP) (Object) this;
        SpiderClimber mount = SpiderClimber.get(rider.ridingEntity);
        if (mount != null) mount.riderInput(rider, strafe, forward, jump);
    }
}
