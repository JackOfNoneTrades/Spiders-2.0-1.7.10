package org.fentanylsolutions.nimblespiders.mixins.early.minecraft;

import java.nio.FloatBuffer;

import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;

import org.fentanylsolutions.nimblespiders.common.SurfaceFrame;
import org.fentanylsolutions.nimblespiders.common.Vec3d;
import org.fentanylsolutions.nimblespiders.common.entity.mob.ClimberRider;
import org.fentanylsolutions.nimblespiders.common.entity.mob.SpiderClimber;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity {

    @Unique
    private final FloatBuffer nimblespiders$rotationMatrix = BufferUtils.createFloatBuffer(16);

    @Unique
    private boolean nimblespiders$restorePlayer;
    @Unique
    private float nimblespiders$head, nimblespiders$prevHead, nimblespiders$body, nimblespiders$prevBody;

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("HEAD"))
    private void nimblespiders$playerLocalAngles(EntityLivingBase entity, double x, double y, double z, float yaw,
        float partialTicks, CallbackInfo ci) {
        SpiderClimber mount = ClimberRider.getMount(entity);
        nimblespiders$restorePlayer = entity instanceof EntityPlayer && mount != null;
        if (!nimblespiders$restorePlayer) return;
        nimblespiders$head = entity.rotationYawHead;
        nimblespiders$prevHead = entity.prevRotationYawHead;
        nimblespiders$body = entity.renderYawOffset;
        nimblespiders$prevBody = entity.prevRenderYawOffset;
        entity.renderYawOffset = entity.prevRenderYawOffset = 0;
        entity.rotationYawHead = entity.prevRotationYawHead = MathHelper
            .wrapAngleTo180_float(entity.rotationYaw - mount.getRiderYaw(partialTicks));
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At("RETURN"))
    private void nimblespiders$restorePlayerAngles(EntityLivingBase entity, double x, double y, double z, float yaw,
        float partialTicks, CallbackInfo ci) {
        if (!nimblespiders$restorePlayer) return;
        entity.rotationYawHead = nimblespiders$head;
        entity.prevRotationYawHead = nimblespiders$prevHead;
        entity.renderYawOffset = nimblespiders$body;
        entity.prevRenderYawOffset = nimblespiders$prevBody;
        nimblespiders$restorePlayer = false;
    }

    @Inject(
        method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/entity/RendererLivingEntity;rotateCorpse(Lnet/minecraft/entity/EntityLivingBase;FFF)V"))
    private void nimblespiders$orient(EntityLivingBase entity, double x, double y, double z, float yaw,
        float partialTicks, CallbackInfo ci) {
        SpiderClimber riderMount = ClimberRider.getMount(entity);
        SpiderClimber climber = riderMount != null ? riderMount : SpiderClimber.get(entity);
        if (climber == null || !climber.isActive()) return;
        SurfaceFrame frame = climber.getRenderFrame(partialTicks);
        if (riderMount != null && entity instanceof EntityPlayer) {
            // RenderPlayer subtracts yOffset along world Y before this hook. Move that offset
            // into the rider's surface frame so the third-person body stays on the seat.
            GL11.glTranslated(
                -frame.up.x * entity.yOffset,
                (1 - frame.up.y) * entity.yOffset,
                -frame.up.z * entity.yOffset);
        }
        if (riderMount == null) {
            Vec3d offset = climber.getRenderOffset(partialTicks);
            GL11.glTranslated(offset.x, offset.y, offset.z);
        }
        nimblespiders$rotationMatrix.clear();
        nimblespiders$rotationMatrix.put((float) frame.right.x)
            .put((float) frame.right.y)
            .put((float) frame.right.z)
            .put(0);
        nimblespiders$rotationMatrix.put((float) frame.up.x)
            .put((float) frame.up.y)
            .put((float) frame.up.z)
            .put(0);
        nimblespiders$rotationMatrix.put((float) frame.forward.x)
            .put((float) frame.forward.y)
            .put((float) frame.forward.z)
            .put(0);
        nimblespiders$rotationMatrix.put(0)
            .put(0)
            .put(0)
            .put(1);
        nimblespiders$rotationMatrix.flip();
        GL11.glMultMatrix(nimblespiders$rotationMatrix);
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
