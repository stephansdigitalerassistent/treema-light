# Huawei AppGallery Connect Dependency Evaluation and Proposed Update Plan

This document evaluates the current Huawei AppGallery Connect dependencies, details available upgrades, reviews security implications, and proposes a path forward.

---

## 1. Analysis of Current Configuration

Currently, the AppGallery Connect (AGC) plugin and SDK dependencies are integrated as local binary assets (JAR/AAR) rather than remote Gradle coordinates.

### Local Artifacts in [app/libs/](file:///home/ubuntu/treema-light/app/libs)
*   **`agcp-1.9.1.303.jar`**: The AppGallery Connect Gradle plugin.
*   **`agconnect-core-1.9.1.303.aar`**: The core runtime library for Huawei mobile services integration.
*   **`agconnect-crash-symbol-lib-1.9.1.303.jar`**: Symbolication tool for crash reports.
*   **`agconnect-apms-plugin-1.6.2.300.jar`**: The standalone APM (App Performance Management) plugin.

### Integration in [build.gradle.kts](file:///home/ubuntu/treema-light/build.gradle.kts)
The buildscript dependencies block references these files using a local repository:
```kotlin
buildscript {
    repositories {
        google()
        mavenCentral()
        flatDir { dir("app/libs") }
    }
    dependencies {
        classpath(libs.kotlin.gradle)
        classpath(libs.android.gradle)

        // Huawei agconnect plugin
        classpath("com.huawei.agconnect:agcp-1.9.1.303")
        classpath("com.huawei.agconnect:agconnect-crash-symbol-lib-1.9.1.303")
        classpath("com.huawei.agconnect:agconnect-apms-plugin-1.6.2.300")
        classpath("com.huawei.agconnect:agconnect-core-1.9.1.303@aar")
    }
}
```

### Integration in [app/build.gradle.kts](file:///home/ubuntu/treema-light/app/build.gradle.kts)
Local dependency references are declared under the `hms` and `hms_work` configurations:
```kotlin
"hmsImplementation"(group = "", name = "agconnect-core-1.9.1.303", ext = "aar")
"hms_workImplementation"(group = "", name = "agconnect-core-1.9.1.303", ext = "aar")
```

> [!NOTE]
> Using `flatDir` for local dependency resolution prompts the following build configuration warning:
> `WARNING: Using flatDir should be avoided because it doesn't support any meta-data formats.`

---

## 2. Evaluation of Available Upgrades

| Dependency | Current Version | Latest Stable Version | Release Date | Upgrade Status |
| :--- | :---: | :---: | :---: | :--- |
| **`agcp`** | `1.9.1.303` | `1.9.5.302` | March 2026 | **Available** |
| **`agconnect-core`** | `1.9.1.303` | `1.9.5.302` | March 2026 | **Available** |
| **`agconnect-crash-symbol-lib`** | `1.9.1.303` | `1.9.5.301` | February 2026 | **Available** |
| **`agconnect-apms-plugin`** | `1.6.2.300` | — | — | **Deprecated** (Integrated into main `agcp` plugin) |

### Key Update & Integration Changes
*   **Deprecation of `agconnect-apms-plugin`**: The standalone APMS plugin is obsolete. Huawei has merged APM instrumentation directly into the main `agcp` plugin. To use it in newer versions, the standalone classpath dependency should be removed, and APMS should be enabled within the `agcp` configuration block in `app/build.gradle.kts`:
    ```kotlin
    agcp {
        enableAPMS = true
    }
    ```
*   **APMS SDK dependency**: The actual SDK itself is now `com.huawei.agconnect:agconnect-apms:1.6.3.300`. Note that the project currently does not depend on the APMS SDK directly in the `dependencies` block, indicating the plugin classpath is likely a legacy addition.

---

## 3. Security and Changelog Review

### **Security Vulnerability Assessment**
*   **General**: No high or critical CVEs (Common Vulnerabilities and Exposures) are currently registered specifically for the `com.huawei.agconnect` SDK packages within the `1.9.1.303` to `1.9.5.302` range.
*   **General Data Protection Regulation (GDPR) / Residency Compliance**: In version `1.9.5.302` (released March 12, 2026), Huawei resolved a critical issue where setting the data processing location failed (an issue introduced in `1.9.3.302`). Upgrading to `1.9.5.302` is highly recommended if users' data routing compliance is required for regulatory purposes.

### **Compatibility and Stability**
*   **Gradle 8.0+ Compatibility**: The version bump from older `1.9.0` series to `1.9.1.300+` was introduced to fix the deprecated `android.registerTransform` API removals. Since this project is using **Gradle 8.13** and **Android Gradle Plugin 8.12.3**, staying at a version >= `1.9.1` is strictly required to build. Upgrading to `1.9.5.302` brings better support for late-stage AGP 8.x/9.x compatibility.

