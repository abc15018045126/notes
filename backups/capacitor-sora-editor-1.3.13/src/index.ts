import { registerPlugin } from '@capacitor/core';
import type { SoraEditorPlugin } from './definitions';

const SoraEditor = registerPlugin<SoraEditorPlugin>('SoraEditor', {
    web: () => import('./web').then(m => new m.SoraEditorWeb()),
});

export * from './definitions';
export { SoraEditor };
