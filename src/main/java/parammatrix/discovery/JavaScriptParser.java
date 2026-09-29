package parammatrix.discovery;

import java.util.List;

/** Replaceable boundary for a future full JavaScript AST implementation. */
public interface JavaScriptParser {
    List<DiscoveredParameter> extract(String script, String documentUrl);
}

