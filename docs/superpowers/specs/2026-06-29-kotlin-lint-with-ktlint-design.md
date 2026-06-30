# Kotlin Lint With ktlint — Design Spec

**Date:** 2026-06-29
**Status:** approved
**Decision:** `docs/decisions/2026-06-29-prd-kotlin-lint-with-ktlint.md`
**Implementation plan:** `docs/superpowers/plans/2026-06-29-kotlin-lint-with-ktlint.md`

## Goal

Add a first, low-risk Kotlin lint and formatting layer to the project using `ktlint`, while keeping the main `build` command unchanged during the initial adoption phase.

## Context

The project uses Kotlin 2.2.21, Java 21, Gradle Wrapper 9.6.0, Spring Boot 4.1.0, and Gradle Kotlin DSL. It currently has no Kotlin lint or formatting library configured.

The accepted direction is incremental:

- provide explicit lint and format commands;
- keep formatting manual and reviewable;
- keep lint outside `build` and `check` in phase 1;
- avoid alpha tooling;
- defer deeper static analysis until a stable `detekt` version aligns with Kotlin 2.2+ and Gradle 9.

## Decision

Use `ktlint` through the Gradle plugin `org.jlleitschuh.gradle.ktlint`.

This gives the project:

- `ktlintCheck` for style validation;
- `ktlintFormat` for local mechanical formatting;
- Kotlin and Gradle Kotlin DSL coverage;
- `.editorconfig` support for shared style settings;
- a small integration surface that fits the current Gradle setup.

## Alternatives Considered

| Option | Outcome | Reason |
|---|---|---|
| `org.jlleitschuh.gradle.ktlint` | Accepted | Direct Kotlin lint/format support with Gradle tasks and low adoption cost |
| Spotless | Deferred | Useful as broader formatter, but wider than the immediate Kotlin lint need |
| detekt | Deferred | Stable line is not the best fit for Kotlin 2.2.x; alpha line was rejected |
| ktfmt | Rejected for now | Strong deterministic formatter, but not a full lint layer and may cause larger visual diffs |
| Qodana / DiKTat | Rejected for now | Too broad or opinionated for the first adoption phase |

## Implementation Shape

The implementation should touch only build and documentation files:

```text
.editorconfig
build.gradle.kts
README.md
```

The implementation must not change:

- application source behavior;
- tests;
- Flyway migrations;
- Docker/JMeter assets;
- Git hooks;
- GitHub Actions.

## Gradle Behavior

The project should expose:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintCheck
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintFormat
```

During phase 1:

- `ktlintCheck` must be runnable explicitly;
- `ktlintFormat` must be runnable explicitly;
- `build` must not run ktlint;
- `check` must not run ktlint.

If the Gradle plugin wires ktlint into `check` by default, the implementation must remove that dependency while leaving the explicit ktlint tasks available.

## EditorConfig

Add a root `.editorconfig` with minimal shared formatting rules:

- UTF-8;
- LF line endings;
- final newline;
- spaces with 4-space indentation;
- trailing whitespace trimming;
- Kotlin official ktlint style;
- trailing commas allowed for Kotlin declarations and call sites.

## Documentation

Update `README.md` with a `Lint` section that documents:

- that lint is configured with ktlint;
- that lint is opt-in in the first phase;
- the `rtk` command for `ktlintCheck`;
- the `rtk` command for `ktlintFormat`;
- the expectation that `ktlintFormat` diffs are reviewed as mechanical changes.

## Verification

The implementation should verify:

1. `ktlintCheck` task exists.
2. `build --dry-run` does not include ktlint tasks.
3. `build` still passes.
4. `ktlintCheck` runs explicitly.
5. If `ktlintCheck` reports existing style violations, the final implementation notes must list the reported files and avoid claiming lint passes.

## Out of Scope

- Adding `detekt`.
- Adding Spotless.
- Adding Qodana or DiKTat.
- Adding Git hooks.
- Adding CI enforcement.
- Running `ktlintFormat` as part of this planning/spec step.
- Making lint part of `build` or `check` in phase 1.

