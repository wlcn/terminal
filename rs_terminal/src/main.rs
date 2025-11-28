
use std::sync::Arc;
use std::fs;
use tokio::sync::Mutex;

mod config;
mod http_server;
mod protocol;
mod session;
mod terminal;
mod transport;

use crate::config::Config;
use crate::session::SessionManager;

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    // 确保日志目录存在
    fs::create_dir_all("logs")?;
    
    // 初始化日志系统，使用log4rs配置文件
    log4rs::init_file("log4rs.yml", Default::default())?;
    
    // 加载配置
    let config = Arc::new(Config::load_default()?);
    log::info!("Configuration loaded successfully");
    log::debug!("Default shell type: {}", config.terminal.default_shell_type);
    log::debug!("Available shells: {:?}", config.terminal.shells.keys());
    
    // 创建会话管理器
    let session_manager = Arc::new(Mutex::new(SessionManager::new(config.clone())));
    
    // 启动HTTP服务器
    let http_session_manager = session_manager.clone();
    let http_config = config.clone();
    tokio::spawn(async move {
        if let Err(e) = crate::http_server::start_server(http_session_manager, http_config).await {
            log::error!("HTTP server error: {}", e);
        }
    });
    
    // 启动WebSocket服务器
    let ws_session_manager = session_manager.clone();
    let ws_config = config.clone();
    tokio::spawn(async move {
        if let Err(e) = transport::websocket::start_server(ws_session_manager, ws_config).await {
            log::error!("WebSocket server error: {}", e);
        }
    });
    
    // 暂时禁用WebTransport服务器，等待API完善
    // let wt_session_manager = session_manager.clone();
    // let wt_config = config.clone();
    // tokio::spawn(async move {
    //     if let Err(e) = transport::webtransport::start_server(wt_session_manager, wt_config).await {
    //         log::error!("WebTransport server error: {}", e);
    //     }
    // });
    
    log::info!("Servers starting: HTTP on http://localhost:8082, WebSocket on ws://localhost:8081");
    
    // 保持主线程运行
    tokio::signal::ctrl_c().await?;
    
    Ok(())
}

