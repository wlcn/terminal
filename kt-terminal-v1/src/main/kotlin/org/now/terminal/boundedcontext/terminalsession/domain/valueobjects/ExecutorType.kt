package org.now.terminal.boundedcontext.terminalsession.domain.valueobjects

import kotlinx.serialization.Serializable

/**
 * Executor Type Value Object
 * 
 * Represents the type of command executor (LOCAL, REMOTE, DOCKER, etc.)
 */
@Serializable
enum class ExecutorType {
    /**
     * Execute commands locally on the host system
     */
    LOCAL,
    
    /**
     * Execute commands on a remote system via SSH
     */
    REMOTE,
    
    /**
     * Execute commands inside a Docker container
     */
    DOCKER,
    
    /**
     * Execute commands inside a Kubernetes pod
     */
    KUBERNETES,
    
    /**
     * Execute commands via WebSocket connection
     */
    WEBSOCKET;
    
    companion object {
        /**
         * Get default executor type based on system configuration
         */
        fun getDefault(): ExecutorType = LOCAL
        
        /**
         * Check if executor type supports real-time output streaming
         */
        fun ExecutorType.supportsRealTimeOutput(): Boolean = 
            this == LOCAL || this == REMOTE || this == WEBSOCKET
        
        /**
         * Check if executor type requires network connectivity
         */
        fun ExecutorType.requiresNetwork(): Boolean = 
            this == REMOTE || this == DOCKER || this == KUBERNETES || this == WEBSOCKET
    }
}