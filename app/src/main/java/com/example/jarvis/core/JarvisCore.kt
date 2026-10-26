package com.example.jarvis.core

/**
 * Protected orchestration boundary.
 *
 * Keep this module small and reviewed. Generated capabilities must not be allowed
 * to rewrite this package or execute arbitrary code with app privileges.
 */
class JarvisCore {
    fun respondToDemoMessage(input: String): String {
        val normalized = input.trim().lowercase()
        return when {
            normalized.contains("status") ->
                "The interface is running. The local language model and device-action modules still need to be connected."
            normalized.contains("offline") ->
                "The interface works offline, but genuine offline AI requires adding a compatible local model runtime and model file."
            normalized.contains("app") ->
                "App-building is planned for a later milestone. First we'll add a restricted capability builder that generates and tests code in a separate workspace."
            else ->
                "I received: \"$input\". This is a demo response, not a language-model answer yet. Next we will connect local AI while keeping the core protected."
        }
    }
}
