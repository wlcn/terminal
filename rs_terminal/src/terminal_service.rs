use std::sync::Arc;
use tokio::sync::mpsc;

use crate::session::SessionManager;
use crate::terminal::TerminalProcess;

// 终端服务 - 处理PTY交互的核心逻辑
pub struct TerminalService {
    session_manager: Arc<SessionManager>,
}

impl TerminalService {
    // 创建新的终端服务
    pub fn new(session_manager: Arc<SessionManager>) -> Self {
        Self {
            session_manager,
        }
    }
    
    // 处理终端输入
    pub async fn handle_terminal_input(&self, session_id: &str, input: String) -> anyhow::Result<()> {
        // 将输入写入终端
        self.session_manager.write_to_session(session_id, &input).await?;
        Ok(())
    }
    
    // 处理终端连接
    pub async fn handle_terminal_connection(&self, session_id: &str, output_sender: mpsc::Sender<String>) -> anyhow::Result<()> {
        // 添加客户端发送者到会话
        self.session_manager.add_client_sender(session_id, output_sender).await;
        Ok(())
    }
    
    // 创建新的终端会话
    pub async fn create_terminal_session(&self) -> anyhow::Result<String> {
        // 创建新会话
        let session_id = self.session_manager.create_session().await?;
        Ok(session_id)
    }
}
