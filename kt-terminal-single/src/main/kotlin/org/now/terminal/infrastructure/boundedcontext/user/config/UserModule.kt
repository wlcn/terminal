package org.now.terminal.infrastructure.boundedcontext.user.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.ktor.ext.get
import org.now.terminal.boundedcontext.user.application.usecase.*
import org.now.terminal.boundedcontext.user.domain.UserRepository
import org.now.terminal.infrastructure.boundedcontext.user.web.controllers.UserController

/**
 * User模块依赖注入配置
 * 配置用户相关依赖到Koin DI容器
 */
val userModule: Module = module {
    
    /**
     * User控制器
     */
    single<UserController> {
        UserController(
            userManagementUseCase = get<UserManagementUseCase>(),
            userQueryUseCase = get<UserQueryUseCase>()
        )
    }
    
    /**
     * User仓储（需要后续实现）
     */
    single<UserRepository> { 
        TODO("实现实际的UserRepository实现")
    }
    
    /**
     * 用户管理用例 - 使用已实现的UseCaseImpl类
     */
    single<UserManagementUseCase> { UserManagementUseCaseImpl(get()) }
    
    /**
     * 用户查询用例 - 使用已实现的UseCaseImpl类
     */
    single<UserQueryUseCase> { UserQueryUseCaseImpl(get()) }
}

/**
 * 用户路由配置
 * 配置Ktor用户端点路由
 */
fun Application.configureUserModule() {
    // 从DI容器获取用户控制器
    val userController = get<UserController>()
    
    // 配置用户路由
    routing {
        configureUserRoutes(userController)
    }
}

/**
 * 配置用户路由
 */
fun Routing.configureUserRoutes(userController: UserController) {
    route("/api/users") {
        // 创建用户
        post {
            val params = call.request.queryParameters
            val user = userController.createUser(
                username = params["username"] ?: "",
                email = params["email"] ?: "",
                passwordHash = params["passwordHash"] ?: "",
                role = params["role"] ?: "USER",
                phoneNumber = params["phoneNumber"],
                sessionLimit = params["sessionLimit"]?.toIntOrNull() ?: 10
            )
            call.respond(HttpStatusCode.Created, user)
        }
        
        // 获取所有用户
        get {
            val users = userController.getUsers()
            call.respond(HttpStatusCode.OK, users)
        }
        
        // 用户搜索
        get("/search") {
            val keyword = call.request.queryParameters["keyword"]
            val users = userController.searchUsers(keyword)
            call.respond(HttpStatusCode.OK, users)
        }
        
        route("/{userId}") {
            // 根据ID获取用户
            get {
                val userId = call.parameters["userId"] ?: ""
                val user = userController.getUserById(userId)
                if (user != null) {
                    call.respond(HttpStatusCode.OK, user)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "用户未找到"))
                }
            }
            
            // 更新用户
            put {
                val userId = call.parameters["userId"] ?: ""
                val params = call.request.queryParameters
                val user = userController.updateUser(
                    userId = userId,
                    username = params["username"],
                    email = params["email"],
                    phoneNumber = params["phoneNumber"]
                )
                call.respond(HttpStatusCode.OK, user)
            }
            
            // 删除用户
            delete {
                val userId = call.parameters["userId"] ?: ""
                userController.deleteUser(userId)
                call.respond(HttpStatusCode.OK, mapOf("message" to "用户删除成功"))
            }
        }
    }
}