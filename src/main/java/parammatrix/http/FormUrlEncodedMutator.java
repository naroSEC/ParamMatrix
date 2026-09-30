package parammatrix.http;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

final class FormUrlEncodedMutator {
    private FormUrlEncodedMutator() {
    }

    static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    static String mutate(String source, String parameterName, String wireName, String wireValue) {
        String input = source == null ? "" : source;
        String[] fields = input.split("&", -1);
        boolean replaced = false;
        StringBuilder result = new StringBuilder(input.length() + wireName.length()
                + wireValue.length() + 2);
        for (String field : fields) {
            if (field.isEmpty() && input.isEmpty()) continue;
            if (result.length() > 0) result.append('&');
            int separator = field.indexOf('=');
            String rawName = separator < 0 ? field : field.substring(0, separator);
            if (!replaced && decoded(rawName).equals(parameterName)) {
                result.append(rawName).append('=').append(wireValue);
                replaced = true;
            } else {
                result.append(field);
            }
        }
        if (!replaced) {
            if (result.length() > 0) result.append('&');
            result.append(wireName).append('=').append(wireValue);
        }
        return result.toString();
    }

    private static String decoded(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException exception) {
            return value;
        }
    }
}
