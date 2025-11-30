# KT Terminal - 现代化Web终端平台

## 🚀 项目简介
基于现代技术栈构建的高性能Web终端应用平台，支持实时终端操作和会话管理，提供Kotlin和Rust两种后端实现，满足不同部署需求。

## 🎬 快速演示
![KT Terminal Demo3](assets/kt-terminal-demo3.gif)

## ✨ 核心功能
- **实时终端交互**: 完整的命令输入输出流程
- **会话管理**: 支持会话创建、查询和终止
- **双后端实现**: Kotlin和Rust两种后端可选
- **WebSocket通信**: 高效的实时双向通信
- **跨平台PTY支持**: 适配不同操作系统
- **配置化管理**: 支持TOML配置文件
- **现代化UI**: 科技感设计，流畅动画效果

## 🛠️ 技术栈

### 前端
- React 18
- TypeScript
- Tailwind CSS
- xterm.js
- WebSocket API

### 后端
- **Kotlin**: Spring Boot, Netty, DDD架构
- **Rust**: Tokio, Axum, portable-pty

## 🔌 通信协议

### WebSocket
- 默认端口: 8081
- 路径: `ws://localhost:8081/ws/{session_id}`
- 纯文本传输，简单高效

## 📋 API接口

### 会话管理
- `POST /api/sessions`: 创建新会话
- `GET /api/sessions`: 列出所有会话
- `GET /api/sessions/{session_id}`: 获取会话详情
- `DELETE /api/sessions/{session_id}`: 终止会话
- `PUT /api/sessions/{session_id}/resize`: 调整终端大小

## 📝 配置说明

### Rust后端配置
配置文件: `rs-terminal/application.toml`

```toml
[http]
port = 8080

[websocket]
port = 8081

[webtransport]
port = 8082

[shell]
program = "bash"
args = ["-l"]
```

## 🤝 贡献指南

欢迎提交Issue和Pull Request！

## 📄 许可证

MIT License