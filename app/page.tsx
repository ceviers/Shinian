"use client";

import { useMemo, useRef, useState } from "react";

type View = "timeline" | "vault" | "website";
type CaptureMode = "text" | "image" | "audio";
type NoteData = {
  id: number;
  day: "today" | "yesterday";
  time: string;
  type: CaptureMode;
  duration?: string;
  title: string;
  body: string;
  color: string;
};

const initialNotes: NoteData[] = [
  {
    id: 1,
    day: "today",
    time: "21:42",
    type: "audio",
    duration: "01:26",
    title: "散步时想到的产品判断",
    body: "真正需要被解决的，也许不是如何记得更多，而是怎样在恰当的时候再次遇见。",
    color: "violet",
  },
  {
    id: 2,
    day: "today",
    time: "16:18",
    type: "image",
    title: "雨停后的窗边",
    body: "光落在桌面上，突然觉得一整天的忙乱都有了停顿。",
    color: "blue",
  },
  {
    id: 3,
    day: "today",
    time: "09:05",
    type: "text",
    title: "关于专注",
    body: "减少选择不是限制，而是在替真正重要的事保留注意力。今天只完成一件关键任务。",
    color: "amber",
  },
  {
    id: 4,
    day: "yesterday",
    time: "22:10",
    type: "text",
    title: "周末阅读随记",
    body: "好的工具不应该让人先学习管理工具，而应该让行动自然发生。",
    color: "green",
  },
  {
    id: 5,
    day: "yesterday",
    time: "14:36",
    type: "audio",
    duration: "00:48",
    title: "会议后的补充",
    body: "下一版把首次记录路径再缩短一步，打开后直接聚焦输入。",
    color: "rose",
  },
];

function MiniIcon({ children }: { children: React.ReactNode }) {
  return <span className="mini-icon" aria-hidden="true">{children}</span>;
}

function LockScreen({ firstUse, onUnlock, onCreateVault, onRestore }: { firstUse: boolean; onUnlock: () => void; onCreateVault: () => void; onRestore: () => void }) {
  return (
    <section className="lock-screen" aria-label="指纹解锁演示">
      <div className="status-row light-status">
        <span>9:41</span>
        <span className="status-symbols">● ◒ ▰</span>
      </div>
      <div className="lock-orbit orbit-one" />
      <div className="lock-orbit orbit-two" />
      <div className="lock-copy">
        <div className="brand-mark">拾</div>
        <p className="eyebrow">拾念 · 随手记录</p>
        <h1>{firstUse ? "开始使用拾念" : "欢迎回来"}</h1>
        <p>让每一个闪念都有迹可循</p>
      </div>
      {firstUse ? (
        <div className="first-use-actions">
          <button className="create-vault-link" onClick={onCreateVault}>创建本机资料库</button>
          <button className="restore-vault-link" onClick={onRestore}>从加密备份恢复</button>
          <small>只有本机没有使用记录时才显示这些入口</small>
        </div>
      ) : (
        <>
          <button className="fingerprint-button" onClick={onUnlock} aria-label="使用指纹解锁"><span className="fingerprint">◎</span></button>
          <p className="unlock-hint">正在等待系统指纹验证</p>
          <button className="passcode-link" onClick={onUnlock}>改用恢复口令</button>
        </>
      )}
      <div className="gesture-bar" />
    </section>
  );
}

