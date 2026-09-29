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
- `config` — thread-safe runtime settings.
- `discovery` — response classification, jsoup-based HTML extraction, replaceable JavaScript parser and
  evidence aggregation.
- `model` — page identity, candidate/evidence and reflection result/context types.
- `http` — marker generation, request mutation, JSON injection boundary, request sender and generated
  request registry.
- `analysis` — reflection context and evidence extraction.
- `testing` — test-module interface, reflection implementation and request-budget coordinator.
- `testing.ssti` — attack-free Phase-6 extension interfaces, engines, payload and result schema.
- `storage` — canonical page/name result repository and UI change notifications.
- `ui` — results table, Burp message editors, evidence view, settings, queue controls and SSTI placeholder.

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

## SSTI extension seam

SSTI is a peer testing module, not a subclass of reflection. `SstiTestEngine` accepts any candidate and
a selected engine set. `SstiPayloadProvider` is engine-specific. A future policy layer may pass every
candidate or only reflected candidates, but the engine interface itself has no reflection dependency.

