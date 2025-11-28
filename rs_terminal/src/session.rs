use std::collections::HashMap;
use std::sync::{Arc, Mutex};
use tokio::io::AsyncReadExt;
use uuid::Uuid;

use crate::config::{Config, ShellConfig};
use crate::terminal::TerminalProcess;

// 终端会话
#[derive(Clone)]
pub(crate) struct Session {
    terminal: TerminalProcess,
    // 客户端发送通道
    client_senders: Arc<Mutex<Vec<tokio::sync::mpsc::Sender<String>>>>,
}

// 会话管理器
pub struct SessionManager {
    sessions: HashMap<String, Session>,
    config: Arc<Config>,
}

impl SessionManager {
    // 创建新的会话管理器
    pub fn new(config: Arc<Config>) -> Self {
        Self {
            sessions: HashMap::new(),
            config,
        }
    }
    
    // 创建新会话
    pub async fn create_session(&mut self) -> anyhow::Result<String> {
        // 生成会话ID
        let session_id = Uuid::new_v4().to_string();
        
        // 获取默认shell配置
        let default_shell_config = self.config.get_default_shell_config();
        
        // 创建终端进程
        let terminal = TerminalProcess::new_with_config(default_shell_config).await?;
        
        // 添加到会话映射
        self.sessions.insert(session_id.clone(), Session {
            terminal: terminal.clone(),
            client_senders: Arc::new(Mutex::new(Vec::new())),
        });
        
        // 启动终端输出监听
        let terminal_clone = terminal.clone();
        let session_id_clone = session_id.clone();
        let client_senders = Arc::new(Mutex::new(Vec::<tokio::sync::mpsc::Sender<String>>::new()));
        
        // 保存客户端发送者的引用到会话中
        let client_senders_clone = client_senders.clone();
        tokio::spawn(async move {
            let mut buffer = [0; 1024];
            
            loop {
                // 只在读取时获取锁
                let mut child = terminal_clone.child.lock().await;
                let stdout = child.stdout.as_mut().unwrap();
                
                let read_result = stdout.read(&mut buffer).await;
                // 立即释放锁，允许其他任务访问
                drop(child);
                
                match read_result {
                    Ok(0) => break,
                    Ok(n) => {
                        let output = String::from_utf8_lossy(&buffer[..n]).to_string();
                        log::debug!("Terminal output for session {}: {:?}", session_id_clone, output);
                        
                        // 发送输出到所有连接的客户端
                        let senders = client_senders_clone.lock().unwrap().clone();
                        for sender in senders {
                            if let Err(e) = sender.send(output.clone()).await {
                                log::error!("Error sending terminal output to client: {}", e);
                                // 移除失效的发送者
                                let mut senders = client_senders_clone.lock().unwrap();
                                senders.retain(|s| !std::ptr::eq(s, &sender));
                            }
                        }
                    },
                    Err(e) => {
                        log::error!("Error reading terminal output: {}", e);
                        break;
                    }
                }
            }
        });
        
        // 更新会话的客户端发送者引用
        if let Some(session) = self.sessions.get_mut(&session_id) {
            session.client_senders = client_senders;
        }
        
        log::info!("Created new session with ID: {} using shell: {:?}", 
                  session_id, default_shell_config.command);
        
        Ok(session_id)
    }
    
    // 添加客户端发送通道
    pub fn add_client_sender(&mut self, session_id: &str, sender: tokio::sync::mpsc::Sender<String>) {
        if let Some(session) = self.sessions.get_mut(session_id) {
            let mut client_senders = session.client_senders.lock().unwrap();
            client_senders.push(sender);
            log::info!("Added client sender for session: {}", session_id);
        }
    }
    
    // 移除客户端发送通道
    pub fn remove_client_sender(&mut self, session_id: &str, sender: &tokio::sync::mpsc::Sender<String>) {
        if let Some(session) = self.sessions.get_mut(session_id) {
            let mut client_senders = session.client_senders.lock().unwrap();
            client_senders.retain(|s| !std::ptr::eq(s, sender));
            log::info!("Removed client sender for session: {}", session_id);
        }
    }
    
    // 写入数据到会话
    pub async fn write_to_session(&mut self, session_id: &str, data: &str) -> anyhow::Result<()> {
        if let Some(session) = self.sessions.get_mut(session_id) {
            session.terminal.write_input(data).await?;
            log::debug!("Wrote data to session {}: {:?}", session_id, data);
        } else {
            anyhow::bail!("Session not found: {}", session_id);
        }
        
        Ok(())
    }
    
    // 关闭会话
    pub async fn close_session(&mut self, session_id: &str) -> anyhow::Result<()> {
        if let Some(mut session) = self.sessions.remove(session_id) {
            session.terminal.close().await?;
            log::info!("Closed session: {}", session_id);
        } else {
            anyhow::bail!("Session not found: {}", session_id);
        }
        
        Ok(())
    }
    
    // 检查会话是否存在
    pub fn session_exists(&self, session_id: &str) -> bool {
        self.sessions.contains_key(session_id)
    }
}
