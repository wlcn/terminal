package org.now.terminal.infrastructure.logging.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.now.terminal.infrastructure.logging.LoggingLifecycleService

/**
 * 日志系统模块的Koin依赖注入配置
 */
val loggingModule: Module = module {
    single { LoggingLifecycleService() }
}