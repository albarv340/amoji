package red.bread.amoji.mixin;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.commands.SharedSuggestionProvider;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import red.bread.amoji.util.EmojiUtil;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsAmojiMixin {
    @Shadow
    @Final
    Minecraft minecraft;

    @Shadow
    @Nullable
    private CompletableFuture<Suggestions> pendingSuggestions;

    @Shadow @Final
    EditBox input;

    @Inject(method = "updateCommandInfo()V", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)

    public void updateCommandInfo(CallbackInfo ci, String string) {
        if (this.minecraft.player != null) {
            int i = this.input.getCursorPosition();
            String string2 = string.substring(0, i);
            int j = CommandSuggestions.getLastWordIndex(string2);
            Collection<String> collection = this.minecraft.player.connection.getSuggestionsProvider().getCustomTabSugggestions();
            // Always makes emojis tabbable, but doesn't seem to override other tab suggestions
            collection.addAll(EmojiUtil.getEmojiSuggestions());
            this.pendingSuggestions = SharedSuggestionProvider.suggest(collection, new SuggestionsBuilder(string2, j));
        }
    }
}