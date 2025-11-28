import React, { useState, useRef } from 'react';
import { TerminalComponent } from './components/Terminal';
import { Header } from './components/Header';
import { SessionListModal } from './components/SessionListModal';
import { ResizeModal } from './components/ResizeModal';
import { listSessions } from './services/terminalApi';

function App() {
  const terminalRef = useRef<any>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  // 协议选择状态
  const [protocol, setProtocol] = useState<'websocket' | 'webtransport' | 'auto'>('auto');
  
  // 会话信息状态 - 使用与后端一致的默认尺寸
  const [currentSessionInfo, setCurrentSessionInfo] = useState<{
    sessionId: string;
    shellType: string;
    terminalSize: { columns: number; rows: number };
  }>({
    sessionId: '',
    shellType: '',
    terminalSize: { columns: 80, rows: 24 } // 与后端默认尺寸保持一致
  });

  const toggleFullscreen = () => {
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen();
      setIsFullscreen(true);
    } else {
      if (document.exitFullscreen) {
        document.exitFullscreen();
        setIsFullscreen(false);
      }
    }
  };

  const handleConnect = () => {
    if (isConnected) {
      // Disconnect
      if (terminalRef.current && terminalRef.current.disconnect) {
        terminalRef.current.disconnect();
      }
      setIsConnected(false);
    } else {
      // Connect
      if (terminalRef.current && terminalRef.current.connect) {
        terminalRef.current.connect();
      }
      setIsConnected(true);
    }
  };

  const handleConnectionStatusChange = (connected: boolean, sessionInfo?: {
    sessionId: string;
    shellType: string;
    terminalSize: { columns: number; rows: number }
  }) => {
    setIsConnected(connected);
    
    if (connected && sessionInfo) {
      // 更新会话信息
      setCurrentSessionInfo({
        sessionId: sessionInfo.sessionId,
        shellType: sessionInfo.shellType,
        terminalSize: sessionInfo.terminalSize
      });
      
      // 根据终端尺寸动态调整布局
      if (sessionInfo.terminalSize) {
        console.log('📏 Terminal size updated:', `${sessionInfo.terminalSize.columns}×${sessionInfo.terminalSize.rows}`);
        // 这里可以根据实际尺寸调整布局，比如设置合适的容器高度
      }
    } else {
      // 断开连接时重置会话信息
      setCurrentSessionInfo({
        sessionId: '',
        shellType: '',
        terminalSize: { columns: 120, rows: 30 }
      });
    }
  };

  // Control panel API operations
  const handleRefresh = () => {
    if (terminalRef.current && terminalRef.current.connect) {
      terminalRef.current.disconnect();
      setTimeout(() => {
        terminalRef.current.connect();
      }, 500);
    }
  };

  // Session List Modal state and handlers
  const [showSessionList, setShowSessionList] = useState(false);
  const [sessionList, setSessionList] = useState<any[]>([]);

  const handleListSessions = async () => {
    try {
      // Get or generate user ID
      let userId = localStorage.getItem('terminal_user_id');
      if (!userId) {
        // Generate user ID in format required by backend: usr_ + 12 lowercase hex characters
        const hexChars = 'abcdef0123456789';
        let hexId = '';
        for (let i = 0; i < 12; i++) {
          hexId += hexChars.charAt(Math.floor(Math.random() * hexChars.length));
        }
        userId = 'usr_' + hexId.toLowerCase();
        localStorage.setItem('terminal_user_id', userId);
      }
      
      const data = await listSessions();
      console.log('Active sessions:', data);
      setSessionList(data.sessions);
      setShowSessionList(true);
    } catch (error) {
      console.error('Failed to list sessions:', error);
    }
  };

  const handleTerminateSession = () => {
    if (terminalRef.current && terminalRef.current.terminate) {
      terminalRef.current.terminate('USER_REQUESTED');
    }
  };

  // Resize Modal state and handlers
  const [showResizeModal, setShowResizeModal] = useState(false);

  const handleResizeTerminal = () => {
    setShowResizeModal(true);
  };

  const handleApplyResize = (columns: number, rows: number) => {
    if (terminalRef.current && terminalRef.current.resize) {
      terminalRef.current.resize(columns, rows);
    }
  };

  return (
    <div className="h-screen flex flex-col bg-gradient-to-br from-tech-bg-darker via-tech-bg-dark to-tech-secondary text-foreground font-sans overflow-hidden">
      {/* Header Component */}
      <Header
        isConnected={isConnected}
        isFullscreen={isFullscreen}
        currentSessionInfo={currentSessionInfo}
        protocol={protocol}
        onProtocolChange={setProtocol}
        onConnect={handleConnect}
        onToggleFullscreen={toggleFullscreen}
        onRefresh={handleRefresh}
        onListSessions={handleListSessions}
        onTerminateSession={handleTerminateSession}
        onResizeTerminal={handleResizeTerminal}
      />

      {/* Session List Modal Component */}
      <SessionListModal
        isOpen={showSessionList}
        sessions={sessionList}
        currentSessionId={currentSessionInfo.sessionId}
        onClose={() => setShowSessionList(false)}
      />
      
      {/* Resize Modal Component */}
      <ResizeModal
        isOpen={showResizeModal}
        currentSize={currentSessionInfo.terminalSize}
        onClose={() => setShowResizeModal(false)}
        onApplyResize={handleApplyResize}
      />

      {/* Main Content - Full width with minimal padding */}
      <main className="flex-1 p-0 overflow-hidden flex flex-col">
        {/* Terminal Container with enhanced border effect - full width */}
        <div className="flex-1 relative">
          {/* Enhanced monitor bezel with glass effect */}
          <div className="absolute inset-0 bg-gradient-to-br from-primary/20 to-accent/20 opacity-40 pointer-events-none"></div>
          
          {/* Monitor frame - enhanced border effect */}
          <div className="absolute inset-0 bg-card/95 backdrop-blur-md border-2 border-primary/30 shadow-2xl overflow-hidden">
            {/* Modern tech elements - monitor screen edges */}
            <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-primary via-accent to-primary opacity-70 animate-pulse-slow pointer-events-none"></div>
            <div className="absolute bottom-0 left-0 w-full h-1 bg-gradient-to-r from-primary via-accent to-primary opacity-70 animate-pulse-slow pointer-events-none"></div>
            {/* Left and right border accents */}
            <div className="absolute top-0 left-0 h-full w-1 bg-gradient-to-b from-primary via-accent to-primary opacity-70 animate-pulse-slow pointer-events-none"></div>
            <div className="absolute top-0 right-0 h-full w-1 bg-gradient-to-b from-primary via-accent to-primary opacity-70 animate-pulse-slow pointer-events-none"></div>
            
            {/* Monitor screen */}
            <div className="h-full relative">
              <TerminalComponent 
                ref={terminalRef}
                className="h-full overflow-hidden" 
                protocol={protocol}
                onConnectionStatusChange={handleConnectionStatusChange}
              />
            </div>
          </div>
        </div>
        
        {/* Author and repository info with glass effect */}
        <div className="mt-2 glass p-3 flex items-center justify-between text-sm text-muted-foreground">
          {/* Modern decorative elements */}
          <div className="absolute inset-0 bg-gradient-to-r from-primary/10 via-accent/10 to-primary/10 opacity-50 pointer-events-none"></div>
          <div className="absolute -top-1 left-1/2 w-24 h-1 bg-gradient-to-r from-transparent via-primary/50 to-transparent animate-pulse-slow pointer-events-none"></div>
          
          {/* Author info */}
          <div className="flex items-center space-x-2 relative z-10">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24" aria-hidden="true">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
            </svg>
            <span className="font-mono">Author: </span>
            <a 
              href="https://github.com/wlcn" 
              target="_blank" 
              rel="noopener noreferrer" 
              className="font-mono hover:text-primary transition-colors"
            >
              long.wang
            </a>
          </div>
          
          {/* Repository info */}
          <div className="flex items-center space-x-2 relative z-10">
            <a 
              href="https://github.com/wlcn/terminal" 
              target="_blank" 
              rel="noopener noreferrer" 
              className="flex items-center space-x-1 hover:text-primary transition-colors"
            >
              <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true">
                <path fillRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" clipRule="evenodd" />
              </svg>
              <span className="font-mono">github.com/wlcn/terminal</span>
            </a>
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;