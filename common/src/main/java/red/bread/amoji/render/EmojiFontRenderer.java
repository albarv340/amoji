package red.bread.amoji.render;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.font.GlyphInfo;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.netty.util.internal.StringUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import net.minecraft.client.gui.font.glyphs.EmptyGlyph;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import red.bread.amoji.Constants;
import red.bread.amoji.api.Emoji;
import red.bread.amoji.util.EmojiUtil;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiFontRenderer extends Font {

    public static final Vector3f SHADOW_OFFSET = new Vector3f(0.0F, 0.0F, 0.03F);
    public static LoadingCache<String, Pair<String, HashMap<Integer, Emoji>>> RECENT_STRINGS = CacheBuilder.newBuilder().expireAfterAccess(60, TimeUnit.SECONDS).build(new CacheLoader<>() {
        @Override
        public @NotNull Pair<String, HashMap<Integer, Emoji>> load(@NotNull String key) {
            return getEmojiFormattedString(key);
        }
    });

    public EmojiFontRenderer(Font fontRenderer) {
        super(fontRenderer.fonts, fontRenderer.filterFishyGlyphs);
    }

    public static Pair<String, HashMap<Integer, Emoji>> getEmojiFormattedString(String text) {
        HashMap<Integer, Emoji> emojis = new LinkedHashMap<>();
        if (!StringUtil.isNullOrEmpty(text)) {
            String unformattedText = ChatFormatting.stripFormatting(text);
            if (StringUtil.isNullOrEmpty(unformattedText))
                return Pair.of(text, emojis);
            for (Emoji emoji : Constants.EMOJI_LIST) {
                Pattern pattern = emoji.getRegex();
                Matcher matcher = pattern.matcher(unformattedText);
                while (matcher.find()) {
                    if (!matcher.group().isEmpty()) {
                        String emojiText = matcher.group();
                        int index = text.indexOf(emojiText);
                        emojis.put(index, emoji);
                        HashMap<Integer, Emoji> clean = new LinkedHashMap<>();
                        for (Integer integer : new ArrayList<>(emojis.keySet())) {
                            if (integer > index) {
                                Emoji e = emojis.get(integer);
                                emojis.remove(integer);
                                clean.put(integer - emojiText.length() + 1, e);
                            }
                        }
                        emojis.putAll(clean);
                    }
                }
            }
        }
        return Pair.of(text, emojis);
    }

    @Override
    public float renderText(String string, float f, float g, int i, boolean bl, Matrix4f matrix4f, MultiBufferSource multiBufferSource, DisplayMode displayMode, int j, int k, boolean bl2) {
        if (string.isEmpty())
            return 0;
        HashMap<Integer, Emoji> emojis = new LinkedHashMap<>();
        try {
            Pair<String, HashMap<Integer, Emoji>> cache = RECENT_STRINGS.get(string);
            string = cache.getLeft();
//            emojis = cache.getRight();
        } catch (ExecutionException e) {
            Constants.LOG.error("Error getting emoji from cache", e);
        }
        EmojiCharacterRenderer emojiCharacterRenderer = new EmojiCharacterRenderer(emojis, multiBufferSource, f, g, i, j, bl, matrix4f, displayMode, k, bl2);
        StringDecomposer.iterateFormatted(string, Style.EMPTY, emojiCharacterRenderer);
        return emojiCharacterRenderer.finish(f);
    }

    @Override
    public int drawInBatch(FormattedCharSequence reorderingProcessor, float x, float y, int color, boolean isShadow, Matrix4f matrix, MultiBufferSource buffer, DisplayMode displayMode, int colorBackgroundIn, int packedLight) {
        if (reorderingProcessor != null) {
            StringBuilder builder = new StringBuilder();
            reorderingProcessor.accept((p_accept_1_, p_accept_2_, ch) -> {
                builder.append((char) ch);
                return true;
            });
            String text = builder.toString();
            if (!text.isEmpty()) {
                color = adjustColor(color);
                HashMap<Integer, Emoji> emojis = new LinkedHashMap<>();
                try {
                    Pair<String, HashMap<Integer, Emoji>> cache = RECENT_STRINGS.get(text);
                    emojis = cache.getRight();
                } catch (ExecutionException e) {
                    Constants.LOG.error("Error getting emoji from cache", e);
                }
                List<FormattedCharSequence> processors = new ArrayList<>();
                HashMap<Integer, Emoji> finalEmojis = emojis;
                AtomicInteger cleanPos = new AtomicInteger();
                AtomicBoolean ignore = new AtomicBoolean(false);
                reorderingProcessor.accept((pos, style, ch) -> {
                    if (!ignore.get()) {
                        if (finalEmojis.get(cleanPos.get()) == null) {
                            processors.add(new CharacterProcessor(cleanPos.getAndIncrement(), style, ch));
                        } else {
                            processors.add(new CharacterProcessor(cleanPos.get(), style, ' '));
                            ignore.set(true);
                            return true;
                        }
                    }
                    if (ignore.get() && ch == ':') {
                        ignore.set(false);
                        cleanPos.getAndIncrement();
                    }
                    return true;
                });
//                StringBuilder builder2 = new StringBuilder();
//                FormattedCharSequence.fromList(processors).accept((p_accept_1_, p_accept_2_, ch) -> {
//                    builder2.append((char) ch);
//                    return true;
//                });
                Matrix4f matrix4f = new Matrix4f(matrix);
                if (isShadow) {
                    EmojiCharacterRenderer emojiCharacterRenderer = new EmojiCharacterRenderer(emojis, buffer, x, y, color, true, matrix, displayMode, packedLight);
                    FormattedCharSequence.fromList(processors).accept(emojiCharacterRenderer);
                    emojiCharacterRenderer.finish(x);
                    matrix4f.translate(SHADOW_OFFSET);
                }
                EmojiCharacterRenderer emojiCharacterRenderer = new EmojiCharacterRenderer(emojis, buffer, x, y, color, false, matrix4f, displayMode, packedLight);
                FormattedCharSequence.fromList(processors).accept(emojiCharacterRenderer);
                return (int) emojiCharacterRenderer.finish(x);
            }
        }
        return super.drawInBatch(reorderingProcessor, x, y, color, isShadow, matrix, buffer, displayMode, colorBackgroundIn, packedLight);
    }

//    @Override
//        public int drawInBatch(FormattedCharSequence formattedCharSequence, float f, float g, int color, boolean isShadow, Matrix4f matrix4f, MultiBufferSource multiBufferSource, DisplayMode displayMode, int colorBackgroundIn, int k) {
////    public void drawInBatch8xOutline(FormattedCharSequence formattedCharSequence, float f, float g, int i, int j, Matrix4f matrix4f, MultiBufferSource multiBufferSource, int k) {
//        int l = adjustColor(color);
//        HashMap<Integer, Emoji> emojis = new LinkedHashMap<>();
//        try {
//            Pair<String, HashMap<Integer, Emoji>> cache = RECENT_STRINGS.get(formattedCharSequence.toString());
//            emojis = cache.getRight();
//        } catch (ExecutionException e) {
//            Constants.LOG.error("Error getting emoji from cache", e);
//        }
//        EmojiCharacterRenderer stringRenderOutput = new EmojiCharacterRenderer(emojis, multiBufferSource, 0.0F, 0.0F, l, false, matrix4f, Font.DisplayMode.NORMAL, k);
//
//        for (int m = -1; m <= 1; ++m) {
//            for (int n = -1; n <= 1; ++n) {
//                if (m != 0 || n != 0) {
//                    float[] fs = new float[]{f};
//                    int finalN = n;
//                    formattedCharSequence.accept((lx, style, o) -> {
//                        boolean bl = style.isBold();
//                        FontSet fontSet = this.getFontSet(style.getFont());
//                        GlyphInfo glyphInfo = fontSet.getGlyphInfo(o, this.filterFishyGlyphs);
//                        stringRenderOutput.x = fs[0] + (float) o * glyphInfo.getShadowOffset();
//                        stringRenderOutput.y = g + (float) finalN * glyphInfo.getShadowOffset();
//                        fs[0] += glyphInfo.getAdvance(bl);
//                        return stringRenderOutput.accept(lx, style.withColor(l), o);
//                    });
//                }
//            }
//        }
//
//        stringRenderOutput.renderCharacters();
//        EmojiCharacterRenderer stringRenderOutput2 = new EmojiCharacterRenderer(emojis, multiBufferSource, f, g, adjustColor(color), false, matrix4f, Font.DisplayMode.POLYGON_OFFSET, k);
//        formattedCharSequence.accept(stringRenderOutput2);
//        stringRenderOutput2.finish(f);
//        return super.drawInBatch(formattedCharSequence, f, g, color, isShadow, matrix4f, multiBufferSource, displayMode, colorBackgroundIn, k);
//    }

    private static int adjustColor(int i) {
        return (i & -67108864) == 0 ? ARGB.opaque(i) : i;
    }

    record CharacterProcessor(int pos, Style style, int character) implements FormattedCharSequence {

        @Override
        public boolean accept(FormattedCharSink iCharacterConsumer) {
            return iCharacterConsumer.accept(pos, style, character);
        }
    }

    class EmojiCharacterRenderer implements FormattedCharSink {
        final MultiBufferSource bufferSource;
        private final boolean drawShadow;
        private final int color;
        private final int backgroundColor;
        private final Matrix4f pose;
        private final DisplayMode mode;
        private final int packedLightCoords;
        private final boolean inverseDepth;
        float x;
        float y;
        private final List<BakedGlyph.GlyphInstance> glyphInstances;
        @Nullable
        private List<BakedGlyph.Effect> effects;
        private final HashMap<Integer, Emoji> emojis;

        private void addEffect(BakedGlyph.Effect effect) {
            if (this.effects == null) {
                this.effects = Lists.newArrayList();
            }

            this.effects.add(effect);
        }

        public EmojiCharacterRenderer(HashMap<Integer, Emoji> emojis, final MultiBufferSource arg2, final float f, final float g, final int i, final boolean bl, final Matrix4f matrix4f, final DisplayMode arg3, final int j) {
            this(emojis, arg2, f, g, i, 0, bl, matrix4f, arg3, j, true);
        }

        public EmojiCharacterRenderer(HashMap<Integer, Emoji> emojis, final MultiBufferSource multiBufferSource, final float f, final float g, final int i, final int j, final boolean bl, final Matrix4f matrix4f, final DisplayMode displayMode, final int k, final boolean bl2) {
            this.emojis = emojis;
            this.glyphInstances = new ArrayList();
            this.bufferSource = multiBufferSource;
            this.x = f;
            this.y = g;
            this.drawShadow = bl;
            this.color = i;
            this.backgroundColor = j;
            this.pose = matrix4f;
            this.mode = displayMode;
            this.packedLightCoords = k;
            this.inverseDepth = bl2;
        }

        public boolean accept(int i, Style style, int j) {
            FontSet fontSet = EmojiFontRenderer.this.getFontSet(style.getFont());
            if (this.emojis.get(i) != null) {
                Emoji emoji = this.emojis.get(i);
                if (emoji != null) {
                    EmojiUtil.renderEmoji(emoji, this.x, this.y, pose, bufferSource, packedLightCoords);
                    this.x += 10;
                    return true;
                }
            } else {
                GlyphInfo glyphInfo = fontSet.getGlyphInfo(j, EmojiFontRenderer.this.filterFishyGlyphs);
                BakedGlyph bakedGlyph = style.isObfuscated() && j != 32 ? fontSet.getRandomGlyph(glyphInfo) : fontSet.getGlyph(j);
                boolean bl = style.isBold();
                TextColor textColor = style.getColor();
                int k = this.getTextColor(textColor);
                int l = this.getShadowColor(style, k);
                float f = glyphInfo.getAdvance(bl);
                float g = i == 0 ? this.x - 1.0F : this.x;
                float h = glyphInfo.getShadowOffset();
                if (!(bakedGlyph instanceof EmptyGlyph)) {
                    float m = bl ? glyphInfo.getBoldOffset() : 0.0F;
                    this.glyphInstances.add(new BakedGlyph.GlyphInstance(this.x, this.y, k, l, bakedGlyph, style, m, h));
                }

                if (style.isStrikethrough()) {
                    this.addEffect(new BakedGlyph.Effect(g, this.y + 4.5F, this.x + f, this.y + 4.5F - 1.0F, this.getOverTextEffectDepth(), k, l, h));
                }

                if (style.isUnderlined()) {
                    this.addEffect(new BakedGlyph.Effect(g, this.y + 9.0F, this.x + f, this.y + 9.0F - 1.0F, this.getOverTextEffectDepth(), k, l, h));
                }

                this.x += f;
                return true;
            }
            return false;
        }

        public float finish(float f) {
            BakedGlyph bakedGlyph = null;
            if (this.backgroundColor != 0) {
                BakedGlyph.Effect effect = new BakedGlyph.Effect(f - 1.0F, this.y + 9.0F, this.x, this.y - 1.0F, this.getUnderTextEffectDepth(), this.backgroundColor);
                bakedGlyph = EmojiFontRenderer.this.getFontSet(Style.DEFAULT_FONT).whiteGlyph();
                VertexConsumer vertexConsumer = this.bufferSource.getBuffer(bakedGlyph.renderType(this.mode));
                bakedGlyph.renderEffect(effect, this.pose, vertexConsumer, this.packedLightCoords);
            }

            this.renderCharacters();
            if (this.effects != null) {
                if (bakedGlyph == null) {
                    bakedGlyph = EmojiFontRenderer.this.getFontSet(Style.DEFAULT_FONT).whiteGlyph();
                }

                VertexConsumer vertexConsumer2 = this.bufferSource.getBuffer(bakedGlyph.renderType(this.mode));

                for (BakedGlyph.Effect effect2 : this.effects) {
                    bakedGlyph.renderEffect(effect2, this.pose, vertexConsumer2, this.packedLightCoords);
                }
            }

            return this.x;
        }

        private int getTextColor(@Nullable TextColor textColor) {
            if (textColor != null) {
                int i = ARGB.alpha(this.color);
                int j = textColor.getValue();
                return ARGB.color(i, j);
            } else {
                return this.color;
            }
        }

        private int getShadowColor(Style style, int i) {
            Integer integer = style.getShadowColor();
            if (integer != null) {
                float f = ARGB.alphaFloat(i);
                float g = ARGB.alphaFloat(integer);
                return f != 1.0F ? ARGB.color(ARGB.as8BitChannel(f * g), integer) : integer;
            } else {
                return this.drawShadow ? ARGB.scaleRGB(i, 0.25F) : 0;
            }
        }

        void renderCharacters() {
            for (BakedGlyph.GlyphInstance glyphInstance : this.glyphInstances) {
                BakedGlyph bakedGlyph = glyphInstance.glyph();
                VertexConsumer vertexConsumer = this.bufferSource.getBuffer(bakedGlyph.renderType(this.mode));
                bakedGlyph.renderChar(glyphInstance, this.pose, vertexConsumer, this.packedLightCoords);
            }

        }

        private float getOverTextEffectDepth() {
            return this.inverseDepth ? 0.01F : -0.01F;
        }

        private float getUnderTextEffectDepth() {
            return this.inverseDepth ? -0.01F : 0.01F;
        }
    }

}
