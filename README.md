# 智能用户运营管理平台

基于 Spring Boot 2.7 + Vue 3 的企业级后台管理系统。项目从基础用户管理 Demo 升级为包含 RBAC 权限、JWT 认证、操作审计、数据可视化、Excel 导入导出和 AI 数据问答能力的完整管理平台。

## 项目亮点

- 完整 RBAC 权限闭环：用户、角色、菜单权限、按钮权限和角色授权配置。
- JWT 登录认证：后端签发 token，前端通过 `Authorization: Bearer <token>` 访问接口。
- 动态菜单与按钮权限：登录后按角色返回菜单和权限码，前端动态渲染导航和操作按钮。
- AI 数据分析：仪表盘支持用户画像、城市集中度、异常数据和运营建议分析。
- AI 日志风控：操作日志支持风险等级、风险分、 高频接口、异常操作和处置建议分析。
- AI 数据问答：基于用户、角色和操作日志上下文进行大模型对话。
- 流式大模型输出：AI 问答使用后端 NDJSON 流式接口和前端打字机渲染。
- 用户自定义大模型 API：可在系统设置中配置 OpenAI-compatible 或 Ollama 接入参数。
- 对话记录管理：AI 问答支持本地会话持久化、搜索、切换、新建和删除。
- 操作审计：通过 AOP 自动记录关键操作日志。
- Excel 能力：支持用户数据导入、导出。
- 前端体验优化：统一后台工作台视觉风格，支持个人中心、系统设置、消息中心。

## 技术栈

后端：

- Spring Boot 2.7.18
- Java 17
- MyBatis-Plus 3.5.3.1
- MySQL 8
- Redis
- JWT 0.9.1
- BCrypt
- Apache POI
- Spring AOP
- Java HttpClient

前端：

- Vue 3.2
- Vue Router 4
- Vuex 4
- Element Plus
- Axios
- ECharts
- html2canvas
- Fetch ReadableStream

## 功能模块

### 认证与权限

- 登录、退出登录
- JWT token 认证
- 用户-角色-权限模型
- 菜单权限控制
- 按钮权限控制
- 角色权限配置弹窗
- 默认角色：
  - `ADMIN`：系统管理员，拥有全部权限
  - `OPERATOR`：运营人员，可维护用户和查看仪表盘
  - `AUDITOR`：审计员，可查看仪表盘和操作日志

### 用户管理

- 用户分页查询
- 用户搜索
- 新增、编辑、删除
- 批量删除
- 用户详情查看
- Excel 导入
- Excel 导出

### 角色管理

- 角色分页查询
- 新增、编辑、删除角色
- 用户角色分配
- 角色菜单和按钮权限配置

### 数据仪表盘

- 用户总数
- 性别比例
- 平均年龄
- 年龄分布
- 城市分布
- 城市 Top 排名
- 仪表盘图片导出
- AI 数据解读

### 操作日志

- 操作日志分页查询
- 请求方法、URL、参数和结果记录
- AI 风险分析
- 高频接口识别
- 风险等级和处置建议

### AI 数据问答

- 左侧菜单入口：`AI 数据问答`
- 基于用户表、角色表和最近操作日志构建只读业务上下文。
- 支持普通问答接口和流式问答接口。
- 支持 OpenAI-compatible 大模型接口。
- 支持 Ollama 本地模型接口。
- 系统设置中可配置：
  - 大模型提供商
  - Base URL
  - API Key
  - 模型名称
- 前端支持 Markdown 基础渲染：
  - 加粗
  - 段落
  - 列表
- 对话记录保存在浏览器 `localStorage`：
  - 返回用户管理页后再进入 AI 页面会恢复当前对话
  - 支持按标题或消息内容搜索
  - 支持新建、切换和删除对话

## 环境要求

- JDK 17+
- MySQL 8+
- Redis 5+，可选；未启动时登录限流会自动降级
- Node.js 16+

项目自带 Maven Wrapper，不要求本机全局安装 Maven。

## 数据库初始化

后端默认连接：

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/user_management?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
spring.datasource.username=root
spring.datasource.password=123456
spring.redis.host=localhost
spring.redis.port=6379
```

初始化表结构和默认权限数据：

```powershell
mysql --default-character-set=utf8mb4 -uroot -p123456 -e "source E:/WorkSpace/Project/Java/springboot-vue-demo/springboot/src/main/resources/db/schema.sql"
```

导入演示用户数据：

```powershell
mysql --default-character-set=utf8mb4 -uroot -p123456 -e "source E:/WorkSpace/Project/Java/springboot-vue-demo/springboot/src/main/resources/db/seed-users.sql"
```

数据库脚本：

- [schema.sql](springboot/src/main/resources/db/schema.sql)
- [seed-users.sql](springboot/src/main/resources/db/seed-users.sql)

主要表：

- `users`
- `role`
- `user_role`
- `permission`
- `role_permission`
- `operation_log`

默认账号：

- 用户名：`admin`
- 密码：`123456`

## 后端启动

```powershell
cd springboot
.\mvnw.cmd spring-boot:run
```

后端地址：

```text
http://localhost:9090
```

后端测试：

```powershell
cd springboot
mvn test
```

后端打包：

```powershell
cd springboot
.\mvnw.cmd -DskipTests package
```

## 前端启动

```powershell
cd vue
npm install
npm run serve
```

前端地址：

```text
http://localhost:8080
```

前端构建：

```powershell
cd vue
npm run build
```

## 大模型配置

### 前端配置

登录后打开右上角系统设置，在“大模型 API 配置”中填写：

- 提供商：`openai-compatible` 或 `ollama`
- Base URL：例如 `https://api.openai.com/v1`
- API Key：OpenAI-compatible 服务需要填写
- 模型名称：例如 `gpt-4o-mini`

