use std::collections::HashMap;
use tokio::io::AsyncReadExt;
use uuid::Uuid;

use crate::terminal::TerminalProcess;

// 终端会话
#[derive(Clone)]
pub(crate) struct Session {
    terminal: TerminalProcess,
    // 客户端发送通道
    client_senders: Vec<tokio::sync::mpsc::Sender<String>>,
}

// 会话管理器
pub struct SessionManager {
    sessions: HashMap<String, Session>,
}

impl SessionManager {
    // 创建新的会话管理器
    pub fn new() -> Self {
        Self {
            sessions: HashMap::new(),
        }
    }
    
    // 创建新会话
    pub async fn create_session(&mut self) -> anyhow::Result<String> {
        // 生成会话ID
        let session_id = Uuid::new_v4().to_string();
        
        // 创建终端进程
        let terminal = TerminalProcess::new().await?;
        
        // 启动终端输出监听
        let terminal_clone = terminal.clone();
        let session_id_clone = session_id.clone();
        let sessions_clone = self.sessions.clone();
        
        tokio::spawn(async move {
            let mut buffer = [0; 1024];
            let mut child = terminal_clone.child.lock().await;
            let stdout = child.stdout.as_mut().unwrap();
            
            loop {
                match stdout.read(&mut buffer).await {
                    Ok(0) => break,
                    Ok(n) => {
                        let output = String::from_utf8_lossy(&buffer[..n]).to_string();
                        log::debug!("Terminal output for session {}: {:?}", session_id_clone, output);
                        
                        // 发送输出到所有连接的客户端
                        let sessions = sessions_clone.clone();
                        if let Some(session) = sessions.get(&session_id_clone) {
                            for sender in &session.client_senders {
                                if let Err(e) = sender.send(output.clone()).await {
                                    log::error!("Error sending terminal output to client: {}", e);
                                }
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
        
        // 添加到会话映射
        self.sessions.insert(session_id.clone(), Session {
            terminal,
            client_senders: Vec::new(),
        });
        
        log::info!("Created new session with ID: {}", session_id);
        
        Ok(session_id)
    }
    
    // 添加客户端发送通道
    pub fn add_client_sender(&mut self, session_id: &str, sender: tokio::sync::mpsc::Sender<String>) {
        if let Some(session) = self.sessions.get_mut(session_id) {
            session.client_senders.push(sender);
            log::info!("Added client sender for session: {}", session_id);
        }
    }
    
    // 移除客户端发送通道
    pub fn remove_client_sender(&mut self, session_id: &str, sender: &tokio::sync::mpsc::Sender<String>) {
        if let Some(session) = self.sessions.get_mut(session_id) {
            session.client_senders.retain(|s| !std::ptr::eq(s, sender));
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
