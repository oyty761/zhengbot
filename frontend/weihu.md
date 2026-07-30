# ZhengBot 前端维护文档

## 项目概述

ZhengBot 是一个古筝智能演奏机器人的 Web 交互平台。前端为纯静态 HTML（无框架），5 个页面同目录互相跳转，动画库使用 GSAP 3.12.5（CDN 加载）。后端为 Spring Boot，目前仅 `instrument-explore` 模块有实现。

---

## 文件清单

| 文件 | 职责 | 状态 |
|---|---|---|
| `index.html` | 首页，4 张模块卡，GSAP 入场动画 | ✅ 完成 |
| `composition.html` | 自由创作：piano-roll 网格编曲 + 拖拽音符 + AI 补全 + 演奏 | ✅ 可用（后端未实现时走离线模式） |
| `explore.html` | 乐器探秘：立体结构讲解 + 历史时间轴 + 琴弦试听 | ✅ 完成，后端已有 |
| `songbook.html` | 我要点歌：占位页，列出 10 个已规划 API | ⏳ 待开发 |
| `voice.html` | 语音交互：占位页，列出 2 个已规划 API | ⏳ 待开发 |

---

## 共享设计约定

### CSS 变量（所有页面共用）

```css
--bg: #f8f5f0;         /* 页面底色 */
--card: #ffffff;       /* 卡片白 */
--text: #2c2416;       /* 主文字 */
--text-sub: #7a7265;   /* 辅助文字 */
--primary: #b8733c;    /* 主色调：木质棕 */
--primary-light: #d4955c;
--accent: #3a7d5c;     /* 强调色：墨绿 */
--border: #e8e2d8;     /* 边框 */
--shadow: 0 2px 12px rgba(44,36,22,.06);
--radius: 14px 或 16px;
--font: 'Segoe UI','PingFang SC','Microsoft YaHei','Noto Sans SC',system-ui,sans-serif;
```

### GSAP 模式

- 所有 GSAP 动画写在 `DOMContentLoaded` 回调里
- 入场序列用 `gsap.timeline()` 串联
- 常用 ease：`power2.out`（默认感）、`power3.out`（卡片上浮）、`back.out(1.7)`（弹跳感）
- stagger 通常在 0.06–0.12 之间
- Hover 效果用 mouseenter/mouseleave + `gsap.to()`

### API 调用封装

每个页面自包含 3 个轻量封装函数：

```javascript
const API = '/api';  // 后端 base path
async function get(path) { /* fetch GET, catch 返回 {code:500} */ }
async function post(path, body) { /* fetch POST + JSON */ }
async function put(path, body) { /* fetch PUT + JSON */ }
```

### 页面导航

- 首页卡片为 `<a href="xxx.html">`（普通链接，非 SPA 路由）
- 子页面顶栏 `←` 按钮为 `<a href="index.html">`
- 不需要 JS 路由，浏览器原生跳转 + GSAP 每页独立播放入场动画

---

## 各页面详解

### 1. index.html — 首页

**功能**：入口页，2×2 卡片网格，logo 弹跳入场。

**关键点**：
- `卡片 .card.coming-soon` 有 `opacity:0.65` + `cursor:default`，hover 不浮起
- Hover 动画只绑 `.card:not(.coming-soon)`：`y:-6, scale:1.02`
- 响应式：`max-width:700px` 时单列布局

---

### 2. composition.html — 自由创作

**功能**：piano-roll 风格网格编曲，左面板拖拽音符到网格，支持拍号/速度/小节数控制，AI 补全，编译演奏。

**核心数据结构** `S`（全局状态对象）：

