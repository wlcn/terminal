import React, { useState, useRef } from 'react';
import { TerminalComponent } from './components/Terminal';
import { Settings, Maximize2, Minimize2, Power, RefreshCw, List, X, Maximize } from 'lucide-react';
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
    showLineNumbers: false,
    cursorStyle: 'block',
    scrollback: 1000,
    autoWrap: true
  });
  
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
    <div className="h-screen bg-gradient-to-br from-tech-bg-darker via-tech-bg-dark to-tech-secondary text-foreground font-sans overflow-hidden">
      {/* Enhanced Futuristic Header */}
      <header className="glass border-b border-border/50 px-4 py-3 fixed top-0 left-0 right-0 overflow-hidden z-50">
        {/* Animated background gradient */}
        <div className="absolute inset-0 bg-gradient-to-r from-transparent via-primary/5 to-transparent animate-pulse"></div>
        
        <div className="flex items-center justify-between max-w-7xl mx-auto relative z-10">
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-3">
              <div className={`w-3 h-3 rounded-full relative ${isConnected ? 'status-connected glow-success' : 'status-disconnected'}`}>
                {/* Pulsing effect */}
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
            {/* Enhanced Main action buttons - 主要操作放在最左侧 */}
            <div className="flex items-center space-x-2 border-r border-border/30 pr-3 mr-3 relative">
              {/* Subtle glow effect */}
              <div className="absolute inset-0 bg-gradient-to-r from-primary/20 to-transparent rounded-lg blur-sm"></div>
              
              <Button
              onClick={handleConnect}
              variant={isConnected ? "destructive" : "default"}
              size="sm"
              className={`h-9 w-9 p-0 relative hover:scale-105 transition-all duration-200 shadow-md ${
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
                className="h-9 w-9 p-0 relative bg-background/80 hover:bg-primary/10 hover:scale-105 transition-all duration-200"
                title={isFullscreen ? "Exit fullscreen" : "Enter fullscreen"}
              >
                {isFullscreen ? <Minimize2 size={16} className="text-primary" /> : <Maximize2 size={16} className="text-primary" />}
              </Button>
            </div>
            
            {/* Enhanced Control Menu - 合并次要功能到下拉菜单 */}
            <div className="flex items-center space-x-2 border-r border-border/30 pr-3 mr-3 relative">
              {/* Subtle glow effect */}
              <div className="absolute inset-0 bg-gradient-to-r from-primary/10 to-transparent rounded-lg blur-sm"></div>
              
              {/* Dropdown menu for secondary functions */}
              <div className="relative">
                <Button
                    variant="outline"
                    size="sm"
                    className={`h-9 w-9 p-0 relative transition-all duration-200 hover:scale-105 shadow-sm ${isConnected ? 'bg-primary/20 hover:bg-primary/30 border-primary/30' : 'bg-background/80 opacity-50 cursor-not-allowed'}`}
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
                
                {/* Dropdown content */}
                {isConnected && showMoreMenu && (
                  <div className="absolute right-0 mt-2 w-48 bg-card border border-border rounded-lg shadow-lg z-100 py-1">
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
            
            {/* Settings button - 设置按钮放在最右侧 */}
            <div className="flex items-center space-x-2 relative">
              {/* Subtle glow effect */}
              <div className="absolute inset-0 bg-gradient-to-r from-primary/10 via-accent/5 to-primary/10 rounded-lg blur-sm"></div>
              
              <Button
                onClick={handleSettings}
                variant="outline"
                size="sm"
                className="h-9 w-9 p-0 relative bg-background/80 hover:bg-primary/10 hover:scale-105 transition-all duration-200"
                title="Settings"
              >
                <Settings size={16} className="text-primary" />
              </Button>
            </div>
          </div>
        </div>

        {/* Dropdowns and Modals - 确保这些元素在header内部，作为header的直接子元素 */}
        
        {/* More Options Dropdown */}
        {isConnected && showMoreMenu && (
          <div className="absolute right-8 top-full mt-1 bg-card border border-border rounded-lg shadow-lg z-100 py-1">
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

        {/* Settings Panel */}
        {showSettings && (
          <div className="absolute right-4 top-full mt-1 bg-card/95 backdrop-blur-xl border border-border/50 rounded-xl shadow-2xl p-4 w-72 z-100 glass">
            {/* Header with gradient */}
            <div className="flex justify-between items-center mb-4 pb-3 border-b border-border/30">
              <h3 className="text-md font-semibold bg-gradient-to-r from-primary to-accent bg-clip-text text-transparent">Terminal Settings</h3>
              <Button 
                onClick={handleSettings}
                variant="ghost"
                size="sm"
                className="h-6 w-6 p-0 hover:bg-destructive/20 hover:text-destructive transition-colors"
              >
                ×
              </Button>
            </div>
            
            {/* Enhanced Version info */}
            <div className="mb-4 p-3 bg-gradient-to-br from-primary/10 to-accent/5 rounded-lg border border-primary/20 relative overflow-hidden">
              {/* Animated background */}
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-primary/5 to-transparent animate-pulse"></div>
              <div className="relative z-10">
                <div className="text-xs text-primary font-mono font-semibold">Version: v1.0.0</div>
                <div className="text-xs text-muted-foreground mt-1">KT Terminal Platform</div>
              </div>
            </div>
            
            <div className="space-y-4">
              {/* Display Settings */}
              <div className="border-b border-border/30 pb-3">
                <h4 className="text-xs font-semibold text-primary uppercase tracking-wider mb-3">Display</h4>
                
                <div className="relative">
                  <label className="block text-sm text-muted-foreground mb-2 font-medium">Font Size</label>
                  <input
                    type="number"
                    value={terminalSettings.fontSize}
                    onChange={(e) => updateTerminalSettings({
                      ...terminalSettings,
                      fontSize: parseInt(e.target.value) || 14
                    })}
                    className="w-full px-3 py-2 bg-background/80 border border-input/50 rounded-lg text-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all duration-200 backdrop-blur-sm"
                    min="8"
                    max="24"
                  />
                </div>
                
                <div className="relative mt-4">
                  <label className="block text-sm text-muted-foreground mb-2 font-medium">Font Family</label>
                  <select
                    value={terminalSettings.fontFamily}
                    onChange={(e) => updateTerminalSettings({
                      ...terminalSettings,
                      fontFamily: e.target.value
                    })}
                    className="w-full px-3 py-2 bg-background/80 border border-input/50 rounded-lg text-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all duration-200 backdrop-blur-sm appearance-none"
                  >
                    <option value="Consolas, 'Courier New', monospace">Consolas</option>
                    <option value="'Courier New', monospace">Courier New</option>
                    <option value="Monaco, 'Menlo', monospace">Monaco</option>
                  </select>
                  {/* Custom dropdown arrow */}
                  <div className="absolute right-3 top-1/2 transform -translate-y-1/2 pointer-events-none">
                    <svg className="w-4 h-4 text-muted-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </div>
                </div>
                
                <div className="relative mt-4">
                  <label className="block text-sm text-muted-foreground mb-2 font-medium">Theme</label>
                  <select
                    value={terminalSettings.theme}
                    onChange={(e) => updateTerminalSettings({
                      ...terminalSettings,
                      theme: e.target.value
                    })}
                    className="w-full px-3 py-2 bg-background/80 border border-input/50 rounded-lg text-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all duration-200 backdrop-blur-sm appearance-none"
                  >
                    <option value="dark">Dark</option>
                    <option value="light">Light</option>
                    <option value="high-contrast">High Contrast</option>
                  </select>
                  {/* Custom dropdown arrow */}
                  <div className="absolute right-3 top-1/2 transform -translate-y-1/2 pointer-events-none">
                    <svg className="w-4 h-4 text-muted-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </div>
                </div>
              </div>
              
              {/* Cursor Settings */}
              <div className="border-b border-border/30 pb-3">
                <h4 className="text-xs font-semibold text-primary uppercase tracking-wider mb-3">Cursor</h4>
                
                <div className="relative">
                  <label className="block text-sm text-muted-foreground mb-2 font-medium">Cursor Style</label>
                  <select
                    value={terminalSettings.cursorStyle}
                    onChange={(e) => updateTerminalSettings({
                      ...terminalSettings,
                      cursorStyle: e.target.value
                    })}
                    className="w-full px-3 py-2 bg-background/80 border border-input/50 rounded-lg text-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all duration-200 backdrop-blur-sm appearance-none"
                  >
                    <option value="block">Block</option>
                    <option value="underline">Underline</option>
                    <option value="bar">Bar</option>
                  </select>
                  {/* Custom dropdown arrow */}
                  <div className="absolute right-3 top-1/2 transform -translate-y-1/2 pointer-events-none">
                    <svg className="w-4 h-4 text-muted-foreground" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </div>
                </div>
              </div>
              
              {/* Behavior Settings */}
              <div className="border-b border-border/30 pb-3">
                <h4 className="text-xs font-semibold text-primary uppercase tracking-wider mb-3">Behavior</h4>
                
                <div className="flex items-center justify-between py-2">
                  <label className="text-sm text-foreground font-medium">Auto Connect</label>
                  <input
                    type="checkbox"
                    checked={terminalSettings.autoConnect}
                    onChange={(e) => updateTerminalSettings({
                      ...terminalSettings,
                      autoConnect: e.target.checked
                    })}
                    className="w-4 h-4 rounded text-primary focus:ring-primary transition-colors"
                  />
                </div>
                
                <div className="flex items-center justify-between py-2">
                  <label className="text-sm text-foreground font-medium">Auto Wrap</label>
                  <input
                    type="checkbox"
                    checked={terminalSettings.autoWrap}
                    onChange={(e) => updateTerminalSettings({
                      ...terminalSettings,
                      autoWrap: e.target.checked
                    })}
                    className="w-4 h-4 rounded text-primary focus:ring-primary transition-colors"
                  />
                </div>
                
                <div className="flex items-center justify-between py-2">
                  <label className="text-sm text-foreground font-medium">Show Line Numbers</label>
                  <input
                    type="checkbox"
                    checked={terminalSettings.showLineNumbers}
                    onChange={(e) => updateTerminalSettings({
                      ...terminalSettings,
                      showLineNumbers: e.target.checked
                    })}
                    className="w-4 h-4 rounded text-primary focus:ring-primary transition-colors"
                  />
                </div>
                
                <div className="relative mt-4">
                  <label className="block text-sm text-muted-foreground mb-2 font-medium">Scrollback Lines</label>
                  <input
                    type="number"
                    value={terminalSettings.scrollback}
                    onChange={(e) => updateTerminalSettings({
                      ...terminalSettings,
                      scrollback: parseInt(e.target.value) || 1000
                    })}
                    className="w-full px-3 py-2 bg-background/80 border border-input/50 rounded-lg text-foreground focus:border-primary focus:outline-none focus:ring-2 focus:ring-primary/20 transition-all duration-200 backdrop-blur-sm"
                    min="100"
                    max="10000"
                  />
                </div>
              </div>
            </div>
          </div>
        )}
      </header>

      {/* Resize Terminal Modal */}
      {showResizeModal && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-card border border-border rounded-xl shadow-2xl w-96 p-6 transform transition-all duration-300 scale-100">
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
              
              {/* Preview */}
              <div className="bg-muted/30 rounded-lg p-3 border border-border">
                <div className="text-xs text-muted-foreground mb-1">Preview</div>
                <div className="flex items-center justify-center">
                  <div 
                    className="bg-primary/20 border border-primary/30 rounded p-2"
                    style={{
                      width: `${resizeColumns * 2}px`,
                      height: `${resizeRows * 2}px`
                    }}
                  >
                    <div className="text-xs text-primary font-mono text-center">
                      {resizeColumns} × {resizeRows}
                    </div>
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

      {/* Main Content - Terminal Focus with Enhanced Tech Design */}
      <main className="flex-1 p-0 overflow-hidden relative z-10 mt-16">
        {/* Animated Background Grid */}
        <div className="absolute inset-0 bg-grid-pattern opacity-5 pointer-events-none"></div>
        
        {/* Subtle Glow Effect */}
        <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-accent/5 pointer-events-none"></div>
        
        <div className="h-full flex flex-col relative">
          {/* Enhanced Terminal Container - Enterprise Tech Design */}
           <Card className="flex-1 m-6 mb-0 border-0 bg-gradient-to-br from-card/95 via-card/80 to-card/90 backdrop-blur-2xl overflow-hidden shadow-2xl relative group rounded-none">
             {/* Advanced Border Glow System */}
             <div className="absolute inset-0 bg-gradient-to-r from-primary/30 via-accent/20 to-primary/30 blur-xl opacity-60 pointer-events-none animate-pulse-slow"></div>
             <div className="absolute inset-0 bg-gradient-to-br from-blue-500/10 via-purple-500/5 to-cyan-500/10 blur-md opacity-40 pointer-events-none"></div>
             
             {/* Corner Accents */}
             <div className="absolute top-0 left-0 w-16 h-16 bg-gradient-to-br from-primary/40 to-transparent pointer-events-none"></div>
             <div className="absolute top-0 right-0 w-16 h-16 bg-gradient-to-bl from-accent/40 to-transparent pointer-events-none"></div>
             
             {/* Scan Line Effect */}
             <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-primary/50 to-transparent animate-scan-line pointer-events-none"></div>
             
             {/* Data Stream Particles */}
             <div className="absolute inset-0 overflow-hidden pointer-events-none">
               {[...Array(8)].map((_, i) => (
                 <div 
                   key={i}
                   className="absolute w-0.5 h-4 bg-gradient-to-b from-primary to-accent opacity-30 animate-data-stream"
                   style={{
                     left: `${Math.random() * 100}%`,
                     animationDelay: `${Math.random() * 3}s`,
                     animationDuration: `${1 + Math.random() * 2}s`
                   }}
                 />
               ))}
             </div>
             
             <CardContent className="p-0 h-full relative z-10">
                <div className="h-full overflow-hidden relative">
                  {/* Terminal Content Enhancement */}
                  <div className="absolute inset-0 bg-gradient-to-b from-primary/10 via-transparent to-accent/10 pointer-events-none"></div>
                  <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_center,_var(--tw-gradient-stops))] from-primary/5 via-transparent to-transparent pointer-events-none"></div>
                  
                  <TerminalComponent 
                    ref={terminalRef}
                    className="h-full overflow-hidden" 
                    onConnectionStatusChange={handleConnectionStatusChange}
                  />
                </div>
              </CardContent>
           </Card>
          
          {/* Enhanced Status Bar */}
          <div className="bg-gradient-to-r from-background/95 to-background/90 backdrop-blur-xl border-t border-primary/20 px-4 py-2">
            <div className="flex items-center justify-between text-xs text-muted-foreground">
              <div className="flex items-center space-x-4">
                <div className="flex items-center space-x-1">
                  <div className={`w-2 h-2 rounded-full ${isConnected ? 'bg-green-500 animate-pulse' : 'bg-red-500'}`}></div>
                  <span>{isConnected ? 'Connected' : 'Disconnected'}</span>
                </div>
                {isConnected && (
                  <div className="flex items-center space-x-1">
                    <div className="w-2 h-2 rounded-full bg-blue-500"></div>
                    <span>Performance: Optimal</span>
                  </div>
                )}
                {isConnected && (
                  <div className="flex items-center space-x-1">
                    <div className="w-2 h-2 rounded-full bg-purple-500"></div>
                    <span>Session: {currentSessionInfo.shellType}</span>
                  </div>
                )}
              </div>
              <div className="flex items-center space-x-2">
                <a 
                  href="https://github.com/wlcn/terminal" 
                  target="_blank" 
                  rel="noopener noreferrer"
                  className="hover:text-primary transition-colors"
                >
                  GitHub
                </a>
                <span className="text-primary">•</span>
                <span>by long.wang</span>
              </div>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}

export default App;