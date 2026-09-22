package tcb.spiderstpo.mixins.late.lotr;

import net.minecraft.entity.Entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import lotr.common.entity.LOTRMountFunctions;
import tcb.spiderstpo.common.entity.mob.SpiderClimber;

@Mixin(LOTRMountFunctions.class)
public abstract class MixinLOTRMountFunctions {

    @Inject(method = "isPlayerControlledMount", at = @At("HEAD"), cancellable = true, remap = false)
    private static void spiderstpo$serverControlled(Entity mount, CallbackInfoReturnable<Boolean> cir) {
        SpiderClimber climber = SpiderClimber.get(mount);
        // Vanilla riding-input packets control enhanced spiders. Do not overwrite the server's
        // climbing position with LOTR's upright, client-simulated mount position packets.
        if (climber != null && climber.isPlayerControlled()) cir.setReturnValue(false);
    }
}
