package org.now.terminal.boundedcontext.terminalsession.domain

import org.now.terminal.kernel.communication.CommunicationProtocol

/**
 * 通讯工厂接口
 */
interface CommunicationFactory {
    fun createCommunication(protocol: CommunicationProtocol, sessionId: SessionId): SessionCommunication
}