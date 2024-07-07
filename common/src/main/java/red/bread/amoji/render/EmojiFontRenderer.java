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
                        unformattedText = unformattedText.replaceFirst(Pattern.quote(emojiText), "☃");
                        text = text.replaceFirst("(?i)" + Pattern.quote(emojiText), "☃");
                    }
                }
            }
        }
        return Pair.of(text, emojis);
    }

    @Override
    public float renderText(String text, float x, float y, int color, boolean isShadow, Matrix4f matrix, MultiBufferSource buffer, DisplayMode displayMode, int colorBackgroundIn, int packedLight) {
        if (text.isEmpty())
            return 0;
        HashMap<Integer, Emoji> emojis = new LinkedHashMap<>();
        try {
            Pair<String, HashMap<Integer, Emoji>> cache = RECENT_STRINGS.get(text);
            text = cache.getLeft();
            emojis = cache.getRight();
        } catch (ExecutionException e) {
            Constants.LOG.error("Error getting emoji from cache", e);
        }
        EmojiCharacterRenderer emojiCharacterRenderer = new EmojiCharacterRenderer(emojis, buffer, x, y, color, isShadow, matrix, displayMode, packedLight);
        StringDecomposer.iterateFormatted(text, Style.EMPTY, emojiCharacterRenderer);
        return emojiCharacterRenderer.finish(colorBackgroundIn, x);
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
                color = (color & -67108864) == 0 ? color | -16777216 : color;
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
                StringBuilder builder2 = new StringBuilder();
                FormattedCharSequence.fromList(processors).accept((p_accept_1_, p_accept_2_, ch) -> {
                    builder2.append((char) ch);
                    return true;
                });
                Matrix4f matrix4f = new Matrix4f(matrix);
                if (isShadow) {
                    EmojiCharacterRenderer emojiCharacterRenderer = new EmojiCharacterRenderer(emojis, buffer, x, y, color, true, matrix, displayMode, packedLight);
                    FormattedCharSequence.fromList(processors).accept(emojiCharacterRenderer);
                    emojiCharacterRenderer.finish(colorBackgroundIn, x);
                    matrix4f.translate(SHADOW_OFFSET);
                }
                EmojiCharacterRenderer emojiCharacterRenderer = new EmojiCharacterRenderer(emojis, buffer, x, y, color, false, matrix4f, displayMode, packedLight);
                FormattedCharSequence.fromList(processors).accept(emojiCharacterRenderer);
                return (int) emojiCharacterRenderer.finish(colorBackgroundIn, x);
            }
        }
        return super.drawInBatch(reorderingProcessor, x, y, color, isShadow, matrix, buffer, displayMode, colorBackgroundIn, packedLight);
    }

    record CharacterProcessor(int pos, Style style, int character) implements FormattedCharSequence {

        @Override
            public boolean accept(FormattedCharSink iCharacterConsumer) {
                return iCharacterConsumer.accept(pos, style, character);
            }
        }

    class EmojiCharacterRenderer implements FormattedCharSink {
        final MultiBufferSource bufferSource;
        private final boolean dropShadow;
        private final float dimFactor;
        private final float r;
        private final float g;
        private final float b;
        private final float a;
        private final Matrix4f pose;
        private final DisplayMode mode;
        private final int packedLightCoords;
        float x;
        float y;
        @Nullable
        private List<BakedGlyph.Effect> effects;
        private final HashMap<Integer, Emoji> emojis;

        private void addEffect(BakedGlyph.Effect effect) {
            if (this.effects == null) {
                this.effects = Lists.newArrayList();
            }

            this.effects.add(effect);
        }

        public EmojiCharacterRenderer(HashMap<Integer, Emoji> emojis, final MultiBufferSource multiBufferSource, final float f, final float g, final int i, final boolean bl, final Matrix4f matrix4f, final DisplayMode displayMode, final int j) {
            this.bufferSource = multiBufferSource;
            this.emojis = emojis;
            this.x = f;
            this.y = g;
            this.dropShadow = bl;
            this.dimFactor = bl ? 0.25F : 1.0F;
            this.r = (float)(i >> 16 & 255) / 255.0F * this.dimFactor;
            this.g = (float)(i >> 8 & 255) / 255.0F * this.dimFactor;
            this.b = (float)(i & 255) / 255.0F * this.dimFactor;
            this.a = (float)(i >> 24 & 255) / 255.0F;
            this.pose = matrix4f;
            this.mode = displayMode;
            this.packedLightCoords = j;
        }

        public boolean accept(int i, Style style, int j) {
            FontSet fontSet = EmojiFontRenderer.this.getFontSet(style.getFont());
            if (this.emojis.get(i) != null) {
                Emoji emoji = this.emojis.get(i);
                if (emoji != null && !this.dropShadow) {
                    EmojiUtil.renderEmoji(emoji, this.x, this.y, pose, bufferSource, packedLightCoords);
                    this.x += 10;
                    return true;
                }
            } else {
                GlyphInfo glyphInfo = fontSet.getGlyphInfo(j, EmojiFontRenderer.this.filterFishyGlyphs);
                BakedGlyph bakedGlyph = style.isObfuscated() && j != 32 ? fontSet.getRandomGlyph(glyphInfo) : fontSet.getGlyph(j);
                boolean bl = style.isBold();
                float f = this.a;
                TextColor textColor = style.getColor();
                float g;
                float h;
                float l;
                if (textColor != null) {
                    int k = textColor.getValue();
                    g = (float) (k >> 16 & 255) / 255.0F * this.dimFactor;
                    h = (float) (k >> 8 & 255) / 255.0F * this.dimFactor;
                    l = (float) (k & 255) / 255.0F * this.dimFactor;
                } else {
                    g = this.r;
                    h = this.g;
                    l = this.b;
                }

                float n;
                float m;
                if (!(bakedGlyph instanceof EmptyGlyph)) {
                    m = bl ? glyphInfo.getBoldOffset() : 0.0F;
                    n = this.dropShadow ? glyphInfo.getShadowOffset() : 0.0F;
                    VertexConsumer vertexConsumer = this.bufferSource.getBuffer(bakedGlyph.renderType(this.mode));
                    EmojiFontRenderer.this.renderChar(bakedGlyph, bl, style.isItalic(), m, this.x + n, this.y + n, this.pose, vertexConsumer, g, h, l, f, this.packedLightCoords);
                }

                m = glyphInfo.getAdvance(bl);
                n = this.dropShadow ? 1.0F : 0.0F;
                if (style.isStrikethrough()) {
                    this.addEffect(new BakedGlyph.Effect(this.x + n - 1.0F, this.y + n + 4.5F, this.x + n + m, this.y + n + 4.5F - 1.0F, 0.01F, g, h, l, f));
                }

                if (style.isUnderlined()) {
                    this.addEffect(new BakedGlyph.Effect(this.x + n - 1.0F, this.y + n + 9.0F, this.x + n + m, this.y + n + 9.0F - 1.0F, 0.01F, g, h, l, f));
                }

                this.x += m;
                return true;
            }
            return false;
        }

        public float finish(int i, float f) {
            if (i != 0) {
                float g = (float)(i >> 24 & 255) / 255.0F;
                float h = (float)(i >> 16 & 255) / 255.0F;
                float j = (float)(i >> 8 & 255) / 255.0F;
                float k = (float)(i & 255) / 255.0F;
                this.addEffect(new BakedGlyph.Effect(f - 1.0F, this.y + 9.0F, this.x + 1.0F, this.y - 1.0F, 0.01F, h, j, k, g));
            }

            if (this.effects != null) {
                BakedGlyph bakedGlyph = EmojiFontRenderer.this.getFontSet(Style.DEFAULT_FONT).whiteGlyph();
                VertexConsumer vertexConsumer = this.bufferSource.getBuffer(bakedGlyph.renderType(this.mode));

                for (BakedGlyph.Effect effect : this.effects) {
                    bakedGlyph.renderEffect(effect, this.pose, vertexConsumer, this.packedLightCoords);
                }
            }

            return this.x;
        }
    }

}
