# Development Workflow

This document describes the development workflow for `capacitor-sora-editor` and its integration with the Notes applications.

## Project Structure

```
notes/
├── capacitor-sora-editor/         # Plugin source code (standalone)
│   └── android/sora-editor/       # Integrated sora-editor source (CORE)
├── sora-editor/                   # Original Android library source (External)
├── notes/                         # Development app (uses local plugin)
└── notes-new/                     # Testing app (uses npm published plugin)
```

## Development Environments

### `notes` - Development Environment

**Purpose**: Active development and debugging of `capacitor-sora-editor`.

**Configuration**:
```json
{
  "dependencies": {
    "capacitor-sora-editor": "file:capacitor-sora-editor"
  }
}
```

**Usage**:
- Modify plugin code directly in `capacitor-sora-editor/android/src`
- Modify core editor code in `capacitor-sora-editor/android/sora-editor/`
- Changes are immediately reflected upon rebuilding the app.

### `notes-new` - Testing Environment

**Purpose**: Validate published npm package as end-users would experience it.

**Configuration**:
```json
{
  "dependencies": {
    "capacitor-sora-editor": "^1.1.0"
  }
}
```

**Usage**:
- Install from npm registry.
- Test the exact package users will receive.
- No special Gradle configuration required for users.

## Development Workflow

### 1. Developing New Features

Work in the `notes` project for rapid iteration. You can modify either the plugin wrapper or the editor core:

```bash
cd notes

# 1. Edit code in capacitor-sora-editor/android/
#    - Wrapper: src/main/java/...
#    - Core Editor: sora-editor/editor/...

# 2. Build and test
npm run build
npx cap sync android
cd android && ./gradlew.bat assembleDebug

# 3. Install to device
./gradlew.bat installDebug
```

### 2. Updating Core Logic (sora-editor)

Since the source is integrated, you no longer need to build AARs or upload to GitHub Releases for functional updates. Simply editing the files in `capacitor-sora-editor/android/sora-editor/` is enough for local development.

## Publishing Workflow

When you are satisfied with your changes and want to release to npm:

### Step 1: Bump Version & Build

```bash
cd capacitor-sora-editor

# Update version (e.g., 1.1.0 -> 1.1.1)
npm version patch

# Build the web part of the plugin
npm run build
```

### Step 2: Publish to npm

```bash
cd capacitor-sora-editor
npm publish
```

The entire `android/sora-editor` source tree will be included in the npm package.

### Step 3: Validate in notes-new

```bash
cd notes-new

# Install the latest version
npm install capacitor-sora-editor@latest

# Standard Capacitor sync and build
npx cap sync android
cd android && ./gradlew.bat assembleDebug
```

## Troubleshooting

### Issue: "Unresolved reference: R"

**Cause**: Package namespace mismatch.
**Solution**: The plugin namespace is set to `io.github.abc15018045126.sora` in `android/build.gradle` to match the core editor's resource usage. Ensure all new activity/fragment code uses the correct R import if necessary.

### Issue: C++ Compilation Errors

**Cause**: Missing NDK or CMake issues.
**Solution**: Ensure you have NDK `29.0.14206865` installed via Android Studio SDK Manager. The build will automatically compile the Oniguruma engine using CMake.

### Issue: Changes not reflected

**Cause**: Gradle cache.
**Solution**:
```bash
cd android
./gradlew.bat clean
```

## Quick Reference

| Task | Command |
|------|---------|
| Local Test | `cd notes && npm run build && npx cap sync android` |
| Sync Android | `npx cap sync android` |
| Build (Dev) | `cd android && ./gradlew.bat assembleDebug` |
| Install | `cd android && ./gradlew.bat installDebug` |
| Publish | `cd capacitor-sora-editor && npm publish` |

## Related Documentation

- [DEPENDENCY_SOLUTION.md](./DEPENDENCY_SOLUTION.md) - Why we use source integration.
- [NO_JITPACK_WARNING.md](./NO_JITPACK_WARNING.md) - Why we avoid JitPack.
