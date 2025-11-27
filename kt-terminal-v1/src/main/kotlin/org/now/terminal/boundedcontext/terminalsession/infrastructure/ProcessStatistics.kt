package org.now.terminal.boundedcontext.terminalsession.infrastructure

/**
 * Process statistics data class - represents statistics about terminal processes
 */
data class ProcessStatistics(
    val totalProcesses: Int,
    val activeProcesses: Int,
    val terminatedProcesses: Int
)