# ParamMatrix architecture

## Invariants

1. A `ParameterCandidate` owns the `PageIdentity` and original `HttpRequestResponse` that produced it.
2. `TestCoordinator` receives candidates from one page and `ReflectionTestEngine` mutates only that
   candidate's `originalExchange`.
3. Discovery rejects JSON responses before HTML auto-detection.
4. JavaScript names enter the model only from an HTTP, URL, form-data, search-parameter or navigation
   sink. Ordinary declarations are not candidates.
5. All parsing and HTTP I/O run through `ActiveTaskQueue`; only view-model refreshes run on Swing EDT.

## Packages and responsibilities

- `burp` — the single Montoya entry point and registration/composition root.
- `core` — workflow orchestration, Proxy auto-analyzer, context menu and bounded/pausable worker queue.
- `scan` — Proxy History/Site Map collection, GET/POST filtering, exclusion rules, optional Cookie Jar
  refresh and scan progress.
- `config` — thread-safe runtime settings.
- `discovery` — response classification, jsoup-based HTML extraction, replaceable JavaScript parser and
  evidence aggregation.
- `model` — page identity, candidate/evidence and reflection result/context types.
- `http` — marker generation, request mutation, JSON injection boundary, request sender and generated
  request registry.
- `analysis` — reflection context and evidence extraction.
- `testing` — test-module interface, reflection implementation and request-budget coordinator.
- `testing.ssti` — safe arithmetic payload providers, execution coordinator, engine and result schema.
- `testing.database` — syntax stress payloads, DB error testing, confirmation and result schema.
- `storage` — canonical page/name result repository and UI change notifications.
- `ui` — result tables, Burp message editors, evidence views, test configuration and global queue controls.

## Data flow

```text
Proxy response or context-menu selection
                 |
                 v
        ExtensionController
                 |
        ActiveTaskQueue workers
                 |
     ContentTypeClassifier ---- JSON => stop
                 |
  ParameterDiscoveryEngine
       |                 |
 Html extractor    JavaScriptParser
       \                 /
        evidence aggregation
                 |
  page-local ParameterCandidate(s)
                 |
       ResultRepository
                 |
       TestCoordinator (optional)
                 |
   ReflectionTestEngine -> RequestMutator -> Montoya Http.sendRequest
                 |
   marker/context/evidence result -> repository -> Swing EDT
```

The History Scan entry point first merges Proxy History and Site Map/crawl records, applies method and
path exclusions, and deduplicates them by `PageIdentity`. Eligible exchanges then enter the same
controller and worker queue as manual analysis, preserving page isolation and all active-test limits.
When enabled for a Scan run, `ScanCookieRefresher` replaces only the test copy's Cookie header with
unexpired Burp Cookie Jar values matching the request host and path. Recorded Burp traffic remains
unchanged, and requests without a matching current cookie retain their original header.

## Page isolation and duplicate policy

`PageIdentity` is scheme + host + port + method + path + sorted request parameter structure. By default,
values are omitted, allowing repeated browsing with different values to count as one endpoint shape.
The automatic controller keeps a concurrent set of analyzed identities. The repository key is
`PageIdentity + parameter name`; multiple discoveries merge evidence into that candidate rather than
discarding sources. Context-menu **Test Reflections** queries only candidates with the selected page's
identity.

## Request safety

The mutator starts from Montoya's immutable original request. It updates supported existing URL/form
parameters or adds new ones; Cookie parameters are never selected as an injection location. Batch and
individual probes share a strict per-page request budget. Scope restriction, delay, concurrency, pause,
pending-queue clear and generated-request fingerprints are independent controls.

## SSTI pipeline

SSTI is a peer testing module, not a subclass of reflection. `SstiCoordinator` applies the selected
parameter policy and per-page request budget, then obtains randomized arithmetic probes from each
`SstiPayloadProvider`. `DefaultSstiTestEngine` mutates the candidate's own original request and records
a detection only when the uniquely wrapped evaluated result appears in the response. `SstiResultRepository`
feeds the independent SSTI result table and Burp message-editor detail view. Reflection filtering is an
optional caller policy rather than an engine dependency.

## Database error pipeline

Database testing is another peer module. `SafeSyntaxPayloadProvider` supplies only short quote,
parenthesis, and backslash boundaries. `DatabaseErrorTestEngine` compares the original and test response
and delegates new-signature matching to `DatabaseErrorSignatureAnalyzer`. `DatabaseTestCoordinator`
enforces an independent per-page budget and optionally repeats positive signature matches. Confirmed
signatures, low-confidence behavior changes, HTTP deltas, evidence, and exchanges are stored in
`DatabaseResultRepository` and rendered in a dedicated result view.
