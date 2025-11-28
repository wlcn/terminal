import React, { useEffect, useRef, useState, forwardRef, useImperativeHandle } from 'react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import { WebLinksAddon } from '@xterm/addon-web-links';
import { WebglAddon } from '@xterm/addon-webgl';
import '@xterm/xterm/css/xterm.css';
import { createSession, resizeTerminal, interruptTerminal, terminateSession } from '../services/terminalApi';
import { APP_CONFIG } from '../config/appConfig';
import { createTerminalCommunication, TerminalCommunication, isWebTransportSupported } from '../services/terminalCommunication';

// WebSocket服务器配置
const WS_SERVER_URL = APP_CONFIG.WS_SERVER.URL;
const WS_SERVER_PATH = APP_CONFIG.WS_SERVER.PATH;

interface TerminalComponentProps {
  className?: string;
  protocol?: 'websocket' | 'webtransport' | 'auto';
  onConnectionStatusChange?: (connected: boolean, sessionInfo?: { sessionId: string; shellType: string; terminalSize: { columns: number; rows: number } }) => void;
  ref?: React.Ref<any>;
}

const TerminalComponent = forwardRef<any, TerminalComponentProps>(({ className, protocol = 'auto', onConnectionStatusChange }, ref) => {
  const terminalRef = useRef<HTMLDivElement>(null);
  const terminal = useRef<Terminal | null>(null);
  const fitAddon = useRef<FitAddon | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [sessionId, setSessionId] = useState<string>('');
  const [shellType, setShellType] = useState<string>('bash');
  const communication = useRef<TerminalCommunication | null>(null);
  const isInitialized = useRef(false);

  // Expose methods to parent component
  useImperativeHandle(ref, () => ({
    connect: connectTerminal,
    disconnect: () => {
      if (sessionId) {
        handleTerminate('USER_DISCONNECTED');
      } else if (communication.current) {
        communication.current.disconnect();
      }
    },
    send: (data: string) => {
      if (communication.current && communication.current.isConnected()) {
        communication.current.send(data);
      }
    },
    resize: handleResize,
    interrupt: handleInterrupt,
    terminate: handleTerminate,
    clear: () => {
      if (terminal.current) {
        terminal.current.clear();
        // 清屏后不自动添加提示符，避免重复的$符号
        // 提示符会在用户输入时由终端自动显示
      }
    },
    updateSettings: (settings: any) => {
      if (terminal.current) {
        // Update font size
        if (settings.fontSize) {
          terminal.current.options.fontSize = settings.fontSize;
        }
        
        // Update font family
        if (settings.fontFamily) {
          terminal.current.options.fontFamily = settings.fontFamily;
        }
        
        // Update theme
        if (settings.theme) {
          switch (settings.theme) {
            case 'light':
              terminal.current.options.theme = {
                background: '#ffffff',
                foreground: '#000000',
                cursor: '#000000',
                selection: '#cce7ff'
              };
              break;
            case 'high-contrast':
              terminal.current.options.theme = {
                background: '#000000',
                foreground: '#ffffff',
                cursor: '#ffffff',
                selection: '#00ff00'
              };
              break;
            case 'dark':
            default:
              terminal.current.options.theme = {
                background: '#1e1e1e',
                foreground: '#cccccc',
                cursor: '#ffffff',
                selection: '#3a3d41'
              };
              break;
          }
        }
        
        // Update cursor style
        if (settings.cursorStyle) {
          terminal.current.options.cursorStyle = settings.cursorStyle as any;
        }
        
        // Update scrollback
        if (settings.scrollback) {
          terminal.current.options.scrollback = settings.scrollback;
        }
        
        // Update auto wrap
        if (settings.autoWrap !== undefined) {
          terminal.current.options.wrap = settings.autoWrap;
        }
        
        // Refresh terminal to apply changes
        terminal.current.refresh(0, terminal.current.rows - 1);
      }
    },
    isConnected: () => isConnected,
    getSessionId: () => sessionId
  }));

  // Connect terminal - one-to-one binding between session and WebSocket
  const connectTerminal = async () => {
    try {
      console.log('🔄 Starting terminal connection process...');
      
      // Get or generate user ID
      let userId = localStorage.getItem('terminal_user_id');
      
      // Check if userId exists and has correct format (usr_ + 12 lowercase hex chars)
      const isValidUserId = userId && userId.startsWith('usr_') && userId.length === 16 && /^usr_[a-f0-9]{12}$/.test(userId);
      
      if (!userId || !isValidUserId) {
        // Generate user ID in format required by backend: usr_ + 12 lowercase hex characters
        const hexChars = 'abcdef0123456789';
        let hexId = '';
        for (let i = 0; i < 12; i++) {
          hexId += hexChars.charAt(Math.floor(Math.random() * hexChars.length));
        }
        userId = 'usr_' + hexId.toLowerCase();
        localStorage.setItem('terminal_user_id', userId);
        
        if (!isValidUserId && userId) {
          console.log('🔄 Replaced invalid userId format with new valid format:', userId);
        }
      }
      
      // 1. Create new session via API
      console.log('📡 Creating new session via API...');
      
      // 获取终端尺寸
      const columns = 80;
      const rows = 24;
      
      const sessionResponse = await createSession(userId, 'Terminal Session');
      const newSessionId = sessionResponse.id; // 后端返回的是id字段，不是sessionId
      const shellType = sessionResponse.shellType; // 后端直接返回shellType字段，不是在configuration中
      setShellType(shellType);
      
      // 使用默认的终端尺寸数据
      const terminalSize = { columns, rows };
      
      console.log('✅ Session created:', newSessionId, 'Shell type:', shellType, 'Terminal size:', `${terminalSize.columns}×${terminalSize.rows}`);
      setSessionId(newSessionId);
      
      // 2. Try to establish communication connection (one-to-one binding)
      console.log('🌐 Attempting to establish communication connection for session...');
      
      try {
        // Determine which protocol to use
        let selectedProtocol: 'websocket' | 'webtransport';
        
        if (protocol === 'auto') {
          // Auto-detect: use WebTransport if supported, otherwise WebSocket
          selectedProtocol = isWebTransportSupported() ? 'webtransport' : 'websocket';
        } else {
          // Use the protocol specified by the user
          selectedProtocol = protocol;
        }
        
        console.log(`📡 Using communication protocol: ${selectedProtocol}`);
        
        // Create communication instance
        communication.current = createTerminalCommunication(newSessionId, selectedProtocol);
        
        // Set up event handlers
        communication.current.on('open', () => {
          console.log('✅ Communication connection established successfully');
          
          // Configure terminal parameters after connection is successful
          configureTerminalForShell(shellType);
          
          // 直接使用尺寸对象调整xterm.js
          if (terminalSize.columns && terminalSize.rows) {
            terminal.current?.resize(terminalSize.columns, terminalSize.rows);
          }
          
          terminal.current?.write('Terminal ready\r\n');
          terminal.current?.write('$ ');
          
          setIsConnected(true);
          
          // 传递会话信息给父组件
          onConnectionStatusChange?.(true, {
            sessionId: newSessionId,
            shellType: shellType,
            terminalSize: terminalSize // 使用尺寸对象
          });
          
          // After successful connection, session and communication have established one-to-one relationship
          console.log(`🔗 Session ${newSessionId} ↔ ${protocol} connection established`);
        });
        
        communication.current.on('message', (data) => {
          console.log('📨 Received terminal output:', data);
          
          // Communication is only used for command line output, display directly
          if (typeof data === 'string') {
            // xterm.js is specifically designed to handle terminal escape sequences, no manual escaping needed
            // Write data directly, let xterm.js handle all ANSI escape sequences
            terminal.current?.write(data);
          }
        });
        
        communication.current.on('close', (event) => {
          console.log('🔌 Communication connection closed');
          console.log(`📊 Close event:`, event);
          
          setIsConnected(false);
          onConnectionStatusChange?.(false);
          terminal.current?.writeln('\r\nCommunication connection closed');
          
          // When connection closes, session should also be terminated (one-to-one relationship)
          if (sessionId) {
            console.log(`🛑 Terminating session ${sessionId} due to communication closure`);
            handleTerminate('COMMUNICATION_CLOSED');
          }
        });
        
        communication.current.on('error', (error) => {
          console.error('❌ Communication connection error:', error);
          terminal.current?.writeln('❌ Communication connection error');
          
          setIsConnected(false);
          onConnectionStatusChange?.(false);
          
          // When communication error occurs, session should also be terminated (one-to-one relationship)
          if (sessionId) {
            console.log(`🛑 Terminating session ${sessionId} due to communication error`);
            handleTerminate('COMMUNICATION_ERROR');
          }
        });
        
        // Connect to the server
        communication.current.connect();
      } catch (error) {
        console.warn('⚠️ Communication connection failed, using fallback mode:', error);
        
        // Configure terminal parameters even without communication
        configureTerminalForShell(shellType);
        
        // 直接使用尺寸对象调整xterm.js
        if (terminalSize.columns && terminalSize.rows) {
          terminal.current?.resize(terminalSize.columns, terminalSize.rows);
        }
        
        terminal.current?.write('$ ');
        
        setIsConnected(true);
        
        // 传递会话信息给父组件
        onConnectionStatusChange?.(true, {
          sessionId: newSessionId,
          shellType: shellType,
          terminalSize: terminalSize // 使用尺寸对象
        });
        
        console.log(`✅ Session ${newSessionId} created (fallback mode)`);
      }
      
    } catch (error) {
      console.error('❌ Failed to connect terminal:', error);
      terminal.current?.writeln('❌ Failed to connect terminal');
      
      setIsConnected(false);
      onConnectionStatusChange?.(false);
    }
  };
  


  // Resize terminal
  const handleResize = async (columns: number, rows: number) => {
    if (!sessionId) {
      console.warn('⚠️ No active session to resize');
      return;
    }
    
    try {
      console.log(`📐 Resizing terminal to ${columns}x${rows}`);
      await resizeTerminal(sessionId, columns, rows);
      
      // 更新xterm.js尺寸
      if (terminal.current) {
        terminal.current.resize(columns, rows);
      }
      
      // 更新父组件状态
      onConnectionStatusChange?.(true, {
        sessionId: sessionId,
        shellType: shellType || 'bash',
        terminalSize: { columns, rows }
      });
      
      console.log('✅ Terminal resized successfully');
    } catch (error) {
      console.error('❌ Failed to resize terminal:', error);
    }
  };
  
  // Interrupt terminal (send Ctrl+C signal)
  const handleInterrupt = async () => {
    if (!sessionId) {
      console.warn('⚠️ No active session to interrupt');
      return;
    }
    
    try {
      console.log('⏹️ Sending interrupt signal to terminal');
      await interruptTerminal(sessionId);
      
      // 在终端中显示中断提示
      if (terminal.current) {
        terminal.current.write('\r\n^C\r\n');
      }
      
      console.log('✅ Terminal interrupted successfully');
    } catch (error) {
      console.error('❌ Failed to interrupt terminal:', error);
    }
  };
  
  // Terminate session - also close communication connection (one-to-one relationship)
  const handleTerminate = async (reason?: string) => {
    if (!sessionId) {
      console.warn('⚠️ No active session to terminate');
      return;
    }
    
    try {
      console.log(`🛑 Terminating session: ${reason || 'USER_REQUESTED'}`);
      
      // First close communication connection
      if (communication.current) {
        communication.current.disconnect();
        console.log('🔌 Communication connection closed');
      }
      
      // Then terminate session
      await terminateSession(sessionId, reason);
      console.log('✅ Session terminated successfully');
      
      // Reset state
      setSessionId('');
      setIsConnected(false);
      onConnectionStatusChange?.(false);
      
    } catch (error) {
      console.error('❌ Failed to terminate session:', error);
      
      // Even if API call fails, ensure communication is closed
      if (communication.current) {
        communication.current.disconnect();
      }
    }
  };

  // Dynamically configure xterm.js parameters based on shell type
  const configureTerminalForShell = (shellType: string | undefined) => {
    if (!terminal.current) return;
    
    // Handle undefined or empty values
    if (!shellType) {
      console.warn('⚠️ Shell type is undefined or empty, using auto-detection');
      shellType = 'AUTO';
    }
    
    console.log(`⚙️ Configuring terminal for shell type: ${shellType}`);
    
    // Set different xterm.js configurations based on shell type
    switch (shellType.toUpperCase()) {
      case 'WINDOWS_CMD':
      case 'WINDOWS_POWERSHELL':
        // Windows environment: enable Windows mode, handle carriage return correctly
        terminal.current.options.windowsMode = true;
        terminal.current.options.convertEol = true; // Convert \n to \r\n
        break;
        
      case 'UNIX':
        // Unix/Linux environment: use Unix-style line endings
        terminal.current.options.windowsMode = false;
        terminal.current.options.convertEol = false; // Keep \n unchanged
        break;
        
      case 'AUTO':
      default:
        // Auto-detection: determine based on browser environment
        const isWindows = navigator.userAgent.includes('Windows');
        terminal.current.options.windowsMode = isWindows;
        terminal.current.options.convertEol = isWindows;
        break;
    }
    
    // Refresh terminal configuration
    terminal.current.refresh(0, terminal.current.rows - 1);
  };


  // Initialize terminal - using xterm.js official best practice configuration
  useEffect(() => {
    if (!terminalRef.current || isInitialized.current) return;

    console.log('🎯 Initializing xterm.js terminal with official best practices...');
    
    // Create terminal instance - using the most concise official recommended configuration
        terminal.current = new Terminal({
          // Basic configuration
          fontSize: 14,
          fontFamily: 'Consolas, "Courier New", monospace',
          theme: {
            background: '#1e1e1e',
            foreground: '#cccccc',
            cursor: '#ffffff',
            selection: '#3a3d41'
          },
          // 使用与后端一致的默认尺寸，避免连接后终端框变化
          cols: 80,
          rows: 24
          // Do not add any special configuration, let xterm.js handle all characters in default way
        });

    // Create and install addons
    fitAddon.current = new FitAddon();
    const webLinksAddon = new WebLinksAddon();
    const webglAddon = new WebglAddon();

    terminal.current.loadAddon(fitAddon.current);
    terminal.current.loadAddon(webLinksAddon);
    terminal.current.loadAddon(webglAddon);

    // Mount to DOM
    terminal.current.open(terminalRef.current);

    // 保持固定的终端大小，不使用fit()方法自动调整
    // 避免初始化后终端框大小变化
    setTimeout(() => {
      // 确保终端保持固定的80x24大小
      terminal.current?.resize(80, 24);
      
      // 窗口大小改变时，保持终端大小不变，不自动调整
      const handleResize = () => {
        // 保持固定大小，不随窗口变化
      };
      
      window.addEventListener('resize', handleResize);
      
      // Cleanup function
      return () => {
        window.removeEventListener('resize', handleResize);
      };
    }, 100);

    // Listen for keyboard input - using the simplest processing method
        terminal.current.onData((data) => {
          console.log('⌨️ Terminal input:', data);
          
          if (communication.current && communication.current.isConnected()) {
            // Do not perform any local echo, let backend handle all output
            // Send all input to backend, backend is responsible for complete command processing and echo
            communication.current.send(data);
          }
        });

    isInitialized.current = true;
    console.log('✅ Terminal initialized with official best practices');
    
    // Display welcome message
    // terminal.current.writeln('🚀 Web Terminal Ready');
    terminal.current.writeln('Click the "Connect" button to start a session');
    terminal.current.write('$ ');

  }, []);

  return (
    <div className={`relative ${className}`}>
      {/* Connection status indicator */}
      <div className="absolute top-2 right-2 z-10 flex items-center space-x-2">
        <div 
          className={`w-3 h-3 rounded-full ${
            isConnected ? 'bg-green-500' : 'bg-red-500'
          }`}
        />
        <span className="text-xs text-white bg-black bg-opacity-50 px-2 py-1 rounded">
          {isConnected ? 'Connected' : 'Disconnected'}
        </span>
        {sessionId && (
          <span className="text-xs text-gray-300 bg-black bg-opacity-50 px-2 py-1 rounded">
            ID: {sessionId}
          </span>
        )}
      </div>
      
      <div 
        ref={terminalRef} 
        className="w-full h-full bg-slate-900 overflow-hidden"
      />
    </div>
  );
});

export { TerminalComponent };