package org.now.terminal.boundedcontext.user.domain

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.now.terminal.boundedcontext.user.domain.valueobjects.Email
import org.now.terminal.boundedcontext.user.domain.valueobjects.PhoneNumber
import org.now.terminal.boundedcontext.user.domain.valueobjects.SessionLimit
import org.now.terminal.boundedcontext.user.domain.valueobjects.UserRole
import org.now.terminal.shared.valueobjects.UserId
import java.time.Instant

/**
 * User Aggregate Root
 */
@Serializable
data class User(
    val id: UserId,
    val username: String,
    val email: Email,
    val phoneNumber: PhoneNumber? = null,
    val role: UserRole,
    val sessionLimit: SessionLimit,
    val passwordHash: String, // Password hash
    val isActive: Boolean = true, // Whether activated
    val isLocked: Boolean = false, // Whether locked
    val loginCount: Int = 0, // Login count
    @Contextual val lastLoginTime: Instant? = null, // Last login time
    @Contextual val createdAt: Instant = Instant.now(), // Creation time
    @Contextual val updatedAt: Instant = Instant.now() // Update time
) {
    
    companion object {
        private const val USERNAME_REQUIRED = "user.username.required"
        private const val USERNAME_LENGTH_INVALID = "user.username.length.invalid"
        private const val USERNAME_CONTAINS_SPACE = "user.username.contains.space"
        private const val USER_ROLE_INVALID = "user.role.invalid"
        private const val SESSION_LIMIT_INVALID = "session.limit.duration.invalid"
        private const val PASSWORD_HASH_REQUIRED = "user.password.hash.required"
        private const val CREATED_AT_REQUIRED = "user.created.at.required"
        private const val UPDATED_AT_REQUIRED = "user.updated.at.required"
    }
    
    init {
        require(username.isNotBlank()) { USERNAME_REQUIRED }
        require(username.length in 3..50) { USERNAME_LENGTH_INVALID }
        require(!username.contains(" ")) { USERNAME_CONTAINS_SPACE }
        require(UserRole.isValidRole(role.name)) { USER_ROLE_INVALID }
        require(sessionLimit.isValid()) { SESSION_LIMIT_INVALID }
        require(passwordHash.isNotBlank()) { PASSWORD_HASH_REQUIRED }
        require(createdAt.isBefore(Instant.now()) || createdAt == Instant.now()) { CREATED_AT_REQUIRED }
        require(updatedAt.isBefore(Instant.now()) || updatedAt == Instant.now()) { UPDATED_AT_REQUIRED }
    }
    
    /**
     * Check if user can create new session
     */
    fun canCreateSession(currentSessions: Int): Boolean {
        return currentSessions < sessionLimit.maxConcurrentSessions
    }
    
    /**
     * Check if user has permission to use specific shell
     */
    fun hasPermissionForShell(shell: String): Boolean {
        return role.allowedShells.contains(shell)
    }
    
    /**
     * Get user's maximum session duration limit
     */
    val maxSessionDuration: Long
        get() = sessionLimit.maxSessionDuration
    
    /**
     * Update email
     */
    fun updateEmail(newEmail: Email): User {
        return this.copy(email = newEmail)
    }
    
    /**
     * Update phone number
     */
    fun updatePhoneNumber(newPhoneNumber: PhoneNumber?): User {
        return this.copy(phoneNumber = newPhoneNumber)
    }
    
    /**
     * Check if phone number is set
     */
    fun hasPhoneNumber(): Boolean = phoneNumber != null
    
    /**
     * Get masked phone number (privacy protection)
     * Returns null if phone number is not set
     */
    fun getMaskedPhoneNumber(): String? {
        return phoneNumber?.getMasked()
    }
    
    /**
     * Get email domain
     */
    fun getEmailDomain(): String = email.getDomain()
    
    /**
     * Get phone carrier information
     */
    fun getPhoneCarrier(): String = phoneNumber?.getCarrier() ?: "user.phone.carrier.unknown"
    
    // Additional business methods
    
    /**
     * Check if user can login
     */
    fun canLogin(): Boolean = isActive && !isLocked
    
    /**
     * Record login
     */
    fun recordLogin(): User {
        return this.copy(
            loginCount = loginCount + 1,
            lastLoginTime = Instant.now(),
            updatedAt = Instant.now()
        )
    }
    
    /**
     * Activate user
     */
    fun activate(): User {
        return this.copy(
            isActive = true,
            updatedAt = Instant.now()
        )
    }
    
    /**
     * Deactivate user
     */
    fun deactivate(): User {
        return this.copy(
            isActive = false,
            updatedAt = Instant.now()
        )
    }
    
    /**
     * Lock user
     */
    fun lock(): User {
        return this.copy(
            isLocked = true,
            updatedAt = Instant.now()
        )
    }
    
    /**
     * Unlock user
     */
    fun unlock(): User {
        return this.copy(
            isLocked = false,
            updatedAt = Instant.now()
        )
    }
    
    /**
     * Reset password
     */
    fun resetPassword(newPasswordHash: String): User {
        return this.copy(
            passwordHash = newPasswordHash,
            updatedAt = Instant.now()
        )
    }
    
    /**
     * Update username
     */
    fun updateUsername(newUsername: String): User {
        return this.copy(
            username = newUsername,
            updatedAt = Instant.now()
        )
    }
    
    /**
     * Update role
     */
    fun updateRole(newRole: UserRole): User {
        return this.copy(
            role = newRole,
            updatedAt = Instant.now()
        )
    }
    
    /**
     * Update session limit
     */
    fun updateSessionLimit(newSessionLimit: SessionLimit): User {
        return this.copy(
            sessionLimit = newSessionLimit,
            updatedAt = Instant.now()
        )
    }
    
    /**
     * Check if password matches
     */
    fun verifyPassword(passwordHash: String): Boolean = this.passwordHash == passwordHash
    
    /**
     * Get user status description
     */
    fun getStatusDescription(): String {
        return when {
            !isActive -> "user.status.inactive"
            isLocked -> "user.status.locked"
            else -> "user.status.active"
        }
    }
    
    /**
     * Check if user is new (created within 24 hours)
     */
    fun isNewUser(): Boolean {
        return createdAt.isAfter(Instant.now().minusSeconds(24 * 60 * 60))
    }
    
    /**
     * Get user activity score (based on login count and last login time)
     */
    fun getActivityScore(): Int {
        val baseScore = loginCount * 10
        val recencyBonus = if (lastLoginTime != null && 
            lastLoginTime.isAfter(Instant.now().minusSeconds(7 * 24 * 60 * 60))) 50 else 0
        return baseScore + recencyBonus
    }
}