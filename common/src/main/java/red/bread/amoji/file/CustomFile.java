package red.bread.amoji.file;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import net.minecraft.client.Minecraft;
import red.bread.amoji.Constants;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class CustomFile extends File {
    public CustomFile(String child) {
        this(Minecraft.getInstance().gameDirectory, child);
    }

    public CustomFile(File parent, String child) {
        super(parent, child);

        if (!super.exists()) {
            Constants.LOG.info("Creating file " + super.getAbsolutePath() + " as it did not exist");
            try {
                File file = new File(child);
                boolean success = file.getParentFile().mkdirs();
                success = success && super.createNewFile();
                if (!success) {
                    Constants.LOG.info("Didn't create file");
                }
                this.writeJson("{}");
            } catch (Exception e) {
               Constants.LOG.error("Error creating CustomFile", e);
            }
        }
    }

    public JsonObject readJson() {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(this), StandardCharsets.UTF_8); JsonReader jsonReader = new JsonReader(reader)) {
            jsonReader.setLenient(true);
            return new Gson().fromJson(jsonReader, JsonObject.class);
        } catch (Exception e) {
            Constants.LOG.error("Error reading json", e);
            this.writeJson("{}");
            return new Gson().fromJson("{}", JsonObject.class);
        }
    }

    public void writeJson(String text) {
        try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(this), StandardCharsets.UTF_8)) {
            fileWriter.write(text, 0, text.length());
        } catch (Exception e) {
            Constants.LOG.error("Error writing json", e);
        }
    }
}
