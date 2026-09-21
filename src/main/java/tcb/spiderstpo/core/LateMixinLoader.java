package tcb.spiderstpo.core;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@LateMixin
public class LateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.spiderstpo.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return loadedMods.contains("lotr") ? Collections.singletonList("lotr.MixinLOTREntitySpiderBase")
            : Collections.emptyList();
    }
}
