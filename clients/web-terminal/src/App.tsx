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
    <div className="min-h-screen bg-gray-900 text-white">
      {/* Simple header */}
      <header className="bg-gray-800 border-b border-gray-700 px-6 py-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-3">
              {/* Simple status indicator */}
              <div className={`w-3 h-3 rounded-full ${isConnected ? 'bg-green-500' : 'bg-red-500'}`}></div>
              
              <div className="flex flex-col">
                <h1 className="text-xl font-bold">KT Terminal</h1>
                <span className={`text-xs font-mono ${isConnected ? 'text-green-400' : 'text-red-400'}`}>
                  {isConnected ? 'CONNECTED' : 'DISCONNECTED'}
                </span>
              </div>
            </div>
            
            {/* Session info display */}
            {isConnected && (
              <div className="flex items-center space-x-4 ml-4 pl-4 border-l border-gray-600">
                <div className="flex flex-col text-sm">
                  <span className="font-mono">SESSION: {currentSessionInfo.sessionId.slice(0, 8)}...</span>
                  <span className="text-gray-400 text-xs">SHELL: {currentSessionInfo.shellType}</span>
                </div>
                <div className="flex flex-col text-sm">
                  <span className="text-blue-400 font-mono">SIZE: {currentSessionInfo.terminalSize}</span>
                  <span className="text-green-400 text-xs">ACTIVE</span>
                </div>
              </div>
            )}
          </div>
          
          <div className="flex items-center space-x-2">
            {/* Control panel button group */}
            {isConnected && (
              <div className="flex items-center space-x-2 border-r border-gray-600 pr-3 mr-3">
                <button
                  onClick={handleRefresh}
                  className="p-2 rounded bg-gray-700 hover:bg-gray-600 border border-gray-600"
                  title="Refresh connection"
                >
                  <RefreshCw size={16} className="text-blue-400" />
                </button>
                
                <button
                  onClick={handleClear}
                  className="p-2 rounded bg-gray-700 hover:bg-gray-600 border border-gray-600"
                  title="Clear terminal"
                >
                  <Square size={16} className="text-orange-400" />
                </button>
                
                <button
                  onClick={handleResizeTerminal}
                  className="p-2 rounded bg-gray-700 hover:bg-gray-600 border border-gray-600"
                  title="Resize terminal"
                >
                  <Monitor size={16} className="text-purple-400" />
                </button>
              </div>
            )}
            
            {/* Session management buttons */}
            {isConnected && (
              <div className="flex items-center space-x-2 border-r border-gray-600 pr-3 mr-3">
                <button
                  onClick={handleListSessions}
                  className="p-2 rounded bg-gray-700 hover:bg-gray-600 border border-gray-600"
                  title="List sessions"
                >
                  <List size={16} className="text-blue-400" />
                </button>
                
                <button
                  onClick={handleTerminateSession}
                  className="p-2 rounded bg-gray-700 hover:bg-gray-600 border border-gray-600"
                  title="Terminate session"
                >
                  <Trash2 size={16} className="text-red-400" />
                </button>
              </div>
            )}
            
            {/* Main action buttons */}
            <div className="flex items-center space-x-2">
              <button
                onClick={handleConnect}
                className={`px-3 py-2 rounded font-medium ${
                  isConnected 
                    ? 'bg-red-600 hover:bg-red-700' 
                    : 'bg-green-600 hover:bg-green-700'
                }`}
              >
                <div className="flex items-center space-x-2">
                  <Power size={14} />
                  <span>{isConnected ? 'Disconnect' : 'Connect'}</span>
                </div>
              </button>
              
              <button
                onClick={toggleFullscreen}
                className="p-2 rounded bg-gray-700 hover:bg-gray-600 border border-gray-600"
                title={isFullscreen ? "Exit fullscreen" : "Enter fullscreen"}
              >
                {isFullscreen ? <Minimize2 size={14} /> : <Maximize2 size={14} />}
              </button>
              
              <button
                onClick={handleSettings}
                className="p-2 rounded bg-gray-700 hover:bg-gray-600 border border-gray-600"
                title="Settings"
              >
                <Settings size={14} />
              </button>
            </div>

            {showSettings && (
              <div className="absolute top-12 right-4 bg-gray-800 border border-gray-600 rounded-lg shadow-lg p-4 w-64 z-50">
                <div className="flex justify-between items-center mb-4">
                  <h3 className="text-md font-semibold">Terminal Settings</h3>
                  <button 
                    onClick={handleSettings}
                    className="text-gray-400 hover:text-white p-1 rounded"
                  >
                    ×
                  </button>
                </div>
                
                {/* Version info */}
                <div className="mb-3 p-2 bg-gray-700 rounded border border-gray-600">
                  <div className="text-xs text-blue-400 font-mono">Version: v1.0.0</div>
                  <div className="text-xs text-gray-400 mt-1">KT Terminal Platform</div>
                </div>
                
                <div className="space-y-3">
                  <div>
                    <label className="block text-sm text-gray-300 mb-1">Font Size</label>
                    <input
                      type="number"
                      value={terminalSettings.fontSize}
                      onChange={(e) => updateTerminalSettings({
                        ...terminalSettings,
                        fontSize: parseInt(e.target.value) || 14
                      })}
                      className="w-full px-2 py-1 bg-gray-700 border border-gray-600 rounded text-white focus:border-blue-500 focus:outline-none"
                      min="8"
                      max="24"
                    />
                  </div>
                  
                  <div>
                    <label className="block text-sm text-gray-300 mb-1">Font Family</label>
                    <select
                      value={terminalSettings.fontFamily}
                      onChange={(e) => updateTerminalSettings({
                        ...terminalSettings,
                        fontFamily: e.target.value
                      })}
                      className="w-full px-2 py-1 bg-gray-700 border border-gray-600 rounded text-white focus:border-blue-500 focus:outline-none"
                    >
                      <option value="Consolas, 'Courier New', monospace">Consolas</option>
                      <option value="'Courier New', monospace">Courier New</option>
                      <option value="Monaco, 'Menlo', monospace">Monaco</option>
                    </select>
                  </div>
                  
                  <div>
                    <label className="block text-sm text-gray-300 mb-1">Theme</label>
                    <select
                      value={terminalSettings.theme}
                      onChange={(e) => updateTerminalSettings({
                        ...terminalSettings,
                        theme: e.target.value
                      })}
                      className="w-full px-2 py-1 bg-gray-700 border border-gray-600 rounded text-white focus:border-blue-500 focus:outline-none"
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