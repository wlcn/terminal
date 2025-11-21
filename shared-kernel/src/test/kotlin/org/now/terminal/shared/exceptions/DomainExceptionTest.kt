package org.now.terminal.shared.exceptions

class DomainExceptionTest {
    
    fun testBasicDomainException() {
        // 直接使用构造函数创建异常实例
        val exception = DomainException(
            code = "TEST_001",
            message = "Test exception",
            context = mapOf("testKey" to "testValue")
        )
        
        // 验证基本属性 - 使用显式类型转换
        val code: String = exception.code
        assert(code == "TEST_001")
        
        val message: String = exception.message
        assert(message == "Test exception")
        
        val contextValue: String? = exception.context["testKey"] as? String
        assert(contextValue == "testValue")
        
        assert(exception is RuntimeException)
    }
}