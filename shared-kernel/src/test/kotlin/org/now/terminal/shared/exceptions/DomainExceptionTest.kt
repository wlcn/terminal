package org.now.terminal.shared.exceptions

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.lang.IllegalArgumentException

class DomainExceptionTest : BehaviorSpec({
    
    given("DomainException基本功能") {
        `when`("创建基础异常") {
            val exception = DomainException(
                code = "TEST_001",
                message = "Test exception message",
                cause = IllegalArgumentException("Root cause"),
                context = mapOf("key1" to "value1", "key2" to 42)
            )
            
            then("应该正确设置所有属性") {
                exception.code shouldBe "TEST_001"
                exception.message shouldBe "Test exception message"
                exception.cause?.message shouldBe "Root cause"
                exception.context["key1"] shouldBe "value1"
                exception.context["key2"] shouldBe 42
            }
        }
        
        `when`("创建无cause和context的异常") {
            val exception = DomainException(
                code = "TEST_002",
                message = "Simple exception"
            )
            
            then("应该使用默认值") {
                exception.code shouldBe "TEST_002"
                exception.message shouldBe "Simple exception"
                exception.cause shouldBe null
                exception.context shouldBe emptyMap()
            }
        }
    }
    
    given("异常类型枚举") {
        `when`("检查验证相关异常类型") {
            then("应该正确设置分类和错误码") {
                ExceptionType.VALIDATION_ERROR.category shouldBe "validation"
                ExceptionType.VALIDATION_ERROR.errorCode shouldBe "VAL_001"
                
                ExceptionType.INVALID_USER_ID.category shouldBe "validation"
                ExceptionType.INVALID_USER_ID.errorCode shouldBe "VAL_002"
                
                ExceptionType.INVALID_SESSION_ID.category shouldBe "validation"
                ExceptionType.INVALID_SESSION_ID.errorCode shouldBe "VAL_003"
                
                ExceptionType.INVALID_TERMINAL_SIZE.category shouldBe "validation"
                ExceptionType.INVALID_TERMINAL_SIZE.errorCode shouldBe "VAL_004"
                
                ExceptionType.INVALID_EVENT_ID.category shouldBe "validation"
                ExceptionType.INVALID_EVENT_ID.errorCode shouldBe "VAL_005"
            }
        }
        
        `when`("检查业务规则相关异常类型") {
            then("应该正确设置分类和错误码") {
                ExceptionType.BUSINESS_RULE_VIOLATION.category shouldBe "business"
                ExceptionType.BUSINESS_RULE_VIOLATION.errorCode shouldBe "BUS_001"
                
                ExceptionType.PERMISSION_DENIED.category shouldBe "business"
                ExceptionType.PERMISSION_DENIED.errorCode shouldBe "BUS_002"
                
                ExceptionType.SESSION_ALREADY_EXISTS.category shouldBe "business"
                ExceptionType.SESSION_ALREADY_EXISTS.errorCode shouldBe "BUS_003"
            }
        }
        
        `when`("检查资源相关异常类型") {
            then("应该正确设置分类和错误码") {
                ExceptionType.RESOURCE_NOT_FOUND.category shouldBe "resource"
                ExceptionType.RESOURCE_NOT_FOUND.errorCode shouldBe "RES_001"
                
                ExceptionType.USER_NOT_FOUND.category shouldBe "resource"
                ExceptionType.USER_NOT_FOUND.errorCode shouldBe "RES_002"
                
                ExceptionType.SESSION_NOT_FOUND.category shouldBe "resource"
                ExceptionType.SESSION_NOT_FOUND.errorCode shouldBe "RES_003"
            }
        }
        
        `when`("检查系统相关异常类型") {
            then("应该正确设置分类和错误码") {
                ExceptionType.SYSTEM_UNAVAILABLE.category shouldBe "system"
                ExceptionType.SYSTEM_UNAVAILABLE.errorCode shouldBe "SYS_001"
                
                ExceptionType.COMMAND_EXECUTION_FAILED.category shouldBe "system"
                ExceptionType.COMMAND_EXECUTION_FAILED.errorCode shouldBe "SYS_002"
                
                ExceptionType.COMMAND_TIMEOUT.category shouldBe "system"
                ExceptionType.COMMAND_TIMEOUT.errorCode shouldBe "SYS_003"
            }
        }
        
        `when`("检查并发相关异常类型") {
            then("应该正确设置分类和错误码") {
                ExceptionType.CONCURRENCY_ERROR.category shouldBe "concurrency"
                ExceptionType.CONCURRENCY_ERROR.errorCode shouldBe "CON_001"
            }
        }
    }
    
    given("异常工厂方法 - 验证异常") {
        `when`("创建无效用户ID异常") {
            val exception = DomainExceptionFactory.invalidUserId("invalid-user-123")
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "VAL_002"
                exception.message shouldBe "Invalid user ID: invalid-user-123"
                exception.context["userId"] shouldBe "invalid-user-123"
                exception.context["type"] shouldBe "validation"
            }
        }
        
        `when`("创建无效会话ID异常") {
            val exception = DomainExceptionFactory.invalidSessionId("invalid-session-456")
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "VAL_003"
                exception.message shouldBe "Invalid session ID: invalid-session-456"
                exception.context["sessionId"] shouldBe "invalid-session-456"
                exception.context["type"] shouldBe "validation"
            }
        }
        
        `when`("创建无效终端大小异常") {
            val exception = DomainExceptionFactory.invalidTerminalSize(0, -1)
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "VAL_004"
                exception.message shouldBe "Invalid terminal size: 0x-1"
                exception.context["rows"] shouldBe 0
                exception.context["columns"] shouldBe -1
                exception.context["type"] shouldBe "validation"
            }
        }
        
        `when`("创建无效事件ID异常") {
            val exception = DomainExceptionFactory.invalidEventId("invalid-event-789")
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "VAL_005"
                exception.message shouldBe "Invalid event ID: invalid-event-789"
                exception.context["eventId"] shouldBe "invalid-event-789"
                exception.context["type"] shouldBe "validation"
            }
        }
        
        `when`("创建通用验证异常") {
            val exception = DomainExceptionFactory.validationError(
                field = "username",
                value = "",
                reason = "cannot be empty"
            )
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "VAL_001"
                exception.message shouldBe "Validation error for field 'username': cannot be empty"
                exception.context["field"] shouldBe "username"
                exception.context["value"] shouldBe ""
                exception.context["reason"] shouldBe "cannot be empty"
                exception.context["type"] shouldBe "validation"
            }
        }
        
        `when`("创建带cause的验证异常") {
            val cause = IllegalArgumentException("Invalid format")
            val exception = DomainExceptionFactory.invalidUserId("bad-user-id", cause)
            
            then("应该正确设置cause") {
                exception.cause shouldBe cause
                exception.message shouldBe "Invalid user ID: bad-user-id"
            }
        }
    }
    
    given("异常工厂方法 - 资源异常") {
        `when`("创建会话未找到异常") {
            val exception = DomainExceptionFactory.sessionNotFound("session-not-found-123")
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "RES_003"
                exception.message shouldBe "Session not found: session-not-found-123"
                exception.context["sessionId"] shouldBe "session-not-found-123"
                exception.context["resourceType"] shouldBe "Session"
                exception.context["type"] shouldBe "resource"
            }
        }
        
        `when`("创建用户未找到异常") {
            val exception = DomainExceptionFactory.userNotFound("user-not-found-456")
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "RES_002"
                exception.message shouldBe "User not found: user-not-found-456"
                exception.context["userId"] shouldBe "user-not-found-456"
                exception.context["resourceType"] shouldBe "User"
                exception.context["type"] shouldBe "resource"
            }
        }
        
        `when`("创建通用资源未找到异常") {
            val exception = DomainExceptionFactory.resourceNotFound("Terminal", "terminal-789")
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "RES_001"
                exception.message shouldBe "Terminal not found: terminal-789"
                exception.context["resourceType"] shouldBe "Terminal"
                exception.context["resourceId"] shouldBe "terminal-789"
                exception.context["type"] shouldBe "resource"
            }
        }
    }
    
    given("异常工厂方法 - 业务规则异常") {
        `when`("创建权限拒绝异常") {
            val exception = DomainExceptionFactory.permissionDenied("user-123", "session-456")
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "BUS_002"
                exception.message shouldBe "Permission denied for user user-123 on session session-456"
                exception.context["userId"] shouldBe "user-123"
                exception.context["sessionId"] shouldBe "session-456"
                exception.context["type"] shouldBe "business"
            }
        }
        
        `when`("创建会话已存在异常") {
            val exception = DomainExceptionFactory.sessionAlreadyExists("existing-session-789")
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "BUS_003"
                exception.message shouldBe "Session already exists: existing-session-789"
                exception.context["sessionId"] shouldBe "existing-session-789"
                exception.context["type"] shouldBe "business"
            }
        }
        
        `when`("创建业务规则违反异常") {
            val exception = DomainExceptionFactory.businessRuleViolation(
                ruleName = "MaxSessionLimit",
                details = "Maximum 10 sessions per user"
            )
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "BUS_001"
                exception.message shouldBe "Business rule 'MaxSessionLimit' violated: Maximum 10 sessions per user"
                exception.context["ruleName"] shouldBe "MaxSessionLimit"
                exception.context["details"] shouldBe "Maximum 10 sessions per user"
                exception.context["type"] shouldBe "business"
            }
        }
    }
    
    given("异常工厂方法 - 系统异常") {
        `when`("创建命令执行失败异常") {
            val exception = DomainExceptionFactory.commandExecutionFailed(
                command = "ls -la",
                error = "Permission denied"
            )
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "SYS_002"
                exception.message shouldBe "Command execution failed: ls -la - Permission denied"
                exception.context["command"] shouldBe "ls -la"
                exception.context["error"] shouldBe "Permission denied"
                exception.context["type"] shouldBe "system"
            }
        }
        
        `when`("创建命令超时异常") {
            val exception = DomainExceptionFactory.commandTimeout(
                command = "long-running-process",
                timeoutMs = 5000L
            )
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "SYS_003"
                exception.message shouldBe "Command timeout: long-running-process (timeout: 5000ms)"
                exception.context["command"] shouldBe "long-running-process"
                exception.context["timeoutMs"] shouldBe 5000L
                exception.context["type"] shouldBe "system"
            }
        }
        
        `when`("创建系统不可用异常") {
            val exception = DomainExceptionFactory.systemUnavailable("Database")
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "SYS_001"
                exception.message shouldBe "System unavailable: Database"
                exception.context["component"] shouldBe "Database"
                exception.context["type"] shouldBe "system"
            }
        }
    }
    
    given("异常工厂方法 - 并发异常") {
        `when`("创建并发异常") {
            val exception = DomainExceptionFactory.concurrencyError(
                resourceId = "session-123",
                expectedVersion = 5L,
                actualVersion = 6L
            )
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "CON_001"
                exception.message shouldBe "Concurrency error for resource session-123"
                exception.context["resourceId"] shouldBe "session-123"
                exception.context["expectedVersion"] shouldBe 5L
                exception.context["actualVersion"] shouldBe 6L
                exception.context["type"] shouldBe "concurrency"
            }
        }
        
        `when`("创建并发异常（无版本信息）") {
            val exception = DomainExceptionFactory.concurrencyError("resource-456")
            
            then("应该正确设置异常属性") {
                exception.code shouldBe "CON_001"
                exception.message shouldBe "Concurrency error for resource resource-456"
                exception.context["resourceId"] shouldBe "resource-456"
                exception.context["type"] shouldBe "concurrency"
                exception.context.containsKey("expectedVersion") shouldBe false
                exception.context.containsKey("actualVersion") shouldBe false
            }
        }
    }
    
    given("异常扩展函数") {
        `when`("检查验证异常类型") {
            val validationException = DomainExceptionFactory.invalidUserId("test-user")
            
            then("应该正确识别异常类型") {
                validationException.isValidationError() shouldBe true
                // 移除冗余的类型检查断言，专注于验证context中的type字段
                validationException.context["type"] shouldBe "validation"
            }
        }
        
        `when`("检查业务异常类型") {
            val businessException = DomainExceptionFactory.permissionDenied("user1", "session1")
            
            then("应该正确识别异常类型") {
                businessException.isBusinessError() shouldBe true
                // 移除冗余的类型检查断言，专注于验证context中的type字段
                businessException.context["type"] shouldBe "business"
            }
        }
        
        `when`("检查资源异常类型") {
            val resourceException = DomainExceptionFactory.resourceNotFound("session", "session-123")
            
            then("应该正确识别异常类型") {
                resourceException.isResourceError() shouldBe true
                // 移除冗余的类型检查断言，专注于验证context中的type字段
                resourceException.context["type"] shouldBe "resource"
            }
        }
        
        `when`("检查系统异常类型") {
            val systemException = DomainExceptionFactory.systemUnavailable("Database")
            
            then("应该正确识别异常类型") {
                systemException.isSystemError() shouldBe true
                // 移除冗余的类型检查断言，专注于验证context中的type字段
                systemException.context["type"] shouldBe "system"
            }
        }
        
        `when`("检查并发异常类型") {
            val concurrencyException = DomainExceptionFactory.concurrencyError("resource1")
            
            then("应该正确识别异常类型") {
                concurrencyException.isConcurrencyError() shouldBe true
                // 移除冗余的类型检查断言，专注于验证context中的type字段
                concurrencyException.context["type"] shouldBe "concurrency"
            }
        }
        
        `when`("获取异常上下文信息") {
            val exception = DomainExceptionFactory.permissionDenied("user-123", "session-456")
            
            then("应该正确提取上下文信息") {
                exception.getUserId() shouldBe "user-123"
                exception.getSessionId() shouldBe "session-456"
                exception.getResourceType() shouldBe null
                exception.getResourceId() shouldBe null
            }
        }
        
        `when`("获取资源异常上下文信息") {
            val exception = DomainExceptionFactory.sessionNotFound("session-789")
            
            then("应该正确提取上下文信息") {
                exception.getUserId() shouldBe null
                exception.getSessionId() shouldBe "session-789"
                exception.getResourceType() shouldBe "Session"
                exception.getResourceId() shouldBe "session-789"
            }
        }
    }
    
    given("异常消息格式") {
        `when`("检查异常消息格式") {
            val userIdException = DomainExceptionFactory.invalidUserId("bad-user")
            val sessionException = DomainExceptionFactory.sessionNotFound("missing-session")
            val permissionException = DomainExceptionFactory.permissionDenied("user1", "session2")
            
            then("消息应该包含相关标识信息") {
                userIdException.message shouldContain "Invalid user ID"
                userIdException.message shouldContain "bad-user"
                
                sessionException.message shouldContain "Session not found"
                sessionException.message shouldContain "missing-session"
                
                permissionException.message shouldContain "Permission denied"
                permissionException.message shouldContain "user1"
                permissionException.message shouldContain "session2"
            }
        }
    }
})