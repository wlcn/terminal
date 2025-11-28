use std::sync::Arc;
use tokio::sync::Mutex;
use serde::{Deserialize, Serialize};

use crate::session::SessionManager;

// 协议消息类型
#[derive(Debug, Deserialize, Serialize)]
pub enum MessageType {
    CreateSession,
    WriteData,
    CloseSession,
    Ping,
}

// 客户端请求消息
#[derive(Debug, Deserialize)]
pub struct ClientMessage {
    pub r#type: MessageType,
    pub session_id: Option<String>,
    pub data: Option<String>,
}

// 服务器响应消息
#[derive(Debug, Serialize)]
pub struct ServerMessage {
    pub r#type: MessageType,
    pub session_id: Option<String>,
    pub data: Option<String>,
    pub error: Option<String>,
}

// 处理客户端消息
pub async fn handle_message(
    client_msg: ClientMessage,
    session_manager: Arc<Mutex<SessionManager>>,
) -> anyhow::Result<ServerMessage> {
    log::debug!("Received message: {:?}", client_msg);
    
    // 处理消息
    let response = match client_msg.r#type {
        MessageType::CreateSession => {
            let mut session_manager = session_manager.lock().await;
            let session_id = session_manager.create_session().await?;
            
            ServerMessage {
                r#type: MessageType::CreateSession,
                session_id: Some(session_id),
                data: None,
                error: None,
            }
        },
        MessageType::WriteData => {
            if let (Some(session_id), Some(data)) = (client_msg.session_id, client_msg.data) {
                let mut session_manager = session_manager.lock().await;
                session_manager.write_to_session(&session_id, &data).await?;
                
                ServerMessage {
                    r#type: MessageType::WriteData,
                    session_id: Some(session_id),
                    data: None,
                    error: None,
                }
            } else {
                ServerMessage {
                    r#type: MessageType::WriteData,
                    session_id: None,
                    data: None,
                    error: Some("Missing session_id or data".to_string()),
                }
            }
        },
        MessageType::CloseSession => {
            if let Some(session_id) = client_msg.session_id {
                let mut session_manager = session_manager.lock().await;
                session_manager.close_session(&session_id).await?;
                
                ServerMessage {
                    r#type: MessageType::CloseSession,
                    session_id: Some(session_id),
                    data: None,
                    error: None,
                }
            } else {
                ServerMessage {
                    r#type: MessageType::CloseSession,
                    session_id: None,
                    data: None,
                    error: Some("Missing session_id".to_string()),
                }
            }
        },
        MessageType::Ping => {
            ServerMessage {
                r#type: MessageType::Ping,
                session_id: None,
                data: Some("Pong".to_string()),
                error: None,
            }
        },
    };
    
    Ok(response)
}


