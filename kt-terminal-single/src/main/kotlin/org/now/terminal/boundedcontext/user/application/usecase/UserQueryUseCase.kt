package org.now.terminal.boundedcontext.user.application.usecase

import org.now.terminal.boundedcontext.user.domain.User
import org.now.terminal.boundedcontext.user.domain.UserRepository
import org.now.terminal.boundedcontext.user.domain.valueobjects.*

/**
 * User Query Use Case - Handles user retrieval and search operations
 */
interface UserQueryUseCase {
    
    /**
     * Get user by ID
     */
    suspend fun getUserById(query: GetUserByIdQuery): User?
    
    /**
     * Get user by username
     */
    suspend fun getUserByUsername(query: GetUserByUsernameQuery): User?
    
    /**
     * Get user by email
     */
    suspend fun getUserByEmail(query: GetUserByEmailQuery): User?
    
    /**
     * Get users by role
     */
    suspend fun getUsersByRole(query: GetUsersByRoleQuery): List<User>
    
    /**
     * Search users with pagination
     */
    suspend fun searchUsers(query: SearchUsersQuery): SearchUsersResult
    
    /**
     * Check if user exists
     */
    suspend fun userExists(query: UserExistsQuery): Boolean
    
    /**
     * Get user statistics
     */
    suspend fun getUserStatistics(query: GetUserStatisticsQuery): UserStatistics
}

/**
 * Queries for User Query Use Case
 */
data class GetUserByIdQuery(
    val userId: UserId
)

data class GetUserByUsernameQuery(
    val username: String
)

data class GetUserByEmailQuery(
    val email: Email
)

data class GetUsersByRoleQuery(
    val role: UserRole
)

data class SearchUsersQuery(
    val keyword: String? = null,
    val role: UserRole? = null,
    val page: Int = 0,
    val size: Int = 20
)

data class UserExistsQuery(
    val username: String? = null,
    val email: Email? = null
)

data class GetUserStatisticsQuery(
    val includeInactive: Boolean = false
)

/**
 * Results for User Query Use Case
 */
data class SearchUsersResult(
    val users: List<User>,
    val totalCount: Long,
    val page: Int,
    val size: Int
)

data class UserStatistics(
    val totalUsers: Long,
    val activeUsers: Long,
    val usersByRole: Map<UserRole, Long>,
    val recentUsers: List<User>
)

/**
 * Implementation of User Query Use Case
 */
class UserQueryUseCaseImpl(
    private val userRepository: UserRepository
) : UserQueryUseCase {
    
    override suspend fun getUserById(query: GetUserByIdQuery): User? {
        return userRepository.findById(query.userId)
    }
    
    override suspend fun getUserByUsername(query: GetUserByUsernameQuery): User? {
        return userRepository.findByUsername(query.username)
    }
    
    override suspend fun getUserByEmail(query: GetUserByEmailQuery): User? {
        return userRepository.findByEmail(query.email)
    }
    
    override suspend fun getUsersByRole(query: GetUsersByRoleQuery): List<User> {
        return userRepository.findAllByRole(query.role)
    }
    
    override suspend fun searchUsers(query: SearchUsersQuery): SearchUsersResult {
        // For now, use basic search - implement proper search logic later
        val users = if (query.keyword != null) {
            userRepository.findByUsernameContaining(query.keyword) + 
            userRepository.findByEmailContaining(query.keyword)
        } else {
            userRepository.findAll()
        }
        
        val filteredUsers = if (query.role != null) {
            users.filter { it.role == query.role }
        } else {
            users
        }
        
        val totalCount = filteredUsers.size.toLong()
        val paginatedUsers = filteredUsers
            .drop(query.page * query.size)
            .take(query.size)
        
        return SearchUsersResult(
            users = paginatedUsers,
            totalCount = totalCount,
            page = query.page,
            size = query.size
        )
    }
    
    override suspend fun userExists(query: UserExistsQuery): Boolean {
        return when {
            query.username != null -> userRepository.existsByUsername(query.username)
            query.email != null -> userRepository.existsByEmail(query.email)
            else -> false
        }
    }
    
    override suspend fun getUserStatistics(query: GetUserStatisticsQuery): UserStatistics {
        val totalUsers = userRepository.count()
        val activeUsers = userRepository.countByActiveStatus(true)
        val usersByRole = UserRole.values().associateWith { role ->
            userRepository.countByRole(role)
        }
        val recentUsers = userRepository.findAll().take(10)
        
        return UserStatistics(
            totalUsers = totalUsers,
            activeUsers = activeUsers,
            usersByRole = usersByRole,
            recentUsers = recentUsers
        )
    }
}