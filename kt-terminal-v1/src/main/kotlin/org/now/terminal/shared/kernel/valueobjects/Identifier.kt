package org.now.terminal.shared.kernel.valueobjects

import kotlinx.serialization.Serializable

/**
 * Base identifier interface
 */
interface Identifier<T> {
    val value: T
    
    fun isValid(): Boolean
    fun equals(other: Identifier<T>): Boolean
}

/**
 * String identifier base class
 */
@Serializable
data class StringIdentifier(
    override val value: String
) : Identifier<String> {
    
    override fun isValid(): Boolean {
        return value.isNotBlank()
    }
    
    override fun equals(other: Identifier<String>): Boolean {
        return this.value == other.value
    }
    
    override fun toString(): String = value
}