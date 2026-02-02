# capacitor-sora-editor

A powerful, high-performance code editor plugin for Capacitor 8+, based on the customized **Sora Editor**. It provides both a full-screen Material 3 editor activity and an overlay editor view.

[![npm version](https://img.shields.io/npm/v/capacitor-sora-editor.svg)](https://www.npmjs.com/package/capacitor-sora-editor)
[![GitHub](https://img.shields.io/github/license/abc15018045126/capacitor-sora-editor)](https://github.com/abc15018045126/capacitor-sora-editor)

## 📦 Installation

```bash
npm install capacitor-sora-editor
npx cap sync android
```

## 🔗 Repository
[https://github.com/abc15018045126/capacitor-sora-editor](https://github.com/abc15018045126/capacitor-sora-editor)

## 1. Features
- **Modern UI**: Full Material 3 design with Jetpack Compose.
- **Rich Configuration**: Supports font size, line spacing, word wrap, and exhaustive color customization.
- **Customizable Scrollbars**: Toggle between standard "Sora" (square) and "Chrome" (rounded) styles.
- **Search & Replace**: Built-in search panel with regex, case sensitivity, and whole word matching.
- **Table of Contents**: Automatic chapter/TOC generation based on content chunks.
- **Persistence**: Built-in JSON-based settings system for saving/loading editor preferences.
- **Selection Control**: Full API for getting/setting cursor position and handling selection.

## 2. Dependencies
This plugin relies on:
- **Capacitor 8.0.0+**
- **Sora Editor (Customized)**: A highly optimized Android code editor.
- **Jetpack Compose**: Used for the editor's UI components and settings screens.
- **Material 3**: Modern design system for all UI elements.

## 3. Detailed Usage Guide

### 3.1 Installation
Add the plugin to your project:
```bash
npm install capacitor-sora-editor
```
Then sync your project with the native Android platform:
```bash
npx cap sync android
```

### 3.2 Full-screen Activity Mode (`openEditor`)
This mode launches a dedicated Android Activity (`ComposeEditorActivity`). It is self-contained, handles its own status bar/navigation bar colors, and includes a full settings UI and TOC panel. Use this for primary file editing.

```typescript
import { SoraEditor } from 'capacitor-sora-editor';
import { Filesystem, Directory } from '@capacitor/filesystem';

async function openMyFile() {
  // 1. Get the native file URI (required by the plugin)
  const { uri } = await Filesystem.getUri({
    path: 'my_notes/draft.txt',
    directory: Directory.Documents
  });

  // 2. Launch the full-screen editor
  await SoraEditor.openEditor({
    filePath: uri,
    autoFocus: true // Automatically focus the editor and show the keyboard
  });
}
```
*Note: This mode automatically saves settings (like theme, font size, etc.) to Android SharedPreferences.*

### 3.3 Overlay View Mode (`start`)
This mode adds a `CodeEditor` view directly to your app's main layout, floating on top of the Capacitor WebView. It's ideal for inline editing or quick code snippets.

```typescript
import { SoraEditor } from 'capacitor-sora-editor';

async function launchOverlay() {
  await SoraEditor.start({
    content: "console.log('Hello Sora!');",
    fontSize: 16,
    fontFamily: "JetBrains Mono",
    showLineNumbers: true,
    wordWrap: false,
    scrollbarStyle: 'rounded', // 'default' (Sora style) or 'rounded' (Chrome style)
    
    // Position/Size (in dp)
    top: 60,
    bottom: 0,
    left: 0,
    right: 0,
    
    // Custom Colors
    backgroundColor: "#1e1e1e",
    cursorColor: "#0078d4",
    handleColor: "#0078d4",
    currentLineBackgroundColor: "#2a2a2a",
    
    // Spacing
    lineSpacingMultiplier: 1.2,
    horizontalPadding: 16
  });
}
```

### 3.4 Content Management
Once the editor is started in overlay mode, use these methods to interact with the text.

```typescript
// Get the current text
const { content } = await SoraEditor.getText();

// Update text programmatically
await SoraEditor.setText({ content: "New content here" });

// History control
await SoraEditor.undo();
await SoraEditor.redo();
```

### 3.5 Selection & Cursor
Control the cursor position precisely.

```typescript
// Get current cursor (0-indexed)
const { line, column } = await SoraEditor.getSelection();

// Move cursor to specific position
await SoraEditor.setSelection({
  line: 10,
  column: 5
});
```

### 3.6 Event Listeners
Respond to user actions within the editor.

```typescript
// Triggered whenever the text changes
const contentSub = await SoraEditor.addListener('onContentChange', () => {
  console.log('Text changed!');
});

// Triggered when the user taps the editor area
const clickSub = await SoraEditor.addListener('onEditorClick', () => {
  console.log('Editor was clicked');
});

// To stop listening:
// contentSub.remove();
```

### 3.7 Closing the Overlay
```typescript
await SoraEditor.close();
```

## 4. Development & Modification Guide

### 4.1 How to Build
To rebuild the TypeScript/JavaScript part of the plugin:
```bash
cd capacitor-sora-editor
npm install
npm run build
```

### 4.2 Modifying the Android Part
The Android source is located in `android/src/main/java/com/abc15018045126/capacitor/soraeditor/`.
- **`compose/ui/EditorScreen.kt`**: The main Jetpack Compose UI. Modify this to change the layout, add buttons, or customize the settings screen.
- **`compose/EditorViewModel.kt`**: Handles the logic, file I/O, and JSON settings persistence.
- **`SoraEditorPlugin.kt`**: The bridge between Capacitor (JS) and Android (Kotlin).

**Steps to modify and test:**
1. Edit the Kotlin files in Android Studio or your favorite IDE.
2. In your main project, run `npx cap copy android`.
3. Run the app: `npx cap run android`.

### 4.3 Adding New Setting Items
1. Add a new property to `EditorUiState` in `EditorViewModel.kt`.
2. Implement a setter function in `EditorViewModel` that updates the state and calls `saveSettings`.
3. Add a corresponding UI control (like `SettingsSwitchItem` or `Slider`) in `EditorScreen.kt`.
4. Update `saveSettings` and `loadSettings` in `EditorViewModel.kt` to ensure your new setting is serialized to/from the settings JSON.

## 5. API Reference (SoraStartOptions)

| Property | Type | Default | Description |
| :--- | :--- | :--- | :--- |
| `content` | `string` | `""` | Initial text content |
| `fontSize` | `number` | `18` | Font size in sp |
| `showLineNumbers` | `boolean` | `true` | Display line numbers on the left |
| `wordWrap` | `boolean` | `false` | Enable/disable horizontal word wrapping |
| `backgroundColor` | `string` | `"#FFFFFF"` | Hex color string |
| `scrollbarStyle` | `'default' \| 'rounded'` | `'default'` | Square or Rounded (Chrome-like) scrollbars |
| `handleStyle` | `'drop' \| 'side_drop' \| 'none'` | `'side_drop'` | Appearance of text selection handles |
| `fontFamily` | `string` | `"Monospace"` | Supports "JetBrains Mono", "Ubuntu", "Roboto" |
| `highlightCurrentLine`| `boolean` | `true` | Highlight the line where the cursor is |
| `keyboardAdjust` | `boolean` | `true` | Enable adjustResize for keyboard handling |
| `symbolBarColor` | `string` | `"#F5F5F5"` | Background color of shortcut symbol bar |
| `symbolTextColor` | `string` | `"#000000"` | Text color of shortcut symbols |
| `symbolBarStyle` | `'rounded' \| 'flat' \| 'classic'` | `'rounded'` | 'flat' has no boxes around symbols |
| `cursorWidth` | `number` | `2.0` | Width of the cursor caret in px |
| `horizontalPadding` | `number` | `12` | Left/right editor padding |

## License
MIT