function LibrarySetup({ onClose, onComplete }: { onClose: () => void; onComplete: () => void }) {
  const [step, setStep] = useState(1);
  const [passphrase, setPassphrase] = useState("");
  const [confirmed, setConfirmed] = useState("");

  return (
    <section className="setup-screen" aria-label="创建资料库">
      <div className="status-row"><span>9:41</span><span className="status-symbols">● ◒ ▰</span></div>
      <header className="setup-header">
        <button onClick={step === 1 ? onClose : () => setStep(step - 1)} aria-label="返回">‹</button>
        <strong>创建资料库</strong>
        <span>{step} / 2</span>
      </header>
      <div className="setup-progress" aria-label={`第 ${step} 步，共 2 步`}>
        {[1, 2].map((item) => <i key={item} className={item <= step ? "active" : ""} />)}
      </div>

      {step === 1 && (
        <div className="setup-content">
          <div className="setup-illustration key-illustration">＊</div>
          <p className="setup-eyebrow">第一步 · 恢复方式</p>
          <h2>设置恢复口令</h2>
          <p className="setup-description">重新安装 App 或更换设备时，需要用它打开导出的加密备份。</p>
          <label className="passphrase-field"><span>恢复口令</span><input type="password" value={passphrase} onChange={(e) => setPassphrase(e.target.value)} placeholder="至少输入 8 个字符" /></label>
          <label className="passphrase-field"><span>再次确认</span><input type="password" value={confirmed} onChange={(e) => setConfirmed(e.target.value)} placeholder="再次输入恢复口令" /></label>
          <div className="setup-note warning-note"><span>!</span><p>拾念无法替你找回恢复口令，请把它保存在安全且容易找到的地方。</p></div>
        </div>
      )}

      {step === 2 && (
        <div className="setup-content">
          <div className="setup-illustration biometric-illustration">◎</div>
          <p className="setup-eyebrow">第二步 · 快速访问</p>
          <h2>启用指纹解锁</h2>
          <p className="setup-description">以后每次启动拾念，验证指纹后即可进入你的时间线。</p>
          <div className="biometric-card"><span>◎</span><div><strong>启动时验证指纹</strong><small>使用 Android 系统生物识别</small></div><span className="toggle on"><i /></span></div>
          <div className="setup-summary">
            <div><span>▣</span><p><small>存储位置</small><strong>App 私有加密目录</strong></p><b>✓</b></div>
            <div><span>＊</span><p><small>恢复方式</small><strong>恢复口令已设置</strong></p><b>✓</b></div>
          </div>
        </div>
      )}

      <footer className="setup-footer">
        <button
          className="primary-setup-action"
          disabled={step === 1 && (passphrase.length < 8 || passphrase !== confirmed)}
          onClick={() => step < 2 ? setStep(step + 1) : onComplete()}
        >
          {step === 2 ? "创建并进入拾念" : "继续"}
        </button>
      </footer>
      <div className="gesture-bar dark-bar" />
    </section>
  );
}

