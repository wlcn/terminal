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
            <div className="flex items-center space-x-3">
              <div className="flex items-center space-x-2">
                <div className={`w-3 h-3 rounded-full animate-pulse ${
                  isConnected ? 'bg-green-500' : 'bg-red-500'
                }`}></div>
                <h1 className="text-xl font-bold text-white">kt-terminal</h1>
              </div>
            </div>
            
            <div className="flex items-center space-x-2">
              <button
                onClick={handleConnect}
                className={`p-3 rounded-lg transition-all duration-300 ${
                  isConnected 
                    ? 'bg-green-500/20 text-green-400 border border-green-500/30 hover:bg-green-500/30' 
                    : 'bg-red-500/20 text-red-400 border border-red-500/30 hover:bg-red-500/30'
                }`}
                title={isConnected ? '断开连接' : '建立连接'}
              >
                <Power size={18} />
              </button>
              
              <button
                onClick={toggleFullscreen}
                className="p-2 text-gray-400 hover:text-white transition-colors hover:bg-white/10 rounded-lg"
                title={isFullscreen ? '退出全屏' : '全屏'}
              >
                {isFullscreen ? <Minimize2 size={20} /> : <Maximize2 size={20} />}
              </button>
              
              <button className="p-2 text-gray-400 hover:text-white transition-colors hover:bg-white/10 rounded-lg" title="设置">
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
              <span>终端会话</span>
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
              <span>状态: {isConnected ? '已连接' : '未连接'}</span>
              <span>•</span>
              <span>后端: localhost:8080</span>
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