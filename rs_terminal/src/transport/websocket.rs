use std::net::SocketAddr;
use std::sync::Arc;
use tokio::net::{TcpListener, TcpStream};
use tokio_tungstenite::{accept_async, tungstenite::protocol::Message};
use futures_util::{stream::StreamExt, sink::SinkExt};

use crate::config::Config;
use crate::session::SessionManager;

// 启动WebSocket服务器 - 简洁异步设计
pub async fn start_server(session_manager: Arc<SessionManager>, _config: Arc<Config>) -> anyhow::Result<()> {
    let addr = SocketAddr::from(([127, 0, 0, 1], 8081));
    let listener = TcpListener::bind(addr).await?;
    
    log::info!("WebSocket server started on ws://localhost:8081");

    // 处理传入的连接 - 每个连接独立处理
    while let Ok((stream, _)) = listener.accept().await {
        let session_manager = session_manager.clone();
        
        // 为每个连接启动独立任务
        tokio::spawn(async move {
            if let Err(e) = handle_connection(stream, session_manager).await {
                log::error!("WebSocket connection error: {}", e);
            }
        });
    }
    
    Ok(())
}

// 处理WebSocket连接 - 简洁的中转设计
async fn handle_connection(
    stream: TcpStream,
    session_manager: Arc<SessionManager>,
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
    let session_id = session_manager.create_session().await?;
    log::info!("Created new session {} for WebSocket connection", session_id);
    
    // 创建终端输出通道
    let (terminal_output_tx, mut terminal_output_rx) = tokio::sync::mpsc::channel::<String>(100);
    
    // 添加客户端发送者到会话
    session_manager.add_client_sender(&session_id, terminal_output_tx.clone()).await;
    
    // 拆分WebSocket流为读写通道
    let (mut ws_write, mut ws_read) = ws_stream.split();
    
    // 创建ping-pong通道
    let (pong_tx, mut pong_rx) = tokio::sync::mpsc::channel::<()>(10);
    
    // 任务1: WebSocket读 → PTY写
    let session_manager_clone = session_manager.clone();
    let session_id_clone = session_id.clone();
    let pong_tx_clone = pong_tx.clone();
    
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
                        log::debug!("About to write to terminal: {:?}", text);
                        match session_manager_clone.write_to_session(&session_id_clone, text).await {
                            Ok(_) => {
                                log::debug!("Successfully wrote to terminal: {:?}", text);
                            },
                            Err(e) => {
                                log::error!("Failed to write to terminal: {}", e);
                                break;
                            }
                        }
                    } else if msg.is_ping() {
                        // 发送ping到pong处理任务
                        if pong_tx_clone.send(()).await.is_err() {
                            log::error!("Failed to send ping to pong task");
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
    
    // 任务2: PTY读 + ping-pong → WebSocket写
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
                // 处理ping-pong
                Some(_) = pong_rx.recv() => {
                    log::debug!("Received ping, sending pong");
                    if let Err(e) = ws_write.send(Message::Pong(Vec::new().into())).await {
                        log::error!("Failed to send pong: {}", e);
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
    
    // 清理资源
    if let Err(e) = session_manager.close_session(&session_id).await {
        log::error!("Failed to close session {}: {}", session_id, e);
    } else {
        log::info!("Closed session {} for WebSocket connection", session_id);
    }
    
    log::info!("WebSocket connection fully closed");
    
    Ok(())
}