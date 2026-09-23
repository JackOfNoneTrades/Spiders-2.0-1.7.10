package org.fentanylsolutions.nimblespiders.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@LateMixin
public class LateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.nimble-spiders.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        final List<String> mixins = new ArrayList<>();
        if (loadedMods.contains("lotr")) {
            mixins.add("lotr.MixinLOTREntitySpiderBase");
            mixins.add("lotr.MixinLOTRMountFunctions");
        }
        if (loadedMods.contains("abyssalcraft")) mixins.add("abyssalcraft.MixinEntityAntiSpider");
        if (loadedMods.contains("mod_Invasion")) {
            mixins.add("invasion.MixinEntityIMSpider");
            mixins.add("invasion.MixinIMMoveHelper");
        }
        return mixins;
    }
}
