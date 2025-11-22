import React, { useState, useRef } from 'react';
import { TerminalComponent } from './components/Terminal';
import { Settings, Maximize2, Minimize2, Power } from 'lucide-react';

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
      // 断开连接
      if (terminalRef.current && terminalRef.current.disconnect) {
        terminalRef.current.disconnect();
      }
      setIsConnected(false);
    } else {
      // 建立连接
      if (terminalRef.current && terminalRef.current.connect) {
        terminalRef.current.connect();
      }
      setIsConnected(true);
    }
  };

  const handleConnectionStatusChange = (connected: boolean) => {
    setIsConnected(connected);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      {/* Top navigation bar */}
      <header className="bg-black/20 backdrop-blur-lg border-b border-white/10">
        <div className="container mx-auto px-4 py-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2">
                <div className="w-3 h-3 bg-red-500 rounded-full animate-pulse"></div>
                <h1 className="text-xl font-bold text-white">Web Terminal</h1>
              </div>
              <span className="text-sm text-gray-300">Modern Terminal Application Platform</span>
            </div>
            
            <div className="flex items-center space-x-2">
              <button
                onClick={handleConnect}
                className={`flex items-center space-x-2 px-4 py-2 rounded-lg transition-colors ${
                  isConnected 
                    ? 'bg-green-500/20 text-green-400 border border-green-500/30' 
                    : 'bg-red-500/20 text-red-400 border border-red-500/30'
                }`}
              >
                <Power size={16} />
                <span>{isConnected ? 'Connected' : 'Disconnected'}</span>
              </button>
              
              <button
                onClick={toggleFullscreen}
                className="p-2 text-gray-400 hover:text-white transition-colors"
              >
                {isFullscreen ? <Minimize2 size={20} /> : <Maximize2 size={20} />}
              </button>
              
              <button className="p-2 text-gray-400 hover:text-white transition-colors">
                <Settings size={20} />
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main content area */}
      <main className="container mx-auto px-4 py-6">
        <div className="bg-black/20 backdrop-blur-lg rounded-xl border border-white/10 overflow-hidden">
          <div className="p-4 border-b border-white/10">
            <div className="flex items-center space-x-2 text-sm text-gray-400">
              <span>Terminal Session</span>
              <span>•</span>
              <span>bash</span>
              <span>•</span>
              <span>80×24</span>
            </div>
          </div>
          
          <div className="h-[calc(100vh-200px)]">
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
              <span>WebSocket: {isConnected ? 'Connected' : 'Disconnected'}</span>
              <span>•</span>
              <span>Backend: localhost:8080</span>
            </div>
            <div>
              <span>Web Terminal v1.0.0</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;