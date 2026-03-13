# 用户管理系统

基于 SpringBoot + Vue 的前后端分离用户管理系统。

## 🚀 技术栈

### 后端
- Spring Boot 2.7.18
- MyBatis-Plus 3.5.3.1
- MySQL 8.0.33
- BCrypt 密码加密
- Apache POi (Excel 导入导出)
- JWT Token 认证

### 前端
- Vue 3 (Composition API)
- Element Plus 2.9.7
- Axios
- Vue Router 4
- Vuex 4
- ECharts 5

## ✨ 功能特性

### 核心功能
- ✅ 用户登录认证（BCrypt 密码加密 + Token 验证）
- ✅ 用户管理（增删改查）
- ✅ 批量删除
- ✅ 分页查询
- ✅ 模糊搜索（用户名/昵称）
- ✅ Excel 数据导出
- ✅ Excel 数据导入
- ✅ Dashboard 数据可视化（ECharts）
- ✅ 权限拦截器
- ✅ 退出登录

### 技术亮点
- 使用 BCrypt 强哈希算法加密密码，保障用户信息安全
- 集成 Apache POi 实现 Excel 数据导入导出
- 基于 ECharts 实现数据可视化
- RESTful API 设计规范
- 前后端分离架构

## 📁 项目结构

