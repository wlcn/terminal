package org.now.terminal.shared.kernel.valueobjects

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Timestamp value object
 */
@Serializable
data class Timestamp(
    val value: Instant
) : Comparable<Timestamp> {
    
    companion object {
        fun now(): Timestamp = Timestamp(Instant.now())
        fun ofEpochMilli(epochMilli: Long): Timestamp = Timestamp(Instant.ofEpochMilli(epochMilli))
        fun ofEpochSecond(epochSecond: Long): Timestamp = Timestamp(Instant.ofEpochSecond(epochSecond))
    }
    
    fun isAfter(other: Timestamp): Boolean = value.isAfter(other.value)
    fun isBefore(other: Timestamp): Boolean = value.isBefore(other.value)
    fun isEqual(other: Timestamp): Boolean = value == other.value
    
    fun plusSeconds(seconds: Long): Timestamp = Timestamp(value.plusSeconds(seconds))
    fun plusMinutes(minutes: Long): Timestamp = Timestamp(value.plusSeconds(minutes * 60))
    fun plusHours(hours: Long): Timestamp = Timestamp(value.plusSeconds(hours * 3600))
    
    fun toEpochMilli(): Long = value.toEpochMilli()
    fun toEpochSecond(): Long = value.epochSecond
    
    override fun compareTo(other: Timestamp): Int = value.compareTo(other.value)
    
    override fun toString(): String = value.toString()
}