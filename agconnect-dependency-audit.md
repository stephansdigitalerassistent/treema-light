# Huawei AGConnect Dependency Audit

This audit reconciles the proposed [`agconnect-update-plan.md`](./agconnect-update-plan.md) against the
**actual** state of the build configuration and the vendored binaries in
[`app/libs/`](./app/libs). It records version mismatches, which steps of the plan have already been
executed (and which remain), redundant local artifacts, and documentation inconsistencies.

> **Headline finding:** The build scripts have already been migrated to remote Maven resolution at
> **`1.9.5.302`** — i.e. the plan's *recommended* Option A is largely implemented. The
> `agconnect-update-plan.md` document still describes a "current" configuration (`flatDir` + four
> local `classpath` entries at `1.9.1.303`) that **no longer matches reality**. The only material
> leftover is that the four stale local AGC binaries are still present on disk and tracked in git.

---

## 1. Actual current configuration (as built today)

### `gradle/libs.versions.toml`
* `agconnectAgcp = "1.9.5.302"` (line 3) — already at the plan's target version.
* `agconnect-agcp = { module = "com.huawei.agconnect:agcp", version.ref = "agconnectAgcp" }` (line 93).

### Root `build.gradle.kts`
* `buildscript.repositories` uses `maven { url = uri("https://developer.huawei.com/repo/") }`
  (line 33) — **no** `flatDir { dir("app/libs") }`.
* `buildscript.dependencies` reduced to a single remote coordinate:
  `classpath(libs.agconnect.agcp)` (line 40) → resolves `com.huawei.agconnect:agcp:1.9.5.302`.
* `allprojects.repositories` already adds `includeGroup("com.huawei.agconnect")` to the Huawei
  `exclusiveContent` filter (line 69). The `flatDir { dir("libs") }` entry (line 57) remains, but it
  is needed for **non‑AGC** local artifacts (see §4).

### `app/build.gradle.kts`
* Plugin applied conditionally for `Hms*` tasks: `plugin("com.huawei.agconnect")` (line 69).
* `hmsPush` pulled remotely with `exclude(group = "com.huawei.agconnect")` (lines 1052–1057).
* AGConnect core pulled **remotely**, not from a local AAR:
  * `"hmsImplementation"("com.huawei.agconnect:agconnect-core:1.9.5.302")` (line 1058)
  * `"hms_workImplementation"("com.huawei.agconnect:agconnect-core:1.9.5.302")` (line 1059)

### Vendored binaries in `app/libs/`
| File | Vendored version | Git status |
| :--- | :---: | :--- |
| `agcp-1.9.1.303.jar` | `1.9.1.303` | tracked |
| `agconnect-core-1.9.1.303.aar` | `1.9.1.303` | tracked |
| `agconnect-crash-symbol-lib-1.9.1.303.jar` | `1.9.1.303` | tracked |
| `agconnect-apms-plugin-1.6.2.300.jar` | `1.6.2.300` | tracked |

(Also present and **out of scope** for AGC: `libgsaverification-client.aar` and the
`arm64-v8a/ armeabi-v7a/ x86/ x86_64/` ABI directories holding JNA `.so` files.)

---

## 2. Version mismatches

The vendored binaries are stale relative to the versions the build actually resolves:

| Artifact | Local `app/libs/` | Resolved by build | Mismatch |
| :--- | :---: | :---: | :--- |
| `agcp` | `1.9.1.303` | `1.9.5.302` (remote, via `libs.agconnect.agcp`) | **Yes** — local jar is 4 patch trains behind |
| `agconnect-core` | `1.9.1.303` | `1.9.5.302` (remote) | **Yes** — local AAR is stale |
| `agconnect-crash-symbol-lib` | `1.9.1.303` | *not referenced anywhere* | n/a — orphaned (see §4) |
| `agconnect-apms-plugin` | `1.6.2.300` | *not referenced anywhere* | n/a — deprecated/orphaned (see §4) |

Because Gradle resolves the remote `1.9.5.302` coordinates, the stale `1.9.1.303` local files are
**inert** for online builds — they are shadowed and never used. The risk is purely confusion and
git bloat, not a broken build.

---

## 3. Plan steps: done vs. outstanding

The plan offered two options. The repository has effectively taken **Option A (remote resolution)**
and additionally jumped straight to `1.9.5.302`.

| Plan item | Status | Evidence |
| :--- | :--- | :--- |
| Option A.1 — Add Huawei Maven repo to buildscript, drop `flatDir{app/libs}`, collapse classpaths to `agcp` | **Done** | `build.gradle.kts:33,40` |
| Option A.1 — Add `includeGroup("com.huawei.agconnect")` to `allprojects` filter | **Done** | `build.gradle.kts:69` |
| Option A.2 — App‑level `agconnect-core` → remote coordinate | **Done (at 1.9.5.302)** | `app/build.gradle.kts:1058–1059` |
| Bump `agcp` / `agconnect-core` to `1.9.5.302` | **Done** | `libs.versions.toml:3` |
| Drop standalone `agconnect-crash-symbol-lib` classpath | **Done** | absent from `build.gradle.kts` buildscript |
| Drop standalone `agconnect-apms-plugin` classpath | **Done** | absent from `build.gradle.kts` buildscript |
| **Option A.3 — Delete obsolete local files from `app/libs/`** | **NOT done** | all four binaries still tracked |

