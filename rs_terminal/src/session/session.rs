use std::collections::HashMap;
use std::sync::{Arc, Mutex, RwLock};
use uuid::Uuid;

use crate::config::Config;
use crate::pty::terminal::TerminalProcess;

// 会话状态枚举
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub(crate) enum SessionStatus {
    Active,
    Terminated,
}

// 终端会话
#[derive(Clone)]
pub(crate) struct Session {
    terminal: TerminalProcess,
    // 客户端发送通道 - 线程安全的发送者列表
    client_senders: Arc<Mutex<Vec<tokio::sync::mpsc::Sender<String>>>>,
    // 会话状态 - 使用AtomicU8确保原子更新
    status: Arc<std::sync::atomic::AtomicU8>,
    // 会话过期时间
    expired_at: u64,
    // 最后活动时间
    last_active_time: Arc<std::sync::atomic::AtomicU64>,
    // 是否已启动终端输出监听任务
    listener_started: Arc<std::sync::atomic::AtomicBool>,
}

impl Session {
    // 创建新会话
    pub(crate) fn new(terminal: TerminalProcess, session_timeout: u64) -> Self {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64;
        
        Self {
            terminal,
            client_senders: Arc::new(Mutex::new(Vec::new())),
            status: Arc::new(std::sync::atomic::AtomicU8::new(SessionStatus::Active as u8)),
            expired_at: now + session_timeout,
            last_active_time: Arc::new(std::sync::atomic::AtomicU64::new(now)),
            listener_started: Arc::new(std::sync::atomic::AtomicBool::new(false)),
        }
    }
    
    // 获取会话状态
    pub(crate) fn get_status(&self) -> SessionStatus {
        match self.status.load(std::sync::atomic::Ordering::SeqCst) {
            0 => SessionStatus::Active,
            1 => SessionStatus::Terminated,
            _ => unreachable!(),
        }
    }
    
    // 设置会话状态
    pub(crate) fn set_status(&self, new_status: SessionStatus) {
        self.status.store(new_status as u8, std::sync::atomic::Ordering::SeqCst);
    }
    
    // 获取最后活动时间
    pub(crate) fn get_last_active_time(&self) -> u64 {
        self.last_active_time.load(std::sync::atomic::Ordering::SeqCst)
    }
    
    // 更新最后活动时间
    pub(crate) fn update_last_active_time(&self) {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64;
        self.last_active_time.store(now, std::sync::atomic::Ordering::SeqCst);
    }
    
    // 检查会话是否过期
    pub(crate) fn is_expired(&self) -> bool {
        let now = std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap()
            .as_millis() as u64;
        now > self.expired_at
    }
}

// 会话管理器 - 完全线程安全设计
#[derive(Clone)]
pub struct SessionManager {
    // 使用RwLock保护会话映射，允许多读单写
    sessions: Arc<RwLock<HashMap<String, Session>>>,
    config: Arc<Config>,
}

impl SessionManager {
    // 创建新的会话管理器
    pub fn new(config: Arc<Config>) -> Self {
        let session_manager = Self {
            sessions: Arc::new(RwLock::new(HashMap::new())),
            config: config.clone(),
        };
        
        // 启动会话过期检查任务
        let session_manager_clone = session_manager.clone();
        tokio::spawn(async move {
            session_manager_clone.session_expiry_checker().await;
        });
        
        session_manager
    }
    

    
    // 创建新会话 - 线程安全，只需要&self
    pub async fn create_session(&self) -> anyhow::Result<String> {
        // 生成会话ID
        let session_id = Uuid::new_v4().to_string();
        
        // 获取默认shell配置
        let default_shell_config = self.config.get_default_shell_config();
        
        // 创建终端进程 - 完全异步，不持有任何锁
        let terminal = TerminalProcess::new_with_config(default_shell_config).await?;
        
        // 创建会话对象
        let session = Session::new(terminal.clone(), self.config.terminal.session_timeout);
        
        // 添加到会话映射 - 只持有写锁一小段时间
        {
            let mut sessions_write = self.sessions.write().unwrap();
            sessions_write.insert(session_id.clone(), session.clone());
        }
        
        log::info!("Created new session with ID: {} using shell: {:?}", 
                  session_id, default_shell_config.command);
        
        // 注意：我们不再在这里启动终端输出监听任务
        // 终端输出监听任务将在第一个客户端连接时启动
        // 这样可以确保只有在有客户端连接时才会读取终端输出，避免输出被丢弃
        
        Ok(session_id)
    }
    
