import React, { useState, useRef } from 'react';
import { TerminalComponent } from './components/Terminal';
import { Settings, Maximize2, Minimize2, Power, RefreshCw, Square, Monitor, List, Trash2 } from 'lucide-react';
import { listSessions } from './services/terminalApi';
import { Button } from './components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from './components/ui/card';

function App() {
  const terminalRef = useRef<any>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [terminalSettings, setTerminalSettings] = useState({
    fontSize: 14,
    fontFamily: "Consolas, 'Courier New', monospace",
    theme: 'dark',
    autoConnect: false,
    showLineNumbers: false
  });
  
  // 会话信息状态
  const [currentSessionInfo, setCurrentSessionInfo] = useState({
    sessionId: '',
    shellType: 'bash',
    terminalSize: { columns: 120, rows: 30 }
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

  const handleClear = () => {
    if (terminalRef.current && terminalRef.current.clear) {
      terminalRef.current.clear();
    }
  };

  const handleListSessions = async () => {
    try {
      // Get or generate user ID
      let userId = localStorage.getItem('terminal_user_id');
      if (!userId) {
        userId = 'web-terminal-user-' + Date.now();
        localStorage.setItem('terminal_user_id', userId);
      }
      
      const data = await listSessions(userId);
      console.log('Active sessions:', data);
      alert(`Active sessions: ${data.sessions?.length || 0}`);
    } catch (error) {
      console.error('Failed to list sessions:', error);
    }
  };

  const handleTerminateSession = () => {
    if (terminalRef.current && terminalRef.current.terminate) {
      terminalRef.current.terminate('USER_REQUESTED');
    }
  };

  const handleResizeTerminal = () => {
    const columns = prompt('Enter columns (width):', '120');
    const rows = prompt('Enter rows (height):', '30');
    
    if (columns && rows && terminalRef.current && terminalRef.current.resize) {
      const cols = parseInt(columns);
      const rws = parseInt(rows);
      
      if (!isNaN(cols) && !isNaN(rws) && cols > 0 && rws > 0) {
        terminalRef.current.resize(cols, rws);
      } else {
        alert('Please enter valid positive numbers for columns and rows.');
      }
    }
  };

  const handleSettings = () => {
    setShowSettings(!showSettings);
  };

  const updateTerminalSettings = (newSettings: any) => {
    setTerminalSettings(newSettings);
    // Apply settings to terminal if connected
    if (isConnected && terminalRef.current && terminalRef.current.updateSettings) {
      terminalRef.current.updateSettings(newSettings);
    }
  };

  return (
    <div className="h-screen bg-background text-foreground font-sans overflow-hidden">
      {/* Minimal Header */}
      <header className="glass border-b border-border px-4 py-3">
        <div className="flex items-center justify-between max-w-7xl mx-auto">
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-3">
              <div className={`w-3 h-3 rounded-full ${isConnected ? 'status-connected' : 'status-disconnected'}`}></div>
              <h1 className="text-lg font-semibold tracking-tight">
                KT Terminal
              </h1>
            </div>
            
            {/* Session info display */}
            {isConnected && (
              <div className="flex items-center space-x-4 ml-4 pl-4 border-l border-border">
                <div className="flex flex-col text-sm">
                  <span className="font-mono text-muted-foreground">SESSION: {currentSessionInfo.sessionId.slice(0, 8)}...</span>
                  <span className="text-xs text-muted-foreground">SHELL: {currentSessionInfo.shellType}</span>
                </div>
                <div className="flex flex-col text-sm">
                  <span className="text-primary font-mono">SIZE: {currentSessionInfo.terminalSize.columns}×{currentSessionInfo.terminalSize.rows}</span>
                  <span className="text-green-400 text-xs">ACTIVE</span>
                </div>
              </div>
            )}
          </div>
          
          <div className="flex items-center space-x-2">
            {/* Control panel button group */}
            {isConnected && (
              <div className="flex items-center space-x-2 border-r border-border pr-3 mr-3">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleRefresh}
                  className="h-8 w-8 p-0"
                  title="Refresh connection"
                >
                  <RefreshCw size={14} />
                </Button>
                
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleClear}
                  className="h-8 w-8 p-0"
                  title="Clear terminal"
                >
                  <Square size={14} />
                </Button>
                
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleResizeTerminal}
                  className="h-8 w-8 p-0"
                  title="Resize terminal"
                >
                  <Monitor size={14} />
                </Button>
              </div>
            )}
            
            {/* Session management buttons */}
            {isConnected && (
              <div className="flex items-center space-x-2 border-r border-border pr-3 mr-3">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleListSessions}
                  className="h-8 w-8 p-0"
                  title="List sessions"
                >
                  <List size={14} />
                </Button>
                
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleTerminateSession}
                  className="h-8 w-8 p-0"
                  title="Terminate session"
                >
                  <Trash2 size={14} />
                </Button>
              </div>
            )}
            
            {/* Main action buttons */}
            <div className="flex items-center space-x-2">
              <Button
                onClick={handleConnect}
                variant={isConnected ? "destructive" : "default"}
                size="sm"
                className="h-8 w-8 p-0"
                title={isConnected ? 'Disconnect' : 'Connect'}
              >
                <Power size={14} />
              </Button>
              
              <Button
                onClick={toggleFullscreen}
                variant="outline"
                size="sm"
                className="h-8 w-8 p-0"
                title={isFullscreen ? "Exit fullscreen" : "Enter fullscreen"}
              >
                {isFullscreen ? <Minimize2 size={14} /> : <Maximize2 size={14} />}
              </Button>
              
              <Button
                onClick={handleSettings}
                variant="outline"
                size="sm"
                className="h-8 w-8 p-0"
                title="Settings"
              >
                <Settings size={14} />
              </Button>
            </div>

            {showSettings && (
              <div className="absolute top-12 right-4 bg-card border border-border rounded-lg shadow-lg p-4 w-64 z-50">
                <div className="flex justify-between items-center mb-4">
                  <h3 className="text-md font-semibold">Terminal Settings</h3>
                  <Button 
                    onClick={handleSettings}
                    variant="ghost"
                    size="sm"
                    className="h-6 w-6 p-0"
                  >
                    ×
                  </Button>
                </div>
                
                {/* Version info */}
                <div className="mb-3 p-2 bg-muted rounded border border-border">
                  <div className="text-xs text-primary font-mono">Version: v1.0.0</div>
                  <div className="text-xs text-muted-foreground mt-1">KT Terminal Platform</div>
                </div>
                
                <div className="space-y-3">
                  <div>
                    <label className="block text-sm text-muted-foreground mb-1">Font Size</label>
                    <input
                      type="number"
                      value={terminalSettings.fontSize}
                      onChange={(e) => updateTerminalSettings({
                        ...terminalSettings,
                        fontSize: parseInt(e.target.value) || 14
                      })}
                      className="w-full px-2 py-1 bg-background border border-input rounded text-foreground focus:border-primary focus:outline-none"
                      min="8"
                      max="24"
                    />
                  </div>
                  
                  <div>
                    <label className="block text-sm text-muted-foreground mb-1">Font Family</label>
                    <select
                      value={terminalSettings.fontFamily}
                      onChange={(e) => updateTerminalSettings({
                        ...terminalSettings,
                        fontFamily: e.target.value
                      })}
                      className="w-full px-2 py-1 bg-background border border-input rounded text-foreground focus:border-primary focus:outline-none"
                    >
                      <option value="Consolas, 'Courier New', monospace">Consolas</option>
                      <option value="'Courier New', monospace">Courier New</option>
                      <option value="Monaco, 'Menlo', monospace">Monaco</option>
                    </select>
                  </div>
                  
                  <div>
                    <label className="block text-sm text-muted-foreground mb-1">Theme</label>
                    <select
                      value={terminalSettings.theme}
                      onChange={(e) => updateTerminalSettings({
                        ...terminalSettings,
                        theme: e.target.value
                      })}
                      className="w-full px-2 py-1 bg-background border border-input rounded text-foreground focus:border-primary focus:outline-none"
                    >
                      <option value="dark">Dark</option>
                      <option value="light">Light</option>
                    </select>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </header>

      {/* Main Content - Terminal Focus */}
      <main className="flex-1 p-0 overflow-hidden">
        <div className="h-full flex flex-col">
          <Card className="flex-1 m-4 mb-0 border-border bg-card rounded-lg overflow-hidden">
            <CardContent className="p-0 h-full">
              <div className="h-full overflow-hidden">
                <TerminalComponent 
                  ref={terminalRef}
                  className="h-full overflow-hidden" 
                  onConnectionStatusChange={handleConnectionStatusChange}
                />
              </div>
            </CardContent>
          </Card>
          
          {/* Status Bar */}
          <div className="px-4 py-2 text-xs text-muted-foreground border-t border-border bg-background">
            <div className="flex items-center justify-between max-w-7xl mx-auto">
              <span>KT Terminal v1.0 • Enterprise Web Terminal</span>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;