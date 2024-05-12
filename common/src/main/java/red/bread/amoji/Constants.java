package red.bread.amoji;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import red.bread.amoji.api.Emoji;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constants {

    public static final String MOD_ID = "amoji";
    public static final String MOD_NAME = "Amojiful";
    public static final String EMOJI_PROVIDER_URL = "https://raw.githubusercontent.com/iamcal/emoji-data/master";
    public static final String EMOJI_DATA_URL = EMOJI_PROVIDER_URL + "/emoji.json";
    public static final String EMOJI_BASE_URL = EMOJI_PROVIDER_URL + "/img-twitter-64/";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_NAME);

    public static final Map<String, List<Emoji>> EMOJI_MAP = new HashMap<>();
    public static final List<Emoji> EMOJI_LIST = new ArrayList<>();
    public static boolean error = false;
}