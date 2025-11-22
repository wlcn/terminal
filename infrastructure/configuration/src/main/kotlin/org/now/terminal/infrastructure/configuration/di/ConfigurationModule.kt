package org.now.terminal.infrastructure.configuration.di

import org.koin.core.module.Module
import org.koin.dsl.module
import org.now.terminal.infrastructure.configuration.ConfigurationLifecycleService

/**
 * 配置管理模块的Koin依赖注入配置
 */
val configurationModule: Module = module {
    single { ConfigurationLifecycleService() }
}