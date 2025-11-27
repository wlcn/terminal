package org.now.terminal.boundedcontext.user.domain

import org.now.terminal.boundedcontext.user.domain.valueobjects.Email
import org.now.terminal.boundedcontext.user.domain.valueobjects.PhoneNumber
import org.now.terminal.boundedcontext.user.domain.valueobjects.SessionLimit
import org.now.terminal.shared.valueobjects.UserId
import org.now.terminal.boundedcontext.user.domain.valueobjects.UserRole
import org.now.terminal.shared.kernel.pagination.CursorPage
import org.now.terminal.shared.kernel.pagination.OffsetPage
import org.now.terminal.shared.kernel.pagination.PageRequest

/**
 * User Repository Interface
 */
interface UserRepository {
    
    // Basic CRUD operations
    suspend fun findById(id: UserId): User?
    suspend fun findByUsername(username: String): User?
    suspend fun save(user: User)
    suspend fun delete(id: UserId)
    suspend fun existsById(id: UserId): Boolean
    
    // Query operations
    suspend fun findByEmail(email: Email): User?
    suspend fun findByPhoneNumber(phoneNumber: PhoneNumber): User?
    suspend fun findAll(): List<User>
    suspend fun findAllByRole(role: UserRole): List<User>
    suspend fun findAllByActiveStatus(active: Boolean): List<User>
    
    // Batch operations
    suspend fun saveAll(users: List<User>)
    suspend fun deleteAll(ids: List<UserId>)
    
    // Statistics operations
    suspend fun count(): Long
    suspend fun countByRole(role: UserRole): Long
    suspend fun countByActiveStatus(active: Boolean): Long
    
    // Existence checks
    suspend fun existsByUsername(username: String): Boolean
    suspend fun existsByEmail(email: Email): Boolean
    suspend fun existsByPhoneNumber(phoneNumber: PhoneNumber): Boolean
    
    // Modern pagination queries - support multiple pagination strategies
    
    // Cursor-based pagination - suitable for infinite scrolling
    suspend fun findAllByCursor(cursor: String?, pageSize: Int): CursorPage<User>
    suspend fun findByRoleByCursor(role: UserRole, cursor: String?, pageSize: Int): CursorPage<User>
    
    // Offset-based pagination - traditional but stable
    suspend fun findAllPaginated(page: Int, size: Int): OffsetPage<User>
    suspend fun findByRolePaginated(role: UserRole, page: Int, size: Int): OffsetPage<User>
    
    // Unified pagination interface - automatically selects based on request type
    suspend fun findPage(request: PageRequest): CursorPage<User>
    suspend fun findPageByRole(role: UserRole, request: PageRequest): CursorPage<User>
    
    // Advanced queries
    suspend fun findByUsernameContaining(keyword: String): List<User>
    suspend fun findByEmailContaining(keyword: String): List<User>
    suspend fun findUsersWithSessionLimitExceeding(limit: Int): List<User>
    
    // Update operations
    suspend fun updateUsername(id: UserId, newUsername: String)
    suspend fun updateEmail(id: UserId, newEmail: Email)
    suspend fun updatePhoneNumber(id: UserId, newPhoneNumber: PhoneNumber?)
    suspend fun updateRole(id: UserId, newRole: UserRole)
    suspend fun updateSessionLimit(id: UserId, newSessionLimit: SessionLimit)
    suspend fun updateActiveStatus(id: UserId, active: Boolean)
    
    // Business-specific operations
    suspend fun incrementLoginCount(id: UserId)
    suspend fun updateLastLoginTime(id: UserId)
    suspend fun resetPassword(id: UserId, newPasswordHash: String)
    suspend fun lockAccount(id: UserId)
    suspend fun unlockAccount(id: UserId)
}