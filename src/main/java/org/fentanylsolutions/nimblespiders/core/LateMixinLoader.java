package org.fentanylsolutions.nimblespiders.core;

import java.util.Arrays;
import java.util.Collections;
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
        return loadedMods.contains("lotr")
            ? Arrays.asList("lotr.MixinLOTREntitySpiderBase", "lotr.MixinLOTRMountFunctions")
            : Collections.emptyList();
    }
}