```javascript
S = {
  compId: null,         // composition_id，后端分配或 Date.now()（离线）
  offline: false,       // 离线模式标记
  editStatus: null,     // EDITING / LOCKED
  notes: [],            // [{string_part_id, start_tick, duration_tick, velocity, note_id?, ai_completion_no?}]
  ticksPerBeat: 480,    // 每拍 tick 数
  timeSig: {num:4, den:4},  // 当前拍号
  bpm: 120,             // 速度
  measures: 4,          // 小节数（2–16）
  aiCompletionId: null, // AI 补全批次 ID
  aiNotes: [],          // 当前 AI 建议音符
  runId: null,          // 演奏 run ID
  commandAssetId: null,  // 编译后的指令 asset ID
}
```

**网格常量**：

```javascript
BEAT_W = 96;   // 每拍像素宽度（2026-07-29 由 48 加宽，容纳细分落点）
SUB = 4;       // 每拍细分数（十六分音符精度）
SUB_W = 24;    // 每个细分格宽度 = BEAT_W / SUB
ROW_H = 26;    // 每行（弦）像素高度
LABEL_W = 38;  // 左侧弦标签宽度
N_STRINGS = 21; // 古筝 21 弦
```

**网格数学**：
- `totalBeats() = measures × beatsPerMeasure()` （总拍数）
- `beatsPerMeasure() = timeSig.num` （每小节拍数）
- `totalCols() = totalBeats() × SUB` （总列数，列为细分格）
- 细分格序号 → 拍位置：`subToBeat(sub) = (sub-1)/SUB + 1`（支持 0.25 步进小数）
- 音符位置：`left = LABEL_W + (startBeat - 1) × BEAT_W`
- 音符宽度：`width = durBeats × BEAT_W`，最小 `SUB_W`

**时值映射（标准乐理）**：
- 全音符 = 整小节（`beatsPerMeasure()` 拍）、二分 = 2 拍、四分 = 1 拍、八分 = ½ 拍、十六分 = ¼ 拍
- `duration_tick = durBeats × ticksPerBeat`，与后端 480 ticks/拍 约定一致
- 拍线（每 4 细分格）与小节线分级加粗：`.beat-line` / `.bar-line`

**拖拽逻辑**（HTML5 DnD）：
- 面板音符 `dragstart` 时 `dataTransfer.setData('text/plain', 'new:' + dur)`
- 谱上音符 `dragstart` 时 `dataTransfer.setData('text/plain', 'move:' + index)`
- 格子 `drop` 时根据前缀判断"新建"还是"移动"
- 拖出谱子外松开会触发 `dragend` 中 `dropEffect==='none'` → 删除
- 点击已放置音符也可删除

**跨小节判定**：
- `wouldCrossMeasure(startBeat, durBeats)`：起点和终点是否在不同小节
- `clampToMeasure(startBeat, durBeats)`：把时长截断到小节末尾
- 放置和移动时都会调用，自动截断超出的部分

**离线模式**：
- 初始化时 API 调用失败 → `S.offline = true`，`S.compId = Date.now()`
- 拖拽放置正常，只是不调后端 API
- AI 补全 / 编译 / 演奏按钮 toast "离线模式不支持xxx"

**后端 API 对接**（composition 模块后端尚未实现）：

| 端点 | 方法 | 触发时机 |
|---|---|---|
| `/api/composition/init` | GET | 点击"初始化工作区" |
| `/api/composition/note` | POST | 拖放音符到网格 |
| `/api/composition/ai-completion` | POST | 点击"请求 AI 补全" |
| `/api/composition/ai-completion/{id}/decision` | PUT | 接受/拒绝 AI 音符 |
| `/api/composition/complete` | POST | 点击"完成创作 & 编译" |
| `/api/performance/run` | POST | 点击"发起演奏" |
| `/api/performance/run/{id}` | GET | 轮询演奏状态（2s 间隔） |

**⚠️ 注意**：
- 后端 composition Controller 不存在，目前只能离线模式使用
- 离线模式下音符数据存内存不持久化
- `buildGrid()` 每次重建整个网格 DOM，但格子事件已改为 `grid-body` 事件委托（2026-07-29 修复），切换拍号/小节数不再重复绑定上千个监听器
- 音符数很多时 `renderNotes()` 全量重建 `<div>`（未做虚拟化）

