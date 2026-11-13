# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this repo is

A harness (not a TCK distribution) that runs the Jakarta EE 11 Web Profile TCKs against Apache TomEE 11. It consumes the TCKs' published Maven artifacts (`jakarta.tck:artifacts-bom`), JUnit 5 and Arquillian's TomEE remote adapter; there is no Ant/`TSDeployment`/checked-in TCK. Maven comes from the wrapper (`./mvnw`); JDK 17 or 21 required (CI uses 21; `JAVA_HOME` must be set for the security runner).

Note: the checked-out branch may be an older-EE branch (e.g. `tomee-10.3.x-ee10`) while pom/README on `master`/`main` describe EE 11 (`11.0.0-SNAPSHOT`, TCK 11.0.3). Check `pom.xml` and `environment/versions.env` for the versions actually in play.

Read `README.md` (overview, CI, environment templates), `KNOWN_ISSUES.md` (central triage list of TomEE/provider gaps), `runner-standalone/README.md` and `runner-webprofile/README.md` (per-suite status) before changing suite behaviour.

## Commands

```sh
# Verify pinned TCK BOM metadata (without --metadata-only it also downloads/verifies Derby jars)
sh environment/verify-inputs.sh --metadata-only

# Smoke gate (checks the TomEE adapter) / default reactor (porting tests + smoke; no TCK partitions)
./mvnw -B -ntp -pl runner-smoke -am verify
./mvnw -B -ntp verify

# Platform catalog partitions: <protocol: servlet|javatest> [<manifest id>]
runner-webprofile/run-platform-suite.sh servlet rest
TOMEE_CLASSIFIER=webprofile runner-webprofile/run-platform-suite.sh javatest persistence-javatest

# One class of one partition, directly
./mvnw -pl runner-webprofile/run -am -Dtck.artifact=rest-platform-tck -Dtck.partition=rest \
  -Dtck.groups='web & !tck-javatest' \
  -Dtck.test=com.sun.ts.tests.jaxrs.platform.servletconfig.JAXRSClientIT verify

# Standalone specification TCKs (one at a time): annotations, concurrency, data, di, el, cdi, cdi-ee,
# servlet, pages, rest, validation, websocket, jsonp, jsonb, debugging, persistence, transactions,
# security, security-old, authentication, faces, faces-old
runner-standalone/run-standalone-suite.sh concurrency [extra mvn args]

# Build a TomEE from any tag/branch/commit into a Maven repo; prints TOMEE_VERSION=<v>
environment/tomee/build-tomee.sh tomee-project-9.1.3
TOMEE_VERSION=<v> TOMEE_CLASSIFIER=plume runner-webprofile/run-platform-suite.sh servlet rest
```

- `TOMEE_CLASSIFIER` ∈ `plume` (default, EclipseLink), `webprofile` (OpenJPA), `microprofile`, `plus`; scripts reject anything else. Only `plume`/`webprofile` have distinct manifest/exclusion expectations; the others reuse `plume`'s.
- `TOMEE_VERSION` unset → pom's mutable snapshot; set it (or `TOMEE_REPO_URL`/`TOMEE_SRC_DIR` for `build-tomee.sh`) to test a specific ref.
- Run unfiltered by exclusions: `-Dtck.exclusions.file=$(pwd)/runner-standalone/exclusions/none.txt` (TestNG runners also accept `-Dtck.exclusions.file=none`).
- No lint step exists.

## Architecture

- `pom.xml` (root): central version management; default build runs no TCK tests. Modules: `tomee-porting`, `runner-smoke`, `runner-webprofile`, `runner-standalone`.
- `tomee-porting/`: Arquillian extension (`TomEETckExtension`, `TomEETestArchiveProcessor`) that adapts TCK deployments to TomEE.
- `environment/`: shared test environment — TomEE config overlay (`tomee/conf`), Derby DDL/DML, `ts.jte` (minimal JavaTest properties), cert generator, free-port/require-free-port scripts, `versions.env` (SHA-256 pins), `build-tomee.sh`. Runners stage these under `target/`, start a temporary Derby, overlay a freshly unpacked TomEE, and never touch a user's installed TomEE or `~/.m2/settings.xml`.
- `runner-webprofile/`: the Platform TCK catalog. `platform-suite.tsv` is the manifest (id, artifact, protocol, JUnit tag expression, expected class counts, failsafe include); `platform-suite-webprofile.tsv` overrides rows for the OpenJPA ZIP. `run-platform-suite.sh` drives `runner-webprofile/run` per partition. Exclusions live in `exclusions/` (and `exclusions/webprofile/`).
- `runner-standalone/`: one module per independently published spec TCK, plus `<id>-install` twin modules that install the TCK bits first (the script prepends them automatically), `tck-common` (shared, incl. TestNG `ExclusionsAnnotationTransformer`), `cdi-tck-porting`, and `exclusions/<id>.txt`. Three flavours:
  - JUnit/Arquillian runners (goal `test`);
  - source-reactor runners (`security`, `authentication`, `faces`) that build the TCK from source, inject a `tomee-remote` profile, and manage their own TomEE (goal `verify`);
  - JavaTest-based runners (`transactions`, `faces-old`, `security-old`; goal `verify`) whose results are converted to JUnit XML by `javatest-report-to-junit.sh`.
- `Jenkinsfile`: authoritative CI. Validate → Build TomEE from `TOMEE_REF`/`TOMEE_REPO_URL` → smoke on JDK 17+21 → fan out every manifest partition and standalone suite as parallel JDK 21 container branches (digest-pinned images). The built `org/apache/tomee` repo slice is stashed and reused by all branches.

## Gotchas

- **Ports**: runners use fixed localhost ports (8080/8443/8005/1527 Derby) unless overridden (`-Dtomee.http.port`, `-Dtomee.https.port`, `-Dtomee.shutdown.port`, `-Dtck.derby.port`; support varies per runner — see runner-standalone README). Run one at a time. The Arquillian remote adapter silently attaches to *any* server already on the HTTP port, so stale TomEEs corrupt results. The `rest` runner deliberately leaves 8080 free for the TCK's own embedded Jetty.
- **Exclusions are reviewed known gaps, not passes.** Formats differ by runner type: surefire/failsafe `excludesFile` globs (`**/Class.java[#method]`), TestNG FQCN/`Class#method` lines, or JavaTest `.jtx` lines. When a TomEE/provider fix lands, delete the matching entries and update `KNOWN_ISSUES.md`; `expected_executed_classes` in the manifest must stay consistent with exclusions.
- Never commit generated keys/certs (`environment/certificates` output goes to `target/`); test credentials in `tomcat-users.xml` are test-only.
