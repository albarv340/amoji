package red.bread.amoji.forge;

import dev.architectury.platform.forge.EventBuses;
import red.bread.amoji.Amoji;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Amoji.MOD_ID)
public class AmojiForge {
    public AmojiForge() {
		// Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(Amoji.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
        Amoji.init();
    }
}