---

### 3. explore.html — 乐器探秘

**功能**：双 Tab（立体结构讲解 / 历史讲解），展示古筝部件、琴弦试听、历史时间轴。

**核心状态**：

```javascript
S = {
  parts: [],           // PartOverviewDTO[]
  selPartId: null,     // 当前选中的部件 ID
  selStringNo: null,   // 当前选中的弦号
  stages: [],          // HistoryStageDTO[]
}
```

**后端 DTO 对照**（已验证与 Java 后端一致）：

| Java DTO | JSON (snake_case) | 前端取值 |
|---|---|---|
| `PartOverviewDTO.partId` | `part_id` | `p.part_id` |
| `PartOverviewDTO.name` | `name` | `p.name` |
| `PartOverviewDTO.desc` | `desc` | `p.desc` |
| `PartOverviewDTO.imageUrl` | `image_url` | `p.image_url` |
| `PartDetailDTO.playRelation` | `play_relation` | `r.data.play_relation` |
| `StringHighlightDTO.stringNo` | `string_no` | `r.data.string_no` |
| `StringHighlightDTO.audioUrl` | `audio_url` | `r.data.audio_url` |
| `HistoryStageDTO.stageId` | `stage_id` | `s.stage_id` |
| `StageSongItemDTO.songId` | `song_id` | `s.song_id` |
| `StageSongItemDTO.name` | `name` | `s.name` |
| `StageSongItemDTO.bg` | `bg` | `s.bg`（tooltip） |

**⚠️ 重要：song-link 参数方式**：
- 后端 Controller 使用 `@RequestParam`，不是 `@RequestBody`
- 前端必须用 `URLSearchParams` + `Content-Type: application/x-www-form-urlencoded`
- 代码路径：`linkSong()` 函数，第 283 行

**琴弦数据**：
- 21 弦硬编码音名数组：`['D2','E2',...,'D6']`
- 音区划分：1-5 低音、6-10 中低、11-14 中音、15-17 中高、18-21 高音
- 后端 `StringProfile` 表通过 `midiNote` 计算 `pitch`（`PitchConverter.midiToPitch()`）
- 弦号校验：后端允许 1~64，但前端只展示 21 根

**后端 API 对接**（全部已有实现）：

| 端点 | 方法 | 对应 Java Controller |
|---|---|---|
| `/api/instrument-explore/home` | GET | `InstrumentExploreController.home()` |
| `/api/instrument-explore/structure/overview` | GET | `structureOverview()` |
| `/api/instrument-explore/structure/part-detail?part_id=` | GET | `partDetail(@RequestParam)` |
| `/api/instrument-explore/structure/string-highlight?string_no=&action=` | GET | `stringHighlight(@RequestParam ×2)` |
| `/api/instrument-explore/history/timeline` | GET | `historyTimeline()` |
| `/api/instrument-explore/history/stage-songs?stage_id=` | GET | `stageSongs(@RequestParam)` |
| `/api/instrument-explore/history/song-link` | POST (form) | `songLink(@RequestParam ×2)` |

---

### 4. songbook.html — 我要点歌

**状态**：占位页。展示模块描述和 10 个已规划接口列表。

**后端 API 参考**：`docs/API/我要点歌板块接口.md`

**核心实体**（后端已有）：
- `PlayableWork`：id, workKind, title, playableStatus
- `Song`：workId, artistName, originPeriod, backgroundText, styleText, featuredExcerpt

**⚠️ 后端 Controller 尚未实现**。

---

### 5. voice.html — 语音交互

**状态**：占位页。展示模块描述和 2 个已规划接口列表。

**后端 API 参考**：`docs/API/语音交互模块接口文档.md`

**⚠️ 后端 Controller 尚未实现**。

---

## 后端代码位置

