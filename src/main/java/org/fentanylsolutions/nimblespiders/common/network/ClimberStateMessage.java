package org.fentanylsolutions.nimblespiders.common.network;

import org.fentanylsolutions.nimblespiders.common.SpiderMod;
import org.fentanylsolutions.nimblespiders.common.Vec3d;
import org.fentanylsolutions.nimblespiders.common.entity.mob.SpiderClimber;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Kept out of DataWatcher to avoid colliding with slots used by spider subclasses. */
public class ClimberStateMessage implements IMessage {

    public int entityId, dimension;
    public Vec3d normal, forward;
    public float headYaw, pitch, riderYaw;
    public float x, y, z;

    public ClimberStateMessage() {}

    public ClimberStateMessage(SpiderClimber state) {
        entityId = state.entity.getEntityId();
        dimension = state.entity.dimension;
        normal = state.orientationNormal;
        forward = state.getOrientation(1)
            .getDirection(state.entity.renderYawOffset, 0);
        headYaw = net.minecraft.util.MathHelper
            .wrapAngleTo180_float(state.entity.rotationYawHead - state.entity.renderYawOffset);
        pitch = state.entity.rotationPitch;
        riderYaw = state.riderYaw;
        x = (float) state.stickingOffsetX;
        y = (float) state.stickingOffsetY;
        z = (float) state.stickingOffsetZ;
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        entityId = buffer.readInt();
        dimension = buffer.readInt();
        normal = new Vec3d(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        forward = new Vec3d(buffer.readFloat(), buffer.readFloat(), buffer.readFloat());
        headYaw = buffer.readFloat();
        pitch = buffer.readFloat();
        x = buffer.readFloat();
        y = buffer.readFloat();
        z = buffer.readFloat();
        riderYaw = buffer.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeInt(entityId);
        buffer.writeInt(dimension);
        buffer.writeFloat((float) normal.x);
        buffer.writeFloat((float) normal.y);
        buffer.writeFloat((float) normal.z);
        buffer.writeFloat((float) forward.x);
        buffer.writeFloat((float) forward.y);
        buffer.writeFloat((float) forward.z);
        buffer.writeFloat(headYaw);
        buffer.writeFloat(pitch);
        buffer.writeFloat(x);
        buffer.writeFloat(y);
        buffer.writeFloat(z);
        buffer.writeFloat(riderYaw);
    }

    public static class Handler implements IMessageHandler<ClimberStateMessage, IMessage> {

        @Override
        public IMessage onMessage(ClimberStateMessage message, MessageContext context) {
            SpiderMod.proxy.receive(message);
            return null;
        }
    }
}
