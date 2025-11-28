use std::sync::Arc;
use tokio::sync::Mutex;
use tokio::process::{Command, Child};
use tokio::io::{AsyncReadExt, AsyncWriteExt};

// 终端进程
#[derive(Clone)]
pub struct TerminalProcess {
    pub child: Arc<Mutex<Child>>,
    output_tx: Arc<Mutex<Option<tokio::sync::mpsc::Sender<String>>>>,
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
        
        log::info!("Created new terminal process with PID: {}", child.id().unwrap());
        
        Ok(Self {
            child: Arc::new(Mutex::new(child)),
            output_tx: Arc::new(Mutex::new(None)),
        })
    }
    
    // 写入输入到终端
    pub async fn write_input(&mut self, data: &str) -> anyhow::Result<()> {
        let mut child = self.child.lock().await;
        
        // 写入数据到终端的标准输入
        child.stdin.as_mut().unwrap().write_all(data.as_bytes()).await?;
        
        Ok(())
    }
    
    // 读取终端输出
    pub async fn read_output(&self, session_id: String) -> anyhow::Result<()> {
        // 简化实现，直接读取输出而不使用异步任务
        // 避免生命周期问题
        let mut buffer = [0; 1024];
        
        // 只读取stdout，简化实现
        let mut child = self.child.lock().await;
        let stdout = child.stdout.as_mut().unwrap();
        
        loop {
            match stdout.read(&mut buffer).await {
                Ok(0) => break,
                Ok(n) => {
                    let output = String::from_utf8_lossy(&buffer[..n]).to_string();
                    log::debug!("Terminal output for session {}: {:?}", session_id, output);
                    
                    // TODO: 实现将输出发送到客户端的逻辑
                    // 目前只是打印到日志
                },
                Err(e) => {
                    log::error!("Error reading terminal output: {}", e);
                    break;
                }
            }
        }
        
        Ok(())
    }
    
    // 关闭终端
    pub async fn close(&mut self) -> anyhow::Result<()> {
        let mut child = self.child.lock().await;
        
        // 终止子进程
        child.kill().await?;
        child.wait().await?;
        
        log::info!("Closed terminal process with PID: {}", child.id().unwrap());
        
        Ok(())
    }
}
