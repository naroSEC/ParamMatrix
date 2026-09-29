package parammatrix.discovery;

import parammatrix.model.DiscoveryConfidence;
import parammatrix.model.ParameterSource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class UrlParameterExtractor {
    List<DiscoveredParameter> extract(String value, ParameterSource source, String detail) {
        List<DiscoveredParameter> results = new ArrayList<>();
        if (value == null) return results;
        int question = value.indexOf('?');
        if (question < 0 || question == value.length() - 1) return results;
        String endpoint = value.substring(0, question);
        String query = value.substring(question + 1);
        int fragment = query.indexOf('#');
        if (fragment >= 0) query = query.substring(0, fragment);
        for (String part : query.split("[&;]")) {
            int equals = part.indexOf('=');
            String rawName = equals >= 0 ? part.substring(0, equals) : part;
            String name = URLDecoder.decode(rawName, StandardCharsets.UTF_8).trim();
            if (isReasonableName(name)) {
                results.add(new DiscoveredParameter(name, source, detail, endpoint,
                        DiscoveryConfidence.HIGH));
            }
        }
        return results;
    }

    static boolean isReasonableName(String name) {
        return !name.isBlank() && name.length() <= 128
                && name.matches("[A-Za-z_][A-Za-z0-9_.\\[\\]-]*");
    }
}

