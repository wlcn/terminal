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
    shellType: 'bash',
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
        shellType: 'bash',
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

      {/* Main Content - Simple and Clean */}
      <main className="flex-1 p-6 overflow-hidden">
        {/* Terminal Container */}
        <div className="h-full bg-card/95 backdrop-blur-xl border border-border/50 shadow-2xl overflow-hidden">
          <TerminalComponent 
            ref={terminalRef}
            className="h-full overflow-hidden" 
            protocol={protocol}
            onConnectionStatusChange={handleConnectionStatusChange}
          />
        </div>
      </main>
    </div>
  );
}

export default App;