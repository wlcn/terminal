use std::sync::Arc;
use tokio::sync::Mutex;
use tokio::process::{Command, Child, ChildStdin, ChildStdout, ChildStderr};
use tokio::io::{AsyncWriteExt, AsyncReadExt};

use crate::config::ShellConfig;

// 终端进程 - 细粒度锁设计，避免死锁
#[derive(Clone)]
pub struct TerminalProcess {
    // 分别使用不同的锁保护stdin、stdout和stderr，避免死锁
    stdin: Arc<Mutex<ChildStdin>>,
    stdout: Arc<Mutex<ChildStdout>>,
    stderr: Arc<Mutex<ChildStderr>>,
    // 保留child用于关闭终端
    child: Arc<Mutex<Child>>,
}

impl TerminalProcess {
    // 创建新的终端进程
    pub async fn new() -> anyhow::Result<Self> {
        // 获取默认shell
        #[cfg(unix)]
        let shell = std::env::var("SHELL").unwrap_or_else(|_| "bash".to_string());
        
        #[cfg(windows)]
        let shell = std::env::var("COMSPEC").unwrap_or_else(|_| "cmd.exe".to_string());
        
        // 创建终端进程
        let mut child = Command::new(shell)
            .stdin(std::process::Stdio::piped())
            .stdout(std::process::Stdio::piped())
            .stderr(std::process::Stdio::piped())
            .spawn()?;
        
        // 提取标准输入输出
        let stdin = child.stdin.take().unwrap();
        let stdout = child.stdout.take().unwrap();
        let stderr = child.stderr.take().unwrap();
        
        log::info!("Created new terminal process with PID: {}", child.id().unwrap());
        
        Ok(Self {
            stdin: Arc::new(Mutex::new(stdin)),
            stdout: Arc::new(Mutex::new(stdout)),
            stderr: Arc::new(Mutex::new(stderr)),
            child: Arc::new(Mutex::new(child)),
        })
    }
    
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
        
        // 设置标准输入输出
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
    
    // 写入输入到终端 - 使用独立的stdin锁，避免死锁
    pub async fn write_input(&self, data: &str) -> anyhow::Result<()> {
        // 使用独立的stdin锁，不影响stdout/stderr读取
        let mut stdin = self.stdin.lock().await;
        
        // 写入数据到终端的标准输入
        stdin.write_all(data.as_bytes()).await?;
        
        Ok(())
    }
    
    // 读取终端输出 - 使用独立的stdout锁，避免死锁
    pub async fn read_output(&self, buffer: &mut [u8]) -> anyhow::Result<String> {
        // 使用独立的stdout锁，不影响stdin写入
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
    
    // 调整终端大小 - 异步设计，只在调整时持有锁
    pub async fn resize(&self, columns: u32, rows: u32) -> anyhow::Result<()> {
        log::info!("Resizing terminal to {} columns x {} rows", columns, rows);
        
        // 获取子进程
        let child = self.child.lock().await;
        let pid = child.id().unwrap();
        
        // 记录调整大小请求
        log::debug!("Resize request for terminal PID {}: {}x{}", pid, columns, rows);
        
        // 注意：在Windows上，调整终端大小需要使用Windows API
        // 这里我们只是记录日志，后续可以实现完整的调整逻辑
        // 对于Unix系统，可以使用pty库来调整大小
        
        Ok(())
    }
    
    // 关闭终端 - 异步设计，只在关闭时持有锁
    pub async fn close(&self) -> anyhow::Result<()> {
        let mut child = self.child.lock().await;
        
        // 终止子进程并等待退出
        child.kill().await?;
        child.wait().await?;
        
        log::info!("Closed terminal process with PID: {}", child.id().unwrap());
        
        Ok(())
    }
}
