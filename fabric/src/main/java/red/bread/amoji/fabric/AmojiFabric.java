package red.bread.amoji.fabric;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import red.bread.amoji.fabric.commands.AmojiCommandFabric;

public class AmojiFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> AmojiCommandFabric.register(dispatcher));
    }
}