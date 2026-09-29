# ParamMatrix

ParamMatrix is a Burp Suite extension for discovering page-specific HTTP parameters and checking
whether they are reflected in the response. It is built with Java and the Montoya API.

The extension treats every request/response pair as an independent page. Parameters found on one
page are never tested against another endpoint.

## Features

- Discovers named HTML form controls and query parameters embedded in links, forms, frames, and
  other URL-bearing attributes
- Examines inline JavaScript at HTTP and navigation sinks, including `fetch`, axios, jQuery Ajax,
  `FormData`, `URLSearchParams`, `location`, and `window.open`
- Distinguishes existing request parameters from newly discovered candidates
- Retains every discovery source when the same parameter appears in more than one place
- Tests reflections in batches, individually, or with individual verification of batch positives
- Records the marker, response excerpt, test exchange, and estimated reflection context
- Runs non-destructive, engine-specific SSTI arithmetic probes with exact evaluated-result matching
- Shows the SSTI payload patterns and exact database stress strings before a test is started
- Keeps SSTI findings in a separate evidence-focused result view
- Applies short database syntax stress strings and detects new DB/driver error signatures
- Separates confirmed DB errors from low-confidence response behavior changes
- Provides manual actions in Proxy history, Site map, and HTTP message editors
- Scans existing Proxy History and Site Map/crawl records with method and path filters
- Supports guarded automatic analysis of Proxy traffic
- Uses Burp message editors for original and test request/response inspection

JSON responses are excluded from discovery, including when HTML auto-detection is enabled. JSON
request bodies may still be used as an injection target when the body is a top-level object.

## Requirements

- Burp Suite with Montoya API 2026.7 support
- Java 21

## Build

The Gradle Wrapper is included.

```powershell
.\gradlew.bat clean test jar
```

On Linux or macOS:

```bash
./gradlew clean test jar
```

The extension JAR is written to:

```text
build/libs/param-matrix-1.3.1.jar
```

jsoup is bundled in the output JAR. The Montoya API is supplied by Burp and is therefore declared as
a compile-only dependency.

## Installation

1. Open **Extensions > Installed** in Burp Suite.
2. Click **Add** and select **Java**.
3. Choose `build/libs/param-matrix-1.3.1.jar`.
4. Confirm that the **ParamMatrix** tab appears.

Automatic analysis is disabled on first load.

The first inner tab, **Discovery & Reflection**, contains discovered parameter candidates and their
reflection status. SSTI and database findings are intentionally kept in the separate **SSTI Results**
and **DB Error Results** tabs.

## Manual analysis

Right-click an HTTP request/response in Proxy history, Site map, or a message editor and open the
**Parameter Analyzer** menu.

- **Extract Parameters** analyzes the selected HTML response and stores its candidates.
- **Test Reflections** tests candidates already associated with the selected page.
- **Extract & Test** performs both operations in one queued task.

Selecting a result shows the original exchange, test exchange, discovery evidence, and reflection
evidence in the lower panel.

## Automatic analysis

Enable **Auto Analysis** from the extension's Settings tab. Proxy responses then follow this pipeline:

```text
response classification
        -> parameter discovery
        -> page-local candidate aggregation
        -> reflection testing
        -> result storage
```

A page identity is derived from protocol, host, port, method, path, and request parameter structure.
Parameter values are ignored by default, so revisiting the same endpoint with different values does
not repeatedly trigger active tests. This behavior can be changed in Settings.

## History scan

The **Scan** tab can collect previously recorded traffic from **Proxy History**, **Site Map / Crawl**,
or both. GET and POST requests can be enabled independently. Other methods are skipped.

Choose **Discover only** to populate the result table without active traffic, or **Discover + reflection
test** to run the configured reflection workflow for every eligible page. Records from both sources are
deduplicated using the same page identity as automatic analysis.

Enable **Include SSTI testing** to run the engines and target policy selected in the **SSTI Test** tab
after discovery. The option is disabled by default.

Enable **Include DB error testing** to apply the signature families and safety settings selected in
the **DB Stress Test** tab. This option is also disabled by default.

Excluded paths accept one rule per line:

```text
/logout                  # exact path and descendants
/static/*                # glob pattern
regex:^/api/v[0-9]+/health$  # regular expression
```

Blank lines and lines beginning with `#` are ignored. The scan progress bar reports eligible pages and
counts records skipped by method, path, or duplicate identity. Active requests continue to respect the
scope, delay, concurrency, and per-page request budget configured in Settings.

## Reflection testing

Each candidate receives an independent marker in the following form:

```text
NARO_<parameter>_<random-id>
```

The default mode sends one batch request and verifies each positive result with a separate request.
The available modes are:

- **Batch Only**
- **Individual Only**
- **Batch + Individual Verification**

Existing URL and form parameters are updated in place. New parameters are added according to the
request method and content type. The request is derived from the original Montoya request, preserving
unrelated headers, cookies, and session state. Cookie parameters are never selected as an injection
location.

