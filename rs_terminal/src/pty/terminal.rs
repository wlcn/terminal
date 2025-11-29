use std::sync::Arc;
use tokio::sync::Mutex;

use crate::config::ShellConfig;

// 平台特定的导入
#[cfg(unix)]
use std::io::{Read, Write};
#[cfg(unix)]
use tokio::process::{Command, Child};
#[cfg(unix)]
use tokio_pty_process::{AsyncPtyMaster, PtyMaster};

#[cfg(windows)]
use tokio::process::{Command, Child, ChildStdin, ChildStdout, ChildStderr};
#[cfg(windows)]
use tokio::io::{AsyncWriteExt, AsyncReadExt};

// 终端进程 - 细粒度锁设计，避免死锁
#[derive(Clone)]
pub struct TerminalProcess {
    #[cfg(unix)]
    // Unix: 使用PTY主设备和进程
    pty_master: Arc<Mutex<AsyncPtyMaster>>,
    #[cfg(unix)]
    child: Arc<Mutex<Child>>,
    
    #[cfg(windows)]
    // Windows: 分别使用不同的锁保护stdin、stdout和stderr，避免死锁
    stdin: Arc<Mutex<ChildStdin>>,
    #[cfg(windows)]
    stdout: Arc<Mutex<ChildStdout>>,
    #[cfg(windows)]
    stderr: Arc<Mutex<ChildStderr>>,
    #[cfg(windows)]
    // 保留child用于关闭终端
    child: Arc<Mutex<Child>>,
}

impl TerminalProcess {
    // 根据配置创建终端进程
    pub async fn new_with_config(shell_config: &ShellConfig) -> anyhow::Result<Self> {
        // 创建命令
        let mut command = Command::new(&shell_config.command[0]);
        
        // 添加命令参数
        if shell_config.command.len() > 1 {
            command.args(&shell_config.command[1..]);
        }
        
        // 设置工作目录，解析环境变量
        if let Some(working_dir) = &shell_config.working_directory {
            // 解析环境变量
            let resolved_dir = if working_dir.is_empty() {
                // 使用当前目录
                ".".to_string()
            } else {
                // 替换环境变量
                let resolved = if working_dir == "${USERPROFILE}" {
                    std::env::var("USERPROFILE").unwrap_or(".".to_string())
                } else {
                    working_dir.clone()
                };
                resolved
            };
            
            log::debug!("Resolved working directory: {:?} -> {:?}", working_dir, resolved_dir);
            command.current_dir(resolved_dir);
        }
        
        // 设置环境变量
        for (key, value) in &shell_config.environment {
            command.env(key, value);
        }
        
        #[cfg(unix)]
        {
            // Unix: 使用tokio-pty-process 0.4.0的正确API创建PTY进程
            // 注意：tokio-pty-process 0.4.0使用旧版tokio API
            let pty_master = AsyncPtyMaster::open()?;
            
            // 创建并配置命令
            let child = command.spawn()?;
            
            log::info!("Created new PTY terminal process with PID: {:?} using command: {:?}", 
                      child.id(), shell_config.command);
            
            Ok(Self {
                pty_master: Arc::new(Mutex::new(pty_master)),
                child: Arc::new(Mutex::new(child)),
            })
        }
        
        #[cfg(windows)]
        {
            // Windows: 设置标准输入输出
            let mut child = command
                .stdin(std::process::Stdio::piped())
                .stdout(std::process::Stdio::piped())
                .stderr(std::process::Stdio::piped())
                .spawn()?;
            
            // 提取标准输入输出
            let stdin = child.stdin.take().unwrap();
            let stdout = child.stdout.take().unwrap();
            let stderr = child.stderr.take().unwrap();
            
            log::info!("Created new terminal process with PID: {} using command: {:?}", 
                      child.id().unwrap(), shell_config.command);
            
            Ok(Self {
                stdin: Arc::new(Mutex::new(stdin)),
                stdout: Arc::new(Mutex::new(stdout)),
                stderr: Arc::new(Mutex::new(stderr)),
                child: Arc::new(Mutex::new(child)),
            })
        }
    }
    
    // 写入输入到终端 - 使用独立的锁，避免死锁
    pub async fn write_input(&self, data: &str) -> anyhow::Result<()> {
        #[cfg(unix)]
        {
            // Unix: 使用独立的PTY主设备锁，不影响读取操作
            let mut pty_master = self.pty_master.lock().await;
            
            // 写入数据到PTY主设备
            // 注意：tokio-pty-process 0.4.0使用旧版tokio API，所以我们需要使用其内置的write_all方法
            // 而不是tokio 1.x的AsyncWriteExt::write_all
            pty_master.write_all(data.as_bytes())?;
            
            Ok(())
        }
        
        #[cfg(windows)]
        {
            // Windows: 使用独立的stdin锁，不影响stdout/stderr读取
            let mut stdin = self.stdin.lock().await;
            
            // 写入数据到终端的标准输入
            stdin.write_all(data.as_bytes()).await?;
            
            Ok(())
        }
    }
    