function TimelineCard({ note, onOpen, onDelete }: { note: NoteData; onOpen: () => void; onDelete: () => void }) {
  const [offset, setOffset] = useState(0);
  const [confirming, setConfirming] = useState(false);
  const [playing, setPlaying] = useState(false);
  const dragStart = useRef(0);
  const startOffset = useRef(0);
  const offsetValue = useRef(0);
  const dragged = useRef(false);

  return (
    <article className={`note-card ${note.color}`}>
      <span className="note-time">{note.time}</span>
      <span className="swipe-shell">
        <button
          className={`delete-reveal ${confirming ? "confirming" : ""}`}
          onClick={() => confirming ? onDelete() : setConfirming(true)}
        >
          <span>♲</span>{confirming ? "确认删除" : "删除"}
        </button>
        <span
          className="note-content"
          style={{ transform: `translateX(${offset}px)`, borderTopRightRadius: `${18 * (1 - Math.abs(offset) / 96)}px`, borderBottomRightRadius: `${18 * (1 - Math.abs(offset) / 96)}px` }}
          role="button"
          tabIndex={0}
          onClick={() => {
            if (dragged.current) { dragged.current = false; return; }
            if (offsetValue.current < -8) {
              offsetValue.current = 0;
              setOffset(0);
              setConfirming(false);
            } else onOpen();
          }}
          onKeyDown={(event) => event.key === "Enter" && onOpen()}
          onPointerDown={(event) => {
            dragStart.current = event.clientX;
            startOffset.current = offsetValue.current;
            dragged.current = false;
            event.currentTarget.setPointerCapture(event.pointerId);
          }}
          onPointerMove={(event) => {
            if (!event.currentTarget.hasPointerCapture(event.pointerId)) return;
            const next = Math.max(-96, Math.min(0, startOffset.current + event.clientX - dragStart.current));
            if (Math.abs(event.clientX - dragStart.current) > 5) dragged.current = true;
            offsetValue.current = next;
            setOffset(next);
          }}
          onPointerUp={(event) => {
            event.currentTarget.releasePointerCapture(event.pointerId);
            const target = offsetValue.current < -34 ? -96 : 0;
            offsetValue.current = target;
            setOffset(target);
            if (target === 0) setConfirming(false);
          }}
        >
          <span className="note-heading-row">
            <strong>{note.title}</strong>
            {note.type === "audio" && <span className="type-chip">◉ {note.duration}</span>}
          </span>
          {note.type === "audio" && (
            <button
              className={`waveform compact-wave playable-wave ${playing ? "playing" : ""}`}
              aria-label={playing ? "暂停录音" : "播放录音"}
              onClick={(event) => { event.stopPropagation(); setPlaying(!playing); }}
            >
              {[2, 5, 8, 4, 10, 6, 3, 9, 5, 7, 3, 6, 8, 4, 2].map((h, i) => <i key={i} style={{ height: `${h + 5}px` }} />)}
            </button>
          )}
          {note.type === "image" && <span className="photo-preview" aria-label="图片缩略图" />}
          <span className="note-body">{note.body}</span>
        </span>
      </span>
    </article>
  );
}

function Timeline({ notes, onCompose, onOpen, onDelete, onOpenSecurity, onOpenWebsite }: { notes: NoteData[]; onCompose: () => void; onOpen: (note: NoteData) => void; onDelete: (id: number) => void; onOpenSecurity: () => void; onOpenWebsite: () => void }) {
  const [filter, setFilter] = useState("全部");
  const [searchOpen, setSearchOpen] = useState(false);
  const [query, setQuery] = useState("");
  const matchesSearch = (note: NoteData) => {
    const matchesType = filter === "全部" || (filter === "文字" && note.type === "text") || (filter === "图片" && note.type === "image") || (filter === "音频" && note.type === "audio");
    const keyword = query.trim().toLowerCase();
    return matchesType && (!keyword || `${note.title}${note.body}`.toLowerCase().includes(keyword));
  };
  const visibleNotes = notes.filter((note) => note.day === "today" && matchesSearch(note));
  const visibleOlderNotes = notes.filter((note) => note.day === "yesterday" && matchesSearch(note));
  return (
    <main className="app-view timeline-view">
      <header className="top-bar">
        <button className="avatar-button" onClick={onOpenSecurity} aria-label="打开数据与安全">拾</button>
        <div className="date-link-wrap">
          <p className="today-label">10月25日 · 星期日</p>
          <button className="hidden-site-link" onClick={onOpenWebsite} aria-label="打开 cevier.com" />
        </div>
      </header>

      <div className="thought-prompt">
        <span className="prompt-spark">✦</span>
        <span><small>今天的微小提示</small><strong>此刻，有什么值得留下？</strong></span>
        <button onClick={onCompose}>写下</button>
      </div>

      <div className="filter-toolbar">
        <div className="filter-row" aria-label="记录筛选">
          {["全部", "文字", "图片", "音频"].map((item) => (
            <button key={item} className={filter === item ? "active" : ""} onClick={() => setFilter(item)}>{item}</button>
          ))}
        </div>
        <button
          className={`inline-search-button ${searchOpen ? "active" : ""}`}
          onClick={() => {
            setSearchOpen(!searchOpen);
            if (searchOpen) setQuery("");
          }}
          aria-label={searchOpen ? "关闭搜索" : "搜索记录"}
        >
          {searchOpen ? "×" : "⌕"}
        </button>
      </div>

      {searchOpen && (
        <div className="inline-search-panel">
          <label className="search-box">
            <span>⌕</span>
            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="搜索记录内容" />
            {query && <button onClick={() => setQuery("")} aria-label="清空搜索">×</button>}
          </label>
          <small>{query ? `找到 ${visibleNotes.length + visibleOlderNotes.length} 条记录` : "搜索只在本机完成"}</small>
        </div>
      )}

      <section className="timeline-section">
        <div className="date-heading"><strong>今天</strong><span>{visibleNotes.length} 条记录</span></div>
        <div className="timeline-rail">
          {visibleNotes.map((note) => (
            <TimelineCard key={note.id} note={note} onOpen={() => onOpen(note)} onDelete={() => onDelete(note.id)} />
          ))}
        </div>
      </section>

      {visibleOlderNotes.length > 0 && (
        <section className="timeline-section older-section">
          <div className="date-heading"><strong>昨天</strong><span>{visibleOlderNotes.length} 条记录</span></div>
          <div className="timeline-rail">
            {visibleOlderNotes.map((note) => <TimelineCard key={note.id} note={note} onOpen={() => onOpen(note)} onDelete={() => onDelete(note.id)} />)}
          </div>
        </section>
      )}
      {visibleNotes.length + visibleOlderNotes.length === 0 && (
        <div className="no-search-results"><span>⌕</span><strong>没有找到相关记录</strong><small>换一个关键词，或调整上方分类</small></div>
      )}
    </main>
  );
}

