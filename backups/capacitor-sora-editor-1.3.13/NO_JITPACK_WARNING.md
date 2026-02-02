# WARNING: DO NOT USE JITPACK

This project strictly avoids using JitPack for dependency resolution.

## Reasons:
1.  **Build Instability**: JitPack builds frequently fail or time out, especially with complex Gradle configurations or submodules (e.g., `sora-editor` structure).
2.  **Environment Mismatches**: JitPack's build environment (Java version, Android SDK) often differs from local or production environments, leading to `Cannot find a Java installation` errors.
3.  **Local Control**: We distribute a pre-built `editor-release.aar` bundled directly within `capacitor-sora-editor`. This ensures that exactly what works locally is what users get.

## Instructions for Maintainers:
*   **NEVER** add `maven { url 'https://jitpack.io' }` to `android/build.gradle`.
*   **ALWAYS** use the local AAR located in `android/libs/editor-release.aar`.
*   **Dependency Format**: Use `compileOnly files("libs/editor-release.aar")` in the plugin's `build.gradle` to avoid "Direct local .aar file dependencies are not supported" errors during packaging.
*   **Consumer Setup**: Consumers (apps using this plugin) will need to ensure the AAR is correctly included or accessible if not automatically handled by Capacitor's tooling.
