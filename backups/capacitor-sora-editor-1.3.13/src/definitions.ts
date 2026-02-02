import type { PluginListenerHandle } from '@capacitor/core';

export interface SoraEditorPlugin {
    start(options: SoraStartOptions): Promise<void>;
    close(): Promise<void>;
    getText(): Promise<{ content: string }>;
    setText(options: { content: string }): Promise<void>;
    getSelection(): Promise<{ line: number; column: number }>;
    setSelection(options: { line: number; column: number }): Promise<void>;
    undo(): Promise<void>;
    redo(): Promise<void>;
    openEditor(options: { filePath: string; autoFocus?: boolean }): Promise<void>;
    addListener(eventName: 'onEditorClick', listenerFunc: () => void): Promise<PluginListenerHandle>;
    addListener(eventName: 'onContentChange', listenerFunc: () => void): Promise<PluginListenerHandle>;
}

export interface SoraStartOptions {
    content?: string;
    top?: number;
    left?: number;
    right?: number;
    bottom?: number;
    width?: number;
    height?: number;
    fontSize?: number;
    showLineNumbers?: boolean;
    wordWrap?: boolean;
    editable?: boolean;
    backgroundColor?: string;
    selectionLine?: number;
    selectionColumn?: number;
    searchMatchBackgroundColor?: string;
    lineSpacingMultiplier?: number;
    lineSpacingExtra?: number;
    wrapLineSpacingMultiplier?: number;
    wrapLineSpacingExtra?: number;
    horizontalPadding?: number;
    highlightCurrentLine?: boolean;
    currentLineBackgroundColor?: string;
    cursorColor?: string;
    handleColor?: string;
    cursorWidth?: number;
    handleStyle?: 'drop' | 'side_drop' | 'none';
    searchAsRegExp?: boolean;
    searchWholeWord?: boolean;
    searchMatchCase?: boolean;
    fontFamily?: string;
    scrollbarColor?: string;
    showScrollLineInfo?: boolean;
    scrollbarStyle?: 'default' | 'rounded';
    keyboardAdjust?: boolean;
    symbolBarColor?: string;
    symbolTextColor?: string;
    symbolBarStyle?: 'rounded' | 'flat' | 'classic';
}
