use axum::{extract::{Path, Query, State}, http::StatusCode, routing::{get, post, delete}, Json, Router};
use tower_http::cors::{Any, CorsLayer};
use std::sync::Arc;
use tokio::sync::Mutex;
use serde::{Deserialize, Serialize};

use crate::config::Config;

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
pub async fn start_server(session_manager: Arc<SessionManager>, config: Arc<Config>) -> anyhow::Result<()> {
    // 创建CORS配置
    let cors = CorsLayer::new()
        .allow_origin(Any)
        .allow_methods(Any)
        .allow_headers(Any);
    
    // 创建路由
    let app = Router::new()
        .route("/api/sessions", post(create_session))
        .route("/api/sessions", get(get_all_sessions))
        .route("/api/sessions/{id}", get(get_session_by_id))
        .route("/api/sessions/{id}/resize", post(resize_terminal))
        .route("/api/sessions/{id}/interrupt", post(interrupt_terminal))
        .route("/api/sessions/{id}", delete(terminate_session))
        .route("/api/sessions/{id}/status", get(get_session_status))
        .route("/api/sessions/{id}/execute", post(execute_command))
        .route("/api/sessions/{id}/execute-check", post(execute_command_check))
        .layer(cors)
        .with_state((session_manager, config));
    
    // 绑定地址并启动服务器
    let addr: std::net::SocketAddr = "127.0.0.1:8082".parse()?;
    log::info!("HTTP server started on http://{}", addr);
    
    let listener = tokio::net::TcpListener::bind(&addr).await?;
    axum::serve(listener, app)
        .await?;
    
    Ok(())
}

