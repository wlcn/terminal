use std::sync::Arc;
use tokio::sync::Mutex;
use tokio::process::{Command, Child};
use tokio::io::AsyncWriteExt;

// 终端进程
#[derive(Clone)]
pub struct TerminalProcess {
    pub child: Arc<Mutex<Child>>,
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
        let child = Command::new(shell)
            .stdin(std::process::Stdio::piped())
            .stdout(std::process::Stdio::piped())
            .stderr(std::process::Stdio::piped())
            .spawn()?;
        
        log::info!("Created new terminal process with PID: {}", child.id().unwrap());
        
        Ok(Self {
            child: Arc::new(Mutex::new(child)),
        })
    }
    
    // 写入输入到终端
    pub async fn write_input(&mut self, data: &str) -> anyhow::Result<()> {
        let mut child = self.child.lock().await;
        
        // 写入数据到终端的标准输入
        child.stdin.as_mut().unwrap().write_all(data.as_bytes()).await?;
        
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
