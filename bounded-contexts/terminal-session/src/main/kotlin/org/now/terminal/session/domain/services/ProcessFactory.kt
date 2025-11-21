package org.now.terminal.session.domain.services

import org.now.terminal.session.domain.valueobjects.PtyConfiguration
import org.now.terminal.shared.valueobjects.SessionId

/**
 * Process工厂接口
 * 负责创建Process实例，实现依赖倒置原则
 */
interface ProcessFactory {
    /**
     * 创建Process实例
     * @param ptyConfig PTY配置
     * @param sessionId 会话ID
     * @return Process实例
     */
    fun createProcess(ptyConfig: PtyConfiguration, sessionId: SessionId): Process
}