function VaultView({ noteCount, onExport, onBack }: { noteCount: number; onExport: () => void; onBack: () => void }) {
  return (
    <main className="app-view vault-view">
      <header className="vault-topbar">
        <button className="back-button" onClick={onBack} aria-label="返回时间线">‹</button>
        <div>
          <p className="today-label">本机资料库</p>
          <h2>数据与安全</h2>
        </div>
      </header>

      <section className="vault-hero">
        <div className="shield-icon">◇</div>
        <div><span className="safe-pill">保护正常</span><h3>256 位本地加密</h3><p>只有你的指纹或恢复口令能打开。</p></div>
      </section>

      <section className="setting-group">
        <p className="group-label">资料库</p>
        <div className="setting-row static-row">
          <MiniIcon>▣</MiniIcon><span><strong>存储位置</strong><small>App 私有加密目录</small></span><b>本机</b>
        </div>
        <div className="setting-row static-row">
          <MiniIcon>↻</MiniIcon><span><strong>跨设备恢复</strong><small>通过恢复口令加密备份迁移</small></span><span className="ok-dot">已开启</span>
        </div>
        <button className="setting-row" onClick={onExport}>
          <MiniIcon>⇧</MiniIcon><span><strong>导出记录</strong><small>加密备份、PDF 或通用 JSON</small></span><b>›</b>
        </button>
      </section>

      <section className="setting-group">
        <p className="group-label">访问保护</p>
        <button className="setting-row">
          <MiniIcon>◎</MiniIcon><span><strong>启动时指纹解锁</strong><small>每次离开 30 秒后重新验证</small></span><span className="toggle on"><i /></span>
        </button>
        <button className="setting-row">
          <MiniIcon>▤</MiniIcon><span><strong>隐藏最近任务预览</strong><small>切换应用时遮盖记录内容</small></span><span className="toggle on"><i /></span>
        </button>
      </section>

      <section className="ai-preview">
        <span className="ai-badge">AI · 未来能力</span>
        <h3>整理，而不窥探</h3>
        <p>未来可选择在本机生成摘要、主题和关联。接入云端 AI 前会再次征得你的明确授权。</p>
        <button disabled>尚未启用</button>
      </section>

      <p className="storage-summary">共 {noteCount} 条记录 · 数据仅保存在本机</p>
    </main>
  );
}

