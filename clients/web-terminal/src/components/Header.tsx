import React, { useRef, useEffect } from 'react';
import { Maximize2, Minimize2, Power, RefreshCw, List, X, Maximize } from 'lucide-react';
import { Button } from './ui/button';

interface HeaderProps {
  isConnected: boolean;
  isFullscreen: boolean;
  currentSessionInfo: {
    sessionId: string;
    shellType: string;
    terminalSize: { columns: number; rows: number };
  };
  protocol: 'websocket' | 'webtransport' | 'auto';
  onProtocolChange: (protocol: 'websocket' | 'webtransport' | 'auto') => void;
  onConnect: () => void;
  onToggleFullscreen: () => void;
  onRefresh: () => void;
  onListSessions: () => void;
  onTerminateSession: () => void;
  onResizeTerminal: () => void;
}

export const Header: React.FC<HeaderProps> = ({
  isConnected,
  isFullscreen,
  currentSessionInfo,
  protocol,
  onProtocolChange,
  onConnect,
  onToggleFullscreen,
  onRefresh,
  onListSessions,
  onTerminateSession,
  onResizeTerminal
}) => {
  const [showMoreMenu, setShowMoreMenu] = React.useState(false);
  const moreMenuRef = useRef<HTMLDivElement>(null);
  
  // 点击外部区域关闭下拉菜单
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (moreMenuRef.current && !moreMenuRef.current.contains(event.target as Node)) {
        setShowMoreMenu(false);
      }
    };
    
    // 添加事件监听器
    document.addEventListener('mousedown', handleClickOutside);
    
    // 清理函数
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  return (
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
          
          {/* Protocol selection */}
          <div className="flex items-center space-x-2 ml-4 pl-4 border-l border-border">
            <span className="text-sm text-muted-foreground">PROTOCOL:</span>
            <select 
              className="px-2 py-1 bg-background/80 border border-primary/30 rounded text-sm text-primary focus:border-primary focus:outline-none"
              value={protocol}
              onChange={(e) => onProtocolChange(e.target.value as 'websocket' | 'webtransport' | 'auto')}
              disabled={isConnected}
            >
              <option value="auto">Auto</option>
              <option value="websocket">WebSocket</option>
              <option value="webtransport">WebTransport</option>
            </select>
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
              onClick={onConnect}
              variant={isConnected ? "destructive" : "default"}
              size="sm"
              className={`h-9 w-9 p-0 hover:scale-105 transition-all duration-200 shadow-md ${isConnected 
                ? 'bg-green-500/30 hover:bg-green-500/40 border-green-500/40' 
                : 'bg-gradient-to-br from-blue-500 to-purple-500 hover:from-blue-600 hover:to-purple-600 text-white shadow-lg animate-pulse'}`}
              title={isConnected ? 'Disconnect' : 'Connect to terminal'}
            >
              <Power size={16} className={isConnected ? "text-white" : "text-white"} />
            </Button>
            
            <Button
              onClick={onToggleFullscreen}
              variant="outline"
              size="sm"
              className="h-9 w-9 p-0 bg-background/80 hover:bg-primary/10 hover:scale-105 transition-all duration-200"
              title={isFullscreen ? "Exit fullscreen" : "Enter fullscreen"}
            >
              {isFullscreen ? <Minimize2 size={16} className="text-primary" /> : <Maximize2 size={16} className="text-primary" />}
            </Button>
          </div>
          
          {/* More options dropdown - 作为按钮的直接子元素 */}
          <div className="relative" ref={moreMenuRef}>
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
                    onRefresh();
                    setShowMoreMenu(false);
                  }}
                  className="w-full flex items-center space-x-2 px-4 py-2 text-sm hover:bg-primary/10 transition-colors"
                >
                  <RefreshCw size={14} className="text-orange-500" />
                  <span>Refresh Terminal</span>
                </button>
                <button
                  onClick={() => {
                    onListSessions();
                    setShowMoreMenu(false);
                  }}
                  className="w-full flex items-center space-x-2 px-4 py-2 text-sm hover:bg-primary/10 transition-colors"
                >
                  <List size={14} className="text-purple-500" />
                  <span>List Sessions</span>
                </button>
                <button
                  onClick={() => {
                    onTerminateSession();
                    setShowMoreMenu(false);
                  }}
                  className="w-full flex items-center space-x-2 px-4 py-2 text-sm text-red-500 hover:bg-red-500/10 transition-colors"
                >
                  <X size={14} className="text-red-500" />
                  <span>Terminate Session</span>
                </button>
                <button
                  onClick={() => {
                    onResizeTerminal();
                    setShowMoreMenu(false);
                  }}
                  className="w-full flex items-center space-x-2 px-4 py-2 text-sm hover:bg-primary/10 transition-colors"
                >
                  <Maximize size={14} className="text-teal-500" />
                  <span>Resize Terminal</span>
                </button>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
};