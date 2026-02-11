# Sora Editor Gutter Customization Summary

## Overview
This update enhances the `capacitor-sora-editor` plugin by adding granular control over the editor's gutter (line number area). Users can now customize the position, alignment, and color of line numbers and the vertical divider.

## New Features

### 1. Line Number Alignment
- **Feature**: Users can now choose how the line number text is aligned within its column.
- **Options**: Left, Center, Right.
- **UI**: New buttons added to the "Editor Settings" -> "Editor Preferences" section.
- **Refinement**: Fixed an issue where the top-most ("sticky") line number would jump or misalign when using Center or Right alignment.

### 2. Line Number Position
- **Feature**: Users can toggle the position of the line number relative to the vertical divider.
- **Options**:
    - **Left of Divider (Default)**: Standard IDE layout (Line Numbers | Divider | Code).
    - **Right of Divider**: Alternative layout (Divider | Line Numbers | Code).
- **UI**: New switch "Line number to the right of divider" added to settings.

### 3. Color Customization
- **Feature**: Users can customized the colors of the gutter elements.
- **Options**:
    - **Line Number Color**: Customizable color for the line number text.
    - **Divider Color**: Customizable color for the vertical line separating line numbers from code.
- **UI**: New color picker sections added for "Line Number Color" and "Divider Color".

## Technical Implementation

### modified Files
- **`CodeEditor.kt`**: Added properties `lineNumberAlign`, `isLineNumberRightOfDivider`, and methods to support them.
- **`EditorRenderer.kt`**: 
    - Updated `drawView` to respect `lineNumberAlign` and `isLineNumberRightOfDivider`.
    - Fixed `paint` alignment in the `drawView` logic for the first visible line number to ensure it matches the user's setting, preventing visual jumping.
- **`EditorViewModel.kt`**: Added state variables and persistence logic for the new settings.
- **`EditorScreen.kt`**: Updated the settings UI.
    - **Refactor**: Extracted the color settings UI logic into a new file `EditorColorSettings.kt` to reduce the size of `EditorScreen.kt` and improve maintainability.
- **`EditorColorSettings.kt`**: New file containing the Composable functions for color customization.

## Verification
- **Build Status**: Successful (`:app:assembleDebug`).
- **Installation**: Successfully installed on the connected device.
- **Testing**:
    1. **Alignment**: Switching between Left, Center, and Right alignment now correctly positions all line numbers, including the top-most one.
    2. **Position**: Toggling the line number position relative to the divider works as expected.
    3. **Color**: Custom colors are applied correctly.
    4. **Refactor Check**: The Settings UI should load and look exactly the same as before, with color options working correctly.