function Composer({ onClose, onSave }: { onClose: () => void; onSave: (note: NoteData) => void }) {
  const [mode, setMode] = useState<CaptureMode>("text");
  const [saved, setSaved] = useState(false);
  const [recording, setRecording] = useState(false);
  const [text, setText] = useState("");

  const save = () => {
    setSaved(true);
    const now = new Date();
    const fallbackTitle = mode === "image" ? "图片记录" : mode === "audio" ? "音频记录" : "新的记录";
    onSave({
      id: Date.now(),
      day: "today",
      time: `${String(now.getHours()).padStart(2, "0")}:${String(now.getMinutes()).padStart(2, "0")}`,
      type: mode,
      duration: mode === "audio" ? "00:18" : undefined,
      title: text.trim().slice(0, 18) || fallbackTitle,
      body: text.trim() || (mode === "image" ? "一张本地保存的照片" : mode === "audio" ? "一段本地保存的录音" : ""),
      color: mode === "text" ? "amber" : mode === "image" ? "blue" : "violet",
    });
    window.setTimeout(onClose, 350);
  };

  return (
    <div className="modal-layer">
      <button className="modal-scrim" onClick={onClose} aria-label="关闭记录面板" />
      <section className="composer-sheet" aria-label="新建记录">
        <div className="sheet-handle" />
        <header><div><small>新建记录</small><strong>捕捉这一刻</strong></div><button onClick={onClose}>×</button></header>
        <div className="capture-tabs">
          <button className={mode === "text" ? "active" : ""} onClick={() => setMode("text")}><span>≡</span>文字</button>
          <button className={mode === "image" ? "active" : ""} onClick={() => setMode("image")}><span>▧</span>图片</button>
          <button className={mode === "audio" ? "active" : ""} onClick={() => setMode("audio")}><span>◉</span>音频</button>
        </div>

        {mode === "text" && (
          <div className="text-capture">
            <textarea value={text} onChange={(e) => setText(e.target.value)} placeholder="写下刚刚闪过的想法…" />
            <div className="capture-meta"><span>今天 21:48</span><span>{text.length} 字</span></div>
          </div>
        )}

        {mode === "image" && (
          <div className="image-capture">
            <div className="camera-preview"><div className="sun-disc" /><span>示例取景画面</span></div>
            <div className="image-actions"><button><span>▧</span>从相册选择</button><button className="shutter" aria-label="拍照" /></div>
            <textarea value={text} onChange={(event) => setText(event.target.value)} placeholder="补充当时的想法…" />
          </div>
        )}

        {mode === "audio" && (
          <div className="audio-capture">
            <p>{recording ? "正在聆听…" : "用声音保存还没成形的想法"}</p>
            <strong>{recording ? "00:18" : "00:00"}</strong>
            <div className={`waveform large-wave ${recording ? "moving" : ""}`}>
              {[3, 7, 11, 5, 14, 9, 4, 12, 16, 8, 5, 13, 9, 6, 10, 4, 7, 3].map((h, i) => <i key={i} style={{ height: `${h * 2}px` }} />)}
            </div>
            <button className={`record-button ${recording ? "recording" : ""}`} onClick={() => setRecording(!recording)}><span /></button>
            <small>{recording ? "点击停止录音" : "点击开始 · 音频只保存在本机"}</small>
          </div>
        )}

        <footer className="composer-footer">
          <span>◈ 已加密</span>
          <button className="save-button" onClick={save}>{saved ? "已保存 ✓" : "保存记录"}</button>
        </footer>
      </section>
    </div>
  );
}

