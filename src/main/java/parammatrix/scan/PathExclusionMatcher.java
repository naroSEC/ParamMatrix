package parammatrix.scan;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public final class PathExclusionMatcher {
    private final List<Pattern> patterns;

    public PathExclusionMatcher(List<String> rules) {
        patterns = new ArrayList<>();
        for (String raw : rules) {
            String rule = raw == null ? "" : raw.trim();
            if (rule.isEmpty() || rule.startsWith("#")) continue;
            try {
                patterns.add(compile(rule));
            } catch (PatternSyntaxException ignored) {
                // Invalid user rules are ignored; the UI documents the accepted syntax.
            }
        }
    }

    public boolean excludes(String path) {
        String normalized = path == null || path.isBlank() ? "/" : path;
        int query = normalized.indexOf('?');
        if (query >= 0) normalized = normalized.substring(0, query);
        for (Pattern pattern : patterns) {
            if (pattern.matcher(normalized).matches()) return true;
        }
        return false;
    }

    private Pattern compile(String rule) {
        if (rule.startsWith("regex:")) {
            return Pattern.compile(rule.substring("regex:".length()), Pattern.CASE_INSENSITIVE);
        }
        if (rule.contains("*") || rule.contains("?")) {
            StringBuilder regex = new StringBuilder("^");
            for (char c : rule.toCharArray()) {
                switch (c) {
                    case '*' -> regex.append(".*");
                    case '?' -> regex.append('.');
                    case '.', '+', '(', ')', '[', ']', '{', '}', '^', '$', '|', '\\' ->
                            regex.append('\\').append(c);
                    default -> regex.append(c);
                }
            }
            return Pattern.compile(regex.append('$').toString(), Pattern.CASE_INSENSITIVE);
        }
        String normalized = rule.endsWith("/") && rule.length() > 1
                ? rule.substring(0, rule.length() - 1) : rule;
        return Pattern.compile("^" + Pattern.quote(normalized) + "(?:/.*)?$",
                Pattern.CASE_INSENSITIVE);
    }
}

