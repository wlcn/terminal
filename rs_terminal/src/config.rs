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

// 主配置结构
#[derive(Debug, Deserialize, Serialize, Clone)]
pub struct Config {
    pub terminal: TerminalConfig,
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
        
        // 如果没有找到配置文件，使用默认配置
        log::warn!("No configuration file found, using default configuration");
        Ok(Self::default())
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

// 默认配置实现
impl Default for Config {
    fn default() -> Self {
        let mut shells = HashMap::new();
        
        // 默认环境变量
        let mut default_env = HashMap::new();
        default_env.insert("TERM".to_string(), "xterm-256color".to_string());
        
        // 添加bash配置
        shells.insert("bash".to_string(), ShellConfig {
            command: vec!["bash".to_string()],
            working_directory: None,
            environment: default_env.clone(),
            terminal_size: None,
        });
        
        // 添加sh配置
        shells.insert("sh".to_string(), ShellConfig {
            command: vec!["sh".to_string()],
            working_directory: None,
            environment: default_env.clone(),
            terminal_size: None,
        });
        
        // 添加cmd配置
        shells.insert("cmd".to_string(), ShellConfig {
            command: vec!["cmd.exe".to_string()],
            working_directory: None,
            environment: default_env.clone(),
            terminal_size: None,
        });
        
        // 添加powershell配置
        shells.insert("powershell".to_string(), ShellConfig {
            command: vec!["powershell.exe".to_string()],
            working_directory: Some(std::env::var("USERPROFILE").unwrap_or(".".to_string())),
            environment: default_env,
            terminal_size: None,
        });
        
        Self {
            terminal: TerminalConfig {
                default_shell_type: "powershell".to_string(),
                default_terminal_size: TerminalSize {
                    columns: 80,
                    rows: 24,
                },
                default_working_directory: ".".to_string(),
                session_timeout: 1800000, // 30分钟
                shells,
            },
        }
    }
}
