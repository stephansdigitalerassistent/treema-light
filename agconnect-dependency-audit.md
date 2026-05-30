# Huawei AGConnect Dependency Audit

This audit reconciles the proposed [`agconnect-update-plan.md`](./agconnect-update-plan.md) against the
**actual** state of the build configuration and the vendored binaries in
[`app/libs/`](./app/libs). It records version mismatches, which steps of the plan have already been
executed (and which remain), redundant local artifacts, and documentation inconsistencies.

> **Headline finding:** The build scripts have already been migrated to remote Maven resolution at
> **`1.9.5.302`** — i.e. the plan's *recommended* Option A is fully implemented. The four stale local
> AGC binaries have been deleted from `app/libs/` and git. APMS and standalone crash-symbol upload
> have been evaluated and are intentionally dropped.

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

### Vendored binaries in `app/libs/` (Deleted in commit `557e35a`)
| File | Vendored version | Git status |
| :--- | :---: | :--- |
| `agcp-1.9.1.303.jar` | `1.9.1.303` | deleted |
| `agconnect-core-1.9.1.303.aar` | `1.9.1.303` | deleted |
| `agconnect-crash-symbol-lib-1.9.1.303.jar` | `1.9.1.303` | deleted |
| `agconnect-apms-plugin-1.6.2.300.jar` | `1.6.2.300` | deleted |

(Also present and **out of scope** for AGC: `libgsaverification-client.aar` and the
`arm64-v8a/ armeabi-v7a/ x86/ x86_64/` ABI directories holding JNA `.so` files.)

---

## 2. Version mismatches

*(Note: Stale local files listed below have been deleted in commit `557e35a`)*

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
| **Option A.3 — Delete obsolete local files from `app/libs/`** | **Done** | all four binaries deleted in commit `557e35a` |

### Outstanding / decision points (All Resolved)
1. **Delete obsolete local binaries (Option A step 3).** **Done** — Stale local binaries have been removed from `app/libs/` and untracked in git.
2. **APMS instrumentation was dropped, not migrated.** **Intentionally Dropped** — Threema does not include proprietary telemetry, tracking, or analytics SDKs like Huawei APMS in its builds. The standalone plugin classpath was a legacy addition and was never applied to the app module.
3. **Crash symbol upload.** **Intentionally Dropped** — Standalone crash-symbol upload has been intentionally dropped. Modern `agcp` has built-in crash-symbol uploading if crash reporting is enabled, and automatic crash reporting upload is currently unsupported/not implemented in the app anyway.

---

## 4. Redundant local `.jar` / `.aar` files (Deleted in commit `557e35a`)

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

* **`agconnect-update-plan.md` has been updated.** The plan is annotated to reflect that remote Maven resolution, local file deletion, and final decisions to drop APMS and standalone crash symbol upload have been completed.
* **`BUILD_LOCAL.md` Notes are partially inaccurate.** The "Notes" section (line 107) states the
  downloaded artifacts are "**not** committed to git (they're in `.gitignore`)". For the AGC binaries
  this is false: all four (`agcp`, `agconnect-core`, `agconnect-crash-symbol-lib`,
  `agconnect-apms-plugin`) are **tracked** in git and none are gitignored. (The note is likely meant
  for the Rust/protobuf prebuilt artifacts.) Its "Version Discrepancy" section (lines 94–103), by
  contrast, is accurate and corroborates this audit.

---

## 6. Recommended follow‑ups (All Resolved)

1. **deps (Done):** Deleted the four stale AGC binaries from `app/libs/` and from git in commit `557e35a`.
2. **docs (Done):** Updated `agconnect-update-plan.md` to reflect that the remote-resolution migration, local-file deletion, and final decisions are complete.
3. **docs (Done):** Corrected the `BUILD_LOCAL.md` "Notes" claim that AGC artifacts are gitignored (updated to reflect they are not gitignored but were git-tracked before deletion).
4. **impl (decision) (Done):** Formally decided that APMS and standalone crash-symbol upload are intentionally dropped.
