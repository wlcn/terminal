use std::sync::Arc;
use tokio::sync::Mutex;
use std::process::Child;

use crate::config::ShellConfig;

// 使用portable-pty的统一API
use portable_pty::{PtyPair, CommandBuilder, PtySize};

// 终端进程 - 使用portable-pty的统一API
#[derive(Clone)]
pub struct TerminalProcess {
    // 异步读取器
    reader: Arc<Mutex<portable_pty::tokio::AsyncReader>>,
    // 异步写入器
    writer: Arc<Mutex<portable_pty::tokio::AsyncWriter>>,
    // 子进程，用于检查是否运行
    child: Arc<Mutex<Child>>,
    // 主PTY，用于调整大小
    pty_pair: Arc<Mutex<PtyPair>>,
}

impl TerminalProcess {
    // 根据配置创建终端进程
    pub async fn new_with_config(shell_config: &ShellConfig) -> anyhow::Result<Self> {
        // 创建命令构建器
        let mut command_builder = CommandBuilder::new(&shell_config.command[0]);
        
        // 添加命令参数
        if shell_config.command.len() > 1 {
            command_builder.args(&shell_config.command[1..]);
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
            command_builder.cwd(resolved_dir);
        }
        
        // 设置环境变量
        for (key, value) in &shell_config.environment {
            command_builder.env(key, value);
        }
        
        // 获取默认的PTY系统
        let pty_system = portable_pty::native_pty_system();
        
        // 创建PTY对
        let mut pty_pair = pty_system.openpty(PtySize {
            rows: 24,
            cols: 80,
            pixel_width: 0,
            pixel_height: 0,
        })?;
        
        // 生成子进程
        let child = pty_pair.master.spawn_command(command_builder)?;
        
        // 创建异步读取器
        let reader = pty_pair.slave.try_clone_reader()?;
        let async_reader = portable_pty::tokio::AsyncReader::from_std(reader)?;
        
        // 创建异步写入器
        let writer = pty_pair.slave.take_writer()?;
        let async_writer = portable_pty::tokio::AsyncWriter::from_std(writer)?;
        
        log::info!("Created new PTY terminal process with PID: {:?} using command: {:?}", 
                  child.id(), shell_config.command);
        
        Ok(Self {
            reader: Arc::new(Mutex::new(async_reader)),
            writer: Arc::new(Mutex::new(async_writer)),
            child: Arc::new(Mutex::new(child)),
            pty_pair: Arc::new(Mutex::new(pty_pair)),
        })
    }
    
    // 写入输入到终端 - 使用独立的锁，避免死锁
    pub async fn write_input(&self, data: &str) -> anyhow::Result<()> {
        let mut writer = self.writer.lock().await;
        writer.write_all(data.as_bytes()).await?;
        Ok(())
    }
    
    // 读取终端输出 - 使用独立的锁，避免死锁
    pub async fn read_output(&self, buffer: &mut [u8]) -> anyhow::Result<String> {
        let mut reader = self.reader.lock().await;
        let n = reader.read(buffer).await?;
        
        if n == 0 {
            Ok(String::new()) // EOF
        } else {
            let output = String::from_utf8_lossy(&buffer[..n]).to_string();
            Ok(output)
        }
    }
    
    // 调整终端大小 - 异步设计，只在调整时持有锁
    pub async fn resize(&self, columns: u32, rows: u32) -> anyhow::Result<()> {
        log::info!("Resizing terminal to {} columns x {} rows", columns, rows);
        
        let pty_pair = self.pty_pair.lock().await;
        let size = PtySize {
            rows: rows as u16,
            cols: columns as u16,
            pixel_width: 0,
            pixel_height: 0,
        };
        
        pty_pair.master.resize(size)?;
        log::debug!("Successfully resized terminal to {}x{}", columns, rows);
        
        Ok(())
    }
    
    // 关闭终端 - 异步设计，只在关闭时持有锁
    pub async fn close(&self) -> anyhow::Result<()> {
        // 关闭读取器和写入器
        drop(self.reader.lock().await);
        drop(self.writer.lock().await);
        
        // 关闭PTY对
        drop(self.pty_pair.lock().await);
        
        // 终止子进程
        let mut child = self.child.lock().await;
        child.kill()?;
        child.wait()?;
        
        log::info!("Closed terminal process");
        Ok(())
    }
    
    // 检查终端进程是否还在运行
    pub async fn is_running(&self) -> bool {
        let mut child = self.child.lock().await;
        match child.try_wait() {
            Ok(None) => true, // 进程还在运行
            _ => false, // 进程已经退出或者发生错误
        }
    }
}