### Outstanding / decision points
1. **Delete obsolete local binaries (Option A step 3).** The four AGC files in §1 are no longer
   referenced by any Gradle script and can be removed from `app/libs/` and git. This is the only
   concrete leftover of the documented plan.
2. **APMS instrumentation was dropped, not migrated.** The plan §2 suggested either enabling APMS in
   the plugin (`agcp { enableAPMS = true }`) or adding the `com.huawei.agconnect:agconnect-apms:1.6.3.300`
   SDK. Neither was done — the standalone plugin classpath was simply removed and nothing replaced it.
   There is **no** `agcp { ... }` configuration block in `app/build.gradle.kts`. If APM telemetry is
   wanted, this is still an open task; if it is intentionally unused, no action is needed.
3. **Crash symbol upload.** The standalone `agconnect-crash-symbol-lib` classpath was removed without a
   replacement. Confirm that crash symbolication (if relied upon) is handled by the bundled `agcp`
   `1.9.5.302` plugin; otherwise this capability has been silently dropped.

---

## 4. Redundant local `.jar` / `.aar` files

All four AGC artifacts below are **redundant** and safe to migrate fully to remote Maven (they are
already resolved remotely or no longer referenced):

| File | Reason it is redundant | Remote equivalent |
| :--- | :--- | :--- |
| `agcp-1.9.1.303.jar` | Plugin now resolves remotely via `libs.agconnect.agcp` | `com.huawei.agconnect:agcp:1.9.5.302` |
| `agconnect-core-1.9.1.303.aar` | Core now resolved remotely in `hms*` configs | `com.huawei.agconnect:agconnect-core:1.9.5.302` |
| `agconnect-crash-symbol-lib-1.9.1.303.jar` | No `classpath`/`implementation` reference remains | `com.huawei.agconnect:agconnect-crash-symbol-lib:1.9.5.301` (only if re-enabled) |
| `agconnect-apms-plugin-1.6.2.300.jar` | Deprecated; no reference remains | `com.huawei.agconnect:agconnect-apms:1.6.3.300` (SDK, only if APMS re-enabled) |

**Not redundant — leave in place** (not AGC, still actively referenced):
* `libgsaverification-client.aar` — referenced by the Google‑services flavors and `treemalight`
  via `flatDir` (`app/build.gradle.kts:1024–1032`).
* `arm64-v8a/`, `armeabi-v7a/`, `x86/`, `x86_64/` — JNA `libjnidispatch.so` files consumed as
  `jniLibs` (`app/build.gradle.kts:591`). These are version‑pinned to the `jna` version in
  `libs.versions.toml`.

Because the `flatDir { dir("libs") }` repository (root `build.gradle.kts:57`) is still required for
`libgsaverification-client.aar`, it must **not** be removed even after the AGC files are deleted.

---

## 5. Documentation inconsistencies discovered

* **`agconnect-update-plan.md` is stale.** Its §1 "Analysis of Current Configuration" describes
  `flatDir { dir("app/libs") }` and four `1.9.1.303` local `classpath` entries. The build no longer
  looks like this — Option A has already been applied at `1.9.5.302`. The plan should be annotated as
  "largely executed; only the local‑file cleanup (Option A.3) remains."
* **`BUILD_LOCAL.md` Notes are partially inaccurate.** The "Notes" section (line 107) states the
  downloaded artifacts are "**not** committed to git (they're in `.gitignore`)". For the AGC binaries
  this is false: all four (`agcp`, `agconnect-core`, `agconnect-crash-symbol-lib`,
  `agconnect-apms-plugin`) are **tracked** in git and none are gitignored. (The note is likely meant
  for the Rust/protobuf prebuilt artifacts.) Its "Version Discrepancy" section (lines 94–103), by
  contrast, is accurate and corroborates this audit.

---

## 6. Recommended follow‑ups (no code changed by this audit)

1. **deps:** Delete the four stale AGC binaries from `app/libs/` and from git (Option A step 3),
   keeping `libgsaverification-client.aar` and the JNA ABI directories.
2. **docs:** Update `agconnect-update-plan.md` to reflect that the remote‑resolution migration to
   `1.9.5.302` is already complete; only local‑file removal is outstanding.
3. **docs:** Correct the `BUILD_LOCAL.md` "Notes" claim that AGC artifacts are gitignored.
4. **impl (decision):** Decide whether APMS and standalone crash‑symbol upload should be re‑enabled
   (via `agcp { enableAPMS = true }` and/or the `agconnect-apms` SDK) or are intentionally dropped.
