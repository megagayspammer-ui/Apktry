package com.example.model

data class ScriptProject(
    val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val requiredPermissions: List<String>,
    val htmlCode: String,
    val lastModified: Long = System.currentTimeMillis()
)

data class ConsoleLog(
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel = LogLevel.INFO,
    val tag: String = "App",
    val message: String
)

enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}