    // 启动终端输出监听任务 - 独立异步任务，不阻塞主线程
    async fn spawn_terminal_listener(&self, terminal: TerminalProcess, session_id: String) {
        let session_manager_clone = self.clone();
        tokio::spawn(async move { 
            let mut buffer = [0; 1024];
            let session_id = session_id.clone();
            
            loop {
                // 检查终端进程是否还在运行
                if !terminal.is_running().await {
                    log::info!("Terminal process for session {} has exited, stopping listener", session_id);
                    break;
                }
                
                // 读取终端输出 - 使用非阻塞方式，避免长时间等待
                let output = tokio::select! {
                    // 尝试读取终端输出，超时时间为50毫秒
                    output_result = tokio::time::timeout(
                        tokio::time::Duration::from_millis(50),
                        terminal.read_output(&mut buffer)
                    ) => {
                        match output_result {
                            Ok(Ok(output)) => output,
                            Ok(Err(e)) => {
                                log::error!("Error reading terminal output for session {}: {}", session_id, e);
                                // 继续循环，不要因为一次错误就退出
                                continue;
                            },
                            Err(_) => {
                                // 超时，没有数据可读，继续循环
                                String::new()
                            }
                        }
                    }
                };
                
                if !output.is_empty() {
                    log::info!("Terminal output for session {}: {:?}", session_id, output);
                    
                    // 从会话管理器中获取最新的会话和client_senders
                    let senders = {
                        // 获取读锁
                        let sessions_read = session_manager_clone.sessions.read().unwrap();
                        
                        // 查找会话
                        match sessions_read.get(&session_id) {
                            Some(session) => {
                                // 获取client_senders并克隆
                                let client_senders_lock = session.client_senders.lock().unwrap();
                                let senders_count = client_senders_lock.len();
                                log::info!("Found {} client senders for session {}", senders_count, session_id);
                                client_senders_lock.clone()
                            },
                            None => {
                                log::warn!("Session {} not found when sending terminal output", session_id);
                                Vec::new()
                            }
                        }
                    };
                    
                    log::info!("Sending terminal output to {} clients for session {}", senders.len(), session_id);
                    
                    // 异步发送，不阻塞主循环
                    for (i, sender) in senders.iter().enumerate() {
                        // 克隆sender，确保它有'static生命周期
                        let sender_clone = sender.clone();
                        let output_clone = output.clone();
                        let session_id_clone = session_id.clone();
                        let session_manager_clone = session_manager_clone.clone();
                        log::info!("Sending output to client {} for session {}", i, session_id_clone);
                        tokio::spawn(async move {
                            if let Err(e) = sender_clone.send(output_clone).await {
                                log::info!("Client sender closed for session {}: {}", session_id_clone, e);
                                // 异步移除失效的发送者
                                
                                // 获取读锁查找会话
                                let sessions_read = session_manager_clone.sessions.read().unwrap();
                                if let Some(session) = sessions_read.get(&session_id_clone) {
                                    let mut client_senders_lock = session.client_senders.lock().unwrap();
                                    client_senders_lock.retain(|s| !std::ptr::eq(s, &sender_clone));
                                    log::info!("Removed closed sender from session {}", session_id_clone);
                                }
                            } else {
                                log::info!("Sent terminal output to client for session {}", session_id_clone);
                            }
                        });
                    }
                }
                
                // 短暂休眠，避免CPU占用过高
                tokio::time::sleep(tokio::time::Duration::from_millis(10)).await;
            }
        });
    }
    
    // 添加客户端发送通道 - 线程安全，只需要&self
    pub async fn add_client_sender(&self, session_id: &str, sender: tokio::sync::mpsc::Sender<String>) {
        // 只持有读锁一小段时间
        let session = {
            let sessions_read = self.sessions.read().unwrap();
            match sessions_read.get(session_id) {
                Some(session) => session.clone(),
                None => {
                    log::error!("Session not found: {}", session_id);
                    return;
                }
            }
        };
        
        // 添加发送者到会话
        {
            let mut client_senders = session.client_senders.lock().unwrap();
            client_senders.push(sender);
            log::info!("Added client sender for session: {}", session_id);
        } // 在这里释放client_senders锁
        
        // 检查是否需要启动终端输出监听任务
        // 使用compare_exchange确保只有一个线程能启动监听任务
        if !session.listener_started.load(std::sync::atomic::Ordering::SeqCst) {
            if session.listener_started.compare_exchange(
                false, true, 
                std::sync::atomic::Ordering::SeqCst, 
                std::sync::atomic::Ordering::SeqCst
            ).is_ok() {
                // 启动终端输出监听任务
                log::info!("Starting terminal output listener for session: {}", session_id);
                self.spawn_terminal_listener(session.terminal.clone(), session_id.to_string()).await;
            }
        }
    }
    
