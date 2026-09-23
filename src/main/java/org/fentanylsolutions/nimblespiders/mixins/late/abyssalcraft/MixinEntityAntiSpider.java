package org.fentanylsolutions.nimblespiders.mixins.late.abyssalcraft;

import net.minecraft.entity.monster.EntityMob;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.fentanylsolutions.nimblespiders.common.Config;
import org.fentanylsolutions.nimblespiders.common.entity.mob.ClimberAccess;
import org.fentanylsolutions.nimblespiders.common.entity.mob.SpiderClimber;
import org.fentanylsolutions.nimblespiders.common.entity.movement.AdvancedClimberPathNavigator;
import org.fentanylsolutions.nimblespiders.common.entity.movement.ClimberLookController;
import org.fentanylsolutions.nimblespiders.common.entity.movement.ClimberMoveController;
import org.fentanylsolutions.nimblespiders.common.entity.movement.SpiderAI;
import org.fentanylsolutions.nimblespiders.common.network.ClimberNetwork;
import org.fentanylsolutions.nimblespiders.mixins.early.minecraft.AccessorEntityLiving;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.shinoow.abyssalcraft.common.entity.anti.EntityAntiSpider;

/** AbyssalCraft's Anti-Spider copies vanilla spider behavior without extending EntitySpider. */
@Mixin(EntityAntiSpider.class)
public abstract class MixinEntityAntiSpider extends EntityMob implements ClimberAccess {

    @Unique
    private SpiderClimber nimblespiders$climber;

    protected MixinEntityAntiSpider(World world) {
        super(world);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nimblespiders$initialize(World world, CallbackInfo ci) {
        if (!Config.isEnabled(getClass())) return;
        // Same body as vanilla spiders, so use the same compact climbing size.
        setSize(0.95F, 0.85F);
        stepHeight = 0.1F;
        nimblespiders$climber = new SpiderClimber(this);
        AccessorEntityLiving access = (AccessorEntityLiving) this;
        access.setMoveHelper(new ClimberMoveController(this));
        access.setLookHelper(new ClimberLookController(this));
        AdvancedClimberPathNavigator navigator = new AdvancedClimberPathNavigator(this, world);
        navigator.setCanSwim(true);
        access.setNavigator(navigator);
        new SpiderAI(this);
    }

    @Override
    public SpiderClimber nimblespiders$getClimber() {
        return nimblespiders$climber;
    }

    @Override
    protected boolean isAIEnabled() {
        return nimblespiders$climber != null || super.isAIEnabled();
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        if (nimblespiders$climber == null) {
            super.moveEntityWithHeading(strafe, forward);
            return;
        }
        if (!nimblespiders$climber.travel(strafe, forward)) {
            super.moveEntityWithHeading(strafe, forward);
            nimblespiders$climber.afterVanillaTravel();
        }
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

    @Inject(method = "isOnLadder", at = @At("HEAD"), cancellable = true)
    private void nimblespiders$disableVanillaClimb(CallbackInfoReturnable<Boolean> cir) {
        if (nimblespiders$climber != null) cir.setReturnValue(false);
    }

    @Override
    protected void fall(float distance) {
        if (nimblespiders$climber == null || Config.fallDamage) super.fall(distance);
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
