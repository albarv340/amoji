package red.bread.amoji.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import red.bread.amoji.util.EmojiUtil;
import red.bread.amoji.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatHoverRenderer {

    public static void render(GuiGraphics guiGraphics, int mouseX, int mouseY, Component hoveredMessage) {
        List<String> emojis = new ArrayList<>();
        String messageString = hoveredMessage.getString();
        Pattern emojiPattern = Pattern.compile(":(\\w+):");
        Matcher emojiMatcher = emojiPattern.matcher(messageString);
//        List<Pair<Integer, Integer>> indexes = new ArrayList<>();
        Collection<String> allEmoji = EmojiUtil.getEmojiSuggestions();
        while (emojiMatcher.find()) {
//            indexes.add(new Pair<>(emojiMatcher.start(), emojiMatcher.end()));
            if (allEmoji.contains(emojiMatcher.group())) {
                emojis.add(emojiMatcher.group());
            }
        }

        if (Util.isShiftDown() && !emojis.isEmpty()) {
            guiGraphics.renderTooltip(Minecraft.getInstance().fontFilterFishy, Component.literal(String.join(" ", emojis)), mouseX, mouseY);
        }
    }
}
