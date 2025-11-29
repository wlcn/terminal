use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::Path;

// 终端尺寸配置
#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct TerminalSize {
    pub columns: u32,
    pub rows: u32,
}

// Shell配置
#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct ShellConfig {
    pub command: Vec<String>,
    pub working_directory: Option<String>,
    pub environment: HashMap<String, String>,
    pub terminal_size: Option<TerminalSize>,
}

// 终端配置
#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct TerminalConfig {
    pub default_shell_type: String,
    pub default_terminal_size: TerminalSize,
    pub default_working_directory: String,
    pub session_timeout: u64,
    pub shells: HashMap<String, ShellConfig>,
}

// HTTP服务器配置
#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct HttpConfig {
    pub port: u16,
    pub use_https: bool,
    pub cert_path: Option<String>,
    pub key_path: Option<String>,
}

// WebSocket服务器配置
#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct WebSocketConfig {
    pub port: u16,
}

// WebTransport服务器配置
#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct WebTransportConfig {
    pub port: u16,
}

// 主配置结构
#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct Config {
    pub terminal: TerminalConfig,
    pub http: HttpConfig,
    pub websocket: WebSocketConfig,
    pub webtransport: WebTransportConfig,
}

impl Config {
    // 从文件加载配置
    pub fn load<P: AsRef<Path>>(path: P) -> anyhow::Result<Self> {
        let settings = config::Config::builder()
            .add_source(config::File::new(path.as_ref().to_str().unwrap(), config::FileFormat::Toml))
            .build()?;
        
        settings.try_deserialize().map_err(|e| anyhow::anyhow!(e))
    }
    
    // 从默认位置加载配置
    pub fn load_default() -> anyhow::Result<Self> {
        // 尝试从多个位置和格式加载配置文件
        let possible_paths = [
            // TOML格式
            Path::new("./application.toml"),
            Path::new("src/main/resources/application.toml"),
            Path::new("../src/main/resources/application.toml"),
            // YAML格式
            Path::new("./application.yml"),
            Path::new("./application.yaml"),
            Path::new("src/main/resources/application.yml"),
            Path::new("src/main/resources/application.yaml"),
            Path::new("../src/main/resources/application.yml"),
            Path::new("../src/main/resources/application.yaml"),
            // JSON格式
            Path::new("./application.json"),
            Path::new("src/main/resources/application.json"),
            Path::new("../src/main/resources/application.json"),
            // HOCON格式（兼容kt-terminal，但可能无法解析）
            Path::new("./application.conf"),
            Path::new("src/main/resources/application.conf"),
            Path::new("../src/main/resources/application.conf"),
        ];
        
        for path in possible_paths {
            if path.exists() {
                log::info!("Loading configuration from: {:?}", path);
                return Self::load(path);
            }
        }
        
        // 如果没有找到配置文件，返回错误
        anyhow::bail!("No configuration file found")
    }
    
    // 获取指定shell类型的配置
    pub fn get_shell_config(&self, shell_type: &str) -> Option<&ShellConfig> {
        self.terminal.shells.get(shell_type)
    }
    
    // 获取默认shell配置
    pub fn get_default_shell_config(&self) -> &ShellConfig {
        self.terminal.shells.get(&self.terminal.default_shell_type)
            .unwrap_or_else(|| {
                log::warn!("Default shell type '{}' not found, using bash as fallback", 
                           self.terminal.default_shell_type);
                self.terminal.shells.get("bash").unwrap()
            })
    }
}


