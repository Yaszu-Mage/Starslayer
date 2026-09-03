package org.starSlayer;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;
import java.io.Reader;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class profanityFilter {
    private static final Set<String> blackList = new HashSet<>();
    private static final Pattern ZERO_WIDTH =
            Pattern.compile("[\\u200B-\\u200D\\u200E\\u200F\\u00AD\\uFEFF\\u2060\\u2062-\\u2064]");
    private static final Pattern COMBINING_MARKS = Pattern.compile("\\p{M}");
    private static final Pattern SEPARATORS = Pattern.compile("[^\\p{L}\\p{N}]+");
    public profanityFilter() {
        try {
            Reader reader = new FileReader("bannedWords.json");
            Gson gson = new Gson();
            List bannedWords = gson.fromJson(reader, List.class);
            blackList.addAll(bannedWords);
        } catch (Exception _) {}
    }
    public static String filter(String message) {
        return "";


    }

    public static boolean isProfane(String message) {
        if (message == null || message.isBlank()) return false;
        for (String word : blackList) {
            message = Normalizer.normalize(message, Normalizer.Form.NFKD);
            String regex = "\\b" + Pattern.quote(word) + "\\b";
            Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
            if (pattern.matcher(message).find()) {
                return true;
            }
        }
        return false;

    }

}
