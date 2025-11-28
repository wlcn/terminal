use std::net::SocketAddr;
use std::sync::Arc;
use tokio::sync::Mutex;
use tokio::net::{TcpListener, TcpStream};
use tokio_tungstenite::{accept_async, tungstenite::protocol::Message};
use futures_util::{stream::StreamExt, sink::SinkExt};

use crate::session::SessionManager;
use crate::protocol::{ClientMessage, MessageType};

// 启动WebSocket服务器
    pub async fn start_server(session_manager: Arc<Mutex<SessionManager>>) -> anyhow::Result<()> {
        let addr = SocketAddr::from(([127, 0, 0, 1], 8080));
        let listener = TcpListener::bind(addr).await?;
        
        log::info!("WebSocket server started on ws://localhost:8080");
    
    // 处理传入的连接
    while let Ok((stream, _)) = listener.accept().await {
        let session_manager = session_manager.clone();
        
        tokio::spawn(async move {
            if let Err(e) = handle_connection(stream, session_manager).await {
                log::error!("WebSocket connection error: {}", e);
            }
        });
    }
    
    Ok(())
}

// 处理WebSocket连接
async fn handle_connection(
    stream: TcpStream,
    session_manager: Arc<Mutex<SessionManager>>,
) -> anyhow::Result<()> {
    // 接受WebSocket连接
    let ws_stream = accept_async(stream).await?;
    
    log::info!("New WebSocket connection established");
    
    // 创建客户端发送通道
    let (tx, mut rx) = tokio::sync::mpsc::channel(100);
    let mut current_session_id: Option<String> = None;
    
    // 使用Arc和Mutex共享WebSocket流
    let ws_stream = Arc::new(Mutex::new(ws_stream));
    
    // 启动发送任务，将终端输出发送到客户端
    let ws_stream_clone = ws_stream.clone();
    let _send_task = tokio::spawn(async move {
        while let Some(output) = rx.recv().await {
            let mut ws_stream = ws_stream_clone.lock().await;
            if let Err(e) = ws_stream.send(Message::Text(output)).await {
                log::error!("Error sending message to WebSocket client: {}", e);
                break;
            }
        }
    });
    
    // 处理传入的消息
    let mut ws_stream = ws_stream.lock().await;
    while let Some(msg) = ws_stream.next().await {
        let msg = msg?;
        
        if msg.is_text() {
            // 解析客户端消息
            let text = msg.to_text()?;
            let client_msg: ClientMessage = serde_json::from_str(text)?;
            
            log::debug!("Received WebSocket message: {:?}", client_msg);
            
            // 处理消息
            let response = crate::protocol::handle_message(client_msg.clone(), session_manager.clone()).await?;
            
            // 发送响应
            let response_json = serde_json::to_string(&response)?;
            if let Err(e) = ws_stream.send(Message::Text(response_json)).await {
                log::error!("Error sending response to WebSocket client: {}", e);
                break;
            }
            
            // 如果是创建会话消息，保存会话ID并添加客户端发送通道
            if client_msg.r#type == MessageType::CreateSession {
                if let Some(session_id) = response.session_id {
                    current_session_id = Some(session_id.clone());
                    
                    // 添加客户端发送通道到会话
                    let mut session_manager = session_manager.lock().await;
                    session_manager.add_client_sender(&session_id, tx.clone());
                }
            }
        }
    }
    
    // 清理资源
    if let Some(session_id) = current_session_id {
        let mut session_manager = session_manager.lock().await;
        session_manager.remove_client_sender(&session_id, &tx);
    }
    
    log::info!("WebSocket connection closed");
    
    Ok(())
}
