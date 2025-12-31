import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Filesystem, Directory, Encoding } from '@capacitor/filesystem';
import { App as CapApp } from '@capacitor/app';
import { registerPlugin } from '@capacitor/core';

const OpenFolder = registerPlugin<any>('OpenFolder');

interface Note {
    id: string;
    title: string;
    content: string;
    time: number;
    isNew?: boolean;
}

const DIR = 'QuickNotes';

const App: React.FC = () => {
    const [view, setView] = useState<'list' | 'editor' | 'settings'>('list');
    const [notes, setNotes] = useState<Note[]>([]);
    const [curId, setCurId] = useState<string | null>(null);
    const [lang, setLang] = useState<'zh' | 'en'>(
        (localStorage.getItem('lang') as 'zh' | 'en') || 'zh'
    );
    const [theme, setTheme] = useState<'dark' | 'light'>(
        (localStorage.getItem('theme') as 'dark' | 'light') || 'dark'
    );
    const [docPath, setDocPath] = useState<string>('...');
    const [searchQuery, setSearchQuery] = useState('');
    const [isLoading, setIsLoading] = useState(true);
    const textareaRef = useRef<HTMLTextAreaElement>(null);

    const t = {
        zh: {
            title: '便签',
            search: '搜索内容...',
            noNotes: '还没有便签',
            noMatch: '没找到匹配项',
            settings: '设置',
            theme: '颜色主题',
            lang: '语言 / Lang',
            path: '数据目录 (点击打开)：',
            delConfirm: '确认删除？',
            placeholder: '写点什么...',
            dark: '深色',
            light: '浅色',
            back: '返回',
            newNote: '新便签.txt'
        },
        en: {
            title: 'Notes',
            search: 'Search...',
            noNotes: 'No notes found',
            noMatch: 'No matches found',
            settings: 'Settings',
            theme: 'Appearance',
            lang: 'Language',
            path: 'Storage (Click to open):',
            delConfirm: 'Delete this note?',
            placeholder: 'Type here...',
            dark: 'Dark',
            light: 'Light',
            back: 'Back',
            newNote: 'New Note.txt'
        }
    }[lang];

    useEffect(() => {
        document.documentElement.setAttribute('data-theme', theme);
        localStorage.setItem('theme', theme);
    }, [theme]);

    useEffect(() => {
        localStorage.setItem('lang', lang);
    }, [lang]);

    const reloadNotes = useCallback(async () => {
        try {
            await Filesystem.mkdir({ path: DIR, directory: Directory.Documents, recursive: true }).catch(() => { });
            const { files } = await Filesystem.readdir({ path: DIR, directory: Directory.Documents });

            const notePromises = files
                .filter(f => f.name.endsWith('.txt'))
                .map(async f => {
                    const content = await Filesystem.readFile({
                        path: `${DIR}/${f.name}`,
                        directory: Directory.Documents,
                        encoding: Encoding.UTF8
                    });
                    return {
                        id: f.name,
                        title: f.name,
                        content: content.data as string,
                        time: (f as any).mtime || Date.now()
                    };
                });

            const loaded = await Promise.all(notePromises);
            setNotes(loaded);

            const uri = await Filesystem.getUri({ path: DIR, directory: Directory.Documents });
            setDocPath(uri.uri);
        } finally {
            setIsLoading(false);
        }
    }, []);

    useEffect(() => {
        reloadNotes();
    }, [reloadNotes]);


    const saveToDisk = useCallback(async (id: string, content: string) => {
        try {
            await Filesystem.writeFile({
                path: `${DIR}/${id}`,
                data: content,
                directory: Directory.Documents,
                encoding: Encoding.UTF8
            });
            setNotes(prev => prev.map(n => n.id === id ? { ...n, content, time: Date.now() } : n));
        } catch (e) { }
    }, []);

    const saveTimeoutRef = useRef<number | undefined>(undefined);
    const handleInput = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        const content = e.target.value;
        if (curId) {
            window.clearTimeout(saveTimeoutRef.current);
            saveTimeoutRef.current = window.setTimeout(() => saveToDisk(curId, content), 300);
        }
    };

    const createNewNote = () => {
        const id = `temp_${Date.now()}.txt`;
        const newNote: Note = { id, title: t.newNote, content: '', time: Date.now(), isNew: true };
        setNotes(prev => [newNote, ...prev]);
        setCurId(id);
        setView('editor');
        setTimeout(() => textareaRef.current?.focus(), 300);
    };

    const openNote = (id: string) => {
        setCurId(id);
        setView('editor');
        setTimeout(() => textareaRef.current?.focus(), 300);
    };

    const closeEditor = useCallback(async () => {
        if (curId && textareaRef.current) {
            const content = textareaRef.current.value;
            const note = notes.find(n => n.id === curId);
            let finalId = curId;
            if (note?.isNew && content.trim()) {
                const now = new Date();
                const dateStr = `${now.getFullYear()}.${String(now.getMonth() + 1).padStart(2, '0')}.${String(now.getDate()).padStart(2, '0')}`;
                const firstLine = content.split('\n')[0].trim().substring(0, 15).replace(/[\\\/:*?"<>|]/g, '');
                if (firstLine) finalId = `${firstLine} ${dateStr}.txt`;
            }

            try {
                await Filesystem.writeFile({ path: `${DIR}/${finalId}`, data: content, directory: Directory.Documents, encoding: Encoding.UTF8 });
                if (finalId !== curId && curId.startsWith('temp_')) {
                    await Filesystem.deleteFile({ path: `${DIR}/${curId}`, directory: Directory.Documents }).catch(() => { });
                }
            } catch (e) { }
        }
        setView('list');
        setCurId(null);
        reloadNotes();
    }, [curId, notes, reloadNotes]);

    useEffect(() => {
        const backHandler = CapApp.addListener('backButton', () => {
            if (view === 'editor') {
                closeEditor();
            } else if (view === 'settings') {
                setView('list');
            } else {
                CapApp.exitApp();
            }
        });
        return () => { backHandler.then(h => h.remove()); };
    }, [view, closeEditor]);

    const deleteNote = async () => {
        if (!curId) return;
        if (window.confirm(t.delConfirm)) {
            try {
                await Filesystem.deleteFile({ path: `${DIR}/${curId}`, directory: Directory.Documents });
                setNotes(prev => prev.filter(n => n.id !== curId));
                setView('list');
                setCurId(null);
            } catch (e) { }
        }
    };

    const curNote = notes.find(n => n.id === curId);

    const filteredNotes = React.useMemo(() => {
        const query = searchQuery.toLowerCase().trim();
        const sorted = [...notes].sort((a, b) => b.time - a.time);
        if (!query) return sorted;
        return sorted.filter(note =>
            note.title.toLowerCase().includes(query) ||
            note.content.toLowerCase().includes(query)
        );
    }, [notes, searchQuery]);

    if (isLoading) return null;

    return (
        <div className="app app-ready">
            <div className={`view ${view === 'list' ? '' : 'view-hidden'}`}>
                <header>
                    <h1>{t.title}</h1>
                    <button className="btn-icon" onClick={() => setView('settings')}>
                        <svg width="24" height="24" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24">
                            <circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 010 2.83 2 2 0 01-2.83 0l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-2 2 2 2 0 01-2-2v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 01-2.83 0 2 2 0 010-2.83l.06-.06a1.65 1.65 0 00.33-1.82 1.65 1.65 0 00-1.51-1H3a2 2 0 01-2-2 2 2 0 012-2h.09A1.65 1.65 0 004.6 9a1.65 1.65 0 00-.33-1.82l-.06-.06a2 2 0 010-2.83 2 2 0 012.83 0l.06.06a1.65 1.65 0 001.82.33H9a1.65 1.65 0 001-1.51V3a2 2 0 012-2 2 2 0 012 2v.09a1.65 1.65 0 001 1.51 1.65 1.65 0 001.82-.33l.06-.06a2 2 0 012.83 0 2 2 0 010 2.83l-.06.06a1.65 1.65 0 00-.33 1.82V9a1.65 1.65 0 001.51 1H21a2 2 0 012 2 2 2 0 01-2 2h-.09a1.65 1.65 0 00-1.51 1z" />
                        </svg>
                    </button>
                </header>
                <div className="search-bar-container">
                    <input
                        type="text"
                        className="search-input"
                        placeholder={t.search}
                        value={searchQuery}
                        onChange={(e) => setSearchQuery(e.target.value)}
                    />
                </div>
                <div className="list-container">
                    {filteredNotes.map(note => (
                        <div key={note.id} className="note-card" onClick={() => openNote(note.id)}>
                            <div className="note-title">{note.title}</div>
                            <div className="note-desc">{note.content.substring(0, 40) || '...'}</div>
                            <div className="note-time">
                                {new Date(note.time).toLocaleString([], { hour: '2-digit', minute: '2-digit', year: 'numeric', month: '2-digit', day: '2-digit' })}
                            </div>
                        </div>
                    ))}
                </div>
                <button id="fab" onClick={createNewNote}>+</button>
            </div>

            <div className={`view ${view === 'editor' ? '' : 'view-hidden'}`}>
                <header>
                    <button className="btn-icon" onClick={closeEditor}>✕</button>
                    <div style={{ fontWeight: 700, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', margin: '0 10px' }}>
                        {curNote?.title || 'Note'}
                    </div>
                    <button className="btn-icon" onClick={deleteNote}>🗑️</button>
                </header>
                <textarea
                    key={curId || 'none'}
                    ref={textareaRef}
                    id="editor-area"
                    placeholder={t.placeholder}
                    defaultValue={curNote?.content || ''}
                    onChange={handleInput}
                />
            </div>

            <div className={`view ${view === 'settings' ? '' : 'view-hidden'}`}>
                <header>
                    <button className="btn-icon" onClick={() => setView('list')}>←</button>
                    <h1>{t.settings}</h1>
                    <div style={{ width: '44px' }}></div>
                </header>
                <div className="settings-content">
                    <div className="settings-row">
                        <span>{t.theme}</span>
                        <button className="theme-btn" onClick={() => setTheme(th => th === 'dark' ? 'light' : 'dark')}>
                            {theme === 'dark' ? t.light : t.dark}
                        </button>
                    </div>
                    <div className="settings-row">
                        <span>{t.lang}</span>
                        <button className="theme-btn" onClick={() => setLang(l => l === 'zh' ? 'en' : 'zh')}>
                            {lang === 'zh' ? 'English' : '中文'}
                        </button>
                    </div>
                    <span className="path-label">{t.path}</span>
                    <div
                        className="path-text"
                        style={{ cursor: 'pointer', border: '1px solid var(--primary)', marginTop: '5px' }}
                        onClick={async () => {
                            try {
                                await OpenFolder.open();
                            } catch (e) {
                                alert('Error: ' + e);
                            }
                        }}
                    >
                        {docPath}
                    </div>
                </div>
            </div>

            <style>{`
        .app { 
          height: 100%; width: 100%; position: relative; 
          background: var(--bg);
        }
        .app-ready { animation: appEntrance 0.5s cubic-bezier(0.175, 0.885, 0.32, 1.2) forwards; }
        @keyframes appEntrance {
          from { opacity: 0; transform: scale(0.95) translateY(10px); }
          to { opacity: 1; transform: scale(1) translateY(0); }
        }
        header { 
          padding: calc(15px + env(safe-area-inset-top)) 20px 15px; 
          display: flex; align-items: center; justify-content: space-between; 
          border-bottom: 1px solid var(--border); background: var(--bg);
        }
        header h1 { margin: 0; font-size: 1.2rem; font-weight: 700; }
        .view { 
          position: absolute; top: 0; left: 0; width: 100%; height: 100%; 
          background: var(--bg); display: flex; flex-direction: column; transition: transform 0.2s ease-out; z-index: 10;
        }
        .view-hidden { transform: translateX(100%); pointer-events: none; }
        .list-container { flex: 1; overflow-y: auto; padding: 10px 15px 120px; }
        .note-card { background: var(--surface); padding: 18px; border-radius: 14px; margin-bottom: 12px; border: 1px solid var(--border); }
        .note-card:active { opacity: 0.6; }
        .note-title { font-weight: 700; font-size: 1rem; margin-bottom: 4px; }
        .note-desc { font-size: 0.85rem; color: var(--text-dim); }
        .note-time { font-size: 0.7rem; color: var(--text-dim); text-align: right; margin-top: 8px; font-style: italic; }
        #editor-area { flex: 1; width: 100%; background: transparent; border: none; color: var(--text); font-size: 1.15rem; line-height: 1.6; padding: 20px; resize: none; outline: none; }
        .btn-icon { padding: 10px; background: transparent; border: none; color: var(--text); display: flex; cursor: pointer; }
        #fab { position: fixed; bottom: calc(30px + env(safe-area-inset-bottom)); right: 25px; width: 64px; height: 64px; border-radius: 32px; background: var(--primary); color: #fff; border: none; font-size: 32px; box-shadow: 0 4px 12px rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 100; cursor: pointer; }
        .settings-content { padding: 20px; }
        .settings-row { display: flex; justify-content: space-between; align-items: center; padding: 15px; background: var(--surface); border-radius: 12px; margin-bottom: 20px; border: 1px solid var(--border); }
        .path-label { font-size: 0.8rem; color: var(--text-dim); margin-bottom: 10px; display: block; }
        .path-text { background: rgba(128,128,128,0.1); padding: 12px; border-radius: 8px; font-family: monospace; font-size: 0.75rem; color: var(--primary); word-break: break-all; }
        .theme-btn { background: var(--primary); color: #fff; border: none; padding: 8px 16px; border-radius: 8px; font-weight: 600; cursor: pointer; }
        .search-bar-container { padding: 10px 15px; background: var(--bg); border-bottom: 1px solid var(--border); }
        .search-input { width: 100%; padding: 10px 15px; border-radius: 10px; border: 1px solid var(--border); background: var(--surface); color: var(--text); font-size: 0.95rem; outline: none; }
        .search-input:focus { border-color: var(--primary); box-shadow: 0 0 0 2px rgba(var(--primary-rgb), 0.2); }
      `}</style>
        </div>
    );
};

export default App;
