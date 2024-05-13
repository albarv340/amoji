package red.bread.amoji.forge;

import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import red.bread.amoji.Constants;

@Mod(Constants.MOD_ID)
public class AmojiForge {
    public AmojiForge() {
		// Submit our event bus to let architectury register our content on the right time
        EventBuses.registerModEventBus(Constants.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
    }
}