Reflection context detection currently covers common HTML text and attribute contexts, script blocks,
JavaScript strings and template literals, comments, URL attributes, and CSS. The result is heuristic
and is always accompanied by the matching response excerpt.

## Safety controls

Active testing can be limited with:

- Burp scope enforcement
- Maximum candidates per page
- Maximum requests per page
- Request delay
- Concurrent worker limit
- Pause and resume controls
- Pending queue cancellation
- Extension-generated request filtering
- Duplicate page identity suppression
- Maximum response size

Parsing and HTTP requests run on worker threads. Swing components are updated on the Event Dispatch
Thread.

## Discovery behavior

HTML discovery covers:

- `input[name]`
- `select[name]`
- `textarea[name]`
- `button[name]`
- Query strings in form actions, anchors, frames, scripts, and common URL attributes

The JavaScript parser is intentionally sink-oriented. It does not collect ordinary variable names.
Only names used in request data, URL construction, query strings, form data, or navigation APIs are
considered. The parser is behind a small interface so it can be replaced by a full JavaScript AST
implementation without changing the discovery engine.

## Project structure

```text
src/main/java/
├── burp/                  Montoya entry point
└── parammatrix/
    ├── analysis/          Reflection context analysis
    ├── config/            Runtime settings
    ├── core/              Workflow, queue, context menu, auto mode
    ├── discovery/         HTML and JavaScript discovery
    ├── http/              Markers, request mutation, request sending
    ├── model/             Candidates, evidence, page identity, results
    ├── storage/           Canonical result repository
    ├── testing/           Test interfaces and reflection engine
    │   └── ssti/          SSTI extension points
    └── ui/                Results, details, settings, queue controls
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for component responsibilities and data flow.

## SSTI testing

The **SSTI Test** tab provides providers for Generic, Jinja2, Twig, FreeMarker, Velocity, Thymeleaf,
and Smarty. Select one or more engines, choose whether to test all discovered parameters or reflected
parameters only, set a per-page request budget and delay, then click **Start SSTI test**.

Each request uses randomized arithmetic operands inside an engine-specific expression. A unique prefix
and suffix surround the expression. A result is marked **DETECTED** only when the response contains the
same prefix and suffix around the calculated value; a literal reflection of the input payload does not
count as detection.

Results are stored in **SSTI Results** with:

- Template engine/provider
- Payload and expected evaluated value
- Detection status and confidence
- Original and test request/response
- Response evidence surrounding the evaluated marker

SSTI is a peer module to reflection testing and does not require a positive reflection result. Active
SSTI requests use the same original-request preservation, Burp scope, generated-request filtering, and
worker queue as reflection tests. Its delay and per-page request limit are configured separately.

## Database error stress testing

The **DB Stress Test** tab applies a small set of short syntax boundary strings—quotes, quote/parenthesis
combinations, and a backslash—to discovered parameters one at a time. It does not send SQL statements,
time-delay expressions, data access queries, or destructive payloads.

Error signatures are available for:

- MySQL and MariaDB
- PostgreSQL
- Microsoft SQL Server
- Oracle
- SQLite
- IBM DB2
- Generic JDBC, ODBC, SQLSTATE, and ORM exceptions

The engine compares each test response to the original response. A result is classified as:

- **DB_ERROR_DETECTED** when a selected DB/driver signature appears only in the test response
- **BEHAVIOR_CHANGED** when the response becomes a server error or changes substantially without a
  recognized DB signature
- **NOT_DETECTED**, **ERROR**, or **SKIPPED** otherwise

Recognized DB errors are repeated once by default and marked verified only when the same database
signature family appears again. The **DB Error Results** tab stores status changes, body-length delta,
signature, confidence, verification status, evidence, and both HTTP exchanges. DB testing has its own
parameter policy, delay, and per-page request budget and does not depend on Reflection or SSTI results.

## Known limitations

- Only inline JavaScript is analyzed; external scripts are not fetched.
- Computed, obfuscated, or deeply nested JavaScript may not be recognized by the current parser.
- JSON request injection supports top-level objects and does not replace existing JSON keys.
- Multipart insertion depends on Montoya's multipart parameter handling for the captured message.
- Reflection context classification is heuristic rather than browser-backed parsing.
- Arithmetic evaluation confirms template expression execution but does not always fingerprint the
  exact engine. Engines with overlapping syntax, notably Jinja2 and Twig, can produce equivalent hits.
- SSTI testing intentionally excludes command execution, file access, environment access, and outbound
  network payloads.
- DB error signatures indicate error behavior, not proof that an injectable SQL statement can be
  controlled. Behavior-only changes are intentionally reported with LOW confidence.
- Settings are kept for the current extension session.
- Automatic analysis observes Proxy responses; traffic from other Burp tools can be analyzed manually.

Use active testing only on systems you are authorized to assess.
