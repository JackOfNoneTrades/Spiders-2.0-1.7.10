package org.fentanylsolutions.nimblespiders.client;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import org.fentanylsolutions.nimblespiders.common.CommonProxy;
import org.fentanylsolutions.nimblespiders.common.entity.mob.SpiderClimber;
import org.fentanylsolutions.nimblespiders.common.network.ClimberStateMessage;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class ClientProxy extends CommonProxy {

    private final Queue<ClimberStateMessage> pending = new ConcurrentLinkedQueue<>();

    @Override
    public void preInit() {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @Override
    public void receive(ClimberStateMessage message) {
        pending.add(message);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Minecraft minecraft = Minecraft.getMinecraft();
        ClimberStateMessage message;
        while ((message = pending.poll()) != null) {
            if (minecraft.theWorld == null || minecraft.theWorld.provider.dimensionId != message.dimension) continue;
            Entity entity = minecraft.theWorld.getEntityByID(message.entityId);
            SpiderClimber climber = SpiderClimber.get(entity);
            if (climber != null) climber.receive(
                message.normal,
                message.forward,
                message.headYaw,
                message.pitch,
                message.x,
                message.y,
                message.z,
                message.riderYaw);
        }
    }
}
