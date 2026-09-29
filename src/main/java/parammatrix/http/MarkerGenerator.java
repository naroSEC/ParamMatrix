package parammatrix.http;

import java.security.SecureRandom;
import java.util.HexFormat;

public final class MarkerGenerator {
    private final SecureRandom random = new SecureRandom();

    public String generate(String parameterName) {
        byte[] bytes = new byte[8];
        random.nextBytes(bytes);
        String safe = parameterName.replaceAll("[^A-Za-z0-9]", "_");
        if (safe.length() > 12) safe = safe.substring(0, 12);
        return "NARO_" + safe + "_" + HexFormat.of().withUpperCase().formatHex(bytes);
    }
}

