use axum::{extract::{Path, Query}, http::StatusCode, response::IntoResponse, routing::{get, post}, Json, Router};
use axum::middleware::from_fn;
use tower_http::cors::{Any, CorsLayer};
use std::sync::Arc;
use tokio::sync::Mutex;
use serde::{Deserialize, Serialize};

use crate::session::SessionManager;
use crate::terminal::TerminalProcess;

// 响应数据结构
#[derive(Serialize)]
pub struct TerminalResizeResponse {
    pub sessionId: String,
    pub terminalSize: TerminalSize,
    pub status: String,
}

#[derive(Serialize)]
pub struct TerminalInterruptResponse {
    pub sessionId: String,
    pub status: String,
}

#[derive(Serialize)]
pub struct TerminalTerminateResponse {
    pub sessionId: String,
    pub reason: String,
    pub status: String,
}

#[derive(Serialize)]
pub struct TerminalStatusResponse {
    pub status: String,
}

// 终端尺寸
#[derive(Serialize, Deserialize, Clone)]
pub struct TerminalSize {
    pub columns: u32,
    pub rows: u32,
}

// 终端会话
#[derive(Serialize, Deserialize)]
pub struct TerminalSession {
    pub id: String,
    pub userId: String,
    pub title: Option<String>,
    pub workingDirectory: String,
    pub shellType: String,
    pub status: String,
    pub terminalSize: TerminalSize,
    pub createdAt: u64,
    pub updatedAt: u64,
    pub lastActiveTime: u64,
    pub expiredAt: u64,
}

// 启动HTTP服务器
pub async fn start_server(session_manager: Arc<Mutex<SessionManager>>) -> anyhow::Result<()> {
    // 创建CORS配置
    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);
    
    // 创建路由
    let app = Router::new()
        .route("/sessions", post(create_session))
        .route("/sessions", get(get_all_sessions))
        .route("/sessions/:id", get(get_session_by_id))
        .route("/sessions/:id/resize", post(resize_terminal))
        .route("/sessions/:id/interrupt", post(interrupt_terminal))
        .route("/sessions/:id", delete(terminate_session))
        .route("/sessions/:id/status", get(get_session_status))
        .route("/sessions/:id/execute", post(execute_command))
        .route("/sessions/:id/execute-check", post(execute_command_check))
        .layer(cors)
        .with_state(session_manager);
    
    // 绑定地址并启动服务器
    let addr = "127.0.0.1:8080".parse()?;
    log::info!("HTTP server started on http://{}", addr);
    
    axum::Server::bind(&addr)
        .serve(app.into_make_service())
        .await?;
    
    Ok(())
}

// 创建新会话
async fn create_session(
    Query(params): Query<CreateSessionParams>,
    State(session_manager): State<Arc<Mutex<SessionManager>>>,
) -> impl IntoResponse {
    // 检查userId是否提供
    let user_id = match params.user_id {
        Some(id) => id,
        None => return (StatusCode::BAD_REQUEST, "Missing userId"),
    };
    
    let mut session_manager = session_manager.lock().await;
    
    // 生成会话ID
    let session_id = session_manager.create_session().await.unwrap();
    
    // 创建会话响应
    let session = TerminalSession {
        id: session_id.clone(),
        userId: user_id,
        title: params.title,
        workingDirectory: params.working_directory.unwrap_or(".".to_string()),
        shellType: params.shell_type.unwrap_or("bash".to_string()),
        status: "ACTIVE".to_string(),
        terminalSize: TerminalSize {
            columns: params.columns.unwrap_or(80),
            rows: params.rows.unwrap_or(24),
        },
        createdAt: std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64,
        updatedAt: std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64,
        lastActiveTime: std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64,
        expiredAt: std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64 + 3600 * 1000,
    };
    
    (StatusCode::CREATED, Json(session))
}

// 获取所有会话
async fn get_all_sessions(
    State(session_manager): State<Arc<Mutex<SessionManager>>>,
) -> impl IntoResponse {
    let session_manager = session_manager.lock().await;
    
    // 返回空列表，后续实现完整逻辑
    (StatusCode::OK, Json(Vec::<TerminalSession>::new()))
}

// 获取会话详情
async fn get_session_by_id(
    Path(id): Path<String>,
    State(session_manager): State<Arc<Mutex<SessionManager>>>,
) -> impl IntoResponse {
    // 后续实现完整逻辑
    (StatusCode::OK, Json(TerminalSession {
        id: id,
        user_id: "default_user".to_string(),
        title: None,
        working_directory: ".".to_string(),
        shell_type: "bash".to_string(),
        status: "ACTIVE".to_string(),
        terminal_size: TerminalSize { columns: 80, rows: 24 },
        created_at: std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64,
        updated_at: std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64,
        last_active_time: std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64,
        expired_at: std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64 + 3600 * 1000,
    }))
}

