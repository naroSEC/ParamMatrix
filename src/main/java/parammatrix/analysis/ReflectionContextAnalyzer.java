package parammatrix.analysis;

import parammatrix.model.ReflectionContext;

import java.util.Locale;

public final class ReflectionContextAnalyzer {
    public ReflectionContext analyze(String body, String marker) {
        int index = body.indexOf(marker);
        if (index < 0) return ReflectionContext.UNKNOWN;
        String before = body.substring(Math.max(0, index - 500), index);
        String lowerBefore = before.toLowerCase(Locale.ROOT);
        String after = body.substring(index, Math.min(body.length(), index + marker.length() + 200));

        int scriptOpen = lowerBefore.lastIndexOf("<script");
        int scriptClose = lowerBefore.lastIndexOf("</script");
        if (scriptOpen > scriptClose) {
            char quote = activeQuote(before);
            if (quote == '\'') return ReflectionContext.JAVASCRIPT_SINGLE_QUOTED_STRING;
            if (quote == '"') return ReflectionContext.JAVASCRIPT_DOUBLE_QUOTED_STRING;
            if (quote == '`') return ReflectionContext.JAVASCRIPT_TEMPLATE_LITERAL;
            return ReflectionContext.SCRIPT_BLOCK;
        }
        int commentOpen = before.lastIndexOf("<!--");
        if (commentOpen > before.lastIndexOf("-->")) return ReflectionContext.COMMENT;
        int styleOpen = lowerBefore.lastIndexOf("<style");
        if (styleOpen > lowerBefore.lastIndexOf("</style")) return ReflectionContext.CSS;

        int tagOpen = before.lastIndexOf('<');
        int tagClose = before.lastIndexOf('>');
        if (tagOpen > tagClose) {
            char quote = activeQuote(before.substring(tagOpen));
            String tagTail = before.substring(tagOpen).toLowerCase(Locale.ROOT);
            boolean url = tagTail.matches("(?s).*(href|src|action|formaction)\\s*=.*");
            if (url) return ReflectionContext.URL_ATTRIBUTE;
            if (quote == '\'') return ReflectionContext.HTML_ATTRIBUTE_SINGLE_QUOTED;
            if (quote == '"') return ReflectionContext.HTML_ATTRIBUTE_DOUBLE_QUOTED;
            return ReflectionContext.HTML_ATTRIBUTE_UNQUOTED;
        }
        if (after.contains("-->") && commentOpen >= 0) return ReflectionContext.COMMENT;
        return ReflectionContext.HTML_TEXT;
    }

    public String evidence(String body, String marker) {
        int index = body.indexOf(marker);
        if (index < 0) return "Marker not present in response body";
        int start = Math.max(0, index - 100);
        int end = Math.min(body.length(), index + marker.length() + 100);
        return body.substring(start, end).replace("\r", "\\r").replace("\n", "\\n");
    }

    private char activeQuote(String text) {
        char active = 0;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '\'' || c == '"' || c == '`') {
                active = active == 0 ? c : active == c ? 0 : active;
            }
        }
        return active;
    }
}

