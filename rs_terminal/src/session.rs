use std::collections::HashMap;
use uuid::Uuid;

use crate::terminal::TerminalProcess;

// 终端会话
struct Session {
    terminal: TerminalProcess,
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
        
        tokio::spawn(async move {
            if let Err(e) = terminal_clone.read_output(session_id_clone).await {
                log::error!("Error reading terminal output: {}", e);
            }
        });
        
        // 添加到会话映射
        self.sessions.insert(session_id.clone(), Session {
            terminal,
        });
        
        log::info!("Created new session with ID: {}", session_id);
        
        Ok(session_id)
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
    
    // 获取会话
    pub fn get_session(&self, session_id: &str) -> Option<&Session> {
        self.sessions.get(session_id)
    }
}
