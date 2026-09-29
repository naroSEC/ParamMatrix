package parammatrix.config;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class ExtensionConfig {
    public enum ReflectionMode { BATCH_ONLY, INDIVIDUAL_ONLY, BATCH_AND_VERIFY }

    public final AtomicBoolean autoAnalysis = new AtomicBoolean(false);
    public final AtomicBoolean inScopeOnly = new AtomicBoolean(true);
    public final AtomicBoolean ignoreGeneratedRequests = new AtomicBoolean(true);
    public final AtomicInteger maximumResponseBytes = new AtomicInteger(2_000_000);
    public final AtomicInteger maximumCandidatesPerPage = new AtomicInteger(100);

    public final AtomicBoolean analyzeHtml = new AtomicBoolean(true);
    public final AtomicBoolean analyzeXhtml = new AtomicBoolean(true);
    public final AtomicBoolean htmlAutoDetection = new AtomicBoolean(false);
    public final AtomicBoolean ignoreJsonResponses = new AtomicBoolean(true);

    public final AtomicBoolean reflectionEnabled = new AtomicBoolean(true);
    public volatile ReflectionMode reflectionMode = ReflectionMode.BATCH_AND_VERIFY;
    public final AtomicInteger requestDelayMillis = new AtomicInteger(150);
    public final AtomicInteger maximumRequestsPerPage = new AtomicInteger(20);
    public final AtomicInteger concurrentActiveTests = new AtomicInteger(5);

    public final AtomicBoolean htmlFormFields = new AtomicBoolean(true);
    public final AtomicBoolean htmlUrls = new AtomicBoolean(true);
    public final AtomicBoolean inlineJavaScript = new AtomicBoolean(true);
    public final AtomicBoolean javaScriptSinkAnalysis = new AtomicBoolean(true);
    public final AtomicBoolean includeLowConfidence = new AtomicBoolean(false);
    public final AtomicBoolean identityIgnoresParameterValues = new AtomicBoolean(true);
}
