package red.bread.amoji.mixin;

import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import red.bread.amoji.render.ChatHoverRenderer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(ChatComponent.class)

public abstract class ChatRenderMixin {

    @Shadow protected abstract int getMessageLineIndexAt(double d, double e);

    @Shadow protected abstract double screenToChatX(double d);

    @Shadow protected abstract double screenToChatY(double d);

    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;

    @Shadow @Final private List<GuiMessage> allMessages;

    @Unique
    private Component amoji$getChatMessageAt(double mouseX, double mouseY) {
        int index = getMessageLineIndexAt(screenToChatX(mouseX), screenToChatY(mouseY));
        if (index != -1) {
            Map<Integer, Integer> visibleToMessageIndex = new HashMap<>();
            int messageIndex = -1;
            for (int i = 0; i < trimmedMessages.size(); i++) {
                if (trimmedMessages.get(i).endOfEntry()) {
                    ++messageIndex;
                }
                visibleToMessageIndex.put(i, messageIndex);
            }
            int clickedMessageIndex = visibleToMessageIndex.getOrDefault(index, -1);
            if (clickedMessageIndex != -1 && clickedMessageIndex < allMessages.size()) {
                return allMessages.get(clickedMessageIndex).content();
            }
        }
        return null;
    }

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIIZ)V",
            at = @At("HEAD"))
    public void render(GuiGraphics guiGraphics, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        Component hoveredMessage = amoji$getChatMessageAt(mouseX, mouseY);
        if (hoveredMessage != null) {
            ChatHoverRenderer.render(guiGraphics, mouseX, mouseY, hoveredMessage);
        }
    }

}
