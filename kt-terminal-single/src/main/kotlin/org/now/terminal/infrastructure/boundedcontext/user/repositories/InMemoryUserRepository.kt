package org.now.terminal.infrastructure.boundedcontext.user.repositories

import org.now.terminal.boundedcontext.user.domain.User
import org.now.terminal.boundedcontext.user.domain.UserRepository
import org.now.terminal.boundedcontext.user.domain.valueobjects.Email
import org.now.terminal.boundedcontext.user.domain.valueobjects.PhoneNumber
import org.now.terminal.boundedcontext.user.domain.valueobjects.SessionLimit
import org.now.terminal.boundedcontext.user.domain.valueobjects.UserId
import org.now.terminal.boundedcontext.user.domain.valueobjects.UserRole
import org.now.terminal.shared.kernel.pagination.CursorPage
import org.now.terminal.shared.kernel.pagination.OffsetPage
import org.now.terminal.shared.kernel.pagination.PageRequest
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Simple in-memory user repository implementation
 * Uses ConcurrentHashMap for thread-safe operations
 */
class InMemoryUserRepository : UserRepository {
    
    private val users = ConcurrentHashMap<String, User>()
    private val idCounter = AtomicLong(1)
    
    override suspend fun findById(id: UserId): User? {
        return users[id.value]
    }
    
    override suspend fun findByUsername(username: String): User? {
        return users.values.find { it.username == username }
    }
    
    override suspend fun save(user: User) {
        users[user.id.value] = user
    }
    
    override suspend fun delete(id: UserId) {
        users.remove(id.value)
    }
    
    override suspend fun existsById(id: UserId): Boolean {
        return users.containsKey(id.value)
    }
    
    override suspend fun findByEmail(email: Email): User? {
        return users.values.find { it.email == email }
    }
    
    override suspend fun findByPhoneNumber(phoneNumber: PhoneNumber): User? {
        return users.values.find { it.phoneNumber == phoneNumber }
    }
    
    override suspend fun findAll(): List<User> {
        return users.values.toList()
    }
    
    override suspend fun findAllByRole(role: UserRole): List<User> {
        return users.values.filter { it.role == role }
    }
    
    override suspend fun findAllByActiveStatus(active: Boolean): List<User> {
        return users.values.filter { it.isActive == active }
    }
    
    override suspend fun saveAll(users: List<User>) {
        users.forEach { save(it) }
    }
    
    override suspend fun deleteAll(ids: List<UserId>) {
        ids.forEach { delete(it) }
    }
    
    override suspend fun count(): Long {
        return users.size.toLong()
    }
    
    override suspend fun countByRole(role: UserRole): Long {
        return users.values.count { it.role == role }.toLong()
    }
    
    override suspend fun countByActiveStatus(active: Boolean): Long {
        return users.values.count { it.isActive == active }.toLong()
    }
    
    override suspend fun existsByUsername(username: String): Boolean {
        return users.values.any { it.username == username }
    }
    
    override suspend fun existsByEmail(email: Email): Boolean {
        return users.values.any { it.email == email }
    }
    
    override suspend fun existsByPhoneNumber(phoneNumber: PhoneNumber): Boolean {
        return users.values.any { it.phoneNumber == phoneNumber }
    }
    
    override suspend fun findAllByCursor(cursor: String?, pageSize: Int): CursorPage<User> {
        val userList = users.values.toList()
        val startIndex = cursor?.let { cursorValue -> userList.indexOfFirst { user -> user.id.value == cursorValue } } ?: 0
        val endIndex = minOf(startIndex + pageSize, userList.size)
        val pageUsers = userList.subList(startIndex, endIndex)
        
        return CursorPage(
            items = pageUsers,
            nextCursor = if (endIndex < userList.size) pageUsers.last().id.value else null,
            hasNext = endIndex < userList.size,
            totalCount = userList.size.toLong()
        )
    }
    
    override suspend fun findByRoleByCursor(role: UserRole, cursor: String?, pageSize: Int): CursorPage<User> {
        val userList = users.values.filter { it.role == role }.toList()
        val startIndex = cursor?.let { cursorValue -> userList.indexOfFirst { user -> user.id.value == cursorValue } } ?: 0
        val endIndex = minOf(startIndex + pageSize, userList.size)
        val pageUsers = userList.subList(startIndex, endIndex)
        
        return CursorPage(
            items = pageUsers,
            nextCursor = if (endIndex < userList.size) pageUsers.last().id.value else null,
            hasNext = endIndex < userList.size,
            totalCount = userList.size.toLong()
        )
    }
    
    override suspend fun findAllPaginated(page: Int, size: Int): OffsetPage<User> {
        val userList = users.values.toList()
        val offset = page * size
        val endIndex = minOf(offset + size, userList.size)
        val pageUsers = userList.subList(offset, endIndex)
        
        return OffsetPage(
            content = pageUsers,
            pageNumber = page,
            pageSize = size,
            totalElements = userList.size.toLong(),
            totalPages = (userList.size + size - 1) / size
        )
    }
    