```
backend/src/main/java/com/guzheng/explore/
├── controller/InstrumentExploreController.java   ← 唯一已实现的 Controller
├── dto/                                           ← 8 个 DTO
├── entity/                                        ← 11 个 Entity
├── mapper/                                        ← MyBatis Mapper 接口
├── service/InstrumentExploreService.java
├── service/impl/InstrumentExploreServiceImpl.java
└── util/PitchConverter.java                       ← MIDI 音符 → 音名转换
```

**Jackson 序列化**：Java camelCase → JSON snake_case（Spring Boot 默认配置已启用）

---

## 修复记录

### 2026-07-29（黄胤锦分工范围：首页 / 自由创作 / 乐器探秘）

**composition.html**
1. **性能**：格子 dragover/dragleave/drop/click 由每格 4 个监听器（16 小节 6/8 拍时 2688 格 × 4 ≈ 1 万个）改为 `grid-body` 事件委托；`buildGrid()` 内层 `innerHTML+=` 循环拼接改为数组 join 一次性写入；删除死变量 `isBarEnd`
2. **bug**：AI 补全 chip 接受/拒绝后从 DOM 移除，`decideAi()` 再用 `querySelectorAll` 下标定位会错位（点后面的 chip 动画作用到错误的 chip 上）→ chip 加 `data-idx`，按属性定位
3. **bug**：拖放新音符后异步回填 `note_id` 用 `S.notes[S.notes.length-1]`，快速连拖时响应乱序会把 id 写到错的音符上 → 改为闭包直接引用 note 对象
4. **bug**：小节数 ± 按钮定义了 `:disabled` 样式但从不禁用 → 边界（2/16）时禁用
5. **样式**：`.controls` sticky `top:52px` 与 topbar 实际高度 58px 不符会露 6px 缝 → 改为 58px

**explore.html**
1. **bug**：琴弦试听每次 `new Audio()`，连点不同弦会多个音频叠播 → 全局 `curAudio` 单例，切弦前停掉上一个
2. 删除死变量 `waveTimer`

**index.html**：检查无 bug，未改动。

### 2026-07-29 二更（网格细分）

**composition.html**
1. 每拍宽度 48→96px，每拍细分 4 个落点（十六分音符精度），一拍内可放 2 个八分 / 4 个十六分
2. 时值保持标准乐理映射：全=整小节、二分=2拍、四分=1拍、八分=½拍、十六分=¼拍（新增十六分音符到音符面板）
3. 落点/点击删除精度同步到 0.25 拍（`onCellClick` 容差 0.6→0.13，小于半个细分格）
4. 格子改挂 `data-sub`（细分格序号），事件委托里 `subToBeat()` 换算小数拍位置；删除失效的 `getCellEl()`
5. 过窄音符（<44px）不渲染文字标签，避免溢出

**songbook.html / voice.html**：属于于昊喆分工（见 `开发日志.md`），保持占位页原样，未动。

---

## 注意事项

1. **后端绝不能动**——这是用户的硬约束。前端只能适配后端已有接口，不能改后端代码。
2. **GSAP CDN**：所有页面依赖 `https://cdnjs.cloudflare.com/ajax/libs/gsap/3.12.5/gsap.min.js`，网络不通时动画失效但不影响基本功能。
3. **API base path**：硬编码为 `/api`，前后端同域部署。
4. **composition 模块无后端**：当前通过离线模式 mock，后端实现后只需去掉 `S.offline` fallback，`initWorkspace()` 中删除 `else` 分支即可。
5. **explore 模块 song-link**：注意 form-encoded POST vs JSON body 的差异，不要改回 JSON。
6. **音符位置用数学计算**：`composition.html` 中 `renderNotes()` 用 `LABEL_W + (startBeat-1)*BEAT_W` 而非 `offsetLeft`，修改网格常量时要同步检查。
7. **新增页面**：保持 CSS 变量一致、tobar 结构一致、GSAP 入场模式一致，参考现有的 `songbook.html` / `voice.html` 模板。
