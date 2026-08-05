# 古筝智能体验系统数据库：JPT 扩展版

## 1. 安全边界

本目录是原“独立重构版”的兼容扩展副本。

- 新数据库名称：`guzheng_experience_jpt`
- 原数据库 `guzheng_experience_rebuild` 不会被创建、删除或修改。
- 旧库 `instrument_explorer` 也不会被修改。
- 原有 27 张业务表及原字段均保留。
- 仅给 `performance_run`、`robot_dispatch` 增加可空的 JPT 追踪字段，旧代码可以继续不传这些字段。
- `jpt_score.work_id` 使用 `ON DELETE RESTRICT`：因为 `current_work_id` 是引用 `work_id` 的 STORED 生成列，MySQL 8.0 不允许该基列外键使用 `ON DELETE CASCADE`。作品已有 JPT 时应归档，不应直接物理删除。

## 2. 文件与执行顺序

1. 执行 `guzheng_experience_jpt.sql`，创建独立新库及全部表。
2. 执行 `jpt_reference_data.sql`，写入 JPT 1.0 默认的 21 弦 D 五声音阶映射。

主建库脚本可重复执行，但会重建 `guzheng_experience_jpt` 中的表，因此已有业务数据环境不要重复运行。参考数据脚本可以重复运行。

## 3. 新增结构

### `jpt_tuning`

保存 JPT `META.tuning` 对应的调弦方案。调弦映射单独版本化，以便将来增加其他调式。

### `jpt_tuning_string`

保存一个调弦方案下稳定的 `string_no -> pitch/MIDI` 映射。JPT 弦号不是 `instrument_part.id`；需要操作机械部件时，通过 `string_no` 与原 `string_profile` 对接。

### `jpt_score`

保存一份确定 JPT 文件的元数据和修订信息：

- 统一关联 `playable_work`，所以歌曲和自由创作都能使用；
- 保存 JPT 版本、速度、拍号、ticks 和调弦；
- `score_asset_id` 指向 `digital_asset` 中的 `.jpt` 文件；
- `revision_no` 支持同一作品多个版本；
- 生成列与唯一索引保证每个作品最多一个 `is_current=TRUE` 的版本；
- 标题和作者保存 META 快照，保证历史文件可复现。

### `jpt_score_note`

结构化保存 JPT `NOTE`：开始 tick、持续 tick、业务弦号、力度、技法和左右手。

音高不重复保存，而是由 `jpt_tuning_string` 推导。复合外键保证音符弦号属于曲谱所声明的调弦方案。自由创作导出时，可以用 `source_composition_note_id` 追溯原编辑态音符。

### `jpt_compilation`

记录从某个确定 JPT 修订版到机器人 `COMMAND` 资源的编译尝试，包括编译器版本、成功结果和机械可达性等失败原因。

### `v_jpt_note_export`

提供生成 JPT 文本所需的扁平字段。后端按 `score_id` 查询并按 `sequence_no` 排序，即可逐行生成 `NOTE`。

## 4. 与旧结构的兼容关系

- `composition_note`：继续作为自由创作的可编辑工作数据。
- `jpt_score_note`：作为导入、锁定或导出后的确定曲谱快照。
- `song_resource`：保留给旧歌曲接口及封面、试听等资源；新 JPT 的统一主关联是 `jpt_score.score_asset_id`。
- `digital_asset.asset_kind='SCORE'`：保存 `.jpt` 文件；建议 MIME 类型使用 `text/plain; charset=utf-8`。
- `digital_asset.asset_kind='COMMAND'`：保存编译后的机械手控制指令，不能与 JPT 混用。
- `performance_run.jpt_score_id`：锁定一次演奏使用的曲谱版本；复合外键保证它属于同一 `work_id`。
- `robot_dispatch.jpt_compilation_id`：追踪发送的指令由哪次 JPT 编译产生。
- 删除 `playable_work` 前必须先显式处理其 `jpt_score`；正常业务流程推荐把作品状态改为 `ARCHIVED`。

## 5. 应用层必须继续校验的规则

数据库负责字段范围、外键和枚举一致性，以下规则应由 JPT 解析器或服务层完成：

1. 文件头、单个 `META` 和 `END` 是否齐全。
2. 未知字段的处理策略以及未知 JPT 版本的拒绝逻辑。
3. `.jpt` 文件解析出的 `pitch` 是否等于调弦表中的 `pitch_name`。
4. 曲谱是否处于 2 至 16 小节范围。
5. `score_asset_id` 指向的数字资源是否确实为 `asset_kind='SCORE'`。
6. `command_asset_id` 是否确实为 `asset_kind='COMMAND'`。
7. 机械手可达性、左右手机构冲突和最大动作速度。

## 6. 推荐业务流程

### 导入 JPT

解析并校验文件，登记 `digital_asset`，创建 `jpt_score`，批量写入 `jpt_score_note`，最后将曲谱状态更新为 `VALIDATED`。

### 自由创作导出 JPT

锁定 `composition`，读取所有 `ACTIVE` 音符，按 `string_profile.string_no` 转换业务弦号，创建新的 `jpt_score` 修订版和音符快照，生成 `.jpt` 文件并登记资源。

### 机器人演奏

以当前 `jpt_score` 创建 `jpt_compilation`，成功后登记 `COMMAND` 资源；创建 `performance_run` 时锁定 `jpt_score_id`，发送时在 `robot_dispatch` 记录 `jpt_compilation_id`。
