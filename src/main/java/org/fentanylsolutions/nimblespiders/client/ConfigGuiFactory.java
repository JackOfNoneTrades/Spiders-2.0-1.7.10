package org.fentanylsolutions.nimblespiders.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;

import org.fentanylsolutions.nimblespiders.common.Config;
import org.fentanylsolutions.nimblespiders.common.SpiderMod;

import cpw.mods.fml.client.IModGuiFactory;
import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;

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
            super(parent, elements(), SpiderMod.MODID, false, false, "Nimble Spiders");
        }

        private static List<IConfigElement> elements() {
            List<IConfigElement> result = new ArrayList<>();
            result.add(new ConfigElement<>(Config.configuration.getCategory("climbing")));
            result.add(new ConfigElement<>(Config.configuration.getCategory("pathfinding")));
            result.add(new ConfigElement<>(Config.configuration.getCategory("falling")));
            result.add(new ConfigElement<>(Config.configuration.getCategory("movement")));
            return result;
        }
    }
}
