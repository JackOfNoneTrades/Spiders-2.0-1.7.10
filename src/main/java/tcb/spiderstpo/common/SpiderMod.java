package tcb.spiderstpo.common;

import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import tcb.spiderstpo.Tags;
import tcb.spiderstpo.common.network.ClimberNetwork;

@Mod(
    modid = SpiderMod.MODID,
    name = "Spiders 2.0 1.7.10",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    dependencies = "required-after:unimixins;",
    guiFactory = "tcb.spiderstpo.client.ConfigGuiFactory",
    customProperties = { @Mod.CustomProperty(k = "license", v = "LGPL-2.1-or-later"),
        @Mod.CustomProperty(k = "issueTrackerUrl", v = "https://github.com/JackOfNoneTrades/Spiders-2.0-1.7.10/issues"),
        @Mod.CustomProperty(k = "iconFile", v = "assets/spiderstpo/icon.png"),
        @Mod.CustomProperty(k = "backgroundFile", v = "assets/spiderstpo/background.png") })
public class SpiderMod {

    public static final String MODID = "spiderstpo";
    @Mod.Instance(MODID)
    public static SpiderMod instance;
    @SidedProxy(clientSide = "tcb.spiderstpo.client.ClientProxy", serverSide = "tcb.spiderstpo.common.CommonProxy")
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
