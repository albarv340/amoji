package red.bread.amoji.util;

import com.google.gson.JsonElement;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.apache.commons.lang3.tuple.Pair;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.w3c.dom.Node;
import red.bread.amoji.Constants;
import red.bread.amoji.api.Emoji;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_NEAREST;

public class EmojiUtil extends RenderType {

    private EmojiUtil(String string, VertexFormat vertexFormat, VertexFormat.Mode mode, int i, boolean bl, boolean bl2, Runnable runnable, Runnable runnable2) {
        super(string, vertexFormat, mode, i, bl, bl2, runnable, runnable2);
    }

    public static RenderType createRenderType(Emoji emoji) {
        CompositeState state = CompositeState.builder().setShaderState(new ShaderStateShard(GameRenderer::getRendertypeTextShader)).setTextureState(new TextureStateShard(emoji.getResourceLocationForBinding(), false, false)).setTransparencyState(new TransparencyStateShard("translucent_transparency", () -> {
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        }, () -> {
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
        }))/*.setAlphaState(new RenderStateShard.AlphaStateShard(0.003921569F))*/.setLightmapState(new LightmapStateShard(true)).createCompositeState(false);
        return RenderType.create("emoji_render", DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP, VertexFormat.Mode.QUADS, 256, false, true, state);
    }

    public static float renderEmoji(Emoji emoji, float x, float y, Matrix4f matrix, MultiBufferSource buffer, int packedLight) {
        float textureSize = 16;
        float textureX = 0 / textureSize;
        float textureY = 0 / textureSize;
        float textureOffset = 16.0F / textureSize;
        float size = 10f;
        float offsetY = 1.0F;
        float offsetX = 0.0F;

        VertexConsumer builder = buffer.getBuffer(createRenderType(emoji));

        builder.addVertex(matrix, x - offsetX, y - offsetY, 0.0f).setColor(255, 255, 255, 255).setUv(textureX, textureY).setUv2(packedLight, 127);
        builder.addVertex(matrix, x - offsetX, y + size - offsetY, 0.0F).setColor(255, 255, 255, 255).setUv(textureX, textureY + textureOffset).setUv2(packedLight, 127);
        builder.addVertex(matrix, x - offsetX + size, y + size - offsetY, 0.0F).setColor(255, 255, 255, 255).setUv(textureX + textureOffset, textureY + textureOffset).setUv2(packedLight, 127);
        builder.addVertex(matrix, x - offsetX + size, y - offsetY, 0.0F).setColor(255, 255, 255, 255).setUv(textureX + textureOffset, textureY / textureSize).setUv2(packedLight, 127);

        return 10f;
    }

    public static String cleanStringForRegex(String string) {
        return string.replaceAll("\\)", "\\\\)").replaceAll("\\(", "\\\\(").replaceAll("\\|", "\\\\|").replaceAll("\\*", "\\\\*");
    }

    public static List<Pair<BufferedImage, Integer>> splitGif(File file) throws IOException {
        List<Pair<BufferedImage, Integer>> images = new ArrayList<>();
        ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
        reader.setInput(ImageIO.createImageInputStream(new FileInputStream(file)), false);
        IIOMetadata metadata = reader.getImageMetadata(0);
        String metaFormatName = metadata.getNativeMetadataFormatName();
        for (int i = 0; i < reader.getNumImages(true); i++) {
            int frameLength = 1;
            BufferedImage image = reader.read(i);
            BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
            newImage.getGraphics().drawImage(image, 0, 0, null);
            IIOMetadataNode root = (IIOMetadataNode) reader.getImageMetadata(i).getAsTree(metaFormatName);
            // Find GraphicControlExtension node
            int nNodes = root.getLength();
            for (int j = 0; j < nNodes; j++) {
                Node node = root.item(j);
                if (node.getNodeName().equalsIgnoreCase("GraphicControlExtension")) {
                    // Get delay value
                    frameLength = Integer.parseInt(((IIOMetadataNode) node).getAttribute("delayTime"));
                    // Check if delay is bugged
                    break;
                }
            }
            images.add(Pair.of(newImage, frameLength));
        }
        return images;
    }

    public static boolean isBaseEmoji(JsonElement element) {
        // Base emojis are loaded as the twitter emojis
        return element.getAsJsonObject().get("has_img_twitter").getAsBoolean();
    }

    public static Collection<String> getEmojiSuggestions() {
        List<String> emojis = new ArrayList<>();
        for (Emoji emoji : Constants.EMOJI_LIST) {
            emojis.addAll(emoji.strings);
        }
        return emojis;
    }
}
