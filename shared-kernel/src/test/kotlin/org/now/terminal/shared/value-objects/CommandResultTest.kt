package org.now.terminal.shared.valueobjects

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldBeNull

class CommandResultTest : StringSpec({

    "should create success result with default exit code" {
        val result = CommandResult.Success("Command executed successfully")
        
        result.output shouldBe "Command executed successfully"
        result.code shouldBe 0
        result.isSuccess() shouldBe true
        result.isFailure() shouldBe false
        result.isTimeout() shouldBe false
    }



    "should create success result with custom exit code" {
        val result = CommandResult.Success("Command executed", 1)
        
        result.output shouldBe "Command executed"
        result.code shouldBe 1
    }

    "should create failure result with default exit code" {
        val result = CommandResult.Failure("Command failed")
        
        result.errorMessage shouldBe "Command failed"
        result.code shouldBe 1
        result.isSuccess() shouldBe false
        result.isFailure() shouldBe true
        result.isTimeout() shouldBe false
    }

    "should create failure result with custom exit code" {
        val result = CommandResult.Failure("Permission denied", 126)
        
        result.errorMessage shouldBe "Permission denied"
        result.code shouldBe 126
    }

    "should create timeout result" {
        val result = CommandResult.Timeout(5000L)
        
        result.timeoutMs shouldBe 5000L
        result.isSuccess() shouldBe false
        result.isFailure() shouldBe false
        result.isTimeout() shouldBe true
    }

    "should get output from success result" {
        val result = CommandResult.Success("output text")
        
        result.getOutputOrNull() shouldBe "output text"
    }

    "should return null output from non-success result" {
        val failureResult = CommandResult.Failure("error")
        val timeoutResult = CommandResult.Timeout(1000L)
        
        failureResult.getOutputOrNull().shouldBeNull()
        timeoutResult.getOutputOrNull().shouldBeNull()
    }

    "should get error message from failure result" {
        val result = CommandResult.Failure("error occurred")
        
        result.getErrorMessageOrNull() shouldBe "error occurred"
    }

    "should return null error message from non-failure result" {
        val successResult = CommandResult.Success("output")
        val timeoutResult = CommandResult.Timeout(1000L)
        
        successResult.getErrorMessageOrNull().shouldBeNull()
        timeoutResult.getErrorMessageOrNull().shouldBeNull()
    }

    "should get exit code from success result" {
        val result = CommandResult.Success("output", 0)
        
        result.getExitCode() shouldBe 0
    }

    "should get exit code from failure result" {
        val result = CommandResult.Failure("error", 127)
        
        result.getExitCode() shouldBe 127
    }

    "should get exit code -1 from timeout result" {
        val result = CommandResult.Timeout(1000L)
        
        result.getExitCode() shouldBe -1
    }

    "should check result types correctly" {
        val success = CommandResult.Success("success")
        val failure = CommandResult.Failure("failure")
        val timeout = CommandResult.Timeout(1000L)
        
        success.isSuccess() shouldBe true
        success.isFailure() shouldBe false
        success.isTimeout() shouldBe false
        
        failure.isSuccess() shouldBe false
        failure.isFailure() shouldBe true
        failure.isTimeout() shouldBe false
        
        timeout.isSuccess() shouldBe false
        timeout.isFailure() shouldBe false
        timeout.isTimeout() shouldBe true
    }

    "should use when expression with sealed class" {
        val success = CommandResult.Success("success")
        val failure = CommandResult.Failure("failure")
        val timeout = CommandResult.Timeout(1000L)
        
        val successType = when (success) {
            is CommandResult.Success -> "success"
            is CommandResult.Failure -> "failure"
            is CommandResult.Timeout -> "timeout"
        }
        
        val failureType = when (failure) {
            is CommandResult.Failure -> "failure"
            is CommandResult.Success -> "success"
            is CommandResult.Timeout -> "timeout"
        }
        
        val timeoutType = when (timeout) {
            is CommandResult.Timeout -> "timeout"
            is CommandResult.Success -> "success"
            is CommandResult.Failure -> "failure"
        }
        
        successType shouldBe "success"
        failureType shouldBe "failure"
        timeoutType shouldBe "timeout"
    }
})