function NoteDetail({ note, onClose, onUpdate }: { note: NoteData; onClose: () => void; onUpdate: (note: NoteData) => void }) {
  const [title, setTitle] = useState(note.title);
  const [body, setBody] = useState(note.body);
  const [playing, setPlaying] = useState(false);
  const [fullImage, setFullImage] = useState(false);
  const [dragY, setDragY] = useState(0);
  const dragStart = useRef(0);
  const dragValue = useRef(0);
  const dragging = useRef(false);
  const changed = title.trim() !== note.title || body.trim() !== note.body;

  return (
    <div className="modal-layer detail-layer">
      <button className="modal-scrim" onClick={onClose} aria-label="关闭详情" />
      <section
        className="detail-sheet interactive-detail"
        style={{ transform: `translateY(${dragY}px)`, opacity: Math.max(0, 1 - dragY / 310) }}
        onPointerDown={(event) => {
          if ((event.target as HTMLElement).closest("input, textarea, button") || event.currentTarget.scrollTop > 0) return;
          dragging.current = true;
          dragStart.current = event.clientY;
          event.currentTarget.setPointerCapture(event.pointerId);
        }}
        onPointerMove={(event) => {
          if (!dragging.current) return;
          const next = Math.max(0, event.clientY - dragStart.current);
          dragValue.current = next;
          setDragY(next);
        }}
        onPointerUp={(event) => {
          if (!dragging.current) return;
          dragging.current = false;
          event.currentTarget.releasePointerCapture(event.pointerId);
          if (dragValue.current > 78) onClose();
          else { dragValue.current = 0; setDragY(0); }
        }}
      >
        <div className="sheet-handle" />
        <header>
          <span />
          <span>今天 {note.time}</span>
          <button
            disabled={!changed || !title.trim()}
            onClick={() => { onUpdate({ ...note, title: title.trim(), body: body.trim() }); onClose(); }}
          >保存</button>
        </header>
        <span className="detail-type">{note.type === "audio" ? `◉ 音频记录 · ${note.duration}` : note.type === "image" ? "▧ 图片记录" : "≡ 文字记录"}</span>
        <label className="detail-field"><span>标题</span><input value={title} onChange={(event) => setTitle(event.target.value)} /></label>
        {note.type === "image" && <button className="photo-preview detail-photo" onClick={() => setFullImage(true)} aria-label="全屏查看图片" />}
        <label className="detail-field"><span>内容</span><textarea value={body} onChange={(event) => setBody(event.target.value)} /></label>
        {note.type === "audio" && (
          <div className="audio-player">
            <button onClick={() => setPlaying(!playing)}>{playing ? "■" : "▶"}</button>
            <button className={`waveform player-wave ${playing ? "playing" : ""}`} onClick={() => setPlaying(!playing)} aria-label={playing ? "暂停录音" : "播放录音"}>
              {[4,8,5,10,6,12,4,7,11,6,9,4,8,5,3].map((h,i)=><i key={i} style={{height:`${h+4}px`}} />)}
            </button>
            <small>{note.duration}</small>
          </div>
        )}
      </section>
      {fullImage && <FullscreenImage onClose={() => setFullImage(false)} />}
    </div>
  );
}

