
use std::sync::Arc;
use tokio::sync::Mutex;

mod protocol;
mod session;
mod terminal;
mod transport;

use crate::session::SessionManager;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // 初始化日志
    env_logger::init();
    
    // 创建会话管理器
    let session_manager = Arc::new(Mutex::new(SessionManager::new()));
    
    // 启动WebSocket服务器
    let ws_session_manager = session_manager.clone();
    tokio::spawn(async move {
        if let Err(e) = transport::websocket::start_server(ws_session_manager).await {
            log::error!("WebSocket server error: {}", e);
        }
    });
    
    // 暂时禁用WebTransport服务器，等待API完善
    // let wt_session_manager = session_manager.clone();
    // tokio::spawn(async move {
    //     if let Err(e) = transport::webtransport::start_server(wt_session_manager).await {
    //         log::error!("WebTransport server error: {}", e);
    //     }
    // });
    
    log::info!("Servers starting: WebSocket on ws://localhost:8080");
    
    // 保持主线程运行
    tokio::signal::ctrl_c().await?;
    
    Ok(())
}

