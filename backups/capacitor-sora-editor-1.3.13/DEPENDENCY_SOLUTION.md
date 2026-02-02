# Dependency Solution: Full Source Integration (Standalone Mode)

## Problem Statement

This Capacitor plugin (`capacitor-sora-editor`) needs to include the `sora-editor` library. We initially tried:
1. **Local Project Dependency** ❌ (Fails for end-users without source)
2. **JitPack Build** ❌ (Unreliable, environment errors)
3. **Remote AAR via Ivy** ❌ (Requires manual GitHub Release uploads and complex Gradle configuration for consumers)
4. **Local AAR Bundling** ❌ (Strict Gradle restriction for Library modules)

## Final (Best) Solution: Full Source Integration

We have integrated the primary modules of `sora-editor` directly into the plugin as source code. This makes the plugin **completely standalone** and **zero-configuration** for users.

### Architecture

The following modules from `sora-editor` have been copied into `capacitor-sora-editor/android/sora-editor/`:
- `editor`: The core editor UI and logic.
- `language-textmate`: TextMate grammar support.
- `oniguruma-native`: JNI bindings for the Oniguruma regex engine.

### Implementation Details

#### 1. Source Sets Configuration
In the plugin's `android/build.gradle`, we manually include these source directories in the `main` sourceSet:

```gradle
sourceSets {
    main {
        java.srcDirs = [
            'src/main/java',
            'sora-editor/editor/src/main/java',
            'sora-editor/language-textmate/src/main/java',
            'sora-editor/oniguruma-native/src/main/java'
        ]
        res.srcDirs = [
            'src/main/res',
            'sora-editor/editor/src/main/res',
            'sora-editor/language-textmate/src/main/res',
            'sora-editor/oniguruma-native/src/main/res'
        ]
        assets.srcDirs = [
            'src/main/assets',
            'sora-editor/editor/src/main/assets',
            'sora-editor/language-textmate/src/main/assets',
            'sora-editor/oniguruma-native/src/main/assets'
        ]
    }
}
```

#### 2. Native Build (C++)
The Oniguruma engine is compiled using CMake directly from the integrated source:

```gradle
externalNativeBuild {
    cmake {
        path = file("sora-editor/oniguruma-native/src/main/cpp/CMakeLists.txt")
        version = "3.22.1"
    }
}
ndkVersion = "29.0.14206865"
```

#### 3. Module Namespace
To match the existing source code packagename and resource references, the plugin's `namespace` is set to:
`io.github.abc15018045126.sora`

## Why This is the Best Approach

| Feature | Remote AAR | Source Integration |
|---------|------------|--------------------|
| **Setup for Users** | Complex (Requires Ivy repo config) | **Zero (Standard npm install)** |
| **Development** | Slow (Build -> Upload -> Download) | **Fast (Direct source edits)** |
| **Stability** | Depends on remote server | **Self-contained** |
| **Binary Size** | Fixed | Optimized by user's proguard/R8 |
| **Sync Issues** | Common (missing AAR in build cache) | **None** |

## Maintenance

As the author, you can now modify the editor code directly within the plugin's `sora-editor` directory. These changes will be picked up immediately by any app using the local plugin (e.g., the `notes` app).

When you are ready to release, simply:
1. `npm version patch`
2. `npm publish`

The complete source code will be packaged into the npm tarball, and Gradle will compile it on the user's machine during their app's build process.
