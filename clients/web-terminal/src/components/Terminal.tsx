import React, { useEffect, useRef, useState, forwardRef, useImperativeHandle } from 'react';
import { Terminal } from '@xterm/xterm';
import { FitAddon } from '@xterm/addon-fit';
import { WebLinksAddon } from '@xterm/addon-web-links';
import { WebglAddon } from '@xterm/addon-webgl';
import '@xterm/xterm/css/xterm.css';

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
    connect: () => {
      if (!isConnected && ws.current?.readyState !== WebSocket.OPEN) {
        connectWebSocket();
      }
    },
    disconnect: () => {
      if (isConnected && ws.current) {
        ws.current.close();
      }
    },
    isConnected: () => isConnected
  }));

  // WebSocket connection function
  const connectWebSocket = async () => {
    console.log('🔄 Attempting WebSocket connection...');
    
    // Directly connect to WebSocket endpoint, backend will automatically create session
    console.log('🌐 WebSocket URL: ws://localhost:8080/ws');
    
    ws.current = new WebSocket('ws://localhost:8080/ws');
    
    ws.current.onopen = () => {
      console.log('✅ WebSocket connection established successfully');
      console.log(`📊 WebSocket readyState: ${ws.current?.readyState}`);
      
      setIsConnected(true);
      onConnectionStatusChange?.(true);
      terminal.current?.writeln('✅ WebSocket connection established');
      terminal.current?.writeln('⏳ Waiting for Session ID from backend...');
      terminal.current?.writeln('');
      terminal.current?.write('$ ');
    };
    
    ws.current.onmessage = (event) => {
      console.log('📨 Received message from server:', event.data);
      
      // Handle terminal output
      if (typeof event.data === 'string') {
        // Check if this is a Session ID message from backend
        if (event.data.startsWith('SESSION_ID:')) {
          const sessionId = event.data.substring('SESSION_ID:'.length);
          console.log('✅ Received Session ID from backend:', sessionId);
          setSessionId(sessionId);
          
          // Update terminal display
          terminal.current?.writeln(`✅ Session ID: ${sessionId}`);
          terminal.current?.writeln('');
          terminal.current?.write('$ ');
        } else {
          // Regular terminal output
          terminal.current?.write(event.data);
        }
      }
    };
    
    ws.current.onclose = (event) => {
      console.log('🔌 WebSocket connection closed');
      console.log(`📊 Close code: ${event.code}, reason: ${event.reason}`);
      console.log(`📊 Was clean: ${event.wasClean}`);
      
      setIsConnected(false);
      onConnectionStatusChange?.(false);
      terminal.current?.writeln('\r\n🔌 WebSocket connection closed');
    };
    
    ws.current.onerror = (error) => {
      console.error('❌ WebSocket connection error:', error);
      console.log(`📊 WebSocket readyState: ${ws.current?.readyState}`);
      
      setIsConnected(false);
      onConnectionStatusChange?.(false);
      terminal.current?.writeln('❌ WebSocket connection error');
    };
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