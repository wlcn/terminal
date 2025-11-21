package org.now.terminal.session.domain.valueobjects

/**
 * 会话终止原因
 */
enum class TerminationReason {
    /** 正常退出 */
    NORMAL,
    
    /** 用户主动终止 */
    USER_REQUESTED,
    
    /** 进程异常 */
    PROCESS_ERROR,
    
    /** 超时 */
    TIMEOUT,
    
    /** 系统错误 */
    SYSTEM_ERROR
}