// 创建新会话
async fn create_session(
    Query(params): Query<CreateSessionParams>,
    State((session_manager, config)): State<(Arc<SessionManager>, Arc<Config>)>,
) -> (StatusCode, Json<TerminalSession>) {
    // 检查userId是否提供
    let user_id = match params.user_id {
        Some(id) => id,
        None => {
            // 返回400 Bad Request
            return (StatusCode::BAD_REQUEST, Json(TerminalSession {
                id: "".to_string(),
                userId: "".to_string(),
                title: params.title,
                workingDirectory: params.working_directory.unwrap_or(config.terminal.default_working_directory.clone()),
                shellType: params.shell_type.unwrap_or(config.terminal.default_shell_type.clone()),
                status: "ERROR".to_string(),
                terminalSize: TerminalSize {
                    columns: params.columns.unwrap_or(config.terminal.default_terminal_size.columns),
                    rows: params.rows.unwrap_or(config.terminal.default_terminal_size.rows),
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
                    .as_millis() as u64 + config.terminal.session_timeout,
            }));
        },
    };
    
    // 生成会话ID
    match session_manager.create_session().await {
        Ok(session_id) => {
            // 获取默认shell类型
            let default_shell_type = config.terminal.default_shell_type.clone();
            
            // 创建会话响应
            let session = TerminalSession {
                id: session_id.clone(),
                userId: user_id,
                title: params.title,
                workingDirectory: params.working_directory.unwrap_or(config.terminal.default_working_directory.clone()),
                shellType: params.shell_type.unwrap_or(default_shell_type),
                status: "ACTIVE".to_string(),
                terminalSize: TerminalSize {
                    columns: params.columns.unwrap_or(config.terminal.default_terminal_size.columns),
                    rows: params.rows.unwrap_or(config.terminal.default_terminal_size.rows),
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
                    .as_millis() as u64 + config.terminal.session_timeout,
            };
            
            (StatusCode::CREATED, Json(session))
        },
        Err(e) => {
            log::error!("Failed to create session: {}", e);
            // 获取默认shell类型
            let default_shell_type = config.terminal.default_shell_type.clone();
            
            (StatusCode::INTERNAL_SERVER_ERROR, Json(TerminalSession {
                id: "".to_string(),
                userId: user_id,
                title: params.title,
                workingDirectory: params.working_directory.unwrap_or(config.terminal.default_working_directory.clone()),
                shellType: params.shell_type.unwrap_or(default_shell_type),
                status: "ERROR".to_string(),
                terminalSize: TerminalSize {
                    columns: params.columns.unwrap_or(config.terminal.default_terminal_size.columns),
                    rows: params.rows.unwrap_or(config.terminal.default_terminal_size.rows),
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
                    .as_millis() as u64 + config.terminal.session_timeout,
            }))
        }
    }
}

// 获取所有会话
async fn get_all_sessions(
    State((session_manager, config)): State<(Arc<SessionManager>, Arc<Config>)>,
) -> (StatusCode, Json<Vec<TerminalSession>>) {
    // 获取所有会话ID
    let session_ids = session_manager.get_all_sessions().await;
    
    // 创建会话响应列表
    let sessions: Vec<TerminalSession> = session_ids.into_iter().map(|session_id| {
        TerminalSession {
            id: session_id.clone(),
            userId: "default_user".to_string(),
            title: None,
            workingDirectory: config.terminal.default_working_directory.clone(),
            shellType: config.terminal.default_shell_type.clone(),
            status: "ACTIVE".to_string(),
            terminalSize: TerminalSize {
                columns: config.terminal.default_terminal_size.columns,
                rows: config.terminal.default_terminal_size.rows
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
                .as_millis() as u64 + config.terminal.session_timeout,
        }
    }).collect();
    
    (StatusCode::OK, Json(sessions))
}

// 获取会话详情
async fn get_session_by_id(
    Path(id): Path<String>,
    State((_session_manager, config)): State<(Arc<SessionManager>, Arc<Config>)>,
) -> (StatusCode, Json<TerminalSession>) {
    // 后续实现完整逻辑
    (StatusCode::OK, Json(TerminalSession {
        id: id,
        userId: "default_user".to_string(),
        title: None,
        workingDirectory: config.terminal.default_working_directory.clone(),
        shellType: config.terminal.default_shell_type.clone(),
        status: "ACTIVE".to_string(),
        terminalSize: TerminalSize {
            columns: config.terminal.default_terminal_size.columns,
            rows: config.terminal.default_terminal_size.rows
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
            .as_millis() as u64 + config.terminal.session_timeout,
    }))
}

// 调整终端大小
async fn resize_terminal(
    Path(id): Path<String>,
    Query(params): Query<ResizeParams>,
    State((session_manager, _config)): State<(Arc<SessionManager>, Arc<Config>)>,
) -> (StatusCode, Json<TerminalResizeResponse>) {
    // 检查cols和rows参数是否提供
    let cols = match params.cols {
        Some(cols) => cols,
        None => {
            // 返回400 Bad Request
            return (StatusCode::BAD_REQUEST, Json(TerminalResizeResponse {
                sessionId: id,
                terminalSize: TerminalSize { columns: 0, rows: 0 },
                status: "ERROR".to_string(),
            }));
        },
    };
    
    let rows = match params.rows {
        Some(rows) => rows,
        None => {
            // 返回400 Bad Request
            return (StatusCode::BAD_REQUEST, Json(TerminalResizeResponse {
                sessionId: id,
                terminalSize: TerminalSize { columns: cols, rows: 0 },
                status: "ERROR".to_string(),
            }));
        },
    };
    
    // 调整终端大小
    match session_manager.resize_session(&id, cols, rows).await {
        Ok(_) => {
            (StatusCode::OK, Json(TerminalResizeResponse {
                sessionId: id,
                terminalSize: TerminalSize { columns: cols, rows: rows },
                status: "ACTIVE".to_string(),
            }))
        },
        Err(e) => {
            log::error!("Failed to resize session {}: {}", id, e);
            // 检查是否是因为会话不存在
            if e.to_string().contains("Session not found") {
                // 返回404 Not Found
                (StatusCode::NOT_FOUND, Json(TerminalResizeResponse {
                    sessionId: id,
                    terminalSize: TerminalSize { columns: cols, rows: rows },
                    status: "ERROR".to_string(),
                }))
            } else {
                // 返回500 Internal Server Error
                (StatusCode::INTERNAL_SERVER_ERROR, Json(TerminalResizeResponse {
                    sessionId: id,
                    terminalSize: TerminalSize { columns: cols, rows: rows },
                    status: "ERROR".to_string(),
                }))
            }
        }
    }
}

// 中断终端
async fn interrupt_terminal(
    Path(id): Path<String>,
    State((session_manager, _config)): State<(Arc<SessionManager>, Arc<Config>)>,
) -> (StatusCode, Json<TerminalInterruptResponse>) {
    // 检查会话是否存在
    if !session_manager.session_exists(&id).await {
        // 返回404 Not Found
        return (StatusCode::NOT_FOUND, Json(TerminalInterruptResponse {
            sessionId: id,
            status: "ERROR".to_string(),
        }));
    }
    
    // 发送中断信号（Ctrl+C）到终端
    // 在Windows上，我们可以发送"\x03"（Ctrl+C）
    // 在Unix系统上，我们可以发送SIGINT信号
    if session_manager.write_to_session(&id, "\x03").await.is_ok() {
        (StatusCode::OK, Json(TerminalInterruptResponse {
            sessionId: id,
            status: "interrupted".to_string(),
        }))
    } else {
        // 返回500 Internal Server Error
        (StatusCode::INTERNAL_SERVER_ERROR, Json(TerminalInterruptResponse {
            sessionId: id,
            status: "ERROR".to_string(),
        }))
    }
}

// 终止会话
async fn terminate_session(
    Path(id): Path<String>,
    State((session_manager, _config)): State<(Arc<SessionManager>, Arc<Config>)>,
) -> (StatusCode, Json<TerminalTerminateResponse>) {
    // 关闭会话
    match session_manager.close_session(&id).await {
        Ok(_) => {
            (StatusCode::OK, Json(TerminalTerminateResponse {
                sessionId: id,
                reason: "User terminated".to_string(),
                status: "TERMINATED".to_string(),
            }))
        },
        Err(e) => {
            log::error!("Failed to terminate session {}: {}", id, e);
            // 检查是否是因为会话不存在
            if e.to_string().contains("Session not found") {
                // 返回404 Not Found
                (StatusCode::NOT_FOUND, Json(TerminalTerminateResponse {
                    sessionId: id,
                    reason: e.to_string(),
                    status: "ERROR".to_string(),
                }))
            } else {
                // 返回500 Internal Server Error
                (StatusCode::INTERNAL_SERVER_ERROR, Json(TerminalTerminateResponse {
                    sessionId: id,
                    reason: e.to_string(),
                    status: "ERROR".to_string(),
                }))
            }
        }
    }
}

// 获取会话状态
async fn get_session_status(
    Path(id): Path<String>,
    State((session_manager, _config)): State<(Arc<SessionManager>, Arc<Config>)>,
) -> (StatusCode, Json<TerminalStatusResponse>) {
    // 检查会话是否存在
    if !session_manager.session_exists(&id).await {
        // 返回404 Not Found
        return (StatusCode::NOT_FOUND, Json(TerminalStatusResponse {
            status: "ERROR".to_string(),
        }));
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
    State((session_manager, _config)): State<(Arc<SessionManager>, Arc<Config>)>,
) -> (StatusCode, String) {
    // 检查command参数是否提供
    let command = match params.command {
        Some(cmd) => cmd,
        None => {
            // 返回400 Bad Request
            return (StatusCode::BAD_REQUEST, "Missing command".to_string());
        },
    };
    
    // 检查会话是否存在
    if !session_manager.session_exists(&id).await {
        // 返回404 Not Found
        return (StatusCode::NOT_FOUND, "Session not found".to_string());
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
    State((session_manager, _config)): State<(Arc<SessionManager>, Arc<Config>)>,
) -> (StatusCode, Json<bool>) {
    // 检查command参数是否提供
    let command = match params.command {
        Some(cmd) => cmd,
        None => return (StatusCode::BAD_REQUEST, Json(false)),
    };
    
    // 检查会话是否存在
    if !session_manager.session_exists(&id).await {
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