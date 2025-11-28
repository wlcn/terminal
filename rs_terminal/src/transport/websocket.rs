use std::net::SocketAddr;
use std::sync::Arc;
use tokio::sync::Mutex;
use tokio::net::{TcpListener, TcpStream};
use tokio_tungstenite::{accept_async, tungstenite::protocol::Message};
use futures_util::{stream::StreamExt, sink::SinkExt};

use crate::config::Config;
use crate::session::SessionManager;
// 移除未使用的导入

// 启动WebSocket服务器
    pub async fn start_server(session_manager: Arc<Mutex<SessionManager>>, _config: Arc<Config>) -> anyhow::Result<()> {
        let addr = SocketAddr::from(([127, 0, 0, 1], 8081));
        let listener = TcpListener::bind(addr).await?;
        
        log::info!("WebSocket server started on ws://localhost:8081");
    
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
    // 接受WebSocket连接，添加错误处理
    let ws_stream = match accept_async(stream).await {
        Ok(stream) => stream,
        Err(e) => {
            log::warn!("Failed to accept WebSocket connection: {}", e);
            // 忽略非WebSocket请求，直接返回
            return Ok(());
        }
    };
    
    log::info!("New WebSocket connection established");
    
    // 创建客户端发送通道 - 明确使用String类型
    let (tx, mut rx) = tokio::sync::mpsc::channel::<String>(100);
    let mut current_session_id: Option<String> = None;
    
    // 使用Arc和Mutex共享WebSocket流
    let ws_stream = Arc::new(Mutex::new(ws_stream));
    
    // 启动发送任务，将终端输出发送到客户端
    let ws_stream_clone = ws_stream.clone();
    let _send_task = tokio::spawn(async move {
        while let Some(output) = rx.recv().await {
            let mut ws_stream = ws_stream_clone.lock().await;
            // 将String转换为Utf8Bytes
            if let Err(e) = ws_stream.send(Message::Text(output.into())).await {
                log::error!("Error sending message to WebSocket client: {}", e);
                break;
            }
        }
    });
    
    // 创建一个新会话用于此WebSocket连接
    let mut session_manager_lock = session_manager.lock().await;
    let session_id = session_manager_lock.create_session().await?;
    current_session_id = Some(session_id.clone());
    session_manager_lock.add_client_sender(&session_id, tx.clone());
    drop(session_manager_lock);
    
    log::info!("Created new session {} for WebSocket connection", session_id);
    
    // 处理传入的消息
    let mut ws_stream = ws_stream.lock().await;
    while let Some(msg) = ws_stream.next().await {
        let msg = msg?;
        
        if msg.is_text() {
            // 获取纯文本消息
            let text = msg.to_text()?;
            
            // 添加日志记录接收到的原始消息
            log::debug!("Received raw WebSocket message: {:?} for session {}", text, session_id);
            
            // 直接将文本写入终端会话
            let mut session_manager_lock = session_manager.lock().await;
            if let Err(e) = session_manager_lock.write_to_session(&session_id, text).await {
                log::error!("Failed to write to session {}: {}", session_id, e);
                // 发送错误信息给客户端
                let error_msg = format!("Error: Failed to write to terminal: {}", e);
                if let Err(ee) = ws_stream.send(Message::Text(error_msg.into())).await {
                    log::error!("Error sending error message: {}", ee);
                    break;
                }
            }
            drop(session_manager_lock);
        } else if msg.is_binary() {
            // 处理二进制消息（如果需要）
            log::debug!("Received binary message for session {}", session_id);
        } else if msg.is_close() {
            // 处理连接关闭
            log::info!("WebSocket connection closing for session {}", session_id);
            break;
        } else if msg.is_ping() {
            // 处理ping消息
            if let Err(e) = ws_stream.send(Message::Pong(Vec::new().into())).await {
                log::error!("Error sending pong: {}", e);
                break;
            }
        }
    }
    
    // 清理资源
    if let Some(session_id) = current_session_id {
        let mut session_manager_lock = session_manager.lock().await;
        session_manager_lock.remove_client_sender(&session_id, &tx);
        if let Err(e) = session_manager_lock.close_session(&session_id).await {
            log::error!("Failed to close session {}: {}", session_id, e);
        } else {
            log::info!("Closed session {} for WebSocket connection", session_id);
        }
    }
    
    log::info!("WebSocket connection closed");
    
    Ok(())
}
