# 古筝智能体验系统后端

当前 Spring Boot 工程包含：

- 乐器探秘：`/api/instrument-explore/**`
- 我要点歌：`/api/songbook/**`

“我要点歌”按照 `docs/API/我要点歌板块接口.md` 和 JPT 扩展版
`guzheng_experience_jpt` 数据库实现。

## 环境要求

- JDK 17
- MySQL 8
- Maven 3.9（项目暂时附带了 Maven 3.9.12）

## 初始化数据库

首次安装或需要重建测试库时，在 `backend` 目录下按照以下顺序执行脚本：

```text
1. database/database_v3(JPT)/guzheng_experience_jpt.sql
2. seed_data.sql
3. songbook_demo_data.sql
4. database/database_v3(JPT)/jpt_reference_data.sql
```

`database/database_v3(JPT)/guzheng_experience_jpt.sql` 会重建新库中的表，已有正式数据时不要重复执行。
`seed_data.sql` 会重置乐器探秘演示数据，也只应在开发或测试环境执行；其中已经登记了 21 根琴弦的真实单音 MP3。

已有数据库不需要重建，直接执行可重复运行的增量脚本：

```text
guzheng_string_audio_data.sql
```

该脚本会登记 21 条 `digital_asset`，并用 `part_resource.resource_role='DEMO_AUDIO'`
分别关联到 `STRING_01` 至 `STRING_21`。MP3 实体文件位于
`src/main/resources/static/assets/audio/guzheng/`。

`songbook_demo_data.sql` 和 `jpt_reference_data.sql` 可以重复执行，不会重复创建同名歌曲、描述词和调弦数据。歌曲图片、预览音频和机器人曲谱仍使用占位地址，接入真实资源后需要替换。

## 启动

PowerShell 示例：

```powershell
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '<你的MySQL密码>'
.\apache-maven-3.9.12\bin\mvn.cmd spring-boot:run
```

默认连接：

```text
jdbc:mysql://localhost:3306/guzheng_experience_jpt
```

如数据库不在本机，可另外设置 `DB_URL`。

## 我要点歌接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/songbook/home` | 点歌首页 |
| GET | `/api/songbook/songs` | 分页浏览或筛选曲库 |
| GET | `/api/songbook/songs/{songId}` | 曲目详情 |
| POST | `/api/songbook/search` | 搜索歌曲，无匹配时自动给出替代 |
| POST | `/api/songbook/recommendations` | 按风格、情绪、场景推荐 |
| POST | `/api/songbook/alternatives` | 主动获取替代曲目 |
| POST | `/api/songbook/performances` | 创建机器人演奏任务 |
| GET | `/api/songbook/performances/{performanceId}` | 查询演奏状态 |
| GET | `/api/songbook/feedback/descriptors` | 获取反馈标签 |
| POST | `/api/songbook/performances/{performanceId}/feedback` | 提交演奏反馈 |

创建演奏任务后状态为 `QUEUED`。机器人调度程序负责继续更新为
`SENDING`、`PLAYING`、`SUCCEEDED`、`FAILED` 或 `CANCELLED`。
提交反馈时必须使用创建演奏任务接口实际返回的 `performanceId`，且任务状态必须为
`SUCCEEDED`；反馈标签 ID 应先通过 `/api/songbook/feedback/descriptors` 查询，不能写死示例值。

错误响应同时使用正确的 HTTP 状态码和响应体 `code`，例如无法识别推荐词返回 HTTP 422，
演奏任务不存在返回 HTTP 404。

## 测试

普通单元测试和 Controller 测试不需要连接数据库：

```powershell
.\apache-maven-3.9.12\bin\mvn.cmd test
```

如需运行基于真实 JPT 扩展版 MySQL 的完整接口测试：

```powershell
$env:RUN_MYSQL_INTEGRATION_TESTS = 'true'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '<你的MySQL密码>'
.\apache-maven-3.9.12\bin\mvn.cmd -Dtest=SongbookApiIntegrationTest test
```

集成测试中的临时歌曲、资源、推荐记录、演奏任务和反馈全部位于测试事务中，
结束后自动回滚。
