import React, { useState, useRef } from 'react';
import { TerminalComponent } from './components/Terminal';
import { Settings, Maximize2, Minimize2, Power, RefreshCw, Square, Monitor, Terminal, List, Trash2, Play, Pause } from 'lucide-react';
import { listSessions } from './services/terminalApi';

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
  
  // 新增：会话信息状态
  const [currentSessionInfo, setCurrentSessionInfo] = useState({
    sessionId: '',
    shellType: 'bash',
    terminalSize: '80×24'
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

  const handleConnectionStatusChange = (connected: boolean, sessionInfo?: { sessionId: string; shellType: string; terminalSize: string }) => {
    setIsConnected(connected);
    
    if (connected && sessionInfo) {
      // 更新会话信息
      setCurrentSessionInfo({
        sessionId: sessionInfo.sessionId,
        shellType: sessionInfo.shellType,
        terminalSize: sessionInfo.terminalSize
      });
    } else {
      // 断开连接时重置会话信息
      setCurrentSessionInfo({
        sessionId: '',
        shellType: 'bash',
        terminalSize: '80×24'
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
    <div className="min-h-screen bg-gradient-to-br from-tech-bg-darker via-tech-bg-dark to-tech-bg-darker relative overflow-hidden">
      {/* Animated background effects */}
      <div className="absolute inset-0 bg-gradient-to-br from-tech-primary/5 via-tech-secondary/3 to-tech-accent/5 animate-pulse"></div>
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-tech-primary/10 via-transparent to-transparent"></div>
      
      {/* Grid pattern overlay */}
      <div className="absolute inset-0 bg-[linear-gradient(rgba(0,255,204,0.03)_1px,transparent_1px),linear-gradient(90deg,rgba(0,255,204,0.03)_1px,transparent_1px)] bg-[size:64px_64px]"></div>
      
      {/* Top navigation bar */}
      <header className="bg-tech-bg-dark/90 backdrop-blur-xl border-b border-tech-border/50 relative z-10 shadow-lg">
        <div className="container mx-auto px-6 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-3">
                {/* Animated status indicator */}
                <div className="relative">
                  <div className={`w-4 h-4 rounded-full ${
                    isConnected 
                      ? 'bg-tech-success shadow-lg shadow-tech-success/50 animate-pulse' 
                      : 'bg-tech-danger shadow-lg shadow-tech-danger/50'
                  }`}></div>
                  <div className={`absolute inset-0 rounded-full animate-ping ${
                    isConnected ? 'bg-tech-success/40' : 'bg-tech-danger/40'
                  }`}></div>
                </div>
                
                <div className="flex flex-col">
                  <h1 className="text-2xl font-bold bg-gradient-to-r from-tech-primary via-tech-secondary to-tech-accent bg-clip-text text-transparent">
                    KT Terminal
                  </h1>
                  <div className="flex items-center space-x-2 text-xs text-tech-primary/70">
                    <span className={`font-mono ${isConnected ? 'text-tech-success' : 'text-tech-danger'}`}>
                      {isConnected ? 'CONNECTED' : 'DISCONNECTED'}
                    </span>
                  </div>
                </div>
              </div>
              
              {/* Session info display */}
              {isConnected && (
                <div className="flex items-center space-x-4 ml-4 pl-4 border-l border-tech-border/30">
                  <div className="flex flex-col text-sm">
                    <span className="text-tech-primary/80 font-mono">SESSION: {currentSessionInfo.sessionId.slice(0, 8)}...</span>
                    <span className="text-tech-secondary/70 text-xs">SHELL: {currentSessionInfo.shellType}</span>
                  </div>
                  <div className="flex flex-col text-sm">
                    <span className="text-tech-accent/80 font-mono">SIZE: {currentSessionInfo.terminalSize}</span>
                    <span className="text-tech-primary/70 text-xs">ACTIVE</span>
                  </div>
                </div>
              )}
            </div>
            
            <div className="flex items-center space-x-3">
              {/* Control panel button group */}
              {isConnected && (
                <div className="flex items-center space-x-2 border-r border-tech-border/30 pr-3 mr-3">
                  <button
                    onClick={handleRefresh}
                    className="group p-3 rounded-xl bg-tech-bg-light/50 border border-tech-border/30 hover:border-tech-primary/50 transition-all duration-300 hover:shadow-lg hover:shadow-tech-primary/20"
                    title="Refresh connection"
                  >
                    <RefreshCw size={18} className="text-tech-primary group-hover:rotate-180 transition-transform duration-500" />
                  </button>
                  
                  <button
                    onClick={handleClear}
                    className="group p-3 rounded-xl bg-tech-bg-light/50 border border-tech-border/30 hover:border-tech-warning/50 transition-all duration-300 hover:shadow-lg hover:shadow-tech-warning/20"
                    title="Clear terminal"
                  >
                    <Square size={18} className="text-tech-warning group-hover:scale-110 transition-transform" />
                  </button>
                  
                  <button
                    onClick={handleResizeTerminal}
                    className="group p-3 rounded-xl bg-tech-bg-light/50 border border-tech-border/30 hover:border-tech-secondary/50 transition-all duration-300 hover:shadow-lg hover:shadow-tech-secondary/20"
                    title="Resize terminal (custom size)"
                  >
                    <Monitor size={18} className="text-tech-secondary group-hover:scale-110 transition-transform" />
                  </button>
                </div>
              )}
              
              {/* Main connect/disconnect button */}
              <button
                onClick={handleConnect}
                className={`group relative p-4 rounded-xl border transition-all duration-300 hover:scale-105 hover:shadow-2xl ${
                  isConnected 
                    ? 'bg-tech-success/10 border-tech-success/30 hover:border-tech-success/50 hover:shadow-tech-success/30' 
                    : 'bg-tech-danger/10 border-tech-danger/30 hover:border-tech-danger/50 hover:shadow-tech-danger/30'
                }`}
                title={isConnected ? 'Disconnect' : 'Connect'}
              >
                <Power size={20} className={`${
                  isConnected ? 'text-tech-success' : 'text-tech-danger'
                } group-hover:scale-110 transition-transform`} />
                
                {/* Glow effect */}
                <div className={`absolute inset-0 rounded-xl blur-md opacity-0 group-hover:opacity-100 transition-opacity ${
                  isConnected ? 'bg-tech-success/20' : 'bg-tech-danger/20'
                }`}></div>
              </button>
              
              <button
                onClick={handleListSessions}
                className="p-2 text-orange-400 hover:text-orange-300 transition-colors hover:bg-orange-500/20 rounded-lg"
                title="View active sessions"
              >
                <List size={18} />
              </button>
              
              <button
                onClick={handleTerminateSession}
                className="p-2 text-red-400 hover:text-red-300 transition-colors hover:bg-red-500/20 rounded-lg"
                title="Terminate current session"
              >
                <Trash2 size={18} />
              </button>
              
              <button
                onClick={toggleFullscreen}
                className="p-2 text-gray-400 hover:text-white transition-colors hover:bg-white/10 rounded-lg"
                title={isFullscreen ? 'Exit fullscreen' : 'Fullscreen'}
              >
                {isFullscreen ? <Minimize2 size={20} /> : <Maximize2 size={20} />}
              </button>
              
              <button 
                onClick={handleSettings}
                className="p-2 text-gray-400 hover:text-white transition-colors hover:bg-white/10 rounded-lg" 
                title="Settings"
              >
                <Settings size={20} />
              </button>

              {showSettings && (
                <div className="absolute top-12 right-4 bg-slate-800 border border-slate-600 rounded-lg shadow-lg p-4 w-80 z-50">
                  <div className="flex justify-between items-center mb-4">
                    <h3 className="text-lg font-semibold text-white">Terminal Settings</h3>
                    <button 
                      onClick={handleSettings}
                      className="text-gray-400 hover:text-white"
                    >
                      ×
                    </button>
                  </div>
                  
                  <div className="space-y-4">
                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-1">Font Size</label>
                      <input
                        type="number"
                        value={terminalSettings.fontSize}
                        onChange={(e) => updateTerminalSettings({
                          ...terminalSettings,
                          fontSize: parseInt(e.target.value) || 14
                        })}
                        className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-white"
                        min="8"
                        max="24"
                      />
                    </div>
                    
                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-1">Font Family</label>
                      <select
                        value={terminalSettings.fontFamily}
                        onChange={(e) => updateTerminalSettings({
                          ...terminalSettings,
                          fontFamily: e.target.value
                        })}
                        className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-white"
                      >
                        <option value="Consolas, 'Courier New', monospace">Consolas</option>
                        <option value="'Courier New', monospace">Courier New</option>
                        <option value="Monaco, 'Menlo', monospace">Monaco</option>
                        <option value="'Fira Code', monospace">Fira Code</option>
                      </select>
                    </div>
                    
                    <div>
                      <label className="block text-sm font-medium text-gray-300 mb-1">Theme</label>
                      <select
                        value={terminalSettings.theme}
                        onChange={(e) => updateTerminalSettings({
                          ...terminalSettings,
                          theme: e.target.value
                        })}
                        className="w-full px-3 py-2 bg-slate-700 border border-slate-600 rounded text-white"
                      >
                        <option value="dark">Dark</option>
                        <option value="light">Light</option>
                        <option value="solarized">Solarized</option>
                      </select>
                    </div>
                    
                    <div className="flex items-center">
                      <input
                        type="checkbox"
                        checked={terminalSettings.autoConnect}
                        onChange={(e) => updateTerminalSettings({
                          ...terminalSettings,
                          autoConnect: e.target.checked
                        })}
                        className="mr-2"
                      />
                      <label className="text-sm text-gray-300">Auto connect on page load</label>
                    </div>
                    
                    <div className="flex items-center">
                      <input
                        type="checkbox"
                        checked={terminalSettings.showLineNumbers}
                        onChange={(e) => updateTerminalSettings({
                          ...terminalSettings,
                          showLineNumbers: e.target.checked
                        })}
                        className="mr-2"
                      />
                      <label className="text-sm text-gray-300">Show line numbers</label>
                    </div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </header>

      {/* Main content area */}
      <main className="container mx-auto px-4 py-6">
        <div className="bg-black/20 backdrop-blur-lg rounded-xl border border-white/10 overflow-hidden">
          <div className="p-3 border-b border-white/10">
            <div className="flex items-center space-x-2 text-sm text-gray-400">
              <span>Terminal Session</span>
              <span>•</span>
              <span>{currentSessionInfo.shellType}</span>
              <span>•</span>
              <span>{currentSessionInfo.terminalSize}</span>
              {currentSessionInfo.sessionId && (
                <>
                  <span>•</span>
                  <span className="text-xs text-gray-500">ID: {currentSessionInfo.sessionId.substring(0, 8)}...</span>
                </>
              )}
            </div>
          </div>
          
          <div className="h-[calc(100vh-180px)]">
            <TerminalComponent 
              ref={terminalRef}
              className="h-full" 
              onConnectionStatusChange={handleConnectionStatusChange}
            />
          </div>
        </div>
      </main>

      {/* Footer status bar */}
      <footer className="bg-black/20 backdrop-blur-lg border-t border-white/10">
        <div className="container mx-auto px-4 py-2">
          <div className="flex items-center justify-between text-sm text-gray-400">
            <div className="flex items-center space-x-4">
              <span>Status: {isConnected ? 'Connected' : 'Disconnected'}</span>
              <span>•</span>
              <span>Backend: localhost:8080</span>
            </div>
            <div>
              <span>kt-terminal v1.0</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;