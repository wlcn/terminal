/**
 * Terminal Management API Service
 * Handles session management, size adjustment operations, separated from WebSocket
 */

import { APP_CONFIG } from '../config/appConfig';

const API_BASE_URL = APP_CONFIG.API_SERVER.BASE_PATH;

/**
 * Create new session
 */
export const createSession = async (userId?: string): Promise<{ sessionId: string; status: string; shellType: string }> => {
  try {
    let url = `${API_BASE_URL}/sessions`;
    if (userId) {
      url += `?userId=${encodeURIComponent(userId)}`;
    }
    
    const response = await fetch(url, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
    });
    
    if (!response.ok) {
      throw new Error(`Failed to create session: ${response.statusText}`);
    }
    
    const responseData = await response.json();
    console.log('📡 API Response:', responseData);
    
    return responseData;
  } catch (error) {
    console.error('❌ Failed to create session:', error);
    throw error;
  }
};

/**
 * Resize terminal
 */
export const resizeTerminal = async (
  sessionId: string, 
  columns: number, 
  rows: number
): Promise<{ sessionId: string; columns: number; rows: number; status: string }> => {
  try {
    const response = await fetch(`${API_BASE_URL}/sessions/${sessionId}/resize`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ columns, rows }),
    });
    
    if (!response.ok) {
      throw new Error(`Failed to resize terminal: ${response.statusText}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('❌ Failed to resize terminal:', error);
    throw error;
  }
};

/**
 * Terminate session
 */
export const terminateSession = async (
  sessionId: string, 
  reason?: string
): Promise<{ sessionId: string; reason: string; status: string }> => {
  try {
    let url = `${API_BASE_URL}/sessions/${sessionId}`;
    if (reason) {
      url += `?reason=${encodeURIComponent(reason)}`;
    }
    
    const response = await fetch(url, {
      method: 'DELETE',
    });
    
    if (!response.ok) {
      throw new Error(`Failed to terminate session: ${response.statusText}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('❌ Failed to terminate session:', error);
    throw error;
  }
};

/**
 * Get active session list
 */
export const listSessions = async (userId?: string): Promise<{ sessions: string[]; count: number }> => {
  try {
    let url = `${API_BASE_URL}/sessions`;
    if (userId) {
      url += `?userId=${encodeURIComponent(userId)}`;
    }
    
    const response = await fetch(url);
    
    if (!response.ok) {
      throw new Error(`Failed to list sessions: ${response.statusText}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('❌ Failed to list sessions:', error);
    throw error;
  }
};

/**
 * Check if session is active
 */
export const checkSessionActive = async (sessionId: string, userId?: string): Promise<boolean> => {
  try {
    const sessions = await listSessions(userId);
    return sessions.sessions.includes(sessionId);
  } catch (error) {
    console.error('❌ Failed to check session status:', error);
    return false;
  }
};