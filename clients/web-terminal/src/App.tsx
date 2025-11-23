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
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      {/* Top navigation bar */}
      <header className="bg-black/20 backdrop-blur-lg border-b border-white/10">
        <div className="container mx-auto px-4 py-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="flex items-center space-x-2">
                <div className={`w-3 h-3 rounded-full animate-pulse ${
                  isConnected ? 'bg-green-500' : 'bg-red-500'
                }`}></div>
                <h1 className="text-xl font-bold text-white">kt-terminal</h1>
              </div>
            </div>
            
            <div className="flex items-center space-x-2">
              {/* Control panel button group */}
              {isConnected && (
                <div className="flex items-center space-x-1 border-r border-white/20 pr-2 mr-2">
                  <button
                    onClick={handleRefresh}
                    className="p-2 text-blue-400 hover:text-blue-300 transition-colors hover:bg-blue-500/20 rounded-lg"
                    title="Refresh connection"
                  >
                    <RefreshCw size={18} />
                  </button>
                  
                  <button
                    onClick={handleClear}
                    className="p-2 text-yellow-400 hover:text-yellow-300 transition-colors hover:bg-yellow-500/20 rounded-lg"
                    title="Clear terminal"
                  >
                    <Square size={18} />
                  </button>
                  

                  
                  <button
                    onClick={handleResizeTerminal}
                    className="p-2 text-purple-400 hover:text-purple-300 transition-colors hover:bg-purple-500/20 rounded-lg"
                    title="Resize terminal (custom size)"
                  >
                    <Monitor size={18} />
                  </button>
                </div>
              )}
              
              <button
                onClick={handleConnect}
                className={`p-3 rounded-lg transition-all duration-300 ${
                  isConnected 
                    ? 'bg-green-500/20 text-green-400 border border-green-500/30 hover:bg-green-500/30' 
                    : 'bg-red-500/20 text-red-400 border border-red-500/30 hover:bg-red-500/30'
                }`}
                title={isConnected ? 'Disconnect' : 'Connect'}
              >
                <Power size={18} />
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