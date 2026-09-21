package tcb.spiderstpo.mixins.late.lotr;

import java.util.ArrayList;

import net.minecraft.entity.ai.EntityAILeapAtTarget;
import net.minecraft.entity.ai.EntityAITasks;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import lotr.common.entity.npc.LOTREntityNPCRideable;
import lotr.common.entity.npc.LOTREntitySpiderBase;
import tcb.spiderstpo.common.Config;
import tcb.spiderstpo.common.entity.mob.ClimberAccess;
import tcb.spiderstpo.common.entity.mob.SpiderClimber;
import tcb.spiderstpo.common.entity.movement.AdvancedClimberPathNavigator;
import tcb.spiderstpo.common.entity.movement.ClimberLeapAtTarget;
import tcb.spiderstpo.common.entity.movement.ClimberLookController;
import tcb.spiderstpo.common.entity.movement.ClimberMoveController;
import tcb.spiderstpo.common.network.ClimberNetwork;

@Mixin(LOTREntitySpiderBase.class)
public abstract class MixinLOTREntitySpiderBase extends LOTREntityNPCRideable implements ClimberAccess {

    @Unique
    private SpiderClimber spiderstpo$climber;
    @Unique
    private float spiderstpo$lastScale = -1;

    protected MixinLOTREntitySpiderBase(World world) {
        super(world);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void spiderstpo$initialize(World world, CallbackInfo ci) {
        if (!Config.isEnabled(getClass())) return;
        // LOTR retains its own size scaling, faction targets, combat and hired NPC tasks.
        // NPC attributes already use task-AI speed units; vanilla spiders need a legacy conversion.
        spiderstpo$climber = new SpiderClimber(this, 1.0F, true, 0.35);
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
    public SpiderClimber spiderstpo$getClimber() {
        return spiderstpo$climber;
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        if (spiderstpo$climber == null) {
            super.moveEntityWithHeading(strafe, forward);
            return;
        }
        if (spiderstpo$climber.isActive()) stepHeight = 0.1F;
        if (!spiderstpo$climber.travel(strafe, forward)) {
            // Preserve LOTR's mount controls, including its climbing stamina.
            super.moveEntityWithHeading(strafe, forward);
            spiderstpo$climber.afterVanillaTravel();
        }
    }

    @Override
    protected float func_110146_f(float yaw, float distance) {
        if (spiderstpo$climber != null && spiderstpo$climber.isActive()
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
    private void spiderstpo$disableVanillaClimb(CallbackInfoReturnable<Boolean> cir) {
        if (spiderstpo$climber != null && spiderstpo$climber.isActive()) cir.setReturnValue(false);
    }

    @Inject(method = "attackEntityFrom", at = @At("HEAD"), cancellable = true)
    private void spiderstpo$fallDamage(DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (spiderstpo$climber != null && source == DamageSource.fall) {
            cir.setReturnValue(Config.fallDamage && super.attackEntityFrom(source, damage));
        }
    }

    @Override
    public void onUpdate() {
        if (spiderstpo$climber != null && spiderstpo$climber.isActive() && getNPCScale() != spiderstpo$lastScale) {
            // LOTR normally applies its scale after movement and the suffocation check. Resolve
            // the full-sized body against the spawn face before either can run.
            double x = posX, y = posY, z = posZ;
            spiderstpo$lastScale = getNPCScale();
            rescaleNPC(spiderstpo$lastScale);
            setPosition(x, y, z);
            spiderstpo$climber.resolveSpawnCollision();
        }
        super.onUpdate();
        // Run after EntityLivingBase's body-yaw update, just like the vanilla spider hook.
        if (spiderstpo$climber == null) return;
        if (worldObj.isRemote) spiderstpo$climber.updateClientAngles();
        else {
            spiderstpo$climber.debug.tick(spiderstpo$climber);
            if (ticksExisted % 3 == 0) ClimberNetwork.send(spiderstpo$climber);
        }
    }
}