    override suspend fun lockAccount(id: UserId) {
        users[id.value]?.let { user ->
            users[user.id.value] = user.copy(isLocked = true)
        }
    }
    
    override suspend fun unlockAccount(id: UserId) {
        users[id.value]?.let { user ->
            users[user.id.value] = user.copy(isLocked = false)
        }
    }
    
    override suspend fun updateUsername(id: UserId, newUsername: String) {
        users[id.value]?.let { user ->
            users[user.id.value] = user.copy(username = newUsername)
        }
    }
    
    override suspend fun updateEmail(id: UserId, newEmail: Email) {
        users[id.value]?.let { user ->
            users[user.id.value] = user.copy(email = newEmail)
        }
    }
    
    override suspend fun updatePhoneNumber(id: UserId, newPhoneNumber: PhoneNumber?) {
        users[id.value]?.let { user ->
            users[user.id.value] = user.copy(phoneNumber = newPhoneNumber)
        }
    }
    
    override suspend fun updateRole(id: UserId, newRole: UserRole) {
        users[id.value]?.let { user ->
            users[user.id.value] = user.copy(role = newRole)
        }
    }
    
    override suspend fun updateSessionLimit(id: UserId, newSessionLimit: SessionLimit) {
        users[id.value]?.let { user ->
            users[user.id.value] = user.copy(sessionLimit = newSessionLimit)
        }
    }
    
    override suspend fun updateActiveStatus(id: UserId, active: Boolean) {
        users[id.value]?.let { user ->
            users[user.id.value] = user.copy(isActive = active)
        }
    }
    
    override suspend fun incrementLoginCount(id: UserId) {
        users[id.value]?.let { user ->
            users[user.id.value] = user.copy(loginCount = user.loginCount + 1)
        }
    }
    
    override suspend fun updateLastLoginTime(id: UserId) {
        users[id.value]?.let { user ->
            users[user.id.value] = user.copy(lastLoginTime = Instant.now())
        }
    }
    
    override suspend fun resetPassword(id: UserId, newPasswordHash: String) {
        users[id.value]?.let { user ->
            users[user.id.value] = user.copy(passwordHash = newPasswordHash)
        }
    }
    
    override suspend fun findByRolePaginated(role: UserRole, page: Int, size: Int): OffsetPage<User> {
        val userList = users.values.filter { it.role == role }.toList()
        val offset = page * size
        val endIndex = minOf(offset + size, userList.size)
        val pageUsers = userList.subList(offset, endIndex)
        
        return OffsetPage(
            content = pageUsers,
            pageNumber = page,
            pageSize = size,
            totalElements = userList.size.toLong(),
            totalPages = (userList.size + size - 1) / size
        )
    }
    
    override suspend fun findPage(request: PageRequest): CursorPage<User> {
        // 直接使用游标分页请求
        val cursorRequest = request as? PageRequest.CursorBased ?: PageRequest.CursorBased()
        return findAllByCursor(cursorRequest.cursor, cursorRequest.pageSize)
    }
    
    override suspend fun findPageByRole(role: UserRole, request: PageRequest): CursorPage<User> {
        // 直接使用游标分页请求
        val cursorRequest = request as? PageRequest.CursorBased ?: PageRequest.CursorBased()
        return findByRoleByCursor(role, cursorRequest.cursor, cursorRequest.pageSize)
    }
    
    override suspend fun findByUsernameContaining(keyword: String): List<User> {
        return users.values.filter { it.username.contains(keyword, ignoreCase = true) }
    }
    
    override suspend fun findByEmailContaining(keyword: String): List<User> {
        return users.values.filter { it.email.value.contains(keyword, ignoreCase = true) }
    }
    
    override suspend fun findUsersWithSessionLimitExceeding(limit: Int): List<User> {
        return users.values.filter { it.sessionLimit.maxConcurrentSessions > limit }
    }
    
    /**
     * Initialize with some sample data for testing
     */
    fun initializeWithSampleData() {
        // Add some sample users for testing
        val sampleUser = User(
            id = UserId("usr_1234567890ab"),
            username = "admin",
            email = Email.create("admin@example.com"),
            passwordHash = "hashed_password_123",
            role = UserRole.ADMIN,
            phoneNumber = PhoneNumber.create("+1234567890"),
            sessionLimit = SessionLimit.default(),
            isActive = true,
            isLocked = false,
            loginCount = 0,
            lastLoginTime = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        val regularUser = User(
            id = UserId("usr_abcdef123456"),
            username = "user",
            email = Email.create("user@example.com"),
            passwordHash = "hashed_password_456",
            role = UserRole.GUEST,
            phoneNumber = PhoneNumber.create("+0987654321"),
            sessionLimit = SessionLimit.default(),
            isActive = true,
            isLocked = false,
            loginCount = 0,
            lastLoginTime = null,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )
        
        // Directly add to the map instead of calling suspend function
        users[sampleUser.id.value] = sampleUser
        users[regularUser.id.value] = regularUser
    }
}