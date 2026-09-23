package org.fentanylsolutions.nimblespiders.mixins.early.minecraft;

import java.nio.FloatBuffer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import org.fentanylsolutions.nimblespiders.common.SurfaceFrame;
import org.fentanylsolutions.nimblespiders.common.Vec3d;
import org.fentanylsolutions.nimblespiders.common.entity.mob.ClimberRider;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer {

    @Shadow
    private Minecraft mc;
    @Shadow
    private float thirdPersonDistance;
    @Shadow
    private float thirdPersonDistanceTemp;
    @Shadow
    private boolean cloudFog;
    @Unique
    private final FloatBuffer nimblespiders$viewMatrix = BufferUtils.createFloatBuffer(16);

    @Inject(method = "orientCamera", at = @At("HEAD"), cancellable = true)
    private void nimblespiders$riderCamera(float partialTicks, CallbackInfo ci) {
        EntityLivingBase player = mc.renderViewEntity;
        SurfaceFrame frame = ClimberRider.getViewFrame(player, partialTicks);
        if (frame == null || player.isPlayerSleeping() || mc.gameSettings.debugCamEnable) return;
        Vec3d eye = ClimberRider.getEyePosition(player, partialTicks);
        if (mc.gameSettings.thirdPersonView == 2) frame = frame.look(180, 0);
        double distance = mc.gameSettings.thirdPersonView > 0
            ? thirdPersonDistanceTemp + (thirdPersonDistance - thirdPersonDistanceTemp) * partialTicks
            : 0;
        // Third-person clipping must follow the rotated view, too, including ceilings.
        if (distance > 0) for (int i = 0; i < 8; i++) {
            double x = ((i & 1) * 2 - 1) * 0.1, y = (((i >> 1) & 1) * 2 - 1) * 0.1, z = (((i >> 2) & 1) * 2 - 1) * 0.1;
            Vec3d start = eye.addVector(x, y, z), end = start.subtract(frame.forward.scale(distance));
            MovingObjectPosition hit = player.worldObj.rayTraceBlocks(
                Vec3.createVectorHelper(start.x, start.y, start.z),
                Vec3.createVectorHelper(end.x, end.y, end.z));
            if (hit != null) distance = Math.min(
                distance,
                Math.max(
                    0,
                    new Vec3d(hit.hitVec.xCoord, hit.hitVec.yCoord, hit.hitVec.zCoord).subtract(start)
                        .lengthVector() - 0.05));
        }
        GL11.glTranslated(0, 0, -distance);
        nimblespiders$viewMatrix.clear();
        // Inverse view basis: Minecraft looks down camera -Z and renders model forward as +Z.
        nimblespiders$viewMatrix.put((float) -frame.right.x)
            .put((float) frame.up.x)
            .put((float) -frame.forward.x)
            .put(0);
        nimblespiders$viewMatrix.put((float) -frame.right.y)
            .put((float) frame.up.y)
            .put((float) -frame.forward.y)
            .put(0);
        nimblespiders$viewMatrix.put((float) -frame.right.z)
            .put((float) frame.up.z)
            .put((float) -frame.forward.z)
            .put(0);
        nimblespiders$viewMatrix.put(0)
            .put(0)
            .put(0)
            .put(1)
            .flip();
        GL11.glMultMatrix(nimblespiders$viewMatrix);
        double x = player.prevPosX + (player.posX - player.prevPosX) * partialTicks;
        double y = player.prevPosY + (player.posY - player.prevPosY) * partialTicks;
        double z = player.prevPosZ + (player.posZ - player.prevPosZ) * partialTicks;
        GL11.glTranslated(x - eye.x, y - eye.y, z - eye.z);
        Vec3d camera = eye.subtract(frame.forward.scale(distance));
        cloudFog = mc.renderGlobal.hasCloudFog(camera.x, camera.y, camera.z, partialTicks);
        ci.cancel();
    }
}
