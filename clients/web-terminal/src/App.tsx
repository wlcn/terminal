import React, { useState, useRef } from 'react';
import { TerminalComponent } from './components/Terminal';
import { Maximize2, Minimize2, Power, RefreshCw, List, X, Maximize, Monitor } from 'lucide-react';
import { listSessions } from './services/terminalApi';
import { Button } from './components/ui/button';
import { Card, CardContent, CardHeader, CardTitle } from './components/ui/card';

function App() {
  const terminalRef = useRef<any>(null);
  const [isConnected, setIsConnected] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  
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



  // 添加session列表模态框状态
  const [showSessionList, setShowSessionList] = useState(false);
  const [sessionList, setSessionList] = useState<any[]>([]);
  const [filteredSessions, setFilteredSessions] = useState<any[]>([]);
  const [filterStatus, setFilterStatus] = useState('all');
  const [sortBy, setSortBy] = useState('createdAt-desc');
  
  // 统计session状态数量
  const [sessionStats, setSessionStats] = useState({ active: 0, terminated: 0 });
  
  // 当sessionList变化时，更新统计信息
  React.useEffect(() => {
    const activeCount = sessionList.filter(session => session.status === 'ACTIVE').length;
    const terminatedCount = sessionList.filter(session => session.status === 'TERMINATED').length;
    setSessionStats({ active: activeCount, terminated: terminatedCount });
  }, [sessionList]);
  
  // 当sessionList、filterStatus或sortBy变化时，更新过滤和排序后的session列表
  React.useEffect(() => {
    let result = [...sessionList];
    
    // 应用状态过滤，忽略大小写
    if (filterStatus !== 'all') {
      result = result.filter(session => session.status?.toUpperCase() === filterStatus);
    }
    
    // 应用排序
    const [field, direction] = sortBy.split('-');
    result.sort((a, b) => {
      const aValue = a[field];
      const bValue = b[field];
      
      if (direction === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });
    
    setFilteredSessions(result);
  }, [sessionList, filterStatus, sortBy]);
  
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
      setSessionList(data.sessions);
      setShowSessionList(true);
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
  const moreMenuRef = useRef<HTMLDivElement>(null);
  
  // 点击外部区域关闭下拉菜单
  React.useEffect(() => {
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

  return (
    <div className="h-screen flex flex-col bg-gradient-to-br from-tech-bg-darker via-tech-bg-dark to-tech-secondary text-foreground font-sans overflow-hidden">
      {/* Simple and Reliable Header */}
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
                onClick={handleConnect}
                variant={isConnected ? "destructive" : "default"}
                size="sm"
                className={`h-9 w-9 p-0 hover:scale-105 transition-all duration-200 shadow-md ${
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
                      handleRefresh();
                      setShowMoreMenu(false);
                    }}
                    className="w-full flex items-center space-x-2 px-4 py-2 text-sm hover:bg-primary/10 transition-colors"
                  >
                    <RefreshCw size={14} className="text-orange-500" />
                    <span>Refresh Terminal</span>
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
                  </button>
                </div>
              )}
            </div>
            

          </div>
        </div>
      </header>

      {/* Resize Terminal Modal */}
      {showResizeModal && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-card border border-border rounded-xl shadow-2xl w-96 p-6">
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
      
      {/* Session List Modal */}
      {showSessionList && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm flex items-center justify-center z-50">
          <div className="bg-card border border-border rounded-xl shadow-2xl w-full max-w-3xl max-h-[90vh] overflow-hidden">
            <div className="flex flex-col md:flex-row md:items-center justify-between p-6 border-b border-border gap-3">
              <h3 className="text-lg font-semibold text-foreground flex items-center gap-2">
                <List size={18} className="text-primary" />
                Terminal Sessions
              </h3>
              
              {/* Session Stats */}
              <div className="flex items-center gap-3">
                <div className="flex items-center gap-1">
                  <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-green-500/20 text-green-500">
                    Active
                  </span>
                  <span className="text-sm font-medium">{sessionStats.active}</span>
                </div>
                <div className="flex items-center gap-1">
                  <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-red-500/20 text-red-500">
                    Terminated
                  </span>
                  <span className="text-sm font-medium">{sessionStats.terminated}</span>
                </div>
              </div>
              
              <Button 
                onClick={() => setShowSessionList(false)}
                variant="ghost" 
                size="sm" 
                className="h-8 w-8 p-0 hover:bg-muted"
              >
                ×
              </Button>
            </div>
            
            {/* Filter and Sort Controls */}
            <div className="p-4 border-b border-border bg-muted/20">
              <div className="flex flex-wrap gap-3 items-center">
                <div className="flex items-center gap-2">
                  <label className="text-sm font-medium text-foreground">Status:</label>
                  <select 
                    className="px-3 py-1.5 bg-background border border-input rounded-md text-sm text-foreground focus:border-primary focus:outline-none"
                    value={filterStatus}
                    onChange={(e) => setFilterStatus(e.target.value)}
                  >
                    <option value="all">All</option>
                    <option value="ACTIVE">Active</option>
                    <option value="TERMINATED">Terminated</option>
                  </select>
                </div>
                <div className="flex items-center gap-2">
                  <label className="text-sm font-medium text-foreground">Sort by:</label>
                  <select 
                    className="px-3 py-1.5 bg-background border border-input rounded-md text-sm text-foreground focus:border-primary focus:outline-none"
                    value={sortBy}
                    onChange={(e) => setSortBy(e.target.value)}
                  >
                    <option value="createdAt-desc">Created (Newest First)</option>
                    <option value="createdAt-asc">Created (Oldest First)</option>
                    <option value="updatedAt-desc">Updated (Newest First)</option>
                    <option value="updatedAt-asc">Updated (Oldest First)</option>
                  </select>
                </div>
              </div>
            </div>
            
            <div className="overflow-y-auto max-h-[calc(90vh-160px)] p-6">
              {filteredSessions.length === 0 ? (
                <div className="text-center py-12 text-muted-foreground">
                  <p>No sessions found matching the filter criteria</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {filteredSessions.map((session) => (
                    <div key={session.id} className="bg-muted/50 rounded-lg p-5 border border-border hover:border-primary/30 transition-colors">
                      <div className="flex flex-col md:flex-row md:items-start justify-between gap-4">
                        {/* Session ID and Status */}
                        <div className="flex-1">
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className="font-mono text-primary text-sm">{session.id}</span>
                            <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${session.status === 'ACTIVE' ? 'bg-green-500/20 text-green-500' : 'bg-red-500/20 text-red-500'}`}>
                              {session.status}
                            </span>
                            {session.id === currentSessionInfo.sessionId && (
                              <span className="px-2 py-0.5 rounded-full text-xs font-medium bg-primary/20 text-primary">
                                Current
                              </span>
                            )}
                          </div>
                          
                          {/* Session Details */}
                          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 mt-3 text-sm">
                            <div className="flex items-center gap-2">
                              <span className="text-muted-foreground">Shell:</span>
                              <span>{session.shellType}</span>
                            </div>
                            <div className="flex items-center gap-2">
                              <span className="text-muted-foreground">Size:</span>
                              <span>{session.terminalSize?.columns}×{session.terminalSize?.rows}</span>
                            </div>
                            <div className="flex items-center gap-2">
                              <span className="text-muted-foreground">User ID:</span>
                              <span className="font-mono">{session.userId}</span>
                            </div>
                            <div className="flex items-center gap-2">
                              <span className="text-muted-foreground">Working Dir:</span>
                              <span className="font-mono truncate max-w-[200px]">{session.workingDirectory}</span>
                            </div>
                          </div>
                        </div>
                        
                        {/* Timestamps */}
                        <div className="flex flex-col gap-2 text-xs text-muted-foreground min-w-[150px]">
                          <div>
                            <div className="font-medium text-foreground">Created</div>
                            <div>{new Date(session.createdAt).toLocaleString()}</div>
                          </div>
                          <div>
                            <div className="font-medium text-foreground">Updated</div>
                            <div>{new Date(session.updatedAt).toLocaleString()}</div>
                          </div>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>
            
            <div className="p-6 border-t border-border flex justify-between items-center">
              <div className="text-sm text-muted-foreground">
                Total: {sessionList.length} sessions
              </div>
              <Button 
                onClick={() => setShowSessionList(false)}
                variant="outline" 
                className="flex-1 max-w-[150px]"
              >
                Close
              </Button>
            </div>
          </div>
        </div>
      )}

      {/* Main Content - Simple and Clean */}
      <main className="flex-1 p-6 overflow-hidden">
        {/* Terminal Container */}
        <div className="h-full bg-card/95 backdrop-blur-xl border border-border/50 rounded-xl shadow-2xl overflow-hidden">
          <TerminalComponent 
            ref={terminalRef}
            className="h-full overflow-hidden" 
            onConnectionStatusChange={handleConnectionStatusChange}
          />
        </div>
      </main>
    </div>
  );
}

export default App;