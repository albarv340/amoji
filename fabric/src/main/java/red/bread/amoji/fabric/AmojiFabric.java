package red.bread.amoji.fabric;

import red.bread.amoji.Amoji;
import net.fabricmc.api.ModInitializer;

public class AmojiFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        Amoji.init();
    }
}