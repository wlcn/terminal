package org.now.terminal.boundedcontext.user.domain.valueobjects

import kotlinx.serialization.Serializable

/**
 * Session Limit Policy Value Object - Contains only user-related business restrictions
 */
@Serializable
data class SessionLimit(
    val maxConcurrentSessions: Int,
    val maxSessionDuration: Long, // milliseconds
    val allowedCommands: Set<String> = emptySet()
) {
    
    companion object {
        private const val CONCURRENT_SESSIONS_INVALID = "session.limit.concurrent.invalid"
        private const val CONCURRENT_SESSIONS_EXCEEDED = "session.limit.concurrent.exceeded"
        private const val SESSION_DURATION_INVALID = "session.limit.duration.invalid"
        private const val SESSION_DURATION_EXCEEDED = "session.limit.duration.exceeded"
        
        /**
         * Get default session limit (for new users)
         */
        fun default(): SessionLimit {
            return SessionLimit(
                maxConcurrentSessions = 3,
                maxSessionDuration = 2 * 60 * 60 * 1000, // 2 hours
                allowedCommands = emptySet()
            )
        }
    }
    
    init {
        require(maxConcurrentSessions > 0) { CONCURRENT_SESSIONS_INVALID }
        require(maxConcurrentSessions <= 100) { CONCURRENT_SESSIONS_EXCEEDED }
        require(maxSessionDuration > 0) { SESSION_DURATION_INVALID }
        require(maxSessionDuration <= 24 * 60 * 60 * 1000) { SESSION_DURATION_EXCEEDED }
    }
    
    /**
     * Check if session limit configuration is valid
     */
    fun isValid(): Boolean {
        return maxConcurrentSessions > 0 && maxSessionDuration > 0
    }
}