"use client";

import { useMemo, useState } from "react";

type View = "timeline" | "vault";
type CaptureMode = "text" | "image" | "audio";

const notes = [
  {
    id: 1,
    time: "21:42",
    type: "audio",
    duration: "01:26",
    title: "散步时想到的产品判断",
    body: "真正需要被解决的，也许不是如何记得更多，而是怎样在恰当的时候再次遇见。",
    color: "violet",
  },
  {
    id: 2,
    time: "16:18",
    type: "image",
    title: "雨停后的窗边",
    body: "光落在桌面上，突然觉得一整天的忙乱都有了停顿。",
    color: "blue",
  },
  {
    id: 3,
    time: "09:05",
    type: "text",
    title: "关于专注",
    body: "减少选择不是限制，而是在替真正重要的事保留注意力。今天只完成一件关键任务。",
    color: "amber",
  },
];

const olderNotes = [
  {
    id: 4,
    time: "22:10",
    type: "text",
    title: "周末阅读随记",
    body: "好的工具不应该让人先学习管理工具，而应该让行动自然发生。",
    color: "green",
  },
  {
    id: 5,
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

function LockScreen({ onUnlock }: { onUnlock: () => void }) {
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
        <h1>欢迎回来</h1>
        <p>让每一个闪念都有迹可循</p>
      </div>
      <button className="fingerprint-button" onClick={onUnlock} aria-label="使用指纹解锁">
        <span className="fingerprint">◎</span>
      </button>
      <p className="unlock-hint">触摸以使用指纹解锁</p>
      <button className="passcode-link" onClick={onUnlock}>改用恢复口令</button>
      <div className="gesture-bar" />
    </section>
  );
}

function TimelineCard({ note, onOpen }: { note: typeof notes[number]; onOpen: () => void }) {
  return (
    <button className={`note-card ${note.color}`} onClick={onOpen}>
      <span className="note-time">{note.time}</span>
      <span className="note-content">
        <span className="note-heading-row">
          <strong>{note.title}</strong>
          <span className="type-chip">
            {note.type === "audio" ? `◉ ${note.duration}` : note.type === "image" ? "▧ 图片" : "文本"}
          </span>
        </span>
        {note.type === "audio" && (
          <span className="waveform compact-wave" aria-hidden="true">
            {[2, 5, 8, 4, 10, 6, 3, 9, 5, 7, 3, 6, 8, 4, 2].map((h, i) => <i key={i} style={{ height: `${h + 5}px` }} />)}
          </span>
        )}
        {note.type === "image" && (
          <span className="photo-strip" aria-label="两张图片缩略图">
            <span className="photo photo-one" />
            <span className="photo photo-two" />
          </span>
        )}
        <span className="note-body">{note.body}</span>
      </span>
    </button>
  );
}

function Timeline({ onCompose, onOpen, onOpenSecurity }: { onCompose: () => void; onOpen: () => void; onOpenSecurity: () => void }) {
  const [filter, setFilter] = useState("全部");
  const [searchOpen, setSearchOpen] = useState(false);
  const [query, setQuery] = useState("");
  const matchesSearch = (note: typeof notes[number]) => {
    const matchesType = filter === "全部" || (filter === "文字" && note.type === "text") || (filter === "图片" && note.type === "image") || (filter === "音频" && note.type === "audio");
    const keyword = query.trim().toLowerCase();
    return matchesType && (!keyword || `${note.title}${note.body}`.toLowerCase().includes(keyword));
  };
  const visibleNotes = notes.filter(matchesSearch);
  const visibleOlderNotes = olderNotes.filter((note) => matchesSearch(note as typeof notes[number]));
  return (
    <main className="app-view timeline-view">
      <header className="top-bar">
        <div>
          <p className="today-label">10月25日 · 星期日</p>
        </div>
        <button className="avatar-button" onClick={onOpenSecurity} aria-label="打开数据与安全">拾</button>
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
            <input autoFocus value={query} onChange={(e) => setQuery(e.target.value)} placeholder="搜索记录内容" />
            {query && <button onClick={() => setQuery("")} aria-label="清空搜索">×</button>}
          </label>
          <small>{query ? `找到 ${visibleNotes.length + visibleOlderNotes.length} 条记录` : "搜索只在本机完成"}</small>
        </div>
      )}

      <section className="timeline-section">
        <div className="date-heading"><strong>今天</strong><span>{visibleNotes.length} 条记录</span></div>
        <div className="timeline-rail">
          {visibleNotes.map((note) => (
            <TimelineCard key={note.id} note={note} onOpen={onOpen} />
          ))}
        </div>
      </section>

      {visibleOlderNotes.length > 0 && (
        <section className="timeline-section older-section">
          <div className="date-heading"><strong>昨天</strong><span>{visibleOlderNotes.length} 条记录</span></div>
          <div className="timeline-rail">
            {visibleOlderNotes.map((note) => <TimelineCard key={note.id} note={note as typeof notes[number]} onOpen={onOpen} />)}
          </div>
        </section>
      )}
      {visibleNotes.length + visibleOlderNotes.length === 0 && (
        <div className="no-search-results"><span>⌕</span><strong>没有找到相关记录</strong><small>换一个关键词，或调整上方分类</small></div>
      )}
    </main>
  );
}

