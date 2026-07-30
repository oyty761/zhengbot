# 古筝智能体验系统后端

当前 Spring Boot 工程包含：

- 乐器探秘：`/api/instrument-explore/**`
- 我要点歌：`/api/songbook/**`

“我要点歌”按照 `docs/API/我要点歌板块接口.md` 和重构版
`guzheng_experience_rebuild` 数据库实现。

## 环境要求

- JDK 17
- MySQL 8
- Maven 3.9（项目暂时附带了 Maven 3.9.12）

## 初始化数据库

先执行项目根目录下的重构版建库脚本：

```text
guzheng_experience_rebuild.sql
```

该脚本会重建数据库表，已有正式数据时不要重复执行。

如需页面联调数据，可再手动执行：

```text
songbook_demo_data.sql
```

演示脚本可重复执行，不会重复创建同名歌曲和描述词。脚本中的资源地址是占位地址，
接入真实图片、音频和机器人曲谱后需要替换。

## 启动

PowerShell 示例：

```powershell
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '你的MySQL密码'
.\apache-maven-3.9.12\bin\mvn.cmd spring-boot:run
```

默认连接：

```text
jdbc:mysql://localhost:3306/guzheng_experience_rebuild
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

## 测试

普通单元测试和 Controller 测试不需要连接数据库：

```powershell
.\apache-maven-3.9.12\bin\mvn.cmd test
```

如需运行基于真实重构版 MySQL 的完整接口测试：

```powershell
$env:RUN_MYSQL_INTEGRATION_TESTS = 'true'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = '你的MySQL密码'
.\apache-maven-3.9.12\bin\mvn.cmd -Dtest=SongbookApiIntegrationTest test
```

集成测试中的临时歌曲、资源、推荐记录、演奏任务和反馈全部位于测试事务中，
结束后自动回滚。