    // 读取终端标准输出 - 使用独立的锁，避免死锁
    pub async fn read_stdout(&self, buffer: &mut [u8]) -> anyhow::Result<String> {
        #[cfg(unix)]
        {
            // Unix: 使用独立的PTY主设备锁，不影响写入操作
            let mut pty_master = self.pty_master.lock().await;
            
            // 同步读取终端输出（tokio-pty-process 0.4.0使用std::io::Read）
            let n = pty_master.read(buffer)?;
            
            // 立即释放锁，允许其他任务访问
            drop(pty_master);
            
            if n == 0 {
                Ok(String::new()) // EOF
            } else {
                let output = String::from_utf8_lossy(&buffer[..n]).to_string();
                Ok(output)
            }
        }
        
        #[cfg(windows)]
        {
            // Windows: 使用独立的stdout锁，不影响stdin写入
            let mut stdout = self.stdout.lock().await;
            
            // 异步读取终端输出
            let read_result = stdout.read(buffer).await;
            
            // 立即释放锁，允许其他任务访问
            drop(stdout);
            
            match read_result {
                Ok(0) => Ok(String::new()), // EOF
                Ok(n) => {
                    let output = String::from_utf8_lossy(&buffer[..n]).to_string();
                    Ok(output)
                },
                Err(e) => Err(e.into()),
            }
        }
    }
    
    // 读取终端标准错误 - 使用独立的锁，避免死锁
    pub async fn read_stderr(&self, _buffer: &mut [u8]) -> anyhow::Result<String> {
        #[cfg(unix)]
        {
            // Unix: PTY模式下stdout和stderr合并，所以直接返回空字符串
            Ok(String::new())
        }
        
        #[cfg(windows)]
        {
            // Windows: 使用独立的stderr锁，不影响其他操作
            let mut stderr = self.stderr.lock().await;
            
            // 异步读取终端错误输出
            let read_result = stderr.read(_buffer).await;
            
            // 立即释放锁，允许其他任务访问
            drop(stderr);
            
            match read_result {
                Ok(0) => Ok(String::new()), // EOF
                Ok(n) => {
                    let output = String::from_utf8_lossy(&_buffer[..n]).to_string();
                    Ok(output)
                },
                Err(e) => Err(e.into()),
            }
        }
    }
    
    // 读取终端输出（同时读取stdout和stderr）
    pub async fn read_output(&self, buffer: &mut [u8]) -> anyhow::Result<String> {
        // 首先尝试读取stdout
        let stdout_result = self.read_stdout(buffer).await;
        
        match stdout_result {
            Ok(stdout_output) if !stdout_output.is_empty() => {
                Ok(stdout_output)
            },
            _ => {
                // 如果stdout没有输出，尝试读取stderr
                self.read_stderr(buffer).await
            }
        }
    }
    
    // 调整终端大小 - 异步设计，只在调整时持有锁
    pub async fn resize(&self, columns: u32, rows: u32) -> anyhow::Result<()> {
        log::info!("Resizing terminal to {} columns x {} rows", columns, rows);
        
        #[cfg(unix)]
        {
            // Unix: 获取PTY主设备
            let pty_master = self.pty_master.lock().await;
            
            // 调用PTY主设备的resize方法调整终端大小，转换参数类型
            // 注意：tokio-pty-process 0.4.0的resize方法签名是resize(rows, cols)
            pty_master.resize(rows as u16, columns as u16)?;
            
            log::debug!("Successfully resized terminal to {}x{}", columns, rows);
        }
        
        #[cfg(windows)]
        {
            // Windows: 获取子进程
            let child = self.child.lock().await;
            let pid = child.id().unwrap();
            
            // 记录调整大小请求
            log::debug!("Resize request for terminal PID {}: {}x{}", pid, columns, rows);
            
            // 注意：在Windows上，调整终端大小需要使用Windows API
            // 这里我们只是记录日志，后续可以实现完整的调整逻辑
        }
        
        Ok(())
    }
    
    // 关闭终端 - 异步设计，只在关闭时持有锁
    pub async fn close(&self) -> anyhow::Result<()> {
        #[cfg(unix)]
        {
            let mut child = self.child.lock().await;
            
            // 获取进程ID
            let pid = child.id();
            
            // 终止子进程并等待退出
            child.kill().await?;
            child.wait().await?;
            
            // 记录关闭日志
            if let Some(pid_value) = pid {
                log::info!("Closed PTY terminal process with PID: {}", pid_value);
            } else {
                log::info!("Closed PTY terminal process");
            }
        }
        
        #[cfg(windows)]
        {
            let mut child = self.child.lock().await;
            
            // 获取进程ID（可能为None）
            let pid = child.id();
            
            // 终止子进程并等待退出
            child.kill().await?;
            child.wait().await?;
            
            // 记录关闭日志，处理PID可能为None的情况
            if let Some(pid_value) = pid {
                log::info!("Closed terminal process with PID: {}", pid_value);
            } else {
                log::info!("Closed terminal process");
            }
        }
        
        Ok(())
    }
    
    // 检查终端进程是否还在运行
    pub async fn is_running(&self) -> bool {
        #[cfg(unix)]
        {
            let mut child = self.child.lock().await;
            match child.try_wait() {
                Ok(None) => true, // 进程还在运行
                _ => false, // 进程已经退出或者发生错误
            }
        }
        
        #[cfg(windows)]
        {
            let mut child = self.child.lock().await;
            match child.try_wait() {
                Ok(None) => true, // 进程还在运行
                _ => false, // 进程已经退出或者发生错误
            }
        }
    }
}