    // 写入数据到会话 - 线程安全，只需要&self
    pub async fn write_to_session(&self, session_id: &str, data: &str) -> anyhow::Result<()> {
        log::debug!("write_to_session called with session_id: {}, data: {:?}", session_id, data);
        
        let session = {
            log::debug!("Acquiring read lock for sessions map");
            let sessions_read = self.sessions.read().unwrap();
            log::debug!("Got read lock for sessions map");
            match sessions_read.get(session_id) {
                Some(session) => {
                    log::debug!("Found session: {}", session_id);
                    session.clone()
                },
                None => {
                    log::debug!("Session not found: {}", session_id);
                    anyhow::bail!("Session not found: {}", session_id)
                },
            }
        };
        
        // 更新最后活动时间
        session.update_last_active_time();
        
        log::debug!("Got session clone, about to call write_input");
        // 释放会话管理器锁后，执行异步写入
        session.terminal.write_input(data).await?;
        log::debug!("Wrote data to session {}: {:?}", session_id, data);
        
        Ok(())
    }
    
    // 关闭会话 - 线程安全，只需要&self，幂等设计
    pub async fn close_session(&self, session_id: &str) -> anyhow::Result<()> {
        // 先从映射中移除会话，避免竞争条件
        let session = {
            let mut sessions_write = self.sessions.write().unwrap();
            match sessions_write.remove(session_id) {
                Some(session) => {
                    // 更新会话状态为Terminated
                    session.set_status(SessionStatus::Terminated);
                    session
                },
                None => anyhow::bail!("Session not found: {}", session_id),
            }
        };
        
        // 释放会话管理器锁后，执行异步关闭
        session.terminal.close().await?;
        log::info!("Closed session: {}", session_id);
        
        Ok(())
    }
    
    // 检查会话是否存在 - 线程安全，只需要&self
    pub async fn session_exists(&self, session_id: &str) -> bool {
        let sessions_read = self.sessions.read().unwrap();
        sessions_read.contains_key(session_id)
    }
    
    // 获取会话状态 - 线程安全，只需要&self
    pub async fn get_session_status(&self, session_id: &str) -> anyhow::Result<SessionStatus> {
        let sessions_read = self.sessions.read().unwrap();
        match sessions_read.get(session_id) {
            Some(session) => Ok(session.get_status()),
            None => anyhow::bail!("Session not found: {}", session_id),
        }
    }
    

    
    // 获取所有会话及其状态 - 线程安全，只需要&self
    pub async fn get_all_sessions_with_status(&self) -> Vec<(String, SessionStatus)> {
        let sessions_read = self.sessions.read().unwrap();
        sessions_read.iter()
            .map(|(id, session)| (id.clone(), session.get_status()))
            .collect()
    }
    
    // 调整终端大小 - 线程安全，只需要&self
    pub async fn resize_session(&self, session_id: &str, columns: u32, rows: u32) -> anyhow::Result<()> {
        // 只持有读锁一小段时间，获取会话引用
        let session = {
            let sessions_read = self.sessions.read().unwrap();
            match sessions_read.get(session_id) {
                Some(session) => session.clone(),
                None => anyhow::bail!("Session not found: {}", session_id),
            }
        };
        
        // 更新最后活动时间
        session.update_last_active_time();
        
        // 释放会话管理器锁后，执行异步调整大小
        session.terminal.resize(columns, rows).await?;
        
        log::info!("Resized session {} to {} columns x {} rows", session_id, columns, rows);
        
        Ok(())
    }
    
    // 会话过期检查器 - 定期检查并关闭过期会话
    async fn session_expiry_checker(&self) {
        log::info!("Starting session expiry checker");
        
        // 每60秒检查一次会话过期
        let mut interval = tokio::time::interval(tokio::time::Duration::from_secs(60));
        
        loop {
            interval.tick().await;
            log::debug!("Running session expiry check");
            
            // 获取所有会话ID
            let session_ids = {
                let sessions_read = self.sessions.read().unwrap();
                sessions_read.keys().cloned().collect::<Vec<String>>()
            };
            
            // 检查每个会话是否过期
            for session_id in session_ids {
                // 获取会话引用
                let session = {
                    let sessions_read = self.sessions.read().unwrap();
                    match sessions_read.get(&session_id) {
                        Some(session) => session.clone(),
                        None => continue,
                    }
                };
                
                // 获取会话的最后活动时间
                let last_active_time = session.get_last_active_time();
                
                // 检查会话是否过期
                if session.is_expired() {
                    log::info!("Session {} has expired (last active: {}), closing it", session_id, last_active_time);
                    
                    // 关闭会话 - close_session方法已经包含了从映射中移除的逻辑
                    if let Err(e) = self.close_session(&session_id).await {
                        log::error!("Failed to close expired session {}: {}", session_id, e);
                    }
                } else {
                    log::debug!("Session {} is active (last active: {})
", session_id, last_active_time);
                }
            }
        }
    }
    
    // 更新会话最后活动时间
    pub async fn update_session_activity(&self, session_id: &str) -> anyhow::Result<()> {
        // 获取会话引用
        let session = {
            let sessions_read = self.sessions.read().unwrap();
            match sessions_read.get(session_id) {
                Some(session) => session.clone(),
                None => anyhow::bail!("Session not found: {}", session_id),
            }
        };
        
        // 更新最后活动时间
        session.update_last_active_time();
        
        Ok(())
    }
}
