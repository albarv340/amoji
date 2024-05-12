package red.bread.amoji.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.bread.amoji.ClientEmojiHandler;

@Mixin(Minecraft.class)
public abstract class MinecraftAmojiMixin {
    @Inject(method = "<init>*", at = @At(value = "RETURN"))
    private void amoji_initEmojis(CallbackInfo callbackInfo) {
        ClientEmojiHandler.setup();
    }
}
