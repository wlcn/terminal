use std::sync::Arc;
use tokio::sync::Mutex;
use tokio::process::{Command, Child};
use tokio::io::AsyncWriteExt;

use crate::config::ShellConfig;

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
    
    // 根据配置创建终端进程
    pub async fn new_with_config(shell_config: &ShellConfig) -> anyhow::Result<Self> {
        // 创建命令
        let mut command = Command::new(&shell_config.command[0]);
        
        // 添加命令参数
        if shell_config.command.len() > 1 {
            command.args(&shell_config.command[1..]);
        }
        
        // 设置工作目录
        if let Some(working_dir) = &shell_config.working_directory {
            command.current_dir(working_dir);
        }
        
        // 设置环境变量
        for (key, value) in &shell_config.environment {
            command.env(key, value);
        }
        
        // 设置标准输入输出
        let child = command
            .stdin(std::process::Stdio::piped())
            .stdout(std::process::Stdio::piped())
            .stderr(std::process::Stdio::piped())
            .spawn()?;
        
        log::info!("Created new terminal process with PID: {} using command: {:?}", 
                  child.id().unwrap(), shell_config.command);
        
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
