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
import net.minecraft.network.chat.FormattedText;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
    public int width(String text) {
        if (text != null) {
            try {
                text = RECENT_STRINGS.get(text).getKey();
            } catch (Exception e) {
                Constants.LOG.error("Error getting emoji", e);
            }
        }
        return super.width(text);
    }

    @Override
    public int width(FormattedText textProperties) {
        return this.width(textProperties.getString());
    }

    @Override
    public int width(FormattedCharSequence processor) {
        StringBuilder builder = new StringBuilder();
        processor.accept((p_accept_1_, p_accept_2_, ch) -> {
            builder.append((char) ch);
            return true;
        });
        return width(builder.toString());
    }

//    @Override
//        public int drawInBatch(String p_228079_1_, float p_228079_2_, float p_228079_3_, int p_228079_4_, boolean p_228079_5_, Matrix4f p_228079_6_, MultiBufferSource p_228079_7_, DisplayMode p_228079_8_, int p_228079_9_, int p_228079_10_) {
//        return super.drawInBatch(p_228079_1_, p_228079_2_, p_228079_3_, p_228079_4_, p_228079_5_, p_228079_6_, p_228079_7_, p_228079_8_, p_228079_9_, p_228079_10_);
//
//    }

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
        final MultiBufferSource buffer;
        private final boolean dropShadow;
        private final float dimFactor;
        private final float r;
        private final float g;
        private final float b;
        private final float a;
        private final Matrix4f matrix;
        private final DisplayMode displayMode;
        private final int packedLight;
        private float x;
        private final float y;
        private final HashMap<Integer, Emoji> emojis;
        @Nullable
        private List<BakedGlyph.Effect> effects;

        public EmojiCharacterRenderer(HashMap<Integer, Emoji> emojis, MultiBufferSource p_i232250_2_, float p_i232250_3_, float p_i232250_4_, int p_i232250_5_, boolean p_i232250_6_, Matrix4f p_i232250_7_, DisplayMode p_i232250_8_, int p_i232250_9_) {
            this.buffer = p_i232250_2_;
            this.emojis = emojis;
            this.x = p_i232250_3_;
            this.y = p_i232250_4_;
            this.dropShadow = p_i232250_6_;
            this.dimFactor = p_i232250_6_ ? 0.25F : 1.0F;
            this.r = (float) (p_i232250_5_ >> 16 & 255) / 255.0F * this.dimFactor;
            this.g = (float) (p_i232250_5_ >> 8 & 255) / 255.0F * this.dimFactor;
            this.b = (float) (p_i232250_5_ & 255) / 255.0F * this.dimFactor;
            this.a = (float) (p_i232250_5_ >> 24 & 255) / 255.0F;
            this.matrix = p_i232250_7_;
            this.displayMode = p_i232250_8_;
            this.packedLight = p_i232250_9_;
        }

        private void addEffect(BakedGlyph.Effect p_238442_1_) {
            if (this.effects == null) {
                this.effects = Lists.newArrayList();
            }

            this.effects.add(p_238442_1_);
        }

        public boolean accept(int pos, Style style, int charInt) {
            FontSet font = EmojiFontRenderer.this.getFontSet(style.getFont());
            if (this.emojis.get(pos) != null) {
                Emoji emoji = this.emojis.get(pos);
                if (emoji != null && !this.dropShadow) {
                    EmojiUtil.renderEmoji(emoji, this.x, this.y, matrix, buffer, packedLight);
                    this.x += 10;
                    return true;
                }
            } else {
                GlyphInfo glyphInfo = font.getGlyphInfo(charInt, (EmojiFontRenderer.this).filterFishyGlyphs);
                BakedGlyph bakedGlyph = style.isObfuscated() && charInt != 32 ? font.getRandomGlyph(glyphInfo) : font.getGlyph(charInt);
                boolean flag = style.isBold();
                float f3 = this.a;
                TextColor color = style.getColor();
                float f;
                float f1;
                float f2;
                if (color != null) {
                    int i = color.getValue();
                    f = (float) (i >> 16 & 255) / 255.0F * this.dimFactor;
                    f1 = (float) (i >> 8 & 255) / 255.0F * this.dimFactor;
                    f2 = (float) (i & 255) / 255.0F * this.dimFactor;
                } else {
                    f = this.r;
                    f1 = this.g;
                    f2 = this.b;
                }

                if (!(bakedGlyph instanceof EmptyGlyph)) {
                    float f5 = flag ? glyphInfo.getBoldOffset() : 0.0F;
                    float f4 = this.dropShadow ? glyphInfo.getShadowOffset() : 0.0F;
                    VertexConsumer iVertexBuilder = this.buffer.getBuffer(bakedGlyph.renderType(this.displayMode));
                    (EmojiFontRenderer.this).renderChar(bakedGlyph, flag, style.isItalic(), f5, this.x + f4, this.y + f4, this.matrix, iVertexBuilder, f, f1, f2, f3, this.packedLight);
                }

                float f6 = glyphInfo.getAdvance(flag);
                float f7 = this.dropShadow ? 1.0F : 0.0F;
                if (style.isStrikethrough()) {
                    this.addEffect(new BakedGlyph.Effect(this.x + f7 - 1.0F, this.y + f7 + 4.5F, this.x + f7 + f6, this.y + f7 + 4.5F - 1.0F, 0.01F, f, f1, f2, f3));
                }

                if (style.isUnderlined()) {
                    this.addEffect(new BakedGlyph.Effect(this.x + f7 - 1.0F, this.y + f7 + 9.0F, this.x + f7 + f6, this.y + f7 + 9.0F - 1.0F, 0.01F, f, f1, f2, f3));
                }

                this.x += f6;
                return true;
            }
            return false;
        }

        public float finish(int p_238441_1_, float p_238441_2_) {
            if (p_238441_1_ != 0) {
                float f = (float) (p_238441_1_ >> 24 & 255) / 255.0F;
                float f1 = (float) (p_238441_1_ >> 16 & 255) / 255.0F;
                float f2 = (float) (p_238441_1_ >> 8 & 255) / 255.0F;
                float f3 = (float) (p_238441_1_ & 255) / 255.0F;
                this.addEffect(new BakedGlyph.Effect(p_238441_2_ - 1.0F, this.y + 9.0F, this.x + 1.0F, this.y - 1.0F, 0.01F, f1, f2, f3, f));
            }

            if (this.effects != null) {
                FontSet fontSet = (EmojiFontRenderer.this).getFontSet(Style.DEFAULT_FONT);
                BakedGlyph bakedGlyph = fontSet.whiteGlyph();
                VertexConsumer iVertexBuilder = this.buffer.getBuffer(bakedGlyph.renderType(this.displayMode));

                for (BakedGlyph.Effect texturedglyph$effect : this.effects) {
                    bakedGlyph.renderEffect(texturedglyph$effect, this.matrix, iVertexBuilder, this.packedLight);
                }
            }

            return this.x;
        }
    }

}
