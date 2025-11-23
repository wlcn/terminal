import React, { useState, useRef } from 'react';
import { TerminalComponent } from './components/Terminal';
import { Settings, Maximize2, Minimize2, Power, RefreshCw, Square, Monitor, Terminal, List, Trash2, Play, Pause } from 'lucide-react';
import { listSessions } from './services/terminalApi';

function App() {
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [isConnected, setIsConnected] = useState(false);
  const terminalRef = useRef<any>(null);

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

  const handleConnectionStatusChange = (connected: boolean) => {
    setIsConnected(connected);
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
    if (terminalRef.current && terminalRef.current.resize) {
      terminalRef.current.resize(120, 30);
    }
  };

  const handleSendCommand = () => {
    const command = prompt('Enter command to send:');
    if (command && terminalRef.current && terminalRef.current.send) {
      terminalRef.current.send(command + '\n');
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
                    onClick={handleSendCommand}
                    className="p-2 text-green-400 hover:text-green-300 transition-colors hover:bg-green-500/20 rounded-lg"
                    title="Send command"
                  >
                    <Play size={18} />
                  </button>
                  
                  <button
                    onClick={handleResizeTerminal}
                    className="p-2 text-purple-400 hover:text-purple-300 transition-colors hover:bg-purple-500/20 rounded-lg"
                    title="Resize terminal (120x30)"
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
              
              <button className="p-2 text-gray-400 hover:text-white transition-colors hover:bg-white/10 rounded-lg" title="Settings">
                <Settings size={20} />
              </button>
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
              <span>bash</span>
              <span>•</span>
              <span>80×24</span>
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