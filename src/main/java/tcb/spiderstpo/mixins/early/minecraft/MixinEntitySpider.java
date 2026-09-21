package tcb.spiderstpo.mixins.early.minecraft;

import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.monster.EntitySpider;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import tcb.spiderstpo.common.Config;
import tcb.spiderstpo.common.entity.mob.ClimberAccess;
import tcb.spiderstpo.common.entity.mob.SpiderClimber;
import tcb.spiderstpo.common.entity.movement.AdvancedClimberPathNavigator;
import tcb.spiderstpo.common.entity.movement.ClimberLookController;
import tcb.spiderstpo.common.entity.movement.ClimberMoveController;
import tcb.spiderstpo.common.entity.movement.SpiderAI;
import tcb.spiderstpo.common.network.ClimberNetwork;

@Mixin(EntitySpider.class)
public abstract class MixinEntitySpider extends EntityMob implements ClimberAccess {

    @Unique
    private SpiderClimber spiderstpo$climber;

    protected MixinEntitySpider(World world) {
        super(world);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void spiderstpo$initialize(World world, CallbackInfo ci) {
        if (!Config.isEnabled(getClass())) return;
        EntitySpider spider = (EntitySpider) (Object) this;
        // Upstream's compact body fits through surface corners; subclass constructors may set their own size.
        setSize(0.95F, 0.85F);
        stepHeight = 0.1F;
        spiderstpo$climber = new SpiderClimber(spider);
        moveHelper = new ClimberMoveController(spider);
        lookHelper = new ClimberLookController(spider);
        navigator = new AdvancedClimberPathNavigator(spider, world);
        navigator.setCanSwim(true);
        new SpiderAI(spider);
    }

    @Override
    public SpiderClimber spiderstpo$getClimber() {
        return spiderstpo$climber;
    }

    @Override
    protected boolean isAIEnabled() {
        return spiderstpo$climber != null || super.isAIEnabled();
    }

    @Override
    public void moveEntityWithHeading(float strafe, float forward) {
        if (spiderstpo$climber == null) {
            super.moveEntityWithHeading(strafe, forward);
            return;
        }
        if (!spiderstpo$climber.travel(strafe, forward)) {
            super.moveEntityWithHeading(strafe, forward);
            spiderstpo$climber.afterVanillaTravel();
        }
    }

    @Override
    protected float func_110146_f(float yaw, float distance) {
        if (spiderstpo$climber != null && !worldObj.isRemote
            && (moveForward != 0 || Math.abs(posY - prevPosY) > 0.0005)) {
            // Vanilla's body helper checks only X/Z motion and treats vertical climbing as idle.
            renderYawOffset = rotationYaw;
            rotationYawHead = renderYawOffset
                + MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(rotationYawHead - renderYawOffset), -75, 75);
            return distance;
        }
        return super.func_110146_f(yaw, distance);
    }

    @Inject(method = "isOnLadder", at = @At("HEAD"), cancellable = true)
    private void spiderstpo$disableVanillaClimb(CallbackInfoReturnable<Boolean> cir) {
        if (spiderstpo$climber != null) cir.setReturnValue(false);
    }

    @Override
    protected void fall(float distance) {
        // Keep the normal damage pipeline, including Forge's event, potions and landing sounds.
        if (spiderstpo$climber == null || Config.fallDamage) super.fall(distance);
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void spiderstpo$sync(CallbackInfo ci) {
        if (spiderstpo$climber != null) {
            if (worldObj.isRemote) spiderstpo$climber.updateClientAngles();
            else spiderstpo$climber.debug.tick(spiderstpo$climber);
        }
        if (spiderstpo$climber != null && !worldObj.isRemote && ticksExisted % 3 == 0)
            ClimberNetwork.send(spiderstpo$climber);
    }
}
