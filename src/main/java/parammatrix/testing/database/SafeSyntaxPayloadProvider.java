package parammatrix.testing.database;

import java.util.List;

public final class SafeSyntaxPayloadProvider implements DatabaseStressPayloadProvider {
    @Override
    public List<DatabaseStressPayload> payloads() {
        return List.of(
                new DatabaseStressPayload("Single quote", "'"),
                new DatabaseStressPayload("Double quote", "\""),
                new DatabaseStressPayload("Single quote and parenthesis", "')"),
                new DatabaseStressPayload("Double quote and parenthesis", "\")"),
                new DatabaseStressPayload("Backslash", "\\"),
                new DatabaseStressPayload("Numeric quote boundary", "1'")
        );
    }
}