function SearchView({ onOpen }: { onOpen: () => void }) {
  const [query, setQuery] = useState("");
  return (
    <main className="app-view search-view">
      <header className="simple-header">
        <p className="today-label">在本机查找</p>
        <h2>搜索记录</h2>
      </header>
      <label className="search-box">
        <span>⌕</span>
        <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="搜索文字或日期" />
        {query && <button onClick={() => setQuery("")}>×</button>}
      </label>
      <div className="quick-searches">
        <button>今天</button><button>有图片</button><button>有音频</button><button>本月</button>
      </div>
      <section className="search-empty">
        {query ? (
          <>
            <p className="result-caption">找到 2 条与“{query}”相关的记录</p>
            <TimelineCard note={notes[0]} onOpen={onOpen} />
            <TimelineCard note={notes[2]} onOpen={onOpen} />
          </>
        ) : (
          <>
            <div className="search-graphic">⌕</div>
            <h3>记得越多，越容易找回</h3>
            <p>所有搜索都只在这台设备上完成。</p>
            <div className="recent-search"><span>最近搜索</span><button onClick={() => setQuery("产品")}>产品想法　↗</button></div>
          </>
        )}
      </section>
    </main>
  );
}

function VaultView({ onExport, onBack }: { onExport: () => void; onBack: () => void }) {
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
        <button className="setting-row">
          <MiniIcon>▣</MiniIcon><span><strong>存储位置</strong><small>Documents / 拾念资料库</small></span><b>›</b>
        </button>
        <button className="setting-row">
          <MiniIcon>↻</MiniIcon><span><strong>卸载后恢复</strong><small>恢复密钥已安全备份</small></span><span className="ok-dot">已开启</span>
        </button>
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

      <p className="storage-summary">共 124 条记录 · 286 MB · 上次完整备份 2 天前</p>
    </main>
  );
}

function Composer({ onClose }: { onClose: () => void }) {
  const [mode, setMode] = useState<CaptureMode>("text");
  const [saved, setSaved] = useState(false);
  const [recording, setRecording] = useState(false);
  const [text, setText] = useState("");

  const save = () => {
    setSaved(true);
    window.setTimeout(onClose, 900);
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
            <textarea autoFocus value={text} onChange={(e) => setText(e.target.value)} placeholder="写下刚刚闪过的想法…" />
            <div className="capture-meta"><span>今天 21:48</span><span>{text.length} 字</span></div>
          </div>
        )}

        {mode === "image" && (
          <div className="image-capture">
            <div className="camera-preview"><div className="sun-disc" /><span>示例取景画面</span></div>
            <div className="image-actions"><button><span>▧</span>从相册选择</button><button className="shutter" aria-label="拍照" /></div>
            <textarea placeholder="补充当时的想法…" />
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

function NoteDetail({ onClose }: { onClose: () => void }) {
  return (
    <div className="modal-layer detail-layer">
      <button className="modal-scrim" onClick={onClose} aria-label="关闭详情" />
      <section className="detail-sheet">
        <div className="sheet-handle" />
        <header><button onClick={onClose}>↓</button><span>今天 21:42</span><button>•••</button></header>
        <span className="detail-type">◉ 音频记录 · 01:26</span>
        <h2>散步时想到的产品判断</h2>
        <p>真正需要被解决的，也许不是如何记得更多，而是怎样在恰当的时候再次遇见。</p>
        <div className="audio-player"><button>▶</button><span className="waveform player-wave">{[4,8,5,10,6,12,4,7,11,6,9,4,8,5,3].map((h,i)=><i key={i} style={{height:`${h+4}px`}} />)}</span><small>01:26</small></div>
        <div className="future-insight"><span>✦</span><div><small>AI 整理 · 未来功能</small><strong>以后可在这里生成摘要、主题与关联记录</strong></div></div>
        <button className="edit-note">编辑这条记录</button>
      </section>
    </div>
  );
}

function ExportSheet({ onClose }: { onClose: () => void }) {
  const [format, setFormat] = useState("加密备份");
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
        <div className="export-scope"><span>导出范围</span><strong>全部 124 条记录　›</strong></div>
        <button className="primary-wide" onClick={onClose}>创建{format}</button>
        <p className="export-note">导出过程完全在本机完成，不会上传任何内容。</p>
      </section>
    </div>
  );
}

export default function Home() {
  const [locked, setLocked] = useState(true);
  const [view, setView] = useState<View>("timeline");
  const [composer, setComposer] = useState(false);
  const [detail, setDetail] = useState(false);
  const [exporting, setExporting] = useState(false);

  const viewLabel = useMemo(() => view === "timeline" ? "时间线" : "数据与安全", [view]);

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
          <div><span>03</span><strong>可带走</strong><small>开放导出与卸载后恢复</small></div>
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
          {locked ? <LockScreen onUnlock={() => setLocked(false)} /> : (
            <div className="phone-content">
              <div className="status-row"><span>9:41</span><span className="status-symbols">● ◒ ▰</span></div>
              {view === "timeline" && <Timeline onCompose={() => setComposer(true)} onOpen={() => setDetail(true)} onOpenSecurity={() => setView("vault")} />}
              {view === "vault" && <VaultView onExport={() => setExporting(true)} onBack={() => setView("timeline")} />}
              {view === "timeline" && <button className="fab" onClick={() => setComposer(true)} aria-label="新建记录"><span>＋</span><b>记录</b></button>}
              <div className="gesture-bar dark-bar" />
            </div>
          )}
          {composer && <Composer onClose={() => setComposer(false)} />}
          {detail && <NoteDetail onClose={() => setDetail(false)} />}
          {exporting && <ExportSheet onClose={() => setExporting(false)} />}
        </div>
        <p className="prototype-caption">交互原型 · 数据均为演示内容</p>
      </section>
    </div>
  );
}
