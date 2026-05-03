# 多人在线协作编辑系统

一个功能完整的实时协作编辑平台，支持多人同时编辑文档、实时同步、视频会议、任务管理等功能。

## ✨ 核心特性

### 📝 文档协作
- **实时同步编辑** - 多人同时编辑同一文档，内容实时同步
- **光标位置显示** - 实时显示其他用户的编辑位置
- **富文本 & Markdown** - 支持两种文档格式，可互相转换
- **文档导出** - 支持导出为 HTML、PDF、Markdown 格式
- **自动保存** - 编辑过程中自动保存，防止内容丢失
- **回收站机制** - 软删除设计，文档可恢复

### 🔍 智能功能
- **AI 智能搜索** - 基于 TF-IDF 和余弦相似度的语义搜索
- **AI 智能归类** - 使用 K-Means 聚类自动对文档进行分类

### 👥 协作功能
- **评论系统** - 支持文档内评论、回复、@提及用户
- **任务管理** - 创建、分配、跟踪文档相关任务
- **协作者管理** - 文档所有者可添加/移除协作者
- **在线状态** - 实时显示文档的在线用户

### 🎥 视频会议
- **实时音视频通话** - 实时音视频传输
- **屏幕共享** - 支持会议中的屏幕共享功能
- **聊天系统** - 会议内实时文字聊天
- **参与者管理** - 显示在线成员，支持媒体状态控制

### 👑 权限体系
- **三种角色** - 查看者 (Viewer)、编辑者 (Editor)、管理员 (Admin)
- **细粒度权限** - 每个文档独立的创建者和协作者权限
- **角色升级** - 查看者可申请升级为编辑者
- **操作日志** - 完整记录用户所有操作

### 🔔 通知系统
- **实时通知** - WebSocket 推送各类通知
- **任务提醒** - 任务创建、完成通知
- **评论回复** - 被回复或被 @ 时收到通知
- **协作者变更** - 被添加/移除协作者时通知

### 📊 系统管理
- **用户管理** - 管理员可查看用户信息、操作日志
- **模板系统** - 支持创建和使用文档模板（公开/私有）
- **标签系统** - 文档标签管理，支持搜索
- **满意度调查** - 编辑者可填写问卷，管理员可统计分析

## 🏗️ 技术架构

| 技术 | 用途 |
|------|------|
| Spring Boot | 核心框架 |
| Spring Data JPA | 数据库交互 |
| Spring WebSocket | 实时通信 |
| MySQL | 关系型数据库 |
| Quill | 富文本编辑器 |
| EasyMDE | Markdown 编辑器 |

## 📁 项目结构

```
src/main/java/com/multiuser_online_editing/
├── controller/                  # 控制层 - REST API 和 WebSocket 端点
│   ├── user_management/         # 用户管理模块
│   ├── document_management/     # 文档管理模块
│   ├── collaboration/           # 实时协作模块
│   ├── communication/           # 通知与通信模块
│   └── system_management/       # 系统管理模块
├── service/                     # 服务层 - 业务逻辑
├── repository/                  # 数据访问层 - JPA 仓储
├── entity/                      # 实体类
├── config/                      # 配置类
│   ├── SecurityConfig.java
│   ├── WebConfig.java
│   └── WebSocketConfig.java
└── util/                        # 工具类
│   ├── AuthTokenFilter.java
│   └── JwtUtils.java
```

## 🗄️ 数据库设计

核心数据表：

| 表名 | 描述 |
|------|------|
| `users` | 用户信息、角色、头像 |
| `documents` | 文档内容、类型、版本、自动保存 |
| `folders` | 文件夹结构、层级管理 |
| `tags` | 文档标签 |
| `templates` | 文档模板（公开/私有） |
| `comments` | 评论及回复、@提及 |
| `tasks` | 任务管理、状态、优先级 |
| `collaboration_sessions` | 协作会话管理 |
| `notifications` | 通知消息 |
| `video_conferences` | 视频会议信息 |
| `operation_logs` | 用户操作日志 |
| `surveys` | 满意度调查问卷 |

## 🚀 快速开始

### 环境要求
- JDK 17+
- MySQL 8.0+
- Maven 3.6+

### 安装步骤

1. **克隆项目**
```bash
git clone 、https://github.com/lytlyt123456/MultiuserOnlineEditing.git
cd MultiuserOnlineEditing
```

2. **配置数据库**
```sql
CREATE DATABASE multiuser_online_editing;
```

3. **修改配置文件** `application.yaml`
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/multiuser_online_editing?useSSL=false&serverTimezone=UTC
spring.datasource.username=your_username
spring.datasource.password=your_password
```

4. **构建并运行**
```bash
mvn clean install
mvn spring-boot:run
```

5. **访问系统**
```
http://localhost:8080
```
