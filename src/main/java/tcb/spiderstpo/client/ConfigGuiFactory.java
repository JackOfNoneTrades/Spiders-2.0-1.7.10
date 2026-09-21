package tcb.spiderstpo.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;
import tcb.spiderstpo.common.Config;
import tcb.spiderstpo.common.SpiderMod;

public class ConfigGuiFactory implements IModGuiFactory {

    @Override
    public void initialize(Minecraft minecraft) {}

    @Override
    public Class<? extends GuiScreen> mainConfigGuiClass() {
        return Screen.class;
    }

    @Override
    public Set<RuntimeOptionCategoryElement> runtimeGuiCategories() {
        return null;
    }

    @Override
    public RuntimeOptionGuiHandler getHandlerFor(RuntimeOptionCategoryElement element) {
        return null;
    }

    public static class Screen extends GuiConfig {

        public Screen(GuiScreen parent) {
            super(parent, elements(), SpiderMod.MODID, false, false, "Spiders 2.0 1.7.10");
        }

        private static List<IConfigElement> elements() {
            List<IConfigElement> result = new ArrayList<>();
            result.add(new ConfigElement<>(Config.configuration.getCategory("climbing")));
            result.add(new ConfigElement<>(Config.configuration.getCategory("pathfinding")));
            result.add(new ConfigElement<>(Config.configuration.getCategory("falling")));
            return result;
        }
    }
}
