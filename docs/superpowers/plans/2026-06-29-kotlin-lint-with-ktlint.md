# Kotlin Lint With ktlint Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add ktlint as the first Kotlin lint/format library, with explicit check and format tasks and no phase-1 dependency from `build` or `check`.

**Architecture:** This is a build-quality change only. It adds Gradle plugin configuration, a root `.editorconfig`, and README commands while keeping application code, tests, migrations, and runtime behavior untouched.

**Tech Stack:** Kotlin 2.2.21, Java 21, Gradle Wrapper 9.6.0, Spring Boot 4.1.0, `org.jlleitschuh.gradle.ktlint` 14.2.0.

---

## Global Constraints

- Prefix every shell command with `rtk`.
- Use the Gradle wrapper and local Gradle cache:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

- Keep the first phase opt-in: `ktlintCheck` and `ktlintFormat` must exist, but `build` and `check` must not run ktlint yet.
- Do not add `detekt`, Spotless, Qodana, DiKTat, Git hooks, or GitHub Actions in this implementation.
- Keep new and edited files ASCII-only.
- Do not commit generated output under `build/`.

---

## File Map

| Action | File | Responsibility |
|---|---|---|
| Create | `.editorconfig` | Shared Kotlin/KTS style inputs for ktlint and IDEs |
| Modify | `build.gradle.kts` | Add ktlint Gradle plugin and keep ktlint opt-in during phase 1 |
| Modify | `README.md` | Document lint check/format commands |
| Read | `docs/decisions/2026-06-29-prd-kotlin-lint-with-ktlint.md` | Source of accepted decision and adoption phases |

---

## Task 1: Add root EditorConfig

**Files:**
- Create: `.editorconfig`

**Intent:** Give ktlint and IDEs the same basic formatting contract without introducing a large rule file.

- [ ] **Step 1: Create `.editorconfig`**

Add this file at the repository root:

```editorconfig
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
indent_style = space
indent_size = 4
trim_trailing_whitespace = true

[*.{kt,kts}]
ktlint_code_style = ktlint_official
ij_kotlin_allow_trailing_comma = true
ij_kotlin_allow_trailing_comma_on_call_site = true
```

- [ ] **Step 2: Verify the file is tracked and ASCII**

Run:

```bash
rtk git status --short .editorconfig
rtk file .editorconfig
```

Expected:

```text
?? .editorconfig
.editorconfig: ASCII text
```

- [ ] **Step 3: Commit Task 1**

Run:

```bash
rtk git add .editorconfig
rtk git commit -m "build: add editorconfig for kotlin linting"
```

Expected: commit succeeds with only `.editorconfig`.

---

## Task 2: Add ktlint Gradle plugin as opt-in tooling

**Files:**
- Modify: `build.gradle.kts`

**Intent:** Add ktlint tasks while preserving the phase-1 decision that `build` and `check` remain free of lint enforcement.

- [ ] **Step 1: Add the ktlint plugin**

In `build.gradle.kts`, update the `plugins` block to include `org.jlleitschuh.gradle.ktlint`:

```kotlin
plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}
```

- [ ] **Step 2: Keep ktlint outside `check` during phase 1**

At the end of `build.gradle.kts`, after the existing `bootBuildImage` task block, add this block:

```kotlin
gradle.projectsEvaluated {
    tasks.named("check").configure {
        setDependsOn(
            getDependsOn().filterNot { dependency ->
                dependency.toString().contains("ktlint", ignoreCase = true)
            },
        )
    }
}
```

This deliberately leaves `ktlintCheck` available while removing direct ktlint task dependencies from `check` after all plugins have configured the task graph.

- [ ] **Step 3: Verify ktlint tasks exist**

Run:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew tasks --group verification
```

Expected:

```text
ktlintCheck - Runs ktlint on all kotlin sources in this project.
```

The exact task list may contain other verification tasks, but `ktlintCheck` must be present.

- [ ] **Step 4: Verify `build` dry-run does not include ktlint**

Run:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build --dry-run
```

Expected:

- Output includes normal build/test lifecycle tasks such as `compileKotlin`, `test`, and `check`.
- Output does not include `ktlintCheck`, `ktlintMainSourceSetCheck`, `ktlintTestSourceSetCheck`, or `ktlintKotlinScriptCheck`.

- [ ] **Step 5: Verify `ktlintCheck` runs explicitly**

Run:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintCheck
```

Expected:

- If it passes: continue to Task 3.
- If it fails with style violations: do not run `ktlintFormat` yet. Read the reported file list, keep the build configuration change, and continue to Task 3 with the failure documented in the final execution notes.
- If it fails because the Gradle configuration is invalid: fix `build.gradle.kts`, rerun this step, and do not continue until the task starts correctly.

- [ ] **Step 6: Commit Task 2**

Run:

```bash
rtk git add build.gradle.kts
rtk git commit -m "build: add ktlint gradle plugin"
```

Expected: commit succeeds with only `build.gradle.kts`.

---

## Task 3: Document lint commands in README

**Files:**
- Modify: `README.md`

**Intent:** Make lint usage discoverable without changing the main build command.

- [ ] **Step 1: Add a Lint section after the Test section**

In `README.md`, after the existing `## Test` section and its list of automated test coverage, add:

````markdown
## Lint

Kotlin linting is configured with ktlint through Gradle. In the first adoption phase, lint is opt-in and does not run as part of `build`.

Check Kotlin and Gradle Kotlin DSL style:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintCheck
```

Autoformat Kotlin and Gradle Kotlin DSL files locally:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintFormat
```

Run `ktlintFormat` only when the resulting diff is reviewed as a mechanical formatting change.
````

- [ ] **Step 2: Verify Markdown fence balance**

Run:

```bash
rtk rg -n "^```" README.md
```

Expected: the number of matching lines is even.

- [ ] **Step 3: Commit Task 3**

Run:

```bash
rtk git add README.md
rtk git commit -m "docs: document ktlint commands"
```

Expected: commit succeeds with only `README.md`.

---

## Task 4: Final verification

**Files:**
- Read: `build.gradle.kts`
- Read: `.editorconfig`
- Read: `README.md`
- Read: `docs/decisions/2026-06-29-prd-kotlin-lint-with-ktlint.md`

**Intent:** Prove the implementation matches the accepted ADR.

- [ ] **Step 1: Run formatting-safe diff check**

Run:

```bash
rtk git diff --check
```

Expected:

No output and exit code 0.

- [ ] **Step 2: Confirm only intended files changed since the task commits**

Run:

```bash
rtk git status --short
```

Expected:

No output. If there are changes, inspect them and either commit the intended changes or revert only accidental changes made during this implementation.

- [ ] **Step 3: Run the main build**

Run:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew build
```

Expected:

```text
BUILD SUCCESSFUL
```

The build output must not show ktlint tasks executing in phase 1.

- [ ] **Step 4: Run explicit lint check once more**

Run:

```bash
rtk env GRADLE_USER_HOME=/Users/fabiano/Developer/nexus-shopping/.gradle-local ./gradlew ktlintCheck
```

Expected:

- `BUILD SUCCESSFUL` if the current source already satisfies ktlint.
- A clear ktlint violation report if the current source needs mechanical formatting. In that case, do not claim lint passes; report the exact failing files and recommend a separate formatting commit.

- [ ] **Step 5: Summarize implementation outcome**

Final response must include:

- Files changed.
- Whether `build` passed.
- Whether `ktlintCheck` passed or which files it reported.
- Confirmation that lint remains opt-in for phase 1.
