use std::sync::Arc;
use std::future::Future;
use std::pin::Pin;

use crate::config::Config;
use crate::pty::terminal_service::TerminalService;

// 协议适配器接口 - 定义通讯协议的通用接口
pub trait ProtocolAdapter {
    // 启动协议服务器
    fn start(&self) -> Pin<Box<dyn Future<Output = anyhow::Result<()>> + Send + '_>>;
}

// 协议适配器工厂 - 创建不同的协议适配器
pub struct ProtocolAdapterFactory;

impl ProtocolAdapterFactory {
    // 创建WebSocket适配器
    pub fn create_websocket_adapter(
        terminal_service: Arc<TerminalService>,
        config: Arc<Config>
    ) -> Arc<dyn ProtocolAdapter + Send + Sync> {
        Arc::new(
            crate::transport::websocket::websocket::WebSocketAdapter::new(terminal_service, config)
        )
    }
    
    // 创建WebTransport适配器
    pub fn create_webtransport_adapter(
        terminal_service: Arc<TerminalService>,
        config: Arc<Config>
    ) -> Arc<dyn ProtocolAdapter + Send + Sync> {
        Arc::new(
            crate::transport::webtransport::webtransport::WebTransportAdapter::new(terminal_service, config)
        )
    }
}
