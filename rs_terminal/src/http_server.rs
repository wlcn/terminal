use axum::{extract::{Path, Query, State}, http::StatusCode, routing::{get, post, delete}, Json, Router};
use tower_http::cors::{Any, CorsLayer};
use std::sync::Arc;
use serde::{Deserialize, Serialize};

use crate::config::Config;
use crate::session::SessionManager;

// 响应数据结构
#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TerminalResizeResponse {
    pub session_id: String,
    pub terminal_size: TerminalSize,
    pub status: String,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TerminalInterruptResponse {
    pub session_id: String,
    pub status: String,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TerminalTerminateResponse {
    pub session_id: String,
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
#[serde(rename_all = "camelCase")]
pub struct TerminalSession {
    pub id: String,
    pub user_id: String,
    pub title: Option<String>,
    pub working_directory: String,
    pub shell_type: String,
    pub status: String,
    pub terminal_size: TerminalSize,
    pub created_at: u64,
    pub updated_at: u64,
    pub last_active_time: u64,
    pub expired_at: u64,
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

// 辅助函数：创建默认的TerminalSession对象
fn create_default_terminal_session(
    id: String,
    user_id: String,
    title: Option<String>,
    working_directory: String,
    shell_type: String,
    status: String,
    columns: u32,
    rows: u32,
    config: &Config,
) -> TerminalSession {
    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_millis() as u64;
    
    TerminalSession {
        id,
        user_id,
        title,
        working_directory,
        shell_type,
        status,
        terminal_size: TerminalSize { columns, rows },
        created_at: now,
        updated_at: now,
        last_active_time: now,
        expired_at: now + config.terminal.session_timeout,
    }
}

// 启动HTTP服务器
pub async fn start_server(session_manager: Arc<SessionManager>, config: Arc<Config>) -> anyhow::Result<()> {
    // 保存端口值，因为config会被移动到app状态中
    let port = config.http.port;
    
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
    let addr: std::net::SocketAddr = format!("127.0.0.1:{}", port).parse()?;
    
    // 暂时只支持HTTP，HTTPS支持需要安装cmake和nasm依赖
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
            let session = create_default_terminal_session(
                "".to_string(),
                "".to_string(),
                params.title,
                params.working_directory.unwrap_or(config.terminal.default_working_directory.clone()),
                params.shell_type.unwrap_or(config.terminal.default_shell_type.clone()),
                "ERROR".to_string(),
                params.columns.unwrap_or(config.terminal.default_terminal_size.columns),
                params.rows.unwrap_or(config.terminal.default_terminal_size.rows),
                &config,
            );
            return (StatusCode::BAD_REQUEST, Json(session));
        },
    };
    
    // 生成会话ID
    match session_manager.create_session().await {
        Ok(session_id) => {
            // 创建会话响应
            let session = create_default_terminal_session(
                session_id.clone(),
                user_id.clone(),
                params.title,
                params.working_directory.unwrap_or(config.terminal.default_working_directory.clone()),
                params.shell_type.unwrap_or(config.terminal.default_shell_type.clone()),
                "ACTIVE".to_string(),
                params.columns.unwrap_or(config.terminal.default_terminal_size.columns),
                params.rows.unwrap_or(config.terminal.default_terminal_size.rows),
                &config,
            );
            
            (StatusCode::CREATED, Json(session))
        },
        Err(e) => {
            log::error!("Failed to create session: {}", e);
            
            // 创建错误响应
            let session = create_default_terminal_session(
                "".to_string(),
                user_id.clone(),
                params.title,
                params.working_directory.unwrap_or(config.terminal.default_working_directory.clone()),
                params.shell_type.unwrap_or(config.terminal.default_shell_type.clone()),
                "ERROR".to_string(),
                params.columns.unwrap_or(config.terminal.default_terminal_size.columns),
                params.rows.unwrap_or(config.terminal.default_terminal_size.rows),
                &config,
            );
            
            (StatusCode::INTERNAL_SERVER_ERROR, Json(session))
        }
    }
}

// 获取所有会话
async fn get_all_sessions(
    State((session_manager, config)): State<(Arc<SessionManager>, Arc<Config>)>,
) -> (StatusCode, Json<Vec<TerminalSession>>) {
    // 获取所有会话及其状态
    let sessions_with_status = session_manager.get_all_sessions_with_status().await;
    
    // 创建会话响应列表
    let sessions: Vec<TerminalSession> = sessions_with_status.into_iter().map(|(session_id, status)| {
        // 转换状态为字符串
        let status_str = match status {
            crate::session::SessionStatus::Active => "ACTIVE".to_string(),
            crate::session::SessionStatus::Terminated => "TERMINATED".to_string(),
        };
        
        create_default_terminal_session(
            session_id.clone(),
            "default_user".to_string(),
            None,
            config.terminal.default_working_directory.clone(),
            config.terminal.default_shell_type.clone(),
            status_str,
            config.terminal.default_terminal_size.columns,
            config.terminal.default_terminal_size.rows,
            &config,
        )
    }).collect();
    
    (StatusCode::OK, Json(sessions))
}

// 获取会话详情
async fn get_session_by_id(
    Path(id): Path<String>,
    State((session_manager, config)): State<(Arc<SessionManager>, Arc<Config>)>,
) -> (StatusCode, Json<TerminalSession>) {
    // 获取会话状态
    let status = match session_manager.get_session_status(&id).await {
        Ok(status) => status,
        Err(_) => {
            // 返回404 Not Found
            let session = create_default_terminal_session(
                id,
                "".to_string(),
                None,
                config.terminal.default_working_directory.clone(),
                config.terminal.default_shell_type.clone(),
                "ERROR".to_string(),
                config.terminal.default_terminal_size.columns,
                config.terminal.default_terminal_size.rows,
                &config,
            );
            return (StatusCode::NOT_FOUND, Json(session));
        },
    };
    
    // 转换状态为字符串
    let status_str = match status {
        crate::session::SessionStatus::Active => "ACTIVE".to_string(),
        crate::session::SessionStatus::Terminated => "TERMINATED".to_string(),
    };
    
    // 返回会话详情
    let session = create_default_terminal_session(
        id,
        "default_user".to_string(),
        None,
        config.terminal.default_working_directory.clone(),
        config.terminal.default_shell_type.clone(),
        status_str,
        config.terminal.default_terminal_size.columns,
        config.terminal.default_terminal_size.rows,
        &config,
    );
    
    (StatusCode::OK, Json(session))
}

// 调整终端大小
async fn resize_terminal(
    Path(id): Path<String>,
    Query(params): Query<ResizeParams>,
    State((session_manager, config)): State<(Arc<SessionManager>, Arc<Config>)>,
) -> (StatusCode, Json<TerminalResizeResponse>) {
    // 检查cols和rows参数是否提供
    let cols = match params.cols {
        Some(cols) => cols,
        None => {
            // 返回400 Bad Request
            return (StatusCode::BAD_REQUEST, Json(TerminalResizeResponse {
                session_id: id,
                terminal_size: TerminalSize { columns: 0, rows: 0 },
                status: "ERROR".to_string(),
            }));
        },
    };
    
    let rows = match params.rows {
        Some(rows) => rows,
        None => {
            // 返回400 Bad Request
            return (StatusCode::BAD_REQUEST, Json(TerminalResizeResponse {
                session_id: id,
                terminal_size: TerminalSize { columns: cols, rows: 0 },
                status: "ERROR".to_string(),
            }));
        },
    };
    
    // 调整终端大小
    match session_manager.resize_session(&id, cols, rows).await {
        Ok(_) => {
            (StatusCode::OK, Json(TerminalResizeResponse {
                session_id: id,
                terminal_size: TerminalSize { columns: cols, rows: rows },
                status: "ACTIVE".to_string(),
            }))
        },
        Err(e) => {
            log::error!("Failed to resize session {}: {}", id, e);
            // 检查是否是因为会话不存在
            if e.to_string().contains("Session not found") {
                // 返回404 Not Found
                (StatusCode::NOT_FOUND, Json(TerminalResizeResponse {
                    session_id: id,
                    terminal_size: TerminalSize { columns: cols, rows: rows },
                    status: "ERROR".to_string(),
                }))
            } else {
                // 返回500 Internal Server Error
                (StatusCode::INTERNAL_SERVER_ERROR, Json(TerminalResizeResponse {
                    session_id: id,
                    terminal_size: TerminalSize { columns: cols, rows: rows },
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
            session_id: id,
            status: "ERROR".to_string(),
        }));
    }
    
    // 发送中断信号（Ctrl+C）到终端
    match session_manager.write_to_session(&id, "\x03").await {
        Ok(_) => {
            (StatusCode::OK, Json(TerminalInterruptResponse {
                session_id: id,
                status: "interrupted".to_string(),
            }))
        },
        Err(e) => {
            log::error!("Failed to interrupt session {}: {}", id, e);
            // 返回500 Internal Server Error
            (StatusCode::INTERNAL_SERVER_ERROR, Json(TerminalInterruptResponse {
                session_id: id,
                status: "ERROR".to_string(),
            }))
        }
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
                session_id: id,
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
                    session_id: id,
                    reason: e.to_string(),
                    status: "ERROR".to_string(),
                }))
            } else {
                // 返回500 Internal Server Error
                (StatusCode::INTERNAL_SERVER_ERROR, Json(TerminalTerminateResponse {
                    session_id: id,
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
    // 获取会话状态
    match session_manager.get_session_status(&id).await {
        Ok(status) => {
            // 转换状态为字符串
            let status_str = match status {
                crate::session::SessionStatus::Active => "ACTIVE".to_string(),
                crate::session::SessionStatus::Terminated => "TERMINATED".to_string(),
            };
            
            (StatusCode::OK, Json(TerminalStatusResponse {
                status: status_str,
            }))
        },
        Err(_) => {
            // 返回404 Not Found
            (StatusCode::NOT_FOUND, Json(TerminalStatusResponse {
                status: "ERROR".to_string(),
            }))
        },
    }
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
    match session_manager.write_to_session(&id, &format!("{}\n", command)).await {
        Ok(_) => {
            (StatusCode::OK, format!("Command executed: {}", command))
        },
        Err(e) => {
            log::error!("Failed to execute command on session {}: {}", id, e);
            (StatusCode::INTERNAL_SERVER_ERROR, "Failed to execute command".to_string())
        }
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
