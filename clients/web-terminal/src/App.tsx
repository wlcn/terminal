import React, { useState, useRef } from 'react';
import { TerminalComponent } from './components/Terminal';
import { Maximize2, Minimize2, Power, RefreshCw, List, X, Maximize } from 'lucide-react';
import { listSessions } from './services/terminalApi';
import { Button } from './components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from './components/ui/card';

function App() {
  const terminalRef = useRef<any>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  
  // 会话信息状态
  const [currentSessionInfo, setCurrentSessionInfo] = useState<{
    sessionId: string;
    shellType: string;
    terminalSize: { columns: number; rows: number };
  }>({
    sessionId: '',
    shellType: 'bash',
    terminalSize: { columns: 80, rows: 24 }
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

  const handleConnectionStatusChange = (connected: boolean, sessionInfo?: { sessionId: string; shellType: string; terminalSize: { columns: number; rows: number } }) => {
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
      alert(`Active sessions: ${data.count || 0}`);
    } catch (error) {
      console.error('Failed to list sessions:', error);
    }
  };

  const handleTerminateSession = () => {
    if (terminalRef.current && terminalRef.current.terminate) {
      terminalRef.current.terminate('USER_REQUESTED');
    }
  };

  const [showResizeModal, setShowResizeModal] = useState(false);
  const [resizeColumns, setResizeColumns] = useState(80);
  const [resizeRows, setResizeRows] = useState(24);
  const [showMoreMenu, setShowMoreMenu] = useState(false);

  const handleResizeTerminal = () => {
    setShowResizeModal(true);
    // 设置当前尺寸作为默认值
    if (currentSessionInfo.terminalSize) {
      setResizeColumns(currentSessionInfo.terminalSize.columns);
      setResizeRows(currentSessionInfo.terminalSize.rows);
    }
  };

  const applyResize = () => {
    if (terminalRef.current && terminalRef.current.resize) {
      terminalRef.current.resize(resizeColumns, resizeRows);
      setShowResizeModal(false);
    }
  };

  const cancelResize = () => {
    setShowResizeModal(false);
    // 重置为当前尺寸
    if (currentSessionInfo.terminalSize) {
      setResizeColumns(currentSessionInfo.terminalSize.columns);
      setResizeRows(currentSessionInfo.terminalSize.rows);
    }
  };

  return (
    <div className="h-screen flex flex-col bg-gradient-to-br from-tech-bg-darker via-tech-bg-dark to-tech-secondary text-foreground font-sans overflow-hidden">
      {/* Simple and Reliable Header */}
      <header className="glass border-b border-border/50 px-4 py-3 relative z-10">
        <div className="flex items-center justify-between max-w-7xl mx-auto">
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-3">
              <div className={`w-3 h-3 rounded-full relative ${isConnected ? 'status-connected glow-success' : 'status-disconnected'}`}>
                <div className={`absolute inset-0 rounded-full animate-ping ${isConnected ? 'bg-green-400' : 'bg-red-400'} opacity-75`}></div>
              </div>
              <h1 className="text-lg font-semibold tracking-tight bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">
                KT Terminal
              </h1>
            </div>
            
            {/* Session info display */}
            {isConnected && (
              <div className="flex items-center space-x-4 ml-4 pl-4 border-l border-border">
                <div className="flex flex-col text-sm">
                  <span className="text-muted-foreground">SHELL: {currentSessionInfo.shellType}</span>
                </div>
                <div className="flex flex-col text-sm">
                  <span className="text-primary font-mono">SIZE: {currentSessionInfo.terminalSize.columns}×{currentSessionInfo.terminalSize.rows}</span>
                </div>
              </div>
            )}
          </div>
          
          <div className="flex items-center space-x-2">
            {/* Main action buttons */}
            <div className="flex items-center space-x-2">
              <Button
                onClick={handleConnect}
                variant={isConnected ? "destructive" : "default"}
                size="sm"
                className={`h-9 w-9 p-0 hover:scale-105 transition-all duration-200 shadow-md ${
                  isConnected 
                    ? 'bg-green-500/30 hover:bg-green-500/40 border-green-500/40' 
                    : 'bg-gradient-to-br from-blue-500 to-purple-500 hover:from-blue-600 hover:to-purple-600 text-white shadow-lg animate-pulse'
                }`}
                title={isConnected ? 'Disconnect' : 'Connect to terminal'}
              >
                <Power size={16} className={isConnected ? "text-white" : "text-white"} />
              </Button>
              
              <Button
                onClick={toggleFullscreen}
                variant="outline"
                size="sm"
                className="h-9 w-9 p-0 bg-background/80 hover:bg-primary/10 hover:scale-105 transition-all duration-200"
                title={isFullscreen ? "Exit fullscreen" : "Enter fullscreen"}
              >
                {isFullscreen ? <Minimize2 size={16} className="text-primary" /> : <Maximize2 size={16} className="text-primary" />}
              </Button>
            </div>
            
            {/* More options dropdown - 作为按钮的直接子元素 */}
            <div className="relative">
              <Button
                variant="outline"
                size="sm"
                className={`h-9 w-9 p-0 hover:scale-105 transition-all duration-200 shadow-sm ${isConnected ? 'bg-primary/20 hover:bg-primary/30 border-primary/30' : 'bg-background/80 opacity-50 cursor-not-allowed'}`}
                title={isConnected ? "More options" : "Connect to enable"}
                disabled={!isConnected}
                onClick={() => setShowMoreMenu(!showMoreMenu)}
              >
                <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={isConnected ? "text-primary" : "text-muted-foreground"}>
                  <circle cx="12" cy="12" r="1"></circle>
                  <circle cx="19" cy="12" r="1"></circle>
                  <circle cx="5" cy="12" r="1"></circle>
                </svg>
              </Button>
              
              {/* Dropdown content - 直接作为按钮的子元素，使用absolute定位 */}
              {isConnected && showMoreMenu && (
                <div className="absolute right-0 top-full mt-1 bg-card border border-border rounded-lg shadow-lg z-50 py-1 w-48">
                  <button
                    onClick={() => {
                      handleRefresh();
                      setShowMoreMenu(false);
                    }}
                    className="w-full flex items-center space-x-2 px-4 py-2 text-sm hover:bg-primary/10 transition-colors"
                  >
                    <RefreshCw size={14} className="text-orange-500" />
                    <span>Refresh Terminal</span>
                    <kbd className="ml-auto text-xs bg-muted px-1.5 py-0.5 rounded">Ctrl+R</kbd>
                  </button>
                  <button
                    onClick={() => {
                      handleListSessions();
                      setShowMoreMenu(false);
                    }}
                    className="w-full flex items-center space-x-2 px-4 py-2 text-sm hover:bg-primary/10 transition-colors"
                  >
                    <List size={14} className="text-purple-500" />
                    <span>List Sessions</span>
                    <kbd className="ml-auto text-xs bg-muted px-1.5 py-0.5 rounded">Ctrl+L</kbd>
                  </button>
                  <button
                    onClick={() => {
                      handleTerminateSession();
                      setShowMoreMenu(false);
                    }}
                    className="w-full flex items-center space-x-2 px-4 py-2 text-sm text-red-500 hover:bg-red-500/10 transition-colors"
                  >
                    <X size={14} className="text-red-500" />
                    <span>Terminate Session</span>
                    <kbd className="ml-auto text-xs bg-muted px-1.5 py-0.5 rounded">Ctrl+Shift+T</kbd>
                  </button>
                  <button
                    onClick={() => {
                      handleResizeTerminal();
                      setShowMoreMenu(false);
                    }}
                    className="w-full flex items-center space-x-2 px-4 py-2 text-sm hover:bg-primary/10 transition-colors"
                  >
                    <Maximize size={14} className="text-teal-500" />
                    <span>Resize Terminal</span>
                    <kbd className="ml-auto text-xs bg-muted px-1.5 py-0.5 rounded">Ctrl+Shift+R</kbd>
                  </button>
                </div>
              )}
            </div>
            

          </div>
        </div>
      </header>

      {/* Resize Terminal Modal */}
      {showResizeModal && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-card border border-border rounded-xl shadow-2xl w-96 p-6">
            <div className="flex items-center justify-between mb-6">
              <h3 className="text-lg font-semibold text-foreground flex items-center gap-2">
                <Monitor size={18} className="text-primary" />
                Resize Terminal
              </h3>
              <Button 
                onClick={cancelResize}
                variant="ghost" 
                size="sm" 
                className="h-8 w-8 p-0 hover:bg-muted"
              >
                ×
              </Button>
            </div>
            
            <div className="space-y-6">
              {/* Current Size Preview */}
              <div className="bg-muted/50 rounded-lg p-4 border border-border">
                <div className="text-sm text-muted-foreground mb-2">Current Size</div>
                <div className="text-2xl font-mono text-primary">
                  {currentSessionInfo.terminalSize.columns} × {currentSessionInfo.terminalSize.rows}
                </div>
              </div>
              
              {/* New Size Controls */}
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-foreground mb-2">
                    Columns (Width)
                  </label>
                  <div className="flex items-center gap-3">
                    <input
                      type="range"
                      min="40"
                      max="200"
                      value={resizeColumns}
                      onChange={(e) => setResizeColumns(parseInt(e.target.value))}
                      className="flex-1 h-2 bg-muted rounded-lg appearance-none cursor-pointer"
                    />
                    <span className="font-mono text-primary bg-muted px-3 py-1 rounded-md min-w-[60px] text-center">
                      {resizeColumns}
                    </span>
                  </div>
                </div>
                
                <div>
                  <label className="block text-sm font-medium text-foreground mb-2">
                    Rows (Height)
                  </label>
                  <div className="flex items-center gap-3">
                    <input
                      type="range"
                      min="10"
                      max="60"
                      value={resizeRows}
                      onChange={(e) => setResizeRows(parseInt(e.target.value))}
                      className="flex-1 h-2 bg-muted rounded-lg appearance-none cursor-pointer"
                    />
                    <span className="font-mono text-primary bg-muted px-3 py-1 rounded-md min-w-[60px] text-center">
                      {resizeRows}
                    </span>
                  </div>
                </div>
              </div>
              
              {/* Action Buttons */}
              <div className="flex gap-3 pt-2">
                <Button 
                  onClick={cancelResize}
                  variant="outline" 
                  className="flex-1"
                >
                  Cancel
                </Button>
                <Button 
                    onClick={() => {
                      setResizeColumns(80);
                      setResizeRows(24);
                    }}
                    variant="outline" 
                    className="flex-1"
                    title="Reset to default size (80×24)"
                  >
                    Reset
                  </Button>
                <Button 
                  onClick={applyResize}
                  className="flex-1 bg-gradient-to-r from-primary to-primary/80 hover:from-primary/90 hover:to-primary/70"
                >
                  Apply Resize
                </Button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Main Content - Simple and Clean */}
      <main className="flex-1 p-6 overflow-hidden">
        {/* Terminal Container */}
        <div className="h-full bg-card/95 backdrop-blur-xl border border-border/50 rounded-xl shadow-2xl overflow-hidden">
          <TerminalComponent 
            ref={terminalRef}
            className="h-full overflow-hidden" 
            onConnectionStatusChange={handleConnectionStatusChange}
          />
        </div>
      </main>
    </div>
  );
}

export default App;