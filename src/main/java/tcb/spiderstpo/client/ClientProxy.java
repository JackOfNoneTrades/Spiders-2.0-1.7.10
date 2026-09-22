package tcb.spiderstpo.client;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import tcb.spiderstpo.common.CommonProxy;
import tcb.spiderstpo.common.entity.mob.SpiderClimber;
import tcb.spiderstpo.common.network.ClimberStateMessage;

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
