# SpringBoot + Vue3 用户管理系统

## 📖 项目简介

基于 **SpringBoot 2.7.18** + **Vue 3** + **Element Plus** 的前后端分离用户管理系统。该系统功能完善，界面美观，包含用户管理、角色管理、数据可视化、Excel 导入导出等核心功能，适合企业级后台管理系统开发参考。

---

## ✨ 主要功能

### 1. 用户管理
- ✅ 用户列表展示（分页）
- ✅ 用户新增、编辑、删除
- ✅ 批量删除
- ✅ 用户搜索（用户名/昵称）
- ✅ 用户详情查看
- ✅ Excel 导入/导出

### 2. 数据可视化仪表盘
- ✅ 用户统计（总数、男女比例、平均年龄）
- ✅ 性别分布饼图
- ✅ 年龄分布柱状图
- ✅ 用户地址分布环形图
- ✅ 城市用户数 TOP10 柱状图
- ✅ 用户数量 Top3 奖牌展示
- ✅ **仪表盘导出为图片**

### 3. 角色管理
- ✅ 角色列表展示（分页）
- ✅ 角色新增、编辑、删除
- ✅ 角色描述管理

### 4. 操作日志
- ✅ 用户操作记录
- ✅ 请求方法记录
- ✅ 操作时间记录

### 5. 安全功能
- ✅ JWT Token 认证
- ✅ BCrypt 密码加密
- ✅ 路由守卫
- ✅ 登录验证

---

## 🛠️ 技术栈

### 后端技术
| 技术           | 版本      | 说明       |
|--------------|---------|----------|
| Spring Boot  | 2.7.18  | 核心框架     |
| MyBatis-Plus | 3.5.3.1 | ORM 框架   |
| MySQL        | 8.0.33  | 数据库      |
| JWT          | 0.9.1   | Token 认证 |
| Lombok       | 1.18.26 | 简化代码     |
| Apache POI   | 5.2.3   | Excel 处理 |
| Spring AOP   | -       | 日志切面     |

### 前端技术
| 技术           | 版本     | 说明      |
|--------------|--------|---------|
| Vue          | 3.2.13 | 核心框架    |
| Vue Router   | 4.0.3  | 路由管理    |
| Vuex         | 4.0.0  | 状态管理    |
| Element Plus | 2.9.7  | UI 组件库  |
| ECharts      | 6.0.0  | 数据可视化   |
| Axios        | 1.8.4  | HTTP 请求 |
| html2canvas  | latest | 截图导出    |

---

---

## 🚀 快速开始

### 环境要求
- JDK 17+
- MySQL 8.0+
- Node.js 16+
- Maven 3.6+

### 1. 数据库配置
sql -- 创建数据库 CREATE DATABASE IF NOT EXISTS user_management DEFAULT CHARSET utf8mb4;
-- 使用数据库 USE user_management;
-- 创建用户表 
CREATE TABLE user ( 
id bigint NOT NULL AUTO_INCREMENT, 
username varchar(50) NOT NULL, 
password varchar(255) NOT NULL, 
nickname varchar(50) DEFAULT NULL, 
age int DEFAULT NULL, 
sex varchar(10) DEFAULT NULL, 
address varchar(255) DEFAULT NULL, 
create_time datetime DEFAULT CURRENT_TIMESTAMP, 
PRIMARY KEY (id) ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
-- 插入测试数据 
INSERT INTO user (username, password, nickname, age, sex, address) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '管理员', 25, '男', '北京市朝阳区');

### 2. 后端启动
bash
进入后端目录
cd springboot
修改数据库配置（application.properties）
spring.datasource.url=jdbc:mysql://localhost:3306/user_management?useUnicode=true&characterEncoding=utf-8&useSSL=false
spring.datasource.username=root
spring.datasource.password=你的密码
Maven 构建
mvn clean package
启动项目
mvn spring-boot:run
或者直接运行 DemoApplication.java
后端访问地址：http://localhost:9090

### 3. 前端启动
bash
进入前端目录
cd vue
安装依赖
npm install
启动开发服务器
npm run serve
构建生产版本
npm run build
前端访问地址：http://localhost:8080

---

## 📸 功能截图

### 仪表盘
- 用户数据统计
- 可视化图表
- Top3 奖牌展示
- 支持导出为图片

### 用户管理
- 列表展示
- 增删改查
- Excel 导入导出
- 批量删除

### 角色管理
- 角色配置
- 权限管理

### 操作日志
- 操作记录
- 时间追踪

---

## 🔧 核心功能说明

### 1. JWT 认证流程
1. 用户登录，后端验证密码
2. 生成 JWT Token 返回给前端
3. 前端存储 Token 到 localStorage
4. 后续请求携带 Token
5. 后端拦截器验证 Token 有效性

### 2. Excel 导入导出
- **导出**：使用 Apache POI 生成 Excel 文件
- **导入**：解析 Excel 文件，批量插入数据

### 3. 仪表盘导出图片
- 使用 html2canvas 库
- 将 DOM 节点转换为 Canvas
- 导出为 PNG 图片下载

### 4. 密码加密
- 使用 BCrypt 强哈希算法
- 支持盐值加密
- 不可逆加密，安全可靠

---

## 🎯 项目亮点

1. **完整的前后端分离架构**
2. **RESTful API 设计规范**
3. **统一返回结果封装**
4. **全局异常处理**
5. **JWT Token 认证**
6. **数据可视化展示**
7. **Excel 导入导出功能**
8. **操作日志记录（AOP）**
9. **响应式 UI 设计**
10. **代码规范，注释清晰**

---

## 📝 API 接口文档

### 用户接口
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/user | GET | 分页查询用户 |
| /api/user/{id} | GET | 查询用户详情 |
| /api/user | POST | 新增用户 |
| /api/user | PUT | 修改用户 |
| /api/user/{id} | DELETE | 删除用户 |
| /api/user/batch | DELETE | 批量删除 |
| /api/user/export | GET | 导出 Excel |
| /api/user/import | POST | 导入 Excel |

### 角色接口
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/role | GET | 分页查询角色 |
| /api/role | POST | 新增角色 |
| /api/role | PUT | 修改角色 |
| /api/role/{id} | DELETE | 删除角色 |

### 日志接口
| 接口 | 方法 | 说明 |
|------|------|------|
| /api/log | GET | 分页查询日志 |

---

## 🔐 默认账号

- 用户名：`admin`
- 密码：`123456`

---

## 💡 常见问题

### 1. 前端启动失败
bash
清除缓存
npm cache clean --force
删除 node_modules
rm -rf node_modules package-lock.json
重新安装
npm install
### 2. 后端数据库连接失败
- 检查 MySQL 服务是否启动
- 检查数据库名、用户名、密码是否正确
- 检查端口是否为 3306

### 3. 跨域问题
- 后端已配置 CORS
- 前端使用 Axios 代理

---

## 📚 学习资源

- [Vue 3 官方文档](https://cn.vuejs.org/)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Element Plus 文档](https://element-plus.org/zh-CN/)
- [MyBatis-Plus 文档](https://baomidou.com/)
- [ECharts 文档](https://echarts.apache.org/zh/index.html)

