import React, { useEffect } from 'react';
import { List } from 'lucide-react';
import { Button } from './ui/button';

interface Session {
  id: string;
  userId: string;
  shellType: string;
  status: string;
  terminalSize: { columns: number; rows: number };
  workingDirectory: string;
  createdAt: number;
  updatedAt: number;
}

interface SessionListModalProps {
  isOpen: boolean;
  sessions: Session[];
  currentSessionId: string;
  onClose: () => void;
}

export const SessionListModal: React.FC<SessionListModalProps> = ({ isOpen, sessions, currentSessionId, onClose }) => {
  const [filteredSessions, setFilteredSessions] = React.useState<Session[]>([]);
  const [filterStatus, setFilterStatus] = React.useState('all');
  const [sortBy, setSortBy] = React.useState('createdAt-desc');
  const [sessionStats, setSessionStats] = React.useState({ active: 0, terminated: 0 });
  
  // 当sessions、filterStatus或sortBy变化时，更新过滤和排序后的session列表
  useEffect(() => {
    let result = [...sessions];
    
    // 应用状态过滤，忽略大小写
    if (filterStatus !== 'all') {
      result = result.filter(session => session.status?.toUpperCase() === filterStatus);
    }
    
    // 应用排序
    const [field, direction] = sortBy.split('-');
    result.sort((a, b) => {
      const aValue = a[field as keyof Session];
      const bValue = b[field as keyof Session];
      
      if (direction === 'asc') {
        return aValue > bValue ? 1 : -1;
      } else {
        return aValue < bValue ? 1 : -1;
      }
    });
    
    setFilteredSessions(result);
  }, [sessions, filterStatus, sortBy]);
  
  // 当sessions变化时，更新统计信息
  useEffect(() => {
    const activeCount = sessions.filter(session => session.status === 'ACTIVE').length;
    const terminatedCount = sessions.filter(session => session.status === 'TERMINATED').length;
    setSessionStats({ active: activeCount, terminated: terminatedCount });
  }, [sessions]);

  if (!isOpen) return null;

  return (
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
            onClick={onClose}
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
                        {session.id === currentSessionId && (
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
            Total: {filteredSessions.length} sessions
          </div>
          <Button 
            onClick={onClose}
            variant="outline" 
            className="flex-1 max-w-[150px]"
          >
            Close
          </Button>
        </div>
      </div>
    </div>
  );
};