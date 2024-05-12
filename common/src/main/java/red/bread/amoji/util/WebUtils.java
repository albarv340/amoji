package red.bread.amoji.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import red.bread.amoji.Constants;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class WebUtils {
    public static String readStringFromURL(String requestURL) {
        try {
            try (Scanner scanner = new Scanner(new URL(requestURL).openStream(), StandardCharsets.UTF_8)) {
                scanner.useDelimiter("\\A");
                return scanner.hasNext() ? scanner.next() : "";
            }
        } catch (IOException e) {
            Constants.LOG.error("", e);
        }
        return "";
    }

    public static JsonElement readJsonFromUrl(String url) {
        String jsonText = readStringFromURL(url);
        return JsonParser.parseString(jsonText);
    }

    public static boolean isValidUrl(String url) {
        try {
            new URL(url);
            return true;
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
