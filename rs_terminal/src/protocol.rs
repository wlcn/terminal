use std::sync::Arc;
use tokio::sync::Mutex;
use serde::{Deserialize, Serialize};

use crate::session::SessionManager;

// 协议消息类型
#[derive(Debug, Deserialize, Serialize)]
enum MessageType {
    CreateSession,
    WriteData,
    CloseSession,
    Ping,
}

// 客户端请求消息
#[derive(Debug, Deserialize)]
struct ClientMessage {
    r#type: MessageType,
    session_id: Option<String>,
    data: Option<String>,
}

// 服务器响应消息
#[derive(Debug, Serialize)]
struct ServerMessage {
    r#type: MessageType,
    session_id: Option<String>,
    data: Option<String>,
    error: Option<String>,
}

