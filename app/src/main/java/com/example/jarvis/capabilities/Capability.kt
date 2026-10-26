package com.example.jarvis.capabilities

/**
 * A capability is metadata for a reviewed action.
 * Do not use this interface to execute arbitrary generated code.
 */
data class Capability(
    val id: String,
    val description: String,
    val requiresConfirmation: Boolean = true,
    val enabled: Boolean = false
)
