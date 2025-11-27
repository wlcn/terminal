import React, { useEffect, useRef, useState, forwardRef, useImperativeHandle } from 'react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import { WebLinksAddon } from '@xterm/addon-web-links';
import { WebglAddon } from '@xterm/addon-webgl';
import '@xterm/xterm/css/xterm.css';
import { createSession, resizeTerminal, terminateSession } from '../services/terminalApi';
import { APP_CONFIG } from '../config/appConfig';

// WebSocket服务器配置
const WS_SERVER_URL = APP_CONFIG.WS_SERVER.URL;
const WS_SERVER_PATH = APP_CONFIG.WS_SERVER.PATH;

interface TerminalComponentProps {
  className?: string;
  onConnectionStatusChange?: (connected: boolean, sessionInfo?: { sessionId: string; shellType: string; terminalSize: { columns: number; rows: number } }) => void;
  ref?: React.Ref<any>;
}

const TerminalComponent = forwardRef<any, TerminalComponentProps>(({ className, onConnectionStatusChange }, ref) => {
  const terminalRef = useRef<HTMLDivElement>(null);
  const terminal = useRef<Terminal | null>(null);
  const fitAddon = useRef<FitAddon | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [sessionId, setSessionId] = useState<string>('');
  const [shellType, setShellType] = useState<string>('bash');
  const ws = useRef<WebSocket | null>(null);
  const isInitialized = useRef(false);

  // Expose methods to parent component
  useImperativeHandle(ref, () => ({
    connect: connectTerminal,
    disconnect: () => {
      if (sessionId) {
        handleTerminate('USER_DISCONNECTED');
      } else if (ws.current) {
        ws.current.close();
      }
    },
    send: (data: string) => {
      if (ws.current && ws.current.readyState === WebSocket.OPEN) {
        ws.current.send(data);
      }
    },
    resize: handleResize,
    terminate: handleTerminate,
    clear: () => {
      if (terminal.current) {
        terminal.current.clear();
        // 清屏后不自动添加提示符，避免重复的$符号
        // 提示符会在用户输入时由终端自动显示
      }
    },
    isConnected: () => isConnected,
    getSessionId: () => sessionId
  }));

  // Connect terminal - one-to-one binding between session and WebSocket
  const connectTerminal = async () => {
    try {
      console.log('🔄 Starting terminal connection process...');
      terminal.current?.writeln('🔄 Starting terminal connection...');
      
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
      terminal.current?.writeln('📡 Creating new session...');
      
      // 获取终端尺寸
      const columns = 80;
      const rows = 24;
      
      const sessionResponse = await createSession(userId, 'Web Terminal Session', '/');
      const newSessionId = sessionResponse.id; // 后端返回的是id字段，不是sessionId
      const shellType = sessionResponse.configuration.shellType;
      setShellType(shellType);
      
      // 使用默认的终端尺寸数据
      const terminalSize = { columns, rows };
      
      console.log('✅ Session created:', newSessionId, 'Shell type:', shellType, 'Terminal size:', `${terminalSize.columns}×${terminalSize.rows}`);
      terminal.current?.writeln(`✅ Session created: ${newSessionId}`);
      terminal.current?.writeln(`🐚 Shell type: ${shellType}`);
      terminal.current?.writeln(`📏 Terminal size: ${terminalSize.columns}×${terminalSize.rows}`);
      setSessionId(newSessionId);
      
      // 2. Try to establish WebSocket connection (one-to-one binding)
      console.log('🌐 Attempting to establish WebSocket connection for session...');
      terminal.current?.writeln('🌐 Attempting WebSocket connection...');
      
      // Use sessionId to establish WebSocket connection
      try {
        ws.current = new WebSocket(`${WS_SERVER_URL}/ws/sessions/${newSessionId}`);
        
        ws.current.onopen = () => {
          console.log('✅ WebSocket connection established successfully');
          terminal.current?.writeln('✅ WebSocket connected');
          
          // Configure terminal parameters after WebSocket connection is successful
          configureTerminalForShell(shellType);
          
          // 直接使用尺寸对象调整xterm.js
          if (terminalSize.columns && terminalSize.rows) {
            terminal.current?.resize(terminalSize.columns, terminalSize.rows);
          }
          
          terminal.current?.writeln('🚀 Terminal ready for command line interaction');
          terminal.current?.writeln('');
          terminal.current?.write('$ ');
          
          setIsConnected(true);
          
          // 传递会话信息给父组件
          onConnectionStatusChange?.(true, {
            sessionId: newSessionId,
            shellType: shellType,
            terminalSize: terminalSize // 使用尺寸对象
          });
          
          // After successful connection, session and WebSocket have established one-to-one relationship
          console.log(`🔗 Session ${newSessionId} ↔ WebSocket connection established`);
        };
      } catch (error) {
        console.warn('⚠️ WebSocket connection failed, using fallback mode:', error);
        terminal.current?.writeln('⚠️ WebSocket connection failed, using fallback mode');
        
        // Configure terminal parameters even without WebSocket
        configureTerminalForShell(shellType);
        
        // 直接使用尺寸对象调整xterm.js
        if (terminalSize.columns && terminalSize.rows) {
          terminal.current?.resize(terminalSize.columns, terminalSize.rows);
        }
        
        terminal.current?.writeln('🚀 Terminal session created successfully');
        terminal.current?.writeln('⚠️ Note: Real-time terminal interaction requires WebSocket support');
        terminal.current?.writeln('💡 You can use command execution APIs instead');
        terminal.current?.writeln('');
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
      
      ws.current.onmessage = (event) => {
        console.log('📨 Received terminal output:', event.data);
        
        // WebSocket is only used for command line output, display directly
        if (typeof event.data === 'string') {
          // xterm.js is specifically designed to handle terminal escape sequences, no manual escaping needed
          // Write data directly, let xterm.js handle all ANSI escape sequences
          terminal.current?.write(event.data);
        }
      };
      
      ws.current.onclose = (event) => {
        console.log('🔌 WebSocket connection closed');
        console.log(`📊 Close code: ${event.code}, reason: ${event.reason}`);
        
        setIsConnected(false);
        onConnectionStatusChange?.(false);
        terminal.current?.writeln('\r\n🔌 WebSocket connection closed');
        
        // When WebSocket closes, session should also be terminated (one-to-one relationship)
        if (sessionId) {
          console.log(`🛑 Terminating session ${sessionId} due to WebSocket closure`);
          handleTerminate('WEBSOCKET_CLOSED');
        }
      };
      
      ws.current.onerror = (error) => {
        console.error('❌ WebSocket connection error:', error);
        terminal.current?.writeln('❌ WebSocket connection error');
        
        setIsConnected(false);
        onConnectionStatusChange?.(false);
        
        // When WebSocket error occurs, session should also be terminated (one-to-one relationship)
        if (sessionId) {
          console.log(`🛑 Terminating session ${sessionId} due to WebSocket error`);
          handleTerminate('WEBSOCKET_ERROR');
        }
      };
      
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
  
  // Terminate session - also close WebSocket connection (one-to-one relationship)
  const handleTerminate = async (reason?: string) => {
    if (!sessionId) {
      console.warn('⚠️ No active session to terminate');
      return;
    }
    
    try {
      console.log(`🛑 Terminating session: ${reason || 'USER_REQUESTED'}`);
      
      // First close WebSocket connection
      if (ws.current) {
        ws.current.close();
        console.log('🔌 WebSocket connection closed');
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
      
      // Even if API call fails, ensure WebSocket is closed
      if (ws.current) {
        ws.current.close();
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
        terminal.current.writeln('🔧 Terminal configured for Windows environment');
        break;
        
      case 'UNIX':
        // Unix/Linux environment: use Unix-style line endings
        terminal.current.options.windowsMode = false;
        terminal.current.options.convertEol = false; // Keep \n unchanged
        terminal.current.writeln('🔧 Terminal configured for Unix/Linux environment');
        break;
        
      case 'AUTO':
      default:
        // Auto-detection: determine based on browser environment
        const isWindows = navigator.userAgent.includes('Windows');
        terminal.current.options.windowsMode = isWindows;
        terminal.current.options.convertEol = isWindows;
        terminal.current.writeln(`🔧 Terminal configured for ${isWindows ? 'Windows' : 'Unix/Linux'} environment (auto-detected)`);
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
      }
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

    // Adjust size
    setTimeout(() => {
      fitAddon.current?.fit();
      
      // Listen for window resize
      const handleResize = () => {
        fitAddon.current?.fit();
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
      
      if (ws.current && ws.current.readyState === WebSocket.OPEN) {
        // Do not perform any local echo, let backend handle all output
        // Send all input to backend, backend is responsible for complete command processing and echo
        ws.current.send(data);
      }
    });

    isInitialized.current = true;
    console.log('✅ Terminal initialized with official best practices');
    
    // Display welcome message
    terminal.current.writeln('🚀 Web Terminal Ready');
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