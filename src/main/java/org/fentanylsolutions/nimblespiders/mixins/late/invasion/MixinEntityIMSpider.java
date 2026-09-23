package org.fentanylsolutions.nimblespiders.mixins.late.invasion;

import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.fentanylsolutions.nimblespiders.common.Config;
import org.fentanylsolutions.nimblespiders.common.entity.mob.ClimberAccess;
import org.fentanylsolutions.nimblespiders.common.entity.mob.SpiderClimber;
import org.fentanylsolutions.nimblespiders.common.network.ClimberNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import invmod.common.entity.EntityIMMob;
import invmod.common.entity.EntityIMSpider;
import invmod.common.nexus.INexusAccess;

/**
 * Invasion spiders keep their own AI, nexus navigation, pounces, egg laying, sizes and fall rules.
 * Only their locomotion is replaced with surface travel; IMMoveHelper steers it along Invasion's paths.
 */
@Mixin(EntityIMSpider.class)
public abstract class MixinEntityIMSpider extends EntityIMMob implements ClimberAccess {

    @Unique
    private SpiderClimber nimblespiders$climber;

    @Shadow(remap = false)
    public abstract int getAirborneTime();

    protected MixinEntityIMSpider(World world, INexusAccess nexus) {
        super(world, nexus);
    }

    @Inject(
        method = "<init>(Lnet/minecraft/world/World;Linvmod/common/nexus/INexusAccess;)V",
        at = @At("RETURN"),
        remap = false)
    private void nimblespiders$initialize(World world, INexusAccess nexus, CallbackInfo ci) {
        if (!Config.isEnabled(getClass())) return;
        nimblespiders$climber = new SpiderClimber(this);
        stepHeight = 0.1F;
    }

    @Override
    public SpiderClimber nimblespiders$getClimber() {
        return nimblespiders$climber;
    }

    /** Pounces and fluids use Invasion's own physics, including the Jumping Spider's low gravity. */
    @Unique
    private boolean nimblespiders$surfaceTravel() {
        return nimblespiders$climber != null && getAirborneTime() == 0;
    }

    @Inject(method = "moveEntityWithHeading", at = @At("HEAD"), cancellable = true)
    private void nimblespiders$travel(float strafe, float forward, CallbackInfo ci) {
        if (nimblespiders$surfaceTravel() && nimblespiders$climber.travel(strafe, forward)) ci.cancel();
    }

    @Inject(method = "moveEntityWithHeading", at = @At("RETURN"))
    private void nimblespiders$afterTravel(float strafe, float forward, CallbackInfo ci) {
        if (nimblespiders$climber != null) nimblespiders$climber.afterVanillaTravel();
    }

    @Override
    public boolean isOnLadder() {
        // Invasion treats any wall contact as a ladder; surface travel replaces that climbing.
        return !nimblespiders$surfaceTravel() && super.isOnLadder();
    }

    @Override
    protected float func_110146_f(float yaw, float distance) {
        if (nimblespiders$climber != null && !worldObj.isRemote
            && (moveForward != 0 || Math.abs(posY - prevPosY) > 0.0005)) {
            renderYawOffset = rotationYaw;
            rotationYawHead = renderYawOffset
                + MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(rotationYawHead - renderYawOffset), -75, 75);
            return distance;
        }
        return super.func_110146_f(yaw, distance);
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void nimblespiders$sync(CallbackInfo ci) {
        if (nimblespiders$climber == null) return;
        if (worldObj.isRemote) nimblespiders$climber.updateClientAngles();
        else {
            nimblespiders$climber.debug.tick(nimblespiders$climber);
            if (ticksExisted % 3 == 0) ClimberNetwork.send(nimblespiders$climber);
        }
    }
}
