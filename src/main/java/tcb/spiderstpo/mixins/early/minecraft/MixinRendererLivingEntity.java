package tcb.spiderstpo.mixins.early.minecraft;

import java.nio.FloatBuffer;

import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import tcb.spiderstpo.common.SurfaceFrame;
import tcb.spiderstpo.common.Vec3d;
import tcb.spiderstpo.common.entity.mob.ClimberRider;
import tcb.spiderstpo.common.entity.mob.SpiderClimber;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity {

    @Unique
    private final FloatBuffer spiderstpo$rotationMatrix = BufferUtils.createFloatBuffer(16);

    @Inject(
        method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;rotateCorpse(Lnet/minecraft/entity/EntityLivingBase;FFF)V"))
    private void spiderstpo$orient(EntityLivingBase entity, double x, double y, double z, float yaw, float partialTicks,
        CallbackInfo ci) {
        SpiderClimber riderMount = ClimberRider.getMount(entity);
        SpiderClimber climber = riderMount != null ? riderMount : SpiderClimber.get(entity);
        if (climber == null || !climber.isActive()) return;
        SurfaceFrame frame = climber.getRenderFrame(partialTicks);
        if (riderMount == null) {
            Vec3d offset = climber.getRenderOffset(partialTicks);
            GL11.glTranslated(offset.x, offset.y, offset.z);
        }
        spiderstpo$rotationMatrix.clear();
        spiderstpo$rotationMatrix.put((float) frame.right.x)
            .put((float) frame.right.y)
            .put((float) frame.right.z)
            .put(0);
        spiderstpo$rotationMatrix.put((float) frame.up.x)
            .put((float) frame.up.y)
            .put((float) frame.up.z)
            .put(0);
        spiderstpo$rotationMatrix.put((float) frame.forward.x)
            .put((float) frame.forward.y)
            .put((float) frame.forward.z)
            .put(0);
        spiderstpo$rotationMatrix.put(0)
            .put(0)
            .put(0)
            .put(1);
        spiderstpo$rotationMatrix.flip();
        GL11.glMultMatrix(spiderstpo$rotationMatrix);
        // Preserve the vanilla/subclass rotateCorpse call (including death animation), cancelling
        // only its heading: the complete world-space heading is already in the matrix above.
        float bodyYaw = entity.prevRenderYawOffset
            + MathHelper.wrapAngleTo180_float(entity.renderYawOffset - entity.prevRenderYawOffset) * partialTicks;
        if (entity.isRiding() && entity.ridingEntity instanceof EntityLivingBase) {
            EntityLivingBase mount = (EntityLivingBase) entity.ridingEntity;
            float mountYaw = mount.prevRenderYawOffset
                + MathHelper.wrapAngleTo180_float(mount.renderYawOffset - mount.prevRenderYawOffset) * partialTicks;
            float headYaw = entity.prevRotationYawHead
                + MathHelper.wrapAngleTo180_float(entity.rotationYawHead - entity.prevRotationYawHead) * partialTicks;
            float difference = MathHelper.clamp_float(MathHelper.wrapAngleTo180_float(headYaw - mountYaw), -85, 85);
            bodyYaw = headYaw - difference;
            if (difference * difference > 2500) bodyYaw += difference * 0.2F;
        }
        GL11.glRotatef(bodyYaw, 0, 1, 0);
    }
}