配置会保存在浏览器 `localStorage` 的 `appSettings` 中，AI 数据问答页会随请求一起发送到后端。

### 后端默认配置

也可以通过 `application.properties` 或环境变量配置默认模型参数：

```properties
ai.chat.enabled=true
ai.chat.provider=openai-compatible
ai.chat.timeout-ms=30000

ai.ollama.base-url=http://localhost:11434
ai.ollama.model=qwen2.5:14b

ai.openai.base-url=${SIYAN_BASE_URL:${OPENAI_BASE_URL:https://api.openai.com/v1}}
ai.openai.api-key=${siyan_key:${SIYAN_API:${OPENAI_API_KEY:}}}
ai.openai.model=${SIYAN_MODEL:${OPENAI_MODEL:gpt-4o-mini}}
```

说明：

- 前端系统设置中的模型配置优先级高于后端默认配置。
- OpenAI-compatible 接口使用 `/chat/completions`。
- Ollama 接口使用 `/api/chat`。
- 大模型不可用时，后端会返回基于规则的兜底分析答案。

## API 概览

### 用户接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/api/user/login` | POST | 登录，返回 token、角色、权限和菜单 |
| `/api/user/logout` | POST | 退出登录 |
| `/api/user` | GET | 分页查询用户 |
| `/api/user/{id}` | GET | 查询用户详情 |
| `/api/user` | POST | 新增用户 |
| `/api/user` | PUT | 修改用户 |
| `/api/user/{id}` | DELETE | 删除用户 |
| `/api/user/batch` | DELETE | 批量删除 |
| `/api/user/export` | GET | 导出 Excel |
| `/api/user/import` | POST | 导入 Excel |

### 角色与权限接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/api/role` | GET | 分页查询角色 |
| `/api/role/all` | GET | 查询全部角色 |
| `/api/role` | POST | 新增角色 |
| `/api/role` | PUT | 修改角色 |
| `/api/role/{id}` | DELETE | 删除角色 |
| `/api/role/user/{userId}` | GET | 查询用户角色 |
| `/api/role/assign` | POST | 分配用户角色 |
| `/api/role/permissions` | GET | 查询权限树 |
| `/api/role/{id}/permissions` | GET | 查询角色已有权限 |
| `/api/role/{id}/permissions` | POST | 保存角色权限 |

### AI 接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/api/ai/dashboard-analysis` | GET | 仪表盘 AI 数据分析 |
| `/api/ai/log-risk-analysis` | GET | 操作日志 AI 风险分析 |
| `/api/ai/chat` | POST | AI 数据问答，普通 JSON 返回 |
| `/api/ai/chat-stream` | POST | AI 数据问答，NDJSON 流式返回 |

`/api/ai/chat-stream` 返回事件：

```json
{"type":"meta","data":{"sources":["users"],"suggestions":["分析年龄分布"],"metrics":{}}}
{"type":"chunk","content":"这是模型返回的增量内容"}
{"type":"done","model":{"provider":"openai-compatible","model":"gpt-4o-mini","fallback":false,"reason":""}}
```

### 日志接口

| 接口 | 方法 | 说明 |
| --- | --- | --- |
| `/api/log` | GET | 分页查询操作日志 |

## 简历描述参考

项目名称：智能用户运营管理平台

项目描述：

> 基于 Spring Boot、Vue 3、MyBatis-Plus、MySQL、Redis 和 JWT 构建的企业级后台管理系统，支持用户管理、RBAC 权限控制、操作审计、数据可视化、Excel 导入导出和 AI 数据问答。

核心职责：

- 设计并实现 RBAC 权限模型，支持用户、角色、菜单权限和按钮权限配置。
- 实现 JWT 登录认证和前端动态菜单渲染，完成路由级和按钮级权限控制。
- 基于 AOP 实现操作日志审计，记录关键接口操作、请求 URL、参数和执行结果。
- 实现用户数据可视化仪表盘，提供城市分布、年龄分布和性别比例统计。
- 设计 AI 分析模块，支持用户画像解读、操作日志风险研判和基于业务数据的大模型问答。
- 实现 OpenAI-compatible / Ollama 大模型接入，支持流式输出、兜底规则回答和对话记录搜索。
- 实现 Excel 用户导入导出，支持批量数据维护。

## 常见问题

如果 PowerShell 直接读取中文文件出现乱码，文件本身仍是 UTF-8，可使用：

```powershell
Get-Content -Encoding UTF8 README.md
```

如果后端启动失败，优先检查：

- MySQL 是否启动
- `user_management` 数据库是否初始化
- `application.properties` 中数据库账号密码是否正确
- 9090 端口是否被占用

如果登录接口提示 Redis 连接失败，当前项目已支持限流降级；重新构建后端并启动即可。
