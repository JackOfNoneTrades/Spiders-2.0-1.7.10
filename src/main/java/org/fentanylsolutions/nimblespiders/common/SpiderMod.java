package org.fentanylsolutions.nimblespiders.common;

import org.apache.logging.log4j.Logger;
import org.fentanylsolutions.nimblespiders.Tags;
import org.fentanylsolutions.nimblespiders.common.network.ClimberNetwork;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

@Mod(
    modid = SpiderMod.MODID,
    name = "Nimble Spiders",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:unimixins;",
    guiFactory = "org.fentanylsolutions.nimblespiders.client.ConfigGuiFactory",
    customProperties = { @Mod.CustomProperty(k = "license", v = "LGPL-2.1-or-later"),
        @Mod.CustomProperty(k = "issueTrackerUrl", v = "https://github.com/JackOfNoneTrades/nimble-spiders/issues"),
        @Mod.CustomProperty(k = "iconFile", v = "assets/nimble-spiders/icon.png"),
        @Mod.CustomProperty(k = "backgroundFile", v = "assets/nimble-spiders/background.png") })
public class SpiderMod {

    public static final String MODID = "nimble-spiders";
    @Mod.Instance(MODID)
    public static SpiderMod instance;
    @SidedProxy(
        clientSide = "org.fentanylsolutions.nimblespiders.client.ClientProxy",
        serverSide = "org.fentanylsolutions.nimblespiders.common.CommonProxy")
    public static CommonProxy proxy;
    public static Logger logger;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        Config.init(event.getModConfigurationDirectory());
        FMLCommonHandler.instance()
            .bus()
            .register(new Config());
        ClimberNetwork.init();
        proxy.preInit();
    }
}
