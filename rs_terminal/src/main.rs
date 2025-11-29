
use std::sync::Arc;
use std::fs;

mod config;
mod http_server;
mod protocol_adapter;
mod session;
mod terminal;
mod terminal_service;
mod transport;

use crate::config::Config;
use crate::protocol_adapter::ProtocolAdapterFactory;
use crate::session::SessionManager;
use crate::terminal_service::TerminalService;

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
    log::debug!("HTTP port: {}", config.http.port);
    log::debug!("WebSocket port: {}", config.websocket.port);
    log::debug!("WebTransport port: {}", config.webtransport.port);
    
    // 创建会话管理器
    let session_manager = Arc::new(SessionManager::new(config.clone()));
    
    // 创建终端服务
    let terminal_service = Arc::new(TerminalService::new(session_manager.clone()));
    
    // 启动HTTP服务器
    let http_session_manager = session_manager.clone();
    let http_config = config.clone();
    tokio::spawn(async move {
        if let Err(e) = crate::http_server::start_server(http_session_manager, http_config).await {
            log::error!("HTTP server error: {}", e);
        }
    });
    
    // 创建并启动WebSocket适配器
    let websocket_adapter = ProtocolAdapterFactory::create_websocket_adapter(terminal_service.clone(), config.clone());
    tokio::spawn(async move {
        if let Err(e) = websocket_adapter.start().await {
            log::error!("WebSocket server error: {}", e);
        }
    });
    
    // 创建并启动WebTransport适配器
    let webtransport_adapter = ProtocolAdapterFactory::create_webtransport_adapter(terminal_service.clone(), config.clone());
    tokio::spawn(async move {
        if let Err(e) = webtransport_adapter.start().await {
            log::error!("WebTransport server error: {}", e);
        }
    });
    
    log::info!(
        "Servers starting: HTTP on http://localhost:{}, WebSocket on ws://localhost:{}, WebTransport on https://localhost:{}",
        config.http.port,
        config.websocket.port,
        config.webtransport.port
    );
    
    // 保持主线程运行
    tokio::signal::ctrl_c().await?;
    
    Ok(())
}

