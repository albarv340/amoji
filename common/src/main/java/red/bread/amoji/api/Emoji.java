package red.bread.amoji.api;

import com.mojang.blaze3d.pipeline.RenderCall;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import red.bread.amoji.Constants;
import red.bread.amoji.util.EmojiUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Emoji implements Predicate<String> {
    private static final ResourceLocation loading_texture = new ResourceLocation(Constants.MOD_ID, "textures/warning.png");
    private static final ResourceLocation noSignal_texture = new ResourceLocation(Constants.MOD_ID, "textures/error.png");
    private static final ResourceLocation error_texture = new ResourceLocation(Constants.MOD_ID, "textures/error.png");

    private static final AtomicInteger threadDownloadCounter = new AtomicInteger(0);
    public String name;
    public List<String> strings = new ArrayList<>();
    public String url;
    public int version = 1;
    public int sort = 0;
    private boolean deleteOldTexture;
    private final List<DownloadImageData> img = new ArrayList<>();
    private List<ResourceLocation> frames = new ArrayList<>();
    private boolean finishedLoading = false;
    private boolean loadedTextures = false;
    private String regex;
    private Pattern regexPattern;
    private Thread imageThread;
    private Thread gifLoaderThread;

    public void checkLoad() {
        if (imageThread == null && !finishedLoading) {
            loadImage();
        } else if (!loadedTextures) {
            loadedTextures = true;
        }
    }

    public ResourceLocation getResourceLocationForBinding() {
        checkLoad();
        if (deleteOldTexture) {
            img.forEach(AbstractTexture::releaseId);
            deleteOldTexture = false;
        }
        return finishedLoading && !frames.isEmpty() ? frames.get((int) (System.currentTimeMillis() / 10D % frames.size())) : loading_texture;
    }

    @Override
    public boolean test(String s) {
        for (String text : strings)
            if (s.equalsIgnoreCase(text)) return true;
        return false;
    }

    public Pattern getRegex() {
        if (regexPattern != null) return regexPattern;
        regexPattern = Pattern.compile(getRegexString());
        return regexPattern;
    }

    public String getRegexString() {
        if (regex != null) return regex;
        List<String> processed = new ArrayList<>();
        for (String string : strings) {
            char last = string.toLowerCase().charAt(string.length() - 1);
            String s = string;
            if (last >= 97 && last <= 122) {
                s = string + "\\b";
            }
            char first = string.toLowerCase().charAt(0);
            if (first >= 97 && first <= 122) {
                s = "\\b" + s;
            }
            processed.add(EmojiUtil.cleanStringForRegex(s));
        }
        regex = String.join("|", processed);
        return regex;
    }

    private void loadImage() {
        File cache = getCache();
        if (cache.exists()) {
            if (getUrl().endsWith(".gif")) {
                if (gifLoaderThread == null) {
                    gifLoaderThread = new Thread("Amoji Texture Downloader #" + threadDownloadCounter.incrementAndGet()) {
                        @Override
                        public void run() {
                            try {
                                loadTextureFrames(EmojiUtil.splitGif(cache));
                            } catch (IOException e) {
                                Constants.LOG.error("Splitting of gif failed", e);
                            }
                        }
                    };
                    this.gifLoaderThread.setDaemon(true);
                    this.gifLoaderThread.start();
                }
            } else {
                try {
                    DownloadImageData imageData = new DownloadImageData(ImageIO.read(cache), loading_texture);
                    ResourceLocation resourceLocation = new ResourceLocation(Constants.MOD_ID, "texures/emoji/" + name.toLowerCase().replaceAll("[^a-z0-9/._-]", "") + "_" + version);
                    Minecraft.getInstance().getTextureManager().register(resourceLocation, imageData);
                    img.add(imageData);
                    frames.add(resourceLocation);
                    this.finishedLoading = true;
                } catch (IOException e) {
                    Constants.LOG.error("Loading emoji " + name + " failed", e);
                }
            }
        } else if (this.imageThread == null) {
            loadTextureFromServer();
        }
    }

    public String getUrl() {
        return url;
    }

    public File getCache() {
        return new File("amoji/cache/" + name + "-" + version);
    }

    public void loadTextureFrames(List<Pair<BufferedImage, Integer>> framesPair) {
        Minecraft.getInstance().executeBlocking(() -> {
            int i = 0;
            for (Pair<BufferedImage, Integer> bufferedImage : framesPair) {
                DownloadImageData imageData = new DownloadImageData(bufferedImage.getKey(), loading_texture);
                ResourceLocation resourceLocation = new ResourceLocation(Constants.MOD_ID, "texures/emoji/" + name.toLowerCase().replaceAll("[^a-z0-9/._-]", "") + "_" + version + "_frame" + i);
                Minecraft.getInstance().getTextureManager().register(resourceLocation, imageData);
                img.add(imageData);
                for (int integer = 0; integer < bufferedImage.getValue(); integer++) {
                    frames.add(resourceLocation);
                }
                ++i;
            }
            Emoji.this.finishedLoading = true;
        });
    }

    protected void loadTextureFromServer() {
        this.imageThread = new Thread("Amoji Texture Downloader #" + threadDownloadCounter.incrementAndGet()) {
            @Override
            public void run() {
                HttpURLConnection httpurlconnection = null;
                try {
                    httpurlconnection = (HttpURLConnection) (new URL(getUrl()).openConnection(Minecraft.getInstance().getProxy()));
                    httpurlconnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
                    httpurlconnection.setDoInput(true);
                    httpurlconnection.setDoOutput(false);
                    httpurlconnection.connect();
                    if (httpurlconnection.getResponseCode() / 100 == 2) {
                        if (getCache() != null) {
                            FileUtils.copyInputStreamToFile(httpurlconnection.getInputStream(), getCache());
                        }
                        Emoji.this.finishedLoading = true;
                        loadImage();
                    } else {
                        Emoji.this.frames = new ArrayList<>();
                        Emoji.this.frames.add(noSignal_texture);
                        Emoji.this.deleteOldTexture = true;
                        Emoji.this.finishedLoading = true;
                    }
                } catch (Exception exception) {
                    Constants.LOG.error("Fetching emoji failed", exception);
                    Emoji.this.frames = new ArrayList<>();
                    Emoji.this.frames.add(error_texture);
                    Emoji.this.deleteOldTexture = true;
                    Emoji.this.finishedLoading = true;
                } finally {
                    if (httpurlconnection != null) {
                        httpurlconnection.disconnect();
                    }
                }
            }
        };
        this.imageThread.setDaemon(true);
        this.imageThread.start();
    }

    public static class DownloadImageData extends SimpleTexture {
        private final BufferedImage cacheFile;
        public boolean textureUploaded;

        public DownloadImageData(BufferedImage cacheFileIn, ResourceLocation textureResourceLocation) {
            super(textureResourceLocation);
            this.cacheFile = cacheFileIn;
        }

        private void setImage(NativeImage nativeImageIn) {
            Minecraft.getInstance().execute(() -> {
                this.textureUploaded = true;
                if (!RenderSystem.isOnRenderThread()) {
                    RenderSystem.recordRenderCall(() -> DownloadImageData.this.upload(nativeImageIn));
                } else {
                    this.upload(nativeImageIn);
                }

            });
        }

        private void upload(NativeImage imageIn) {
            TextureUtil.prepareImage(this.getId(), imageIn.getWidth(), imageIn.getHeight());
            imageIn.upload(0, 0, 0, true);
        }

        @Nullable
        private NativeImage loadTexture(InputStream inputStreamIn) {
            NativeImage nativeimage = null;
            try {
                nativeimage = NativeImage.read(inputStreamIn);
            } catch (IOException ioexception) {
                Constants.LOG.warn("Error while loading the skin texture", ioexception);
            }
            return nativeimage;
        }

        @Override
        public void load(ResourceManager resourceManager) throws RuntimeException {
            if (this.cacheFile == null) {
                return;
            }
            new Thread(() -> {
                try {
                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    ImageIO.write(this.cacheFile, "png", os);
                    InputStream is = new ByteArrayInputStream(os.toByteArray());
                    setImage(this.loadTexture(is));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }

    }
}
