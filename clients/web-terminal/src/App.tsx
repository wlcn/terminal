import React, { useState } from 'react';
import { TerminalComponent } from './components/Terminal';
import { Settings, Maximize2, Minimize2, Power } from 'lucide-react';

function App() {
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [isConnected, setIsConnected] = useState(false);

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
    setIsConnected(!isConnected);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      {/* 顶部导航栏 */}
      <header className="bg-black/20 backdrop-blur-lg border-b border-white/10">
        <div className="container mx-auto px-4 py-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-4">
              <div className="flex items-center space-x-2">
                <div className="w-3 h-3 bg-red-500 rounded-full animate-pulse"></div>
                <h1 className="text-xl font-bold text-white">Web Terminal</h1>
              </div>
              <span className="text-sm text-gray-300">现代化终端应用平台</span>
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
                <span>{isConnected ? '已连接' : '未连接'}</span>
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

      {/* 主内容区域 */}
      <main className="container mx-auto px-4 py-6">
        <div className="bg-black/20 backdrop-blur-lg rounded-xl border border-white/10 overflow-hidden">
          <div className="p-4 border-b border-white/10">
            <div className="flex items-center space-x-2 text-sm text-gray-400">
              <span>终端会话</span>
              <span>•</span>
              <span>bash</span>
              <span>•</span>
              <span>80×24</span>
            </div>
          </div>
          
          <div className="h-[calc(100vh-200px)]">
            <TerminalComponent className="h-full" />
          </div>
        </div>
      </main>

      {/* 底部状态栏 */}
      <footer className="bg-black/20 backdrop-blur-lg border-t border-white/10">
        <div className="container mx-auto px-4 py-2">
          <div className="flex items-center justify-between text-sm text-gray-400">
            <div className="flex items-center space-x-4">
              <span>WebSocket: {isConnected ? '已连接' : '未连接'}</span>
              <span>•</span>
              <span>后端: localhost:8080</span>
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