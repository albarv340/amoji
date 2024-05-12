package red.bread.amoji;

import com.google.gson.JsonElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.world.level.block.entity.BlockEntityType;
import red.bread.amoji.api.Emoji;
import red.bread.amoji.api.EmojiCategory;
import red.bread.amoji.render.EmojiFontRenderer;
import red.bread.amoji.util.EmojiUtil;
import red.bread.amoji.util.WebUtils;

import java.util.*;
import java.util.stream.Collectors;

public class ClientEmojiHandler {
    public static final List<EmojiCategory> CATEGORIES = new ArrayList<>();
    public static Font oldFontRenderer;
    public static List<String> ALL_EMOJIS = new ArrayList<>();
    public static HashMap<EmojiCategory, List<Emoji[]>> SORTED_EMOJIS_FOR_SELECTION = new LinkedHashMap<>();
    public static int lineAmount;

    public static void setup() {
        preInitEmojis();
        initEmojis();
        indexEmojis();
        Constants.LOG.info("Loaded " + Constants.EMOJI_LIST.size() + " emojis");
    }

    // Currently unused
    public static void indexEmojis() {
        ALL_EMOJIS = Constants.EMOJI_LIST.stream().map(emoji -> emoji.strings).flatMap(Collection::stream).collect(Collectors.toList());
        SORTED_EMOJIS_FOR_SELECTION = new LinkedHashMap<>();
        for (EmojiCategory category : CATEGORIES) {
            ++lineAmount;
            Emoji[] array = new Emoji[9];
            int i = 0;
            for (Emoji emoji : Constants.EMOJI_MAP.getOrDefault(category.name(), new ArrayList<>())) {
                array[i] = emoji;
                ++i;
                if (i >= array.length) {
                    SORTED_EMOJIS_FOR_SELECTION.computeIfAbsent(category, s -> new ArrayList<>()).add(array);
                    array = new Emoji[9];
                    i = 0;
                    ++lineAmount;
                }
            }
            if (i > 0) {
                SORTED_EMOJIS_FOR_SELECTION.computeIfAbsent(category, s -> new ArrayList<>()).add(array);
                ++lineAmount;
            }
        }
    }

    private static void preInitEmojis() {
        CATEGORIES.addAll(Arrays.asList("Smileys & Emotion", "Animals & Nature", "Food & Drink", "Activities", "Travel & Places", "Objects", "Symbols", "Flags").stream().map(s -> new EmojiCategory(s, false)).collect(Collectors.toList()));
        loadBaseEmojis();
        loadAVOEmojiAPIEmojis();
    }


    public static void loadBaseEmojis() {
        try {
            for (JsonElement element : WebUtils.readJsonFromUrl(Constants.EMOJI_DATA_URL).getAsJsonArray()) {
                if (EmojiUtil.isBaseEmoji(element)) {
                    Emoji emoji = new Emoji();
                    emoji.name = element.getAsJsonObject().get("short_name").getAsString();
                    emoji.url = Constants.EMOJI_BASE_URL + element.getAsJsonObject().get("image").getAsString();
                    emoji.sort = element.getAsJsonObject().get("sort_order").getAsInt();
                    element.getAsJsonObject().get("short_names").getAsJsonArray().forEach(jsonElement -> emoji.strings.add(":" + jsonElement.getAsString() + ":"));
                    Constants.EMOJI_MAP.computeIfAbsent(element.getAsJsonObject().get("category").getAsString(), s -> new ArrayList<>()).add(emoji);
                    Constants.EMOJI_LIST.add(emoji);
                }
            }
            Constants.EMOJI_MAP.values().forEach(emojis -> emojis.sort(Comparator.comparingInt(o -> o.sort)));
        } catch (Exception e) {
            Constants.error = true;
            Constants.LOG.error("Amoji encountered an error while loading", e);
        }
    }

    public static void loadAVOEmojiAPIEmojis() {
        try {
            CATEGORIES.add(0, new EmojiCategory("AVO", false));
            for (Map.Entry<String, JsonElement> entry : WebUtils.readJsonFromUrl("https://script.google.com/macros/s/AKfycbzjrfYF_GDwuCEksMghivp8dLKAk-UtXGE3zl0fu8s4hzK60J3iFfh1QhtZJUvjUM8a/exec").getAsJsonObject().entrySet()) {
                Emoji emoji = new Emoji();
                emoji.name = entry.getKey();
                emoji.url = entry.getValue().getAsString();
                emoji.strings.add(":" + entry.getKey() + ":");
                Constants.EMOJI_MAP.computeIfAbsent("AVO", s -> new ArrayList<>()).add(emoji);
                Constants.EMOJI_LIST.add(emoji);
            }
        } catch (Exception e) {
            Constants.error = true;
            Constants.LOG.error("Amoji encountered an error while loading", e);
        }
    }

    private static void initEmojis() {
        if (!Constants.error) {
            oldFontRenderer = Minecraft.getInstance().font;
            Minecraft.getInstance().font = new EmojiFontRenderer(Minecraft.getInstance().font);
            Minecraft.getInstance().getEntityRenderDispatcher().font = Minecraft.getInstance().font;
            BlockEntityRenderers.register(BlockEntityType.SIGN, p_173571_ -> {
                SignRenderer signRenderer = new SignRenderer(p_173571_);
                signRenderer.font = Minecraft.getInstance().font;
                return signRenderer;
            });
        }
    }
}

