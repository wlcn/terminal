use std::sync::Arc;
use std::net::SocketAddr;
use std::future::Future;
use std::pin::Pin;

use tokio::net::{TcpListener, TcpStream};
use tokio_tungstenite::{accept_async, tungstenite::protocol::Message};
use futures_util::{stream::StreamExt, sink::SinkExt};
use tokio::sync::mpsc;

use crate::config::Config;
use crate::protocol_adapter::ProtocolAdapter;
use crate::terminal_service::TerminalService;

// WebSocket适配器 - 实现ProtocolAdapter接口
pub struct WebSocketAdapter {
    terminal_service: Arc<TerminalService>,
    config: Arc<Config>,
}

impl WebSocketAdapter {
    // 创建新的WebSocket适配器
    pub fn new(terminal_service: Arc<TerminalService>, config: Arc<Config>) -> Self {
        Self {
            terminal_service,
            config,
        }
    }
}

// 实现ProtocolAdapter接口
impl ProtocolAdapter for WebSocketAdapter {
    fn start(&self) -> Pin<Box<dyn Future<Output = anyhow::Result<()>> + Send + '_>> {
        let terminal_service = self.terminal_service.clone();
        let port = self.config.websocket.port;
        
        Box::pin(async move {
            let addr = SocketAddr::from(([127, 0, 0, 1], port));
            let listener = TcpListener::bind(addr).await?;
            
            log::info!("WebSocket server started on ws://localhost:{}", port);

            // 处理传入的连接 - 每个连接独立处理
            while let Ok((stream, _)) = listener.accept().await {
                let terminal_service = terminal_service.clone();
                
                // 为每个连接启动独立任务
                tokio::spawn(async move {
                    if let Err(e) = handle_connection(stream, terminal_service).await {
                        log::error!("WebSocket connection error: {}", e);
                    }
                });
            }
            
            Ok(())
        })
    }
}

// 处理WebSocket连接 - 简洁的中转设计
async fn handle_connection(
    stream: TcpStream,
    terminal_service: Arc<TerminalService>,
) -> anyhow::Result<()> {
    // 1. 接受WebSocket连接
    let ws_stream = match accept_async(stream).await {
        Ok(stream) => stream,
        Err(e) => {
            log::warn!("Failed to accept WebSocket connection: {}", e);
            return Ok(());
        }
    };
    
    log::info!("New WebSocket connection established");
    
    // 2. 创建终端会话
    let session_id = terminal_service.create_terminal_session().await?;
    log::info!("Created new session {} for WebSocket connection", session_id);
    
    // 创建终端输出通道
    let (terminal_output_tx, mut terminal_output_rx) = mpsc::channel::<String>(100);
    
    // 3. 处理终端连接
    terminal_service.handle_terminal_connection(&session_id, terminal_output_tx.clone()).await?;
    
    // 拆分WebSocket流为读写通道
    let (mut ws_write, mut ws_read) = ws_stream.split();
    
    // 任务1: WebSocket读 → PTY写
    let terminal_service_clone = terminal_service.clone();
    let session_id_clone = session_id.clone();
    
    let ws_read_task = tokio::spawn(async move {
        log::debug!("Started WebSocket read task (WebSocket → PTY)");
        
        // 监听WebSocket消息
        while let Some(msg_result) = ws_read.next().await {
            match msg_result {
                Ok(msg) => {
                    if msg.is_text() {
                        // 直接将WebSocket消息转发到终端
                        let text = match msg.to_text() {
                            Ok(text) => text,
                            Err(e) => {
                                log::error!("Failed to parse WebSocket text: {}", e);
                                continue;
                            }
                        };
                        
                        log::debug!("WebSocket → PTY: {:?} (session: {})", text, session_id_clone);
                        
                        // 写入到终端 - 异步操作，不阻塞
                        if let Err(e) = terminal_service_clone.handle_terminal_input(&session_id_clone, text.to_string()).await {
                            log::error!("Failed to write to terminal: {}", e);
                            break;
                        }
                    } else if msg.is_close() {
                        log::info!("WebSocket connection closing (read task)");
                        break;
                    }
                },
                Err(e) => {
                    log::error!("WebSocket read error: {}", e);
                    break;
                }
            }
        }
        
        log::debug!("WebSocket read task completed");
    });
    
    // 任务2: PTY读 → WebSocket写
    let session_id_clone = session_id.clone();
    
    let ws_write_task = tokio::spawn(async move {
        log::debug!("Started WebSocket write task (PTY → WebSocket)");
        
        loop {
            tokio::select! {
                // 监听终端输出
                Some(output) = terminal_output_rx.recv() => {
                    log::debug!("PTY → WebSocket: {:?} (session: {})", output, session_id_clone);
                    
                    // 发送到WebSocket - 异步操作，不阻塞
                    if let Err(e) = ws_write.send(Message::Text(output.into())).await {
                        log::error!("Failed to send terminal output to WebSocket: {}", e);
                        break;
                    }
                },
                // 所有通道关闭，退出
                else => {
                    log::debug!("All channels closed, exiting write task");
                    break;
                }
            }
        }
        
        log::debug!("WebSocket write task completed");
    });
    
    // 等待任一任务结束，然后清理
    tokio::select! {
        _ = ws_read_task => {
            log::info!("WebSocket read task finished");
        },
        _ = ws_write_task => {
            log::info!("WebSocket write task finished");
        }
    }
    
    Ok(())
}
