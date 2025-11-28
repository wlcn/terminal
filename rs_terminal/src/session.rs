use std::collections::HashMap;
use std::sync::{Arc, Mutex, RwLock};
use uuid::Uuid;

use crate::config::{Config, ShellConfig};
use crate::terminal::TerminalProcess;

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
}

impl Session {
    // 创建新会话
    pub(crate) fn new(terminal: TerminalProcess) -> Self {
        Self {
            terminal,
            client_senders: Arc::new(Mutex::new(Vec::new())),
            status: Arc::new(std::sync::atomic::AtomicU8::new(SessionStatus::Active as u8)),
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
}

// 会话管理器 - 完全线程安全设计
pub struct SessionManager {
    // 使用RwLock保护会话映射，允许多读单写
    sessions: Arc<RwLock<HashMap<String, Session>>>,
    config: Arc<Config>,
}

impl SessionManager {
    // 创建新的会话管理器
    pub fn new(config: Arc<Config>) -> Self {
        Self {
            sessions: Arc::new(RwLock::new(HashMap::new())),
            config,
        }
    }
    
    // 获取共享引用
    pub fn clone_arc(&self) -> Arc<Self> {
        Arc::new(Self {
            sessions: self.sessions.clone(),
            config: self.config.clone(),
        })
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
        let session = Session::new(terminal.clone());
        
        // 获取会话的client_senders
        let client_senders = session.client_senders.clone();
        
        // 添加到会话映射 - 只持有写锁一小段时间
        {
            let mut sessions_write = self.sessions.write().unwrap();
            sessions_write.insert(session_id.clone(), session.clone());
        }
        
        // 启动终端输出监听 - 完全独立的任务，不持有会话管理器锁
        self.spawn_terminal_listener(terminal, session_id.clone(), client_senders).await;
        
        log::info!("Created new session with ID: {} using shell: {:?}", 
                  session_id, default_shell_config.command);
        
        Ok(session_id)
    }
    
    // 启动终端输出监听任务 - 独立异步任务，不阻塞主线程
    async fn spawn_terminal_listener(&self, terminal: TerminalProcess, session_id: String, client_senders: Arc<Mutex<Vec<tokio::sync::mpsc::Sender<String>>>>) {
        tokio::spawn(async move { 
            let mut buffer = [0; 1024];
            
            loop {
                // 检查终端进程是否还在运行
                if !terminal.is_running().await {
                    log::info!("Terminal process for session {} has exited, stopping listener", session_id);
                    break;
                }
                
                // 读取终端输出 - 只在读取时持有终端锁
                let output = match terminal.read_output(&mut buffer).await {
                    Ok(output) => output,
                    Err(e) => {
                        log::error!("Error reading terminal output for session {}: {}", session_id, e);
                        // 继续循环，不要因为一次错误就退出
                        continue;
                    }
                };
                
                if !output.is_empty() {
                    log::debug!("Terminal output for session {}: {:?}", session_id, output);
                    
                    // 发送输出到所有连接的客户端 - 只持有锁一小段时间
                    let senders = {
                        let client_senders_lock = client_senders.lock().unwrap();
                        client_senders_lock.clone()
                    };
                    
                    // 异步发送，不阻塞主循环
                    for sender in senders {
                        let output_clone = output.clone();
                        let client_senders_clone = client_senders.clone();
                        tokio::spawn(async move {
                            if let Err(_e) = sender.send(output_clone).await {
                                log::debug!("Client sender closed, removing from session");
                                // 异步移除失效的发送者
                                let mut client_senders_lock = client_senders_clone.lock().unwrap();
                                client_senders_lock.retain(|s| !std::ptr::eq(s, &sender));
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
        let mut client_senders = session.client_senders.lock().unwrap();
        client_senders.push(sender);
        log::info!("Added client sender for session: {}", session_id);
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
        
        log::debug!("Got session clone, about to call write_input");
        // 释放会话管理器锁后，执行异步写入
        session.terminal.write_input(data).await?;
        log::debug!("Wrote data to session {}: {:?}", session_id, data);
        
        Ok(())
    }
    
    // 关闭会话 - 线程安全，只需要&self
    pub async fn close_session(&self, session_id: &str) -> anyhow::Result<()> {
        // 获取会话引用
        let session = {
            let sessions_read = self.sessions.read().unwrap();
            match sessions_read.get(session_id) {
                Some(session) => session.clone(),
                None => anyhow::bail!("Session not found: {}", session_id),
            }
        };
        
        // 更新会话状态为Terminated
        session.set_status(SessionStatus::Terminated);
        
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
    
    // 获取所有会话ID - 线程安全，只需要&self
    pub async fn get_all_session_ids(&self) -> Vec<String> {
        let sessions_read = self.sessions.read().unwrap();
        sessions_read.keys().cloned().collect()
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
        
        // 释放会话管理器锁后，执行异步调整大小
        session.terminal.resize(columns, rows).await?;
        
        log::info!("Resized session {} to {} columns x {} rows", session_id, columns, rows);
        
        Ok(())
    }
}
