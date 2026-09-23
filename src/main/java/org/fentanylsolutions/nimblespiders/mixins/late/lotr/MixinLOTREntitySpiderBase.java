package org.fentanylsolutions.nimblespiders.mixins.late.lotr;

import java.util.ArrayList;

import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.fentanylsolutions.nimblespiders.common.Config;
import org.fentanylsolutions.nimblespiders.common.entity.mob.ClimberAccess;
import org.fentanylsolutions.nimblespiders.common.entity.mob.SpiderClimber;
import org.fentanylsolutions.nimblespiders.common.entity.movement.AdvancedClimberPathNavigator;
import org.fentanylsolutions.nimblespiders.common.entity.movement.ClimberLeapAtTarget;
import org.fentanylsolutions.nimblespiders.common.entity.movement.ClimberLookController;
import org.fentanylsolutions.nimblespiders.common.entity.movement.ClimberMoveController;
import org.fentanylsolutions.nimblespiders.common.network.ClimberNetwork;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import lotr.common.entity.npc.LOTREntityNPCRideable;
import lotr.common.entity.npc.LOTREntitySpiderBase;

@Mixin(LOTREntitySpiderBase.class)
public abstract class MixinLOTREntitySpiderBase extends LOTREntityNPCRideable implements ClimberAccess {

    @Unique
    private SpiderClimber nimblespiders$climber;
    @Unique
    private float nimblespiders$lastScale = -1;

    @Shadow(remap = false)
    public abstract int getSpiderClimbTime();

    @Shadow(remap = false)
    public abstract void setSpiderClimbTime(int ticks);

    @Unique
    private int nimblespiders$previousClimbTime;

    @Inject(method = "onLivingUpdate", at = @At("HEAD"))
    private void nimblespiders$rememberStamina(CallbackInfo ci) {
        nimblespiders$previousClimbTime = getSpiderClimbTime();
    }

    @Inject(method = "onLivingUpdate", at = @At("RETURN"))
    private void nimblespiders$mountedStamina(CallbackInfo ci) {
        if (worldObj.isRemote || nimblespiders$climber == null || !nimblespiders$climber.isPlayerControlled()) return;
        // Surface travel marks wall/ceiling contact as onGround; LOTR normally interprets that
        // as resting. Preserve its 100-tick climbing limit using the actual attachment normal.
        boolean climbing = nimblespiders$climber.orientationNormal.y < 0.7;
        int ticks = onGround && !climbing && !nimblespiders$climber.isDropping() ? 0
            : climbing ? Math.min(100, nimblespiders$previousClimbTime + 1) : nimblespiders$previousClimbTime;
        setSpiderClimbTime(ticks);
        if (ticks >= 100 && climbing && !nimblespiders$climber.isDropping()) {
            nimblespiders$climber.debug.event("MOUNT_DROP reason=stamina climbTicks=" + ticks);
            nimblespiders$climber.dropFromSurface();
        }
        if (ticksExisted % 20 == 0 && nimblespiders$climber.debug.enabled())
            nimblespiders$climber.debug.event("MOUNT_STAMINA climbTicks=" + ticks + " climbing=" + climbing);
    }

    @Inject(method = "shouldRenderClimbingMeter", at = @At("HEAD"), cancellable = true, remap = false)
    private void nimblespiders$climbingMeter(CallbackInfoReturnable<Boolean> cir) {
        if (nimblespiders$climber != null && nimblespiders$climber.isPlayerControlled())
            cir.setReturnValue(getSpiderClimbTime() > 0 && nimblespiders$climber.orientationNormal.y < 0.7);
    }

    protected MixinLOTREntitySpiderBase(World world) {
        super(world);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void nimblespiders$initialize(World world, CallbackInfo ci) {
        if (!Config.isEnabled(getClass())) return;
        // LOTR retains its own size scaling, faction targets, combat and hired NPC tasks.
        // NPC attributes already use task-AI speed units; vanilla spiders need a legacy conversion.
        nimblespiders$climber = new SpiderClimber(this, 1.0F, true, 0.35);
        moveHelper = new ClimberMoveController(this);
        lookHelper = new ClimberLookController(this);
        boolean avoidsWater = navigator.getAvoidsWater();
        navigator = new AdvancedClimberPathNavigator(this, world);
        navigator.setAvoidsWater(avoidsWater);
        navigator.setCanSwim(true);
        stepHeight = 0.1F;
        for (Object value : new ArrayList<>(tasks.taskEntries)) {
            EntityAITasks.EntityAITaskEntry entry = (EntityAITasks.EntityAITaskEntry) value;
            if (entry.action instanceof EntityAILeapAtTarget) {
                tasks.removeTask(entry.action);
                tasks.addTask(entry.priority, new ClimberLeapAtTarget(this));
            }
        }
    }

    @Override
    public SpiderClimber nimblespiders$getClimber() {
        return nimblespiders$climber;
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        if (nimblespiders$climber == null) {
            super.moveEntityWithHeading(strafe, forward);
            return;
        }
        if (nimblespiders$climber.isActive()) stepHeight = 0.1F;
        if (!nimblespiders$climber.travel(strafe, forward)) {
            // Controlled drops/fluids use vanilla gravity without LOTR's client position control.
            if (nimblespiders$climber.isPlayerControlled()) super.super_moveEntityWithHeading(
                nimblespiders$climber.getRiderStrafe(),
                nimblespiders$climber.getRiderForward());
            else super.moveEntityWithHeading(strafe, forward);
            nimblespiders$climber.afterVanillaTravel();
        }
    }

    @Override
    protected float func_110146_f(float yaw, float distance) {
        if (nimblespiders$climber != null && nimblespiders$climber.isActive()
            && !worldObj.isRemote
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
        if (nimblespiders$climber != null && nimblespiders$climber.isActive()) cir.setReturnValue(false);
    }

    @Inject(method = "attackEntityFrom", at = @At("HEAD"), cancellable = true)
    private void nimblespiders$fallDamage(DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (nimblespiders$climber != null && source == DamageSource.fall) {
            cir.setReturnValue(Config.fallDamage && super.attackEntityFrom(source, damage));
        }
    }

    @Override
    public void onUpdate() {
        if (nimblespiders$climber != null) nimblespiders$climber
            .setPlayerControlled(isNPCTamed() && riddenByEntity instanceof net.minecraft.entity.player.EntityPlayer);
        if (nimblespiders$climber != null && nimblespiders$climber.isActive()
            && getNPCScale() != nimblespiders$lastScale) {
            // LOTR normally applies its scale after movement and the suffocation check. Resolve
            // the full-sized body against the spawn face before either can run.
            double x = posX, y = posY, z = posZ;
            nimblespiders$lastScale = getNPCScale();
            rescaleNPC(nimblespiders$lastScale);
            setPosition(x, y, z);
            nimblespiders$climber.resolveSpawnCollision();
        }
        super.onUpdate();
        // Run after EntityLivingBase's body-yaw update, just like the vanilla spider hook.
        if (nimblespiders$climber == null) return;
        if (worldObj.isRemote) nimblespiders$climber.updateClientAngles();
        else {
            nimblespiders$climber.debug.tick(nimblespiders$climber);
            if (ticksExisted % 3 == 0) ClimberNetwork.send(nimblespiders$climber);
        }
    }
}
