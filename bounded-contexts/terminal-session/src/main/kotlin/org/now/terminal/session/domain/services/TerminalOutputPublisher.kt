package org.now.terminal.session.domain.services

import org.now.terminal.shared.valueobjects.SessionId

/**
 * 终端输出发布器接口
 * 业务模块只依赖此接口，不关心具体实现
 */
interface TerminalOutputPublisher {
    
    /**
     * 发布终端输出
     * @param sessionId 会话ID
     * @param output 输出内容
     */
    fun publishOutput(sessionId: SessionId, output: String)
}