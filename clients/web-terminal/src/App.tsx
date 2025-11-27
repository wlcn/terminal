import React, { useState, useRef } from 'react';
import { TerminalComponent } from './components/Terminal';
import { Settings, Maximize2, Minimize2, Power, RefreshCw, Monitor, List, Trash2, X, Maximize } from 'lucide-react';
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
  const [currentSessionInfo, setCurrentSessionInfo] = useState<{
    sessionId: string;
    shellType: string;
    terminalSize: { columns: number; rows: number };
  }>({
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
  const [resizeColumns, setResizeColumns] = useState(120);
  const [resizeRows, setResizeRows] = useState(30);

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
      <header className="glass border-b border-border/50 px-4 py-3 relative overflow-hidden">
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
            
            {/* Enhanced Session management buttons - 会话管理放在中间，保持位置稳定 */}
            <div className="flex items-center space-x-2 border-r border-border/30 pr-3 mr-3 relative">
              {/* Subtle glow effect */}
              <div className="absolute inset-0 bg-gradient-to-r from-accent/20 to-transparent rounded-lg blur-sm"></div>
              
              <Button
                variant="outline"
                size="sm"
                onClick={handleListSessions}
                className={`h-9 w-9 p-0 relative transition-all duration-200 hover:scale-105 shadow-sm ${
                  isConnected 
                    ? 'bg-purple-500/20 hover:bg-purple-500/30 border-purple-500/30' 
                    : 'bg-background/80 opacity-50 cursor-not-allowed'
                }`}
                title={isConnected ? "List sessions" : "Connect to enable"}
                disabled={!isConnected}
              >
                <List size={16} className={isConnected ? "text-purple-500" : "text-muted-foreground"} />
              </Button>
              
              <Button
                variant="outline"
                size="sm"
                onClick={handleTerminateSession}
                className={`h-9 w-9 p-0 relative transition-all duration-200 hover:scale-105 shadow-sm ${
                  isConnected 
                    ? 'bg-red-500/20 hover:bg-red-500/30 border-red-500/30' 
                    : 'bg-background/80 opacity-50 cursor-not-allowed'
                }`}
                title={isConnected ? "Terminate session" : "Connect to enable"}
                disabled={!isConnected}
              >
                <X size={16} className={isConnected ? "text-red-500" : "text-muted-foreground"} />
              </Button>
            </div>
            
            {/* Enhanced Control buttons - 控制按钮放在右侧，保持位置稳定 */}
            <div className="flex items-center space-x-2 border-r border-border/30 pr-3 mr-3 relative">
              {/* Subtle glow effect */}
              <div className="absolute inset-0 bg-gradient-to-r from-primary/10 to-transparent rounded-lg blur-sm"></div>
              
              <Button
                variant="outline"
                size="sm"
                onClick={handleRefresh}
                className={`h-9 w-9 p-0 relative transition-all duration-200 hover:scale-105 shadow-sm ${
                  isConnected 
                    ? 'bg-orange-500/20 hover:bg-orange-500/30 border-orange-500/30' 
                    : 'bg-background/80 opacity-50 cursor-not-allowed'
                }`}
                title={isConnected ? "Refresh terminal" : "Connect to enable"}
                disabled={!isConnected}
              >
                <RefreshCw size={16} className={isConnected ? "text-orange-500" : "text-muted-foreground"} />
              </Button>
              
              <Button
                variant="outline"
                size="sm"
                onClick={handleClear}
                className={`h-9 w-9 p-0 relative transition-all duration-200 hover:scale-105 shadow-sm ${
                  isConnected 
                    ? 'bg-gray-500/20 hover:bg-gray-500/30 border-gray-500/30' 
                    : 'bg-background/80 opacity-50 cursor-not-allowed'
                }`}
                title={isConnected ? "Clear terminal" : "Connect to enable"}
                disabled={!isConnected}
              >
                <Trash2 size={16} className={isConnected ? "text-gray-500" : "text-muted-foreground"} />
              </Button>
              
              <Button
                variant="outline"
                size="sm"
                onClick={handleResizeTerminal}
                className={`h-9 w-9 p-0 relative transition-all duration-200 hover:scale-105 shadow-sm ${
                  isConnected 
                    ? 'bg-teal-500/20 hover:bg-teal-500/30 border-teal-500/30' 
                    : 'bg-background/80 opacity-50 cursor-not-allowed'
                }`}
                title={isConnected ? "Resize terminal" : "Connect to enable"}
                disabled={!isConnected}
              >
                <Maximize size={16} className={isConnected ? "text-teal-500" : "text-muted-foreground"} />
              </Button>
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

            {showSettings && (
              <div className="absolute top-12 right-4 bg-card/95 backdrop-blur-xl border border-border/50 rounded-xl shadow-2xl p-4 w-72 z-50 glass">
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
                  
                  <div className="relative">
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
                  
                  <div className="relative">
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
              </div>
            )}
          </div>
        </div>
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
                    setResizeColumns(120);
                    setResizeRows(30);
                  }}
                  variant="outline" 
                  className="flex-1"
                  title="Reset to default size (120×30)"
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
      <main className="flex-1 p-0 overflow-hidden relative">
        {/* Animated Background Grid */}
        <div className="absolute inset-0 bg-grid-pattern opacity-5 pointer-events-none"></div>
        
        {/* Subtle Glow Effect */}
        <div className="absolute inset-0 bg-gradient-to-br from-primary/5 via-transparent to-accent/5 pointer-events-none"></div>
        
        <div className="h-full flex flex-col relative z-10">
          {/* Enhanced Terminal Container - Enterprise Tech Design */}
           <Card className="flex-1 m-6 mb-0 border-0 bg-gradient-to-br from-card/95 via-card/80 to-card/90 backdrop-blur-2xl overflow-hidden shadow-2xl relative group">
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
                  href="https://github.com/wlcn/kt-terminal" 
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