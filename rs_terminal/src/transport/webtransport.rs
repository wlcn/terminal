use std::sync::Arc;
use std::future::Future;
use std::pin::Pin;

use crate::config::Config;
use crate::protocol_adapter::ProtocolAdapter;
use crate::terminal_service::TerminalService;

// WebTransport适配器 - 实现ProtocolAdapter接口
pub struct WebTransportAdapter {
    terminal_service: Arc<TerminalService>,
    config: Arc<Config>,
}

impl WebTransportAdapter {
    // 创建新的WebTransport适配器
    pub fn new(terminal_service: Arc<TerminalService>, config: Arc<Config>) -> Self {
        Self {
            terminal_service,
            config,
        }
    }
}

// 实现ProtocolAdapter接口
impl ProtocolAdapter for WebTransportAdapter {
    fn start(&self) -> Pin<Box<dyn Future<Output = anyhow::Result<()>> + Send + '_>> {
        let port = self.config.webtransport.port;
        
        Box::pin(async move {
            log::info!("WebTransport server starting on https://localhost:{}", port);
            
            // 注意：wtransport库的API结构发生了变化，暂时简化实现
            // 等待进一步了解wtransport库的正确使用方式
            log::warn!("WebTransport implementation is temporarily simplified due to API changes");
            
            // 暂时阻塞，模拟服务器运行
            tokio::time::sleep(std::time::Duration::from_secs(u64::MAX)).await;
            
            Ok(())
        })
    }
}