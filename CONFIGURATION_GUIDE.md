# kt-terminal 配置系统使用指南

## 概述

kt-terminal 现在支持灵活的配置加载机制，可以根据不同的环境和操作系统自动加载相应的配置文件。配置加载优先级为：**环境配置 > 操作系统配置 > 基础配置**。

## 配置文件结构

### 基础配置文件
- `application.conf` - 基础配置，包含所有环境的默认值

### 环境特定配置文件
- `application-prod.conf` - 生产环境配置
- `application-test.conf` - 测试环境配置
- `application-dev.conf` - 开发环境配置

### 操作系统特定配置文件
- `application-windows.conf` - Windows 系统配置
- `application-linux.conf` - Linux 系统配置
- `application-mac.conf` - macOS 系统配置

## 配置加载方式

### 1. 自动检测（默认）
系统会自动检测当前操作系统并加载相应的配置文件：

```bash
# 自动检测操作系统
./gradlew.bat :gateways:websocket-gateway:run
```

### 2. 手动指定操作系统
如果自动检测不可靠，可以手动指定操作系统类型：

```bash
# 指定使用 Windows 配置
./gradlew.bat :gateways:websocket-gateway:run --args="--os=windows"

# 指定使用 Linux 配置  
./gradlew.bat :gateways:websocket-gateway:run --args="--os=linux"

# 指定使用 macOS 配置
./gradlew.bat :gateways:websocket-gateway:run --args="--os=mac"
```

### 3. 组合使用环境和操作系统配置

```bash
# 生产环境 + Windows 配置
./gradlew.bat :gateways:websocket-gateway:run --args="--env=prod --os=windows"

# 测试环境 + Linux 配置
./gradlew.bat :gateways:websocket-gateway:run --args="--env=test --os=linux"
```

### 4. 指定端口（向后兼容）

```bash
# 指定端口（传统方式）
./gradlew.bat :gateways:websocket-gateway:run --args="8081"

# 指定端口（新方式）
./gradlew.bat :gateways:websocket-gateway:run --args="--port=8081"

# 完整参数组合
./gradlew.bat :gateways:websocket-gateway:run --args="--port=8081 --env=prod --os=windows"
```

## 命令行参数说明

| 参数 | 说明 | 示例 |
|------|------|------|
| `--port=<端口号>` | 指定服务器端口 | `--port=8081` |
| `--env=<环境名>` | 指定运行环境 | `--env=prod` |
| `--os=<系统类型>` | 指定操作系统 | `--os=windows` |

## 配置覆盖规则

配置文件的加载和覆盖遵循以下规则：

1. **基础配置** (`application.conf`) - 提供默认值
2. **操作系统配置** (`application-{os}.conf`) - 覆盖基础配置中的系统相关设置
3. **环境配置** (`application-{env}.conf`) - 覆盖所有前面的配置

## 示例配置

### Windows 特定配置 (`application-windows.conf`)
```hocon
terminal {
  pty {
    defaultCommand = "cmd /c echo Welcome to kt-terminal on Windows"
    defaultWorkingDirectory = "C:\\Users"
    customShellPath = "E:\\Program Files\\Git\\bin\\bash.exe"
  }
}
```

### Linux 特定配置 (`application-linux.conf`)
```hocon
terminal {
  pty {
    defaultCommand = "/bin/bash -c 'echo Welcome to kt-terminal on Linux'"
    defaultWorkingDirectory = "/home/user"
    customShellPath = "/bin/bash"
  }
}
```

## 最佳实践

1. **开发环境**：使用默认配置或指定开发环境
2. **测试环境**：使用测试环境配置确保与生产环境一致
3. **生产环境**：始终指定生产环境配置
4. **跨平台部署**：为每个操作系统创建专门的配置文件
5. **故障排查**：当自动检测失败时，手动指定操作系统类型

## 故障排除

### 常见问题

1. **配置未生效**：检查命令行参数是否正确传递
2. **操作系统检测失败**：手动指定 `--os` 参数
3. **端口冲突**：使用 `--port` 参数指定不同端口
4. **配置加载错误**：检查配置文件语法是否正确

### 调试信息

启动时查看日志中的配置信息：
```
📋 Configuration: environment=prod, osType=windows
```

这表示系统正在使用生产环境的 Windows 配置。