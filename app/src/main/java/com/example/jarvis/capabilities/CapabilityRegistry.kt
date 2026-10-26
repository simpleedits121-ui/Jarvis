package com.example.jarvis.capabilities

/**
 * Minimal registry placeholder. Future capabilities should be reviewed,
 * permission-scoped, tested and explicitly enabled before registration.
 */
class CapabilityRegistry {
    private val capabilities = mutableMapOf<String, Capability>()

    fun registerReviewed(capability: Capability) {
        require(capability.id.matches(Regex("[a-z0-9._-]{1,64}"))) {
            "Capability IDs must be short, lowercase identifiers."
        }
        capabilities[capability.id] = capability
    }

    fun list(): List<Capability> = capabilities.values.sortedBy { it.id }

    fun find(id: String): Capability? = capabilities[id]
}
