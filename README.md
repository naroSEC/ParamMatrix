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
- Provides manual actions in Proxy history, Site map, and HTTP message editors
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
build/libs/param-matrix-1.0.0.jar
```

jsoup is bundled in the output JAR. The Montoya API is supplied by Burp and is therefore declared as
a compile-only dependency.

## Installation

1. Open **Extensions > Installed** in Burp Suite.
2. Click **Add** and select **Java**.
3. Choose `build/libs/param-matrix-1.0.0.jar`.
4. Confirm that the **Parameter Analyzer** tab appears.

Automatic analysis is disabled on first load.

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

## SSTI extension points

The current release does not send SSTI payloads. It includes interfaces and result models for adding
engine-specific providers for Generic, Jinja2, Twig, FreeMarker, Velocity, Thymeleaf, and Smarty.

SSTI testing is designed as a peer module to reflection testing. It can operate on all discovered
parameters or only reflected parameters without making reflection a hard dependency.

## Known limitations

- Only inline JavaScript is analyzed; external scripts are not fetched.
- Computed, obfuscated, or deeply nested JavaScript may not be recognized by the current parser.
- JSON request injection supports top-level objects and does not replace existing JSON keys.
- Multipart insertion depends on Montoya's multipart parameter handling for the captured message.
- Reflection context classification is heuristic rather than browser-backed parsing.
- Settings are kept for the current extension session.
- Automatic analysis observes Proxy responses; traffic from other Burp tools can be analyzed manually.

Use active testing only on systems you are authorized to assess.
