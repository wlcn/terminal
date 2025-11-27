// Application Configuration
// Centralized configuration for all environment-specific settings

export const APP_CONFIG = {
  // WebSocket server configuration
WS_SERVER: {
  URL: 'ws://localhost:8080',
  PATH: '/ws'
},
  
  // API server configuration
  API_SERVER: {
    URL: 'http://localhost:8080',
    BASE_PATH: '/api'
  },
  
  // Application settings
  APP: {
    NAME: 'KT Terminal',
    VERSION: 'v1.0.0',
    DESCRIPTION: 'Enterprise Web Terminal'
  }
} as const;

export default APP_CONFIG;