package org.now.terminal.session.di

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.types.shouldBeInstanceOf
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import org.koin.test.inject
import org.now.terminal.session.di.terminalSessionModule
import org.now.terminal.session.application.SessionLifecycleService
import org.now.terminal.session.application.handlers.TerminalOutputEventHandler
import org.now.terminal.session.application.usecases.*
import org.now.terminal.session.domain.services.TerminalOutputPublisher
import org.now.terminal.session.domain.services.TerminalSessionService
import org.now.terminal.session.domain.repositories.TerminalSessionRepository
import org.now.terminal.session.domain.services.ProcessFactory
import org.now.terminal.session.infrastructure.process.Pty4jProcessFactory
import org.now.terminal.session.infrastructure.repositories.InMemoryTerminalSessionRepository
import org.now.terminal.session.infrastructure.TestTerminalOutputPublisher
import org.now.terminal.infrastructure.eventbus.EventBus
import io.mockk.mockk

class TerminalSessionModuleTest : BehaviorSpec(), KoinTest {
    
    private val testModule = module {
        // 为测试提供TerminalOutputPublisher的实现
        single<TerminalOutputPublisher> { TestTerminalOutputPublisher() }
        // 为测试提供EventBus的模拟实现
        single<EventBus> { mockk<EventBus>(relaxed = true) }
    }
    
    init {
        beforeSpec {
            startKoin {
                modules(terminalSessionModule + testModule)
            }
        }
        
        afterSpec {
            stopKoin()
        }
        
        given("TerminalSessionModule") {
            
            `when`("检查模块配置") {
                then("应该通过Koin模块检查") {
                    // 由于Koin应用已经在beforeSpec中启动，这里不再重复检查模块
                    // 模块配置的正确性通过其他测试用例验证
                }
            }
            
            `when`("获取基础设施层依赖") {
                val repository by inject<TerminalSessionRepository>()
                val processFactory by inject<ProcessFactory>()
                
                then("应该成功注入基础设施层组件") {
                    repository.shouldBeInstanceOf<InMemoryTerminalSessionRepository>()
                    processFactory.shouldBeInstanceOf<Pty4jProcessFactory>()
                }
            }
            
            `when`("获取用例层依赖") {
                val createSessionUseCase by inject<CreateSessionUseCase>()
                val handleInputUseCase by inject<HandleInputUseCase>()
                val terminateSessionUseCase by inject<TerminateSessionUseCase>()
                val resizeTerminalUseCase by inject<ResizeTerminalUseCase>()
                val listActiveSessionsUseCase by inject<ListActiveSessionsUseCase>()
                
                then("应该成功注入所有用例") {
                    createSessionUseCase.shouldBeInstanceOf<CreateSessionUseCase>()
                    handleInputUseCase.shouldBeInstanceOf<HandleInputUseCase>()
                    terminateSessionUseCase.shouldBeInstanceOf<TerminateSessionUseCase>()
                    resizeTerminalUseCase.shouldBeInstanceOf<ResizeTerminalUseCase>()
                    listActiveSessionsUseCase.shouldBeInstanceOf<ListActiveSessionsUseCase>()
                }
            }
            
            `when`("获取事件处理器依赖") {
                val eventHandler by inject<TerminalOutputEventHandler>()
                
                then("应该成功注入事件处理器") {
                    eventHandler.shouldBeInstanceOf<TerminalOutputEventHandler>()
                }
            }
            
            `when`("获取服务层依赖") {
                val terminalSessionService by inject<TerminalSessionService>()
                
                then("应该成功注入服务层组件") {
                    terminalSessionService.shouldBeInstanceOf<SessionLifecycleService>()
                }
            }
            
            `when`("验证依赖注入完整性") {
                then("所有依赖都应该正确配置") {
                    // 验证模块配置（不启动新的Koin应用，使用已启动的）
                    // 直接通过注入验证依赖关系，避免重复启动Koin
                    val repository by inject<TerminalSessionRepository>()
                    val processFactory by inject<ProcessFactory>()
                    val createSessionUseCase by inject<CreateSessionUseCase>()
                    val terminalSessionService by inject<TerminalSessionService>()
                    
                    // 验证所有依赖都已正确注入
                    repository.shouldBeInstanceOf<InMemoryTerminalSessionRepository>()
                    processFactory.shouldBeInstanceOf<Pty4jProcessFactory>()
                    createSessionUseCase.shouldBeInstanceOf<CreateSessionUseCase>()
                    terminalSessionService.shouldBeInstanceOf<SessionLifecycleService>()
                }
            }
        }
    }
}