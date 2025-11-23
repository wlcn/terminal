import React, { useEffect, useRef, useState, forwardRef, useImperativeHandle } from 'react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import { WebLinksAddon } from '@xterm/addon-web-links';
import { WebglAddon } from '@xterm/addon-webgl';
import '@xterm/xterm/css/xterm.css';
import { createSession, resizeTerminal, terminateSession, checkSessionActive } from '../services/terminalApi';

interface TerminalComponentProps {
  className?: string;
  onConnectionStatusChange?: (connected: boolean) => void;
  ref?: React.Ref<any>;
}

const TerminalComponent = forwardRef<any, TerminalComponentProps>(({ className, onConnectionStatusChange }, ref) => {
  const terminalRef = useRef<HTMLDivElement>(null);
  const terminal = useRef<Terminal | null>(null);
  const fitAddon = useRef<FitAddon | null>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [sessionId, setSessionId] = useState<string>('');
  const ws = useRef<WebSocket | null>(null);
  const isInitialized = useRef(false);

  // 暴露方法给父组件
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
        terminal.current.write('$ ');
      }
    },
    isConnected: () => isConnected,
    getSessionId: () => sessionId
  }));

  // 连接终端 - session和WebSocket一对一绑定
  const connectTerminal = async () => {
    try {
      console.log('🔄 Starting terminal connection process...');
      terminal.current?.writeln('🔄 Starting terminal connection...');
      
      // 获取或生成用户ID
      let userId = localStorage.getItem('terminal_user_id');
      if (!userId) {
        userId = 'web-terminal-user-' + Date.now();
        localStorage.setItem('terminal_user_id', userId);
      }
      
      // 1. 通过API创建新会话
      console.log('📡 Creating new session via API...');
      terminal.current?.writeln('📡 Creating new session...');
      
      const sessionResponse = await createSession(userId);
      const newSessionId = sessionResponse.sessionId;
      const shellType = sessionResponse.shellType;
      
      console.log('✅ Session created:', newSessionId, 'Shell type:', shellType);
      terminal.current?.writeln(`✅ Session created: ${newSessionId}`);
      terminal.current?.writeln(`🐚 Shell type: ${shellType}`);
      setSessionId(newSessionId);
      
      // 2. 立即建立WebSocket连接（一对一绑定）
      console.log('🌐 Establishing WebSocket connection for session...');
      terminal.current?.writeln('🌐 Establishing WebSocket connection...');
      
      // 使用sessionId建立WebSocket连接
      ws.current = new WebSocket(`ws://localhost:8080/ws/${newSessionId}`);
      
      ws.current.onopen = () => {
        console.log('✅ WebSocket connection established successfully');
        terminal.current?.writeln('✅ WebSocket connected');
        
        // 在WebSocket连接成功后配置终端参数
        configureTerminalForShell(shellType);
        
        terminal.current?.writeln('🚀 Terminal ready for command line interaction');
        terminal.current?.writeln('');
        terminal.current?.write('$ ');
        
        setIsConnected(true);
        onConnectionStatusChange?.(true);
        
        // 连接成功后，session和WebSocket已建立一对一关系
        console.log(`🔗 Session ${newSessionId} ↔ WebSocket connection established`);
      };
      
      ws.current.onmessage = (event) => {
        console.log('📨 Received terminal output:', event.data);
        
        // WebSocket仅用于命令行输出，直接显示
        if (typeof event.data === 'string') {
          // xterm.js 专门设计用于处理终端转义序列，不需要手动转义
          // 直接写入数据，让xterm.js处理所有ANSI转义序列
          terminal.current?.write(event.data);
        }
      };
      
      ws.current.onclose = (event) => {
        console.log('🔌 WebSocket connection closed');
        console.log(`📊 Close code: ${event.code}, reason: ${event.reason}`);
        
        setIsConnected(false);
        onConnectionStatusChange?.(false);
        terminal.current?.writeln('\r\n🔌 WebSocket connection closed');
        
        // WebSocket关闭时，session也应该被终止（一对一关系）
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
        
        // WebSocket错误时，session也应该被终止（一对一关系）
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
  
  // 调整终端尺寸
  const handleResize = async (columns: number, rows: number) => {
    if (!sessionId) {
      console.warn('⚠️ No active session to resize');
      return;
    }
    
    try {
      console.log(`📐 Resizing terminal to ${columns}x${rows}`);
      await resizeTerminal(sessionId, columns, rows);
      console.log('✅ Terminal resized successfully');
    } catch (error) {
      console.error('❌ Failed to resize terminal:', error);
    }
  };
  
  // 终止会话 - 同时关闭WebSocket连接（一对一关系）
  const handleTerminate = async (reason?: string) => {
    if (!sessionId) {
      console.warn('⚠️ No active session to terminate');
      return;
    }
    
    try {
      console.log(`🛑 Terminating session: ${reason || 'USER_REQUESTED'}`);
      
      // 先关闭WebSocket连接
      if (ws.current) {
        ws.current.close();
        console.log('🔌 WebSocket connection closed');
      }
      
      // 然后终止session
      await terminateSession(sessionId, reason);
      console.log('✅ Session terminated successfully');
      
      // 重置状态
      setSessionId('');
      setIsConnected(false);
      onConnectionStatusChange?.(false);
      
    } catch (error) {
      console.error('❌ Failed to terminate session:', error);
      
      // 即使API调用失败，也要确保WebSocket关闭
      if (ws.current) {
        ws.current.close();
      }
    }
  };

  // 根据shell类型动态配置xterm.js参数
  const configureTerminalForShell = (shellType: string | undefined) => {
    if (!terminal.current) return;
    
    // 处理undefined或空值的情况
    if (!shellType) {
      console.warn('⚠️ Shell type is undefined or empty, using auto-detection');
      shellType = 'AUTO';
    }
    
    console.log(`⚙️ Configuring terminal for shell type: ${shellType}`);
    
    // 根据shell类型设置不同的xterm.js配置
    switch (shellType.toUpperCase()) {
      case 'WINDOWS_CMD':
      case 'WINDOWS_POWERSHELL':
        // Windows环境：启用Windows模式，正确处理回车符
        terminal.current.options.windowsMode = true;
        terminal.current.options.convertEol = true; // 将\n转换为\r\n
        terminal.current.writeln('🔧 Terminal configured for Windows environment');
        break;
        
      case 'UNIX':
        // Unix/Linux环境：使用Unix风格的行结束符
        terminal.current.options.windowsMode = false;
        terminal.current.options.convertEol = false; // 保持\n不变
        terminal.current.writeln('🔧 Terminal configured for Unix/Linux environment');
        break;
        
      case 'AUTO':
      default:
        // 自动检测：根据浏览器环境判断
        const isWindows = navigator.userAgent.includes('Windows');
        terminal.current.options.windowsMode = isWindows;
        terminal.current.options.convertEol = isWindows;
        terminal.current.writeln(`🔧 Terminal configured for ${isWindows ? 'Windows' : 'Unix/Linux'} environment (auto-detected)`);
        break;
    }
    
    // 刷新终端配置
    terminal.current.refresh(0, terminal.current.rows - 1);
  };

  // 生成随机会话ID
  const generateSessionId = () => {
    return 'session-' + Math.random().toString(36).substr(2, 9);
  };

  // 初始化终端 - 使用xterm.js官方最佳实践配置
  useEffect(() => {
    if (!terminalRef.current || isInitialized.current) return;

    console.log('🎯 Initializing xterm.js terminal with official best practices...');
    
    // 创建终端实例 - 使用最简洁的官方推荐配置
    terminal.current = new Terminal({
      // 基础配置
      fontSize: 14,
      fontFamily: 'Consolas, "Courier New", monospace',
      theme: {
        background: '#1e1e1e',
        foreground: '#cccccc',
        cursor: '#ffffff',
        selection: '#3a3d41'
      }
      // 不添加任何特殊配置，让xterm.js按默认方式处理所有字符
    });

    // 创建并安装插件
    fitAddon.current = new FitAddon();
    const webLinksAddon = new WebLinksAddon();
    const webglAddon = new WebglAddon();

    terminal.current.loadAddon(fitAddon.current);
    terminal.current.loadAddon(webLinksAddon);
    terminal.current.loadAddon(webglAddon);

    // 挂载到DOM
    terminal.current.open(terminalRef.current);

    // 调整尺寸
    setTimeout(() => {
      fitAddon.current?.fit();
      
      // 监听窗口大小变化
      const handleResize = () => {
        fitAddon.current?.fit();
      };
      
      window.addEventListener('resize', handleResize);
      
      // 清理函数
      return () => {
        window.removeEventListener('resize', handleResize);
      };
    }, 100);

    // 监听键盘输入 - 使用最简单的处理方式
    terminal.current.onData((data) => {
      console.log('⌨️ Terminal input:', data);
      
      if (ws.current && ws.current.readyState === WebSocket.OPEN) {
        // 不进行任何本地回显，完全由后端处理所有输出
        // 发送所有输入到后端，后端负责完整的命令处理和回显
        ws.current.send(data);
      }
    });

    isInitialized.current = true;
    console.log('✅ Terminal initialized with official best practices');
    
    // 显示欢迎信息
    terminal.current.writeln('🚀 Web Terminal Ready');
    terminal.current.writeln('Type "connect" to start a session');
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
        className="w-full h-full bg-slate-900 rounded-lg overflow-hidden"
      />
    </div>
  );
});

export { TerminalComponent };