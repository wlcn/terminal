use axum::{extract::{Path, Query, State}, http::StatusCode, routing::{get, post, delete}, Json, Router};
use tower_http::cors::{Any, CorsLayer};
use std::sync::Arc;
use tokio::sync::Mutex;
use serde::{Deserialize, Serialize};

use crate::session::SessionManager;

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
        .route("/api/sessions", post(create_session))
        .route("/api/sessions", get(get_all_sessions))
        .route("/api/sessions/:id", get(get_session_by_id))
        .route("/api/sessions/:id/resize", post(resize_terminal))
        .route("/api/sessions/:id/interrupt", post(interrupt_terminal))
        .route("/api/sessions/:id", delete(terminate_session))
        .route("/api/sessions/:id/status", get(get_session_status))
        .route("/api/sessions/:id/execute", post(execute_command))
        .route("/api/sessions/:id/execute-check", post(execute_command_check))
        .layer(cors)
        .with_state(session_manager);
    
    // 绑定地址并启动服务器
    let addr: std::net::SocketAddr = "127.0.0.1:8080".parse()?;
    log::info!("HTTP server started on http://{}", addr);
    
    let listener = tokio::net::TcpListener::bind(&addr).await?;
    axum::serve(listener, app)
        .await?;
    
    Ok(())
}

// 创建新会话
async fn create_session(
    Query(params): Query<CreateSessionParams>,
    State(session_manager): State<Arc<Mutex<SessionManager>>>,
) -> (StatusCode, Json<TerminalSession>) {
    // 检查userId是否提供
    let user_id = match params.user_id {
        Some(id) => id,
        None => panic!("Missing userId"),
    };
    
    let mut session_manager = session_manager.lock().await;
    
    // 生成会话ID
    match session_manager.create_session().await {
        Ok(session_id) => {
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
        },
        Err(e) => {
            log::error!("Failed to create session: {}", e);
            (StatusCode::INTERNAL_SERVER_ERROR, Json(TerminalSession {
                id: "".to_string(),
                userId: user_id,
                title: params.title,
                workingDirectory: params.working_directory.unwrap_or(".".to_string()),
                shellType: params.shell_type.unwrap_or("bash".to_string()),
                status: "ERROR".to_string(),
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
            }))
        }
    }
}

// 获取所有会话
async fn get_all_sessions(
    State(session_manager): State<Arc<Mutex<SessionManager>>>,
) -> (StatusCode, Json<Vec<TerminalSession>>) {
    let _session_manager = session_manager.lock().await;
    
    // 返回空列表，后续实现完整逻辑
    (StatusCode::OK, Json(Vec::<TerminalSession>::new()))
}

// 获取会话详情
async fn get_session_by_id(
    Path(id): Path<String>,
    State(_session_manager): State<Arc<Mutex<SessionManager>>>,
) -> (StatusCode, Json<TerminalSession>) {
    // 后续实现完整逻辑
    (StatusCode::OK, Json(TerminalSession {
        id: id,
        userId: "default_user".to_string(),
        title: None,
        workingDirectory: ".".to_string(),
        shellType: "bash".to_string(),
        status: "ACTIVE".to_string(),
        terminalSize: TerminalSize { columns: 80, rows: 24 },
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
    }))
}

// 调整终端大小
async fn resize_terminal(
    Path(id): Path<String>,
    Query(params): Query<ResizeParams>,
    State(session_manager): State<Arc<Mutex<SessionManager>>>,
) -> (StatusCode, Json<TerminalResizeResponse>) {
    // 检查cols和rows参数是否提供
    let cols = match params.cols {
        Some(cols) => cols,
        None => panic!("Missing or invalid columns"),
    };
    
    let rows = match params.rows {
        Some(rows) => rows,
        None => panic!("Missing or invalid rows"),
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
) -> (StatusCode, Json<TerminalInterruptResponse>) {
    let session_manager = session_manager.lock().await;
    
    // 检查会话是否存在
    if !session_manager.session_exists(&id) {
        panic!("Session not found");
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
) -> (StatusCode, Json<TerminalTerminateResponse>) {
    let mut session_manager = session_manager.lock().await;
    
    // 检查会话是否存在
    if !session_manager.session_exists(&id) {
        panic!("Session not found");
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
) -> (StatusCode, Json<TerminalStatusResponse>) {
    let session_manager = session_manager.lock().await;
    
    // 检查会话是否存在
    if !session_manager.session_exists(&id) {
        panic!("Session not found");
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
) -> (StatusCode, String) {
    // 检查command参数是否提供
    let command = match params.command {
        Some(cmd) => cmd,
        None => panic!("Missing command"),
    };
    
    let mut session_manager = session_manager.lock().await;
    
    // 检查会话是否存在
    if !session_manager.session_exists(&id) {
        panic!("Session not found");
    }
    
    // 执行命令
    if session_manager.write_to_session(&id, &format!("{}\n", command)).await.is_ok() {
        (StatusCode::OK, format!("Command executed: {}", command))
    } else {
        (StatusCode::INTERNAL_SERVER_ERROR, "Failed to execute command".to_string())
    }
}

// 执行命令并检查成功
async fn execute_command_check(
    Path(id): Path<String>,
    Query(params): Query<ExecuteParams>,
    State(session_manager): State<Arc<Mutex<SessionManager>>>,
) -> (StatusCode, Json<bool>) {
    // 检查command参数是否提供
    let command = match params.command {
        Some(cmd) => cmd,
        None => return (StatusCode::BAD_REQUEST, Json(false)),
    };
    
    let mut session_manager = session_manager.lock().await;
    
    // 检查会话是否存在
    if !session_manager.session_exists(&id) {
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