function FullscreenImage({ onClose }: { onClose: () => void }) {
  const [scale, setScale] = useState(1);
  const [dragY, setDragY] = useState(0);
  const startY = useRef(0);
  const currentDrag = useRef(0);
  return (
    <section
      className="image-viewer"
      style={{ background: `rgba(0,0,0,${Math.max(0, .96 - Math.abs(dragY) / 270)})` }}
      onPointerDown={(event) => { if (scale === 1) { startY.current = event.clientY; event.currentTarget.setPointerCapture(event.pointerId); } }}
      onPointerMove={(event) => {
        if (!event.currentTarget.hasPointerCapture(event.pointerId) || scale !== 1) return;
        currentDrag.current = event.clientY - startY.current;
        setDragY(currentDrag.current);
      }}
      onPointerUp={(event) => {
        if (event.currentTarget.hasPointerCapture(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId);
        if (Math.abs(currentDrag.current) > 72) onClose();
        else { currentDrag.current = 0; setDragY(0); }
      }}
    >
      <button className="viewer-close" onClick={onClose}>×</button>
      <div className="viewer-photo" style={{ transform: `translateY(${dragY}px) scale(${scale})`, opacity: Math.max(.05, 1 - Math.abs(dragY) / 180) }} onDoubleClick={() => setScale(scale === 1 ? 2.5 : 1)} />
      <div className="zoom-controls"><button onClick={() => setScale(Math.max(1, scale - .5))}>−</button><b>{scale.toFixed(1)}×</b><button onClick={() => setScale(Math.min(5, scale + .5))}>＋</button></div>
    </section>
  );
}

function ExportSheet({ noteCount, onClose }: { noteCount: number; onClose: () => void }) {
  const [format, setFormat] = useState("加密备份");
  const [passphrase, setPassphrase] = useState("");
  return (
    <div className="modal-layer">
      <button className="modal-scrim" onClick={onClose} aria-label="关闭导出" />
      <section className="export-sheet">
        <div className="sheet-handle" />
        <header><div><small>数据导出</small><strong>带走你的全部记录</strong></div><button onClick={onClose}>×</button></header>
        <div className="export-options">
          {["加密备份", "PDF 文档", "通用 JSON"].map((item, index) => (
            <button key={item} className={format === item ? "selected" : ""} onClick={() => setFormat(item)}>
              <span className="radio"><i /></span><span><strong>{item}</strong><small>{index === 0 ? "可在新设备完整恢复，推荐" : index === 1 ? "适合阅读和打印" : "适合迁移至其他工具"}</small></span>
            </button>
          ))}
        </div>
        {format === "加密备份" && (
          <label className="passphrase-field export-passphrase">
            <span>当前恢复口令</span>
            <input type="password" value={passphrase} onChange={(event) => setPassphrase(event.target.value)} placeholder="用于加密跨设备备份" />
          </label>
        )}
        <div className="export-scope"><span>导出范围</span><strong>全部 {noteCount} 条记录 ›</strong></div>
        <button className="primary-wide" disabled={format === "加密备份" && passphrase.length < 8} onClick={onClose}>创建{format}</button>
        <p className="export-note">导出过程完全在本机完成，不会上传任何内容。</p>
      </section>
    </div>
  );
}

function RestoreSheet({ onClose, onComplete }: { onClose: () => void; onComplete: () => void }) {
  const [fileSelected, setFileSelected] = useState(false);
  const [passphrase, setPassphrase] = useState("");
  return (
    <div className="modal-layer">
      <button className="modal-scrim" onClick={onClose} aria-label="关闭恢复" />
      <section className="export-sheet restore-sheet">
        <div className="sheet-handle" />
        <header><div><small>跨设备恢复</small><strong>导入加密备份</strong></div><button onClick={onClose}>×</button></header>
        <button className={`backup-picker ${fileSelected ? "selected" : ""}`} onClick={() => setFileSelected(true)}>
          <span>▣</span><span><strong>{fileSelected ? "拾念备份.shinian" : "选择备份文件"}</strong><small>{fileSelected ? "文件已就绪" : "从这台设备选择加密备份"}</small></span><b>{fileSelected ? "✓" : "›"}</b>
        </button>
        <label className="passphrase-field export-passphrase"><span>恢复口令</span><input type="password" value={passphrase} onChange={(event) => setPassphrase(event.target.value)} placeholder="输入旧手机导出时使用的口令" /></label>
        <p className="restore-copy">记录、图片与音频会使用这台手机的新设备密钥重新加密。</p>
        <button className="primary-wide" disabled={!fileSelected || passphrase.length < 8} onClick={onComplete}>恢复并进入拾念</button>
      </section>
    </div>
  );
}

function WebsiteView({ onBack }: { onBack: () => void }) {
  return (
    <main className="website-view">
      <header><button onClick={onBack}>‹</button><strong>cevier.com</strong><span>⋮</span></header>
      <iframe title="cevier.com" src="https://cevier.com" />
    </main>
  );
}

export default function Home() {
  const [locked, setLocked] = useState(true);
  const [firstUse, setFirstUse] = useState(true);
  const [setup, setSetup] = useState(false);
  const [view, setView] = useState<View>("timeline");
  const [notes, setNotes] = useState<NoteData[]>(initialNotes);
  const [composer, setComposer] = useState(false);
  const [detail, setDetail] = useState<NoteData | null>(null);
  const [exporting, setExporting] = useState(false);
  const [restoring, setRestoring] = useState(false);

  const viewLabel = useMemo(() => view === "timeline" ? "时间线" : view === "vault" ? "数据与安全" : "内置网页", [view]);

  return (
    <div className="prototype-page">
      <aside className="design-brief">
        <div className="brief-brand"><span>拾</span><b>拾念</b></div>
        <p className="brief-kicker">ANDROID 16 · LOCAL-FIRST</p>
        <h1>把闪过的念头，<br />安静地留在自己手里。</h1>
        <p className="brief-copy">一套为快速记录而设计的移动端原型。默认离线、默认加密，文字、图片与声音沿时间自然生长。</p>
        <div className="principle-list">
          <div><span>01</span><strong>一步即记</strong><small>首页随时唤起记录面板</small></div>
          <div><span>02</span><strong>本地优先</strong><small>数据不离开用户设备</small></div>
          <div><span>03</span><strong>可带走</strong><small>加密备份支持跨设备恢复</small></div>
        </div>
        <div className="demo-guide">
          <span className="pulse-dot" />
          <p><strong>当前演示：{locked ? "指纹解锁" : viewLabel}</strong><br />点击手机内的按钮浏览关键流程</p>
        </div>
      </aside>

      <section className="phone-stage">
        <div className="ambient-shape shape-one" /><div className="ambient-shape shape-two" />
        <div className="phone-frame">
          <div className="camera-cutout" />
          {setup ? <LibrarySetup onClose={() => setSetup(false)} onComplete={() => { setFirstUse(false); setSetup(false); setLocked(false); }} /> : locked ? <LockScreen firstUse={firstUse} onUnlock={() => setLocked(false)} onCreateVault={() => setSetup(true)} onRestore={() => setRestoring(true)} /> : (
            <div className="phone-content">
              <div className="status-row"><span>9:41</span><span className="status-symbols">● ◒ ▰</span></div>
              {view === "timeline" && <Timeline notes={notes} onCompose={() => setComposer(true)} onOpen={setDetail} onDelete={(id) => setNotes((items) => items.filter((item) => item.id !== id))} onOpenSecurity={() => setView("vault")} onOpenWebsite={() => setView("website")} />}
              {view === "vault" && <VaultView noteCount={notes.length} onExport={() => setExporting(true)} onBack={() => setView("timeline")} />}
              {view === "website" && <WebsiteView onBack={() => setView("timeline")} />}
              {view === "timeline" && <button className="fab" onClick={() => setComposer(true)} aria-label="新建记录"><span>＋</span></button>}
              <div className="gesture-bar dark-bar" />
            </div>
          )}
          {composer && <Composer onClose={() => setComposer(false)} onSave={(note) => setNotes((items) => [note, ...items])} />}
          {detail && <NoteDetail note={detail} onClose={() => setDetail(null)} onUpdate={(updated) => setNotes((items) => items.map((item) => item.id === updated.id ? updated : item))} />}
          {exporting && <ExportSheet noteCount={notes.length} onClose={() => setExporting(false)} />}
          {restoring && <RestoreSheet onClose={() => setRestoring(false)} onComplete={() => { setRestoring(false); setFirstUse(false); setLocked(false); }} />}
        </div>
        <p className="prototype-caption">交互原型 · 数据均为演示内容</p>
      </section>
    </div>
  );
}
