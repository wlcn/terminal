use std::net::SocketAddr;
use std::sync::Arc;
use tokio::sync::Mutex;
use tokio::net::{TcpListener, TcpStream};
use tokio_tungstenite::{accept_async, tungstenite::protocol::Message};
use futures_util::{stream::StreamExt, sink::SinkExt};

use crate::session::SessionManager;
use crate::protocol::ClientMessage;

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
    let mut ws_stream = accept_async(stream).await?;
    
    log::info!("New WebSocket connection established");
    
    // 处理传入的消息
    while let Some(msg) = ws_stream.next().await {
        let msg = msg?;
        
        if msg.is_text() {
            // 解析客户端消息
            let text = msg.to_text()?;
            let client_msg: ClientMessage = serde_json::from_str(text)?;
            
            log::debug!("Received WebSocket message: {:?}", client_msg);
            
            // 处理消息
            let response = crate::protocol::handle_message(client_msg, session_manager.clone()).await?;
            
            // 发送响应
            let response_json = serde_json::to_string(&response)?;
            ws_stream.send(Message::Text(response_json)).await?;
        }
    }
    
    log::info!("WebSocket connection closed");
    
    Ok(())
}