---

## 4. Proposed Update Plan

We propose two options to execute this upgrade:

### Option A: Transition to Remote Repository Resolution (Recommended)
This approach removes local binary maintenance in `app/libs/`, resolving the Gradle metadata warnings and ensuring dependencies resolve correctly transitively.

1.  **Modify Project-Level `build.gradle.kts`**:
    Add the developer repository to the buildscript and project repositories, and update the coordinates:
    ```diff
     buildscript {
         repositories {
             google()
             mavenCentral()
    -        flatDir { dir("app/libs") }
    +        maven { url = uri("https://developer.huawei.com/repo/") }
         }
         dependencies {
             classpath(libs.kotlin.gradle)
             classpath(libs.android.gradle)
     
    -        // Huawei agconnect plugin
    -        classpath("com.huawei.agconnect:agcp-1.9.1.303")
    -        classpath("com.huawei.agconnect:agconnect-crash-symbol-lib-1.9.1.303")
    -        classpath("com.huawei.agconnect:agconnect-apms-plugin-1.6.2.300")
    -        classpath("com.huawei.agconnect:agconnect-core-1.9.1.303@aar")
    +        classpath("com.huawei.agconnect:agcp:1.9.5.302")
         }
     }
     
     allprojects {
         repositories {
             google()
             mavenCentral()
             maven("https://jitpack.io")
             flatDir { dir("libs") }
     
             // Huawei
             exclusiveContent {
                 forRepository {
                     maven("https://developer.huawei.com/repo/")
                 }
                 filter {
                     includeGroup("com.huawei.hms")
                     includeGroup("com.huawei.android.hms")
                     includeGroup("com.huawei.hmf")
    +                includeGroup("com.huawei.agconnect")
                 }
             }
         }
     }
    ```

2.  **Modify App-Level `app/build.gradle.kts`**:
    Update the implementation dependency definition to pull from the repository:
    ```diff
    -    "hmsImplementation"(group = "", name = "agconnect-core-1.9.1.303", ext = "aar")
    -    "hms_workImplementation"(group = "", name = "agconnect-core-1.9.1.303", ext = "aar")
    +    "hmsImplementation"("com.huawei.agconnect:agconnect-core:1.9.5.302")
    +    "hms_workImplementation"("com.huawei.agconnect:agconnect-core:1.9.5.302")
    ```

3.  **Delete Obsolete Local Files**:
    Remove `agcp-1.9.1.303.jar`, `agconnect-core-1.9.1.303.aar`, `agconnect-crash-symbol-lib-1.9.1.303.jar`, and `agconnect-apms-plugin-1.6.2.300.jar` from `app/libs/`.

---

### Option B: Retain Local Binary Files (Vendored Upgrade)
If offline compilation or strict control over third-party Maven calls is mandatory, the local files must be manually replaced:

1.  **Download New Dependencies**:
    Fetch the following artifacts from the Huawei Maven Repository (`https://developer.huawei.com/repo/`):
    *   `com.huawei.agconnect:agcp:1.9.5.302` (save as `agcp-1.9.5.302.jar`)
    *   `com.huawei.agconnect:agconnect-core:1.9.5.302` (save as `agconnect-core-1.9.5.302.aar`)
    *   `com.huawei.agconnect:agconnect-crash-symbol-lib:1.9.5.301` (save as `agconnect-crash-symbol-lib-1.9.5.301.jar`)

2.  **Update `app/libs/`**:
    Delete the old `.jar`/`.aar` files, and place the newly downloaded ones in `app/libs/`.

3.  **Update `build.gradle.kts`**:
    ```diff
         // Huawei agconnect plugin
    -    classpath("com.huawei.agconnect:agcp-1.9.1.303")
    -    classpath("com.huawei.agconnect:agconnect-crash-symbol-lib-1.9.1.303")
    -    classpath("com.huawei.agconnect:agconnect-apms-plugin-1.6.2.300")
    -    classpath("com.huawei.agconnect:agconnect-core-1.9.1.303@aar")
    +    classpath("com.huawei.agconnect:agcp-1.9.5.302")
    +    classpath("com.huawei.agconnect:agconnect-crash-symbol-lib-1.9.5.301")
    +    classpath("com.huawei.agconnect:agconnect-core-1.9.5.302@aar")
    ```

4.  **Update `app/build.gradle.kts`**:
    ```diff
    -    "hmsImplementation"(group = "", name = "agconnect-core-1.9.1.303", ext = "aar")
    -    "hms_workImplementation"(group = "", name = "agconnect-core-1.9.1.303", ext = "aar")
    +    "hmsImplementation"(group = "", name = "agconnect-core-1.9.5.302", ext = "aar")
    +    "hms_workImplementation"(group = "", name = "agconnect-core-1.9.5.302", ext = "aar")
    ```
