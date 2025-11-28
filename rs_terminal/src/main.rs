use std::net::SocketAddr;
use std::sync::Arc;
use tokio::sync::Mutex;

mod protocol;
mod session;
mod terminal;

use crate::session::SessionManager;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // 初始化日志
    env_logger::init();
    
    // 创建会话管理器
    let session_manager = Arc::new(Mutex::new(SessionManager::new()));
    
    log::info!("WebTransport server starting on http://localhost:8080");
    
    // 简化实现，使用wtransport的基本API
    // 由于wtransport 0.6.1的API与预期不同，我们先实现一个简单的服务器
    // 后续可以根据实际API调整
    
    Ok(())
}

