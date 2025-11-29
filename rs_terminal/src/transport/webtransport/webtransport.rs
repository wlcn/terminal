use std::sync::Arc;
use std::future::Future;
use std::pin::Pin;
use std::net::SocketAddr;
use std::str::FromStr;

use tokio::sync::mpsc;

// 使用wtransport 0.6.1正确的导入路径
use wtransport::endpoint::IncomingSession;
use wtransport::Endpoint;
use wtransport::ServerConfig;
use wtransport::Identity;

use crate::config::Config;
use crate::protocol::adapter::ProtocolAdapter;
use crate::pty::terminal_service::TerminalService;

// WebTransport适配器 - 实现ProtocolAdapter接口
pub struct WebTransportAdapter {
    terminal_service: Arc<TerminalService>,
    config: Arc<Config>,
}

impl WebTransportAdapter {
    // 创建新的WebTransport适配器
    pub fn new(terminal_service: Arc<TerminalService>, config: Arc<Config>) -> Self {
        Self {
            terminal_service,
            config,
        }
    }
}

// 实现ProtocolAdapter接口
impl ProtocolAdapter for WebTransportAdapter {
    fn start(&self) -> Pin<Box<dyn Future<Output = anyhow::Result<()>> + Send + '_>> {
        let terminal_service = self.terminal_service.clone();
        let port = self.config.webtransport.port;
        
        Box::pin(async move {
            log::info!("WebTransport server starting on https://localhost:{}", port);
            
            // 参考wtransport 0.6.1官方示例创建服务器配置
            let config = ServerConfig::builder()
                .with_bind_address(SocketAddr::from_str(&format!("0.0.0.0:{}", port))?)
                // 使用自签名证书，仅用于开发测试
                .with_identity(Identity::self_signed(["localhost"]).unwrap())
                // 添加更多的调试信息
                .build();
            
            let server = Endpoint::server(config)?;
            
            log::info!("WebTransport server started successfully on https://localhost:{}", port);
            log::info!("WebTransport server is ready to accept connections");
            log::info!("WebTransport server will handle requests to /webtransport/* paths");
            
            // 处理服务器连接
            loop {
                log::debug!("WebTransport server waiting for incoming connections...");
                let incoming_session = server.accept().await;
                log::debug!("WebTransport server accepted incoming session");
                let terminal_service = terminal_service.clone();
                
                // 为每个连接启动独立任务
                tokio::spawn(async move {
                    log::debug!("Starting new task to handle WebTransport connection");
                    if let Err(e) = handle_incoming_session(incoming_session, terminal_service).await {
                        log::error!("WebTransport connection error: {}", e);
                    } else {
                        log::debug!("WebTransport connection handled successfully");
                    }
                });
            }
        })
    }
}

// 处理WebTransport传入会话
async fn handle_incoming_session(
    incoming_session: IncomingSession,
    terminal_service: Arc<TerminalService>,
) -> anyhow::Result<()> {
    log::info!("Waiting for WebTransport session request...");
    
    // 等待会话请求
    let session_request = incoming_session.await?;
    
    // 从URL路径获取session_id
    let path = session_request.path();
    let session_id = path
        .split('/')
        .last()
        .ok_or(anyhow::anyhow!("Invalid WebTransport URL path"))?
        .to_string();
    
    log::info!("New WebTransport session: Authority: '{}', Path: '{}', Session ID: {}", 
              session_request.authority(), path, session_id);
    
    // 接受连接
    let connection = session_request.accept().await?;
    log::info!("WebTransport connection established for session {}", session_id);
    
    // 处理连接
    handle_connection(connection, terminal_service, session_id).await
}

// 处理WebTransport连接
async fn handle_connection(
    connection: wtransport::Connection,
    terminal_service: Arc<TerminalService>,
    session_id: String,
) -> anyhow::Result<()> {
    // 创建终端输出通道
    let (terminal_output_tx, terminal_output_rx) = mpsc::channel::<String>(100);
    
    // 添加客户端发送者到会话
    terminal_service.handle_terminal_connection(&session_id, terminal_output_tx.clone()).await?;
    
    // 启动两个任务：一个读取终端输出并发送到客户端，一个处理客户端输入
    tokio::select! {
        _ = handle_terminal_output(connection.clone(), terminal_output_rx) => {
            log::debug!("WebTransport terminal output task completed");
        }
        _ = handle_client_input(connection, terminal_service, session_id.clone()) => {
            log::debug!("WebTransport client input task completed");
        }
    }
    
    Ok(())
}

// 处理终端输出，发送到WebTransport客户端
async fn handle_terminal_output(
    connection: wtransport::Connection,
    mut terminal_output_rx: mpsc::Receiver<String>,
) -> anyhow::Result<()> {
    while let Some(output) = terminal_output_rx.recv().await {
        log::debug!("PTY -> WebTransport: {:?}", output);
        
        // 打开双向流发送数据
        let mut stream = connection.open_bi().await?.await?;
        stream.0.write_all(output.as_bytes()).await?;
    }
    
    Ok(())
}

// 处理客户端输入，写入到终端
async fn handle_client_input(
    connection: wtransport::Connection,
    terminal_service: Arc<TerminalService>,
    session_id: String,
) -> anyhow::Result<()> {
    let mut buffer = vec![0; 4096].into_boxed_slice();
    
    loop {
        // 接受双向流
        let mut stream = connection.accept_bi().await?;
        log::debug!("Accepted WebTransport bidirectional stream");
        
        // 读取客户端数据
        while let Some(bytes_read) = stream.1.read(&mut buffer).await? {
            if bytes_read == 0 {
                break;
            }
            
            let input = String::from_utf8_lossy(&buffer[..bytes_read]).to_string();
            log::debug!("WebTransport -> PTY: {:?} (session: {})", input, session_id);
            
            // 写入到终端
            terminal_service.handle_terminal_input(&session_id, input).await?;
        }
    }
}