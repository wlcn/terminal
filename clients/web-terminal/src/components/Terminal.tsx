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
      
      console.log('✅ Session created:', newSessionId);
      terminal.current?.writeln(`✅ Session created: ${newSessionId}`);
      setSessionId(newSessionId);
      
      // 2. 立即建立WebSocket连接（一对一绑定）
      console.log('🌐 Establishing WebSocket connection for session...');
      terminal.current?.writeln('🌐 Establishing WebSocket connection...');
      
      // 使用sessionId建立WebSocket连接
      ws.current = new WebSocket(`ws://localhost:8080/ws/${newSessionId}`);
      
      ws.current.onopen = () => {
        console.log('✅ WebSocket connection established successfully');
        terminal.current?.writeln('✅ WebSocket connected');
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

  // 生成随机会话ID
  const generateSessionId = () => {
    return 'session-' + Math.random().toString(36).substr(2, 9);
  };

  useEffect(() => {
    if (!terminalRef.current) return;

    // 使用setTimeout确保DOM完全渲染
    const initTerminal = () => {
      // 创建终端实例
      terminal.current = new Terminal({
        theme: {
          background: '#0f172a',
          foreground: '#f8fafc',
          cursor: '#f8fafc',
          selection: '#334155',
        },
        fontSize: 14,
        fontFamily: '"Fira Code", "Cascadia Code", "Courier New", monospace',
        cursorBlink: true,
        allowTransparency: true,
      });

      // 创建插件
      fitAddon.current = new FitAddon();
      const webLinksAddon = new WebLinksAddon();
      const webglAddon = new WebglAddon();

      // 加载插件
      terminal.current.loadAddon(fitAddon.current);
      terminal.current.loadAddon(webLinksAddon);
      terminal.current.loadAddon(webglAddon);

      // 打开终端
      terminal.current.open(terminalRef.current);
      
      // 延迟执行fit，确保终端容器已完全渲染
      setTimeout(() => {
        fitAddon.current?.fit();
        
        // Add welcome message
        terminal.current?.writeln('🚀 Welcome to Web Terminal');
        terminal.current?.writeln('📡 Ready to connect - click "Connect" button to start');
        terminal.current?.writeln('');
        
        isInitialized.current = true;
      }, 100);

      // 处理窗口大小变化
      const handleResize = () => {
        fitAddon.current?.fit();
      };

      window.addEventListener('resize', handleResize);

      // 处理键盘输入
      terminal.current.onData((data) => {
        // 发送键盘输入到后端
        if (ws.current && ws.current.readyState === WebSocket.OPEN) {
          ws.current.send(data);
        }
        
        // 智能本地回显：只回显普通字符，不处理特殊控制字符
        // 这样可以避免重复显示，同时让用户看到自己的输入
        if (data.length === 1 && data.charCodeAt(0) >= 32 && data.charCodeAt(0) <= 126) {
          terminal.current?.write(data);
        }
      });

      return () => {
        window.removeEventListener('resize', handleResize);
        if (ws.current) {
          ws.current.close();
        }
        terminal.current?.dispose();
      };
    };

    const timer = setTimeout(initTerminal, 100);
    
    return () => {
      clearTimeout(timer);
    };
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