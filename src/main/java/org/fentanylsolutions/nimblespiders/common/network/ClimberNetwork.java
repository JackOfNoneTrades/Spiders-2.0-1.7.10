package org.fentanylsolutions.nimblespiders.common.network;

import org.fentanylsolutions.nimblespiders.common.SpiderMod;
import org.fentanylsolutions.nimblespiders.common.entity.mob.SpiderClimber;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class ClimberNetwork {

    private static SimpleNetworkWrapper channel;

    public static void init() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(SpiderMod.MODID);
        channel.registerMessage(ClimberStateMessage.Handler.class, ClimberStateMessage.class, 0, Side.CLIENT);
    }

    public static void send(SpiderClimber climber) {
        if (channel != null) channel.sendToAllAround(
            new ClimberStateMessage(climber),
            new NetworkRegistry.TargetPoint(
                climber.entity.dimension,
                climber.entity.posX,
                climber.entity.posY,
                climber.entity.posZ,
                128));
    }
}