// 调整终端大小
async fn resize_terminal(
    Path(id): Path<String>,
    Query(params): Query<ResizeParams>,
    State(session_manager): State<Arc<Mutex<SessionManager>>>,
) -> impl IntoResponse {
    // 检查cols和rows参数是否提供
    let cols = match params.cols {
        Some(cols) => cols,
        None => return (StatusCode::BAD_REQUEST, "Missing or invalid columns"),
    };
    
    let rows = match params.rows {
        Some(rows) => rows,
        None => return (StatusCode::BAD_REQUEST, "Missing or invalid rows"),
    };
    
    let mut session_manager = session_manager.lock().await;
    
    // 后续实现完整逻辑
    (StatusCode::OK, Json(TerminalResizeResponse {
        sessionId: id,
        terminalSize: TerminalSize { columns: cols, rows: rows },
        status: "ACTIVE".to_string(),
    }))
}

// 中断终端
async fn interrupt_terminal(
    Path(id): Path<String>,
    State(session_manager): State<Arc<Mutex<SessionManager>>>,
) -> impl IntoResponse {
    let session_manager = session_manager.lock().await;
    
    // 检查会话是否存在
    if !session_manager.session_exists(&id) {
        return (StatusCode::NOT_FOUND, "Session not found");
    }
    
    // 后续实现完整逻辑
    (StatusCode::OK, Json(TerminalInterruptResponse {
        sessionId: id,
        status: "interrupted".to_string(),
    }))
}

// 终止会话
async fn terminate_session(
    Path(id): Path<String>,
    State(session_manager): State<Arc<Mutex<SessionManager>>>,
) -> impl IntoResponse {
    let mut session_manager = session_manager.lock().await;
    
    // 检查会话是否存在
    if session_manager.get_session(&id).is_none() {
        return (StatusCode::NOT_FOUND, "Session not found");
    }
    
    session_manager.close_session(&id).await.unwrap();
    
    (StatusCode::OK, Json(TerminalTerminateResponse {
        sessionId: id,
        reason: "User terminated".to_string(),
        status: "TERMINATED".to_string(),
    }))
}

// 获取会话状态
async fn get_session_status(
    Path(id): Path<String>,
    State(session_manager): State<Arc<Mutex<SessionManager>>>,
) -> impl IntoResponse {
    let session_manager = session_manager.lock().await;
    
    // 检查会话是否存在
    if session_manager.get_session(&id).is_none() {
        return (StatusCode::NOT_FOUND, "Session not found");
    }
    
    // 后续实现完整逻辑
    (StatusCode::OK, Json(TerminalStatusResponse {
        status: "ACTIVE".to_string(),
    }))
}

// 执行命令
async fn execute_command(
    Path(id): Path<String>,
    Query(params): Query<ExecuteParams>,
    State(session_manager): State<Arc<Mutex<SessionManager>>>,
) -> impl IntoResponse {
    // 检查command参数是否提供
    let command = match params.command {
        Some(cmd) => cmd,
        None => return (StatusCode::BAD_REQUEST, "Missing command"),
    };
    
    let mut session_manager = session_manager.lock().await;
    
    // 检查会话是否存在
    if session_manager.get_session(&id).is_none() {
        return (StatusCode::NOT_FOUND, "Session not found");
    }
    
    // 执行命令
    if session_manager.write_to_session(&id, &format!("{}\n", command)).await.is_ok() {
        (StatusCode::OK, format!("Command executed: {}", command))
    } else {
        (StatusCode::INTERNAL_SERVER_ERROR, "Failed to execute command")
    }
}

// 执行命令并检查成功
async fn execute_command_check(
    Path(id): Path<String>,
    Query(params): Query<ExecuteParams>,
    State(session_manager): State<Arc<Mutex<SessionManager>>>,
) -> impl IntoResponse {
    // 检查command参数是否提供
    let command = match params.command {
        Some(cmd) => cmd,
        None => return (StatusCode::BAD_REQUEST, Json(false)),
    };
    
    let mut session_manager = session_manager.lock().await;
    
    // 检查会话是否存在
    if session_manager.get_session(&id).is_none() {
        return (StatusCode::NOT_FOUND, Json(false));
    }
    
    // 执行命令
    let success = session_manager.write_to_session(&id, &format!("{}\n", command)).await.is_ok();
    (StatusCode::OK, Json(success))
}

// 请求参数
#[derive(Deserialize)]
struct CreateSessionParams {
    #[serde(rename = "userId")]
    user_id: Option<String>,
    title: Option<String>,
    #[serde(rename = "workingDirectory")]
    working_directory: Option<String>,
    #[serde(rename = "shellType")]
    shell_type: Option<String>,
    columns: Option<u32>,
    rows: Option<u32>,
}

#[derive(Deserialize)]
struct ResizeParams {
    cols: Option<u32>,
    rows: Option<u32>,
}

#[derive(Deserialize)]
struct ExecuteParams {
    command: Option<String>,
    #[serde(rename = "timeoutMs")]
    timeout_ms: Option<u64>,
}

// 状态提取器
#[derive(Clone)]
struct State<T>(T);

impl<T> axum::extract::FromRef<T> for State<T> {
    fn from_ref(input: &T) -> Self {
        State(input.clone())
    }
}
