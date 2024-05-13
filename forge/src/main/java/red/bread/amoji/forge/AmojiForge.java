package red.bread.amoji.forge;

import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import red.bread.amoji.Constants;
import red.bread.amoji.commands.AmojiCommand;

@Mod(Constants.MOD_ID)
public class AmojiForge {
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onClientCommandInit(RegisterClientCommandsEvent event) {
        AmojiCommand.register(event.getDispatcher());
    }

    public AmojiForge() {
        MinecraftForge.EVENT_BUS.register(this);
    }
}