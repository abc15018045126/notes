import { WebPlugin } from '@capacitor/core';
import type { SoraEditorPlugin, SoraStartOptions } from './definitions';

export class SoraEditorWeb extends WebPlugin implements SoraEditorPlugin {
    async start(options: SoraStartOptions): Promise<void> {
        console.log('SoraEditor start', options);
    }
    async close(): Promise<void> {
        console.log('SoraEditor close');
    }
    async getText(): Promise<{ content: string }> {
        return { content: '' };
    }
    async setText(options: { content: string }): Promise<void> {
        console.log('SoraEditor setText', options);
    }
    async getSelection(): Promise<{ line: number; column: number }> {
        return { line: 0, column: 0 };
    }
    async setSelection(options: { line: number; column: number }): Promise<void> {
        console.log('SoraEditor setSelection', options);
    }
    async undo(): Promise<void> {
        console.log('SoraEditor undo');
    }
    async redo(): Promise<void> {
        console.log('SoraEditor redo');
    }
    async openEditor(options: { filePath: string; autoFocus?: boolean }): Promise<void> {
        console.log('SoraEditor openEditor', options);
    }
}
