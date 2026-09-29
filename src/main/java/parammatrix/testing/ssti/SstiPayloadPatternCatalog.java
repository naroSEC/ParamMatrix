package parammatrix.testing.ssti;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class SstiPayloadPatternCatalog {
    private static final Map<SstiEngine, List<String>> PATTERNS = patterns();

    private SstiPayloadPatternCatalog() {
    }

    public static List<String> patternsFor(SstiEngine engine) {
        return PATTERNS.getOrDefault(engine, List.of());
    }

    public static String displayPattern(String pattern) {
        return pattern
                .replace("%1$d", "<left>")
                .replace("%2$d", "<right>")
                .replace("%3$s", "PMX_SSTI_<random>")
                .replace("%4$s", "_END");
    }

    private static Map<SstiEngine, List<String>> patterns() {
        Map<SstiEngine, List<String>> values = new EnumMap<>(SstiEngine.class);
        values.put(SstiEngine.GENERIC, List.of(
                "%3$s{{%1$d*%2$d}}%4$s",
                "%3$s${%1$d*%2$d}%4$s"));
        values.put(SstiEngine.JINJA2, List.of("%3$s{{%1$d*%2$d}}%4$s"));
        values.put(SstiEngine.TWIG, List.of("%3$s{{%1$d*%2$d}}%4$s"));
        values.put(SstiEngine.FREEMARKER, List.of("%3$s${%1$d*%2$d}%4$s"));
        values.put(SstiEngine.VELOCITY,
                List.of("#set($pmx=%1$d*%2$d)%3$s${pmx}%4$s"));
        values.put(SstiEngine.THYMELEAF,
                List.of("%3$s[[${%1$d*%2$d}]]%4$s"));
        values.put(SstiEngine.SMARTY, List.of("%3$s{%1$d*%2$d}%4$s"));
        return Map.copyOf(values);
    }
}
