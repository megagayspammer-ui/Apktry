package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ScriptRepository
import com.example.model.ConsoleLog
import com.example.model.LogLevel
import com.example.model.PermissionItem
import com.example.model.PermissionRegistry
import com.example.model.ScriptProject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class StudioTab(val label: String) {
    PERMISSIONS("Permissions"),
    EDITOR("Code Studio"),
    RUNNER("Live Runner")
}

data class StudioUiState(
    val projects: List<ScriptProject> = emptyList(),
    val currentProjectId: String = "",
    val editorCode: String = "",
    val activeTab: StudioTab = StudioTab.EDITOR,
    val permissionsGrantedMap: Map<String, Boolean> = emptyMap(),
    val consoleLogs: List<ConsoleLog> = emptyList(),
    val showDocsDialog: Boolean = false,
    val showNewProjectDialog: Boolean = false,
    val pendingPermissionRequest: String? = null
) {
    val currentProject: ScriptProject?
        get() = projects.find { it.id == currentProjectId } ?: projects.firstOrNull()
}

class PermScriptViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScriptRepository(application)

    private val _uiState = MutableStateFlow(StudioUiState())
    val uiState: StateFlow<StudioUiState> = _uiState.asStateFlow()

    init {
        loadProjects()
        refreshPermissions()
    }

    private fun loadProjects() {
        val list = repository.getInitialProjects()
        val first = list.firstOrNull()
        _uiState.update {
            it.copy(
                projects = list,
                currentProjectId = first?.id ?: "",
                editorCode = first?.htmlCode ?: ""
            )
        }
    }

    fun refreshPermissions() {
        val context = getApplication<Application>()
        val map = mutableMapOf<String, Boolean>()
        for (item in PermissionRegistry.ALL_PERMISSIONS) {
            map[item.id] = item.isGranted(context)
        }
        _uiState.update { it.copy(permissionsGrantedMap = map) }
    }

    fun selectProject(projectId: String) {
        val p = _uiState.value.projects.find { it.id == projectId } ?: return
        _uiState.update {
            it.copy(
                currentProjectId = p.id,
                editorCode = p.htmlCode
            )
        }
    }

    fun updateEditorCode(newCode: String) {
        _uiState.update { it.copy(editorCode = newCode) }
        // Auto-save into current project in memory
        val currId = _uiState.value.currentProjectId
        val updatedList = _uiState.value.projects.map {
            if (it.id == currId) it.copy(htmlCode = newCode, lastModified = System.currentTimeMillis()) else it
        }
        _uiState.update { it.copy(projects = updatedList) }
        repository.saveProjects(updatedList)
    }

    fun createProject(name: String, description: String) {
        val newProj = ScriptProject(
            id = "proj_" + UUID.randomUUID().toString().take(8),
            name = name.ifBlank { "Untitled App" },
            description = description.ifBlank { "Custom PermScript mini-app" },
            iconName = "code",
            requiredPermissions = emptyList(),
            htmlCode = """<!DOCTYPE html>
<html>
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$name</title>
  <style>
    body { background: #0d1117; color: #f0f6fc; font-family: sans-serif; padding: 20px; text-align: center; }
    button { background: #38bdf8; border: none; padding: 12px 20px; border-radius: 8px; font-weight: bold; }
  </style>
</head>
<body>
  <h2>$name</h2>
  <p>Write your script here. Full Android.* API available!</p>
  <button onclick="Android.toast('Running!')">Test</button>
</body>
</html>"""
        )
        val updatedList = listOf(newProj) + _uiState.value.projects
        _uiState.update {
            it.copy(
                projects = updatedList,
                currentProjectId = newProj.id,
                editorCode = newProj.htmlCode,
                showNewProjectDialog = false
            )
        }
        repository.saveProjects(updatedList)
    }

    fun duplicateCurrentProject() {
        val curr = _uiState.value.currentProject ?: return
        val newProj = curr.copy(
            id = "proj_" + UUID.randomUUID().toString().take(8),
            name = "${curr.name} (Copy)",
            lastModified = System.currentTimeMillis()
        )
        val updatedList = listOf(newProj) + _uiState.value.projects
        _uiState.update {
            it.copy(
                projects = updatedList,
                currentProjectId = newProj.id,
                editorCode = newProj.htmlCode
            )
        }
        repository.saveProjects(updatedList)
    }

    fun deleteCurrentProject() {
        val projects = _uiState.value.projects
        if (projects.size <= 1) return
        val currId = _uiState.value.currentProjectId
        val remaining = projects.filterNot { it.id == currId }
        val next = remaining.first()
        _uiState.update {
            it.copy(
                projects = remaining,
                currentProjectId = next.id,
                editorCode = next.htmlCode
            )
        }
        repository.saveProjects(remaining)
    }

    fun resetAllProjects() {
        val defaults = repository.resetToDefaults()
        val first = defaults.first()
        _uiState.update {
            it.copy(
                projects = defaults,
                currentProjectId = first.id,
                editorCode = first.htmlCode
            )
        }
    }

    fun setActiveTab(tab: StudioTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun setDocsDialogVisible(show: Boolean) {
        _uiState.update { it.copy(showDocsDialog = show) }
    }

    fun setNewProjectDialogVisible(show: Boolean) {
        _uiState.update { it.copy(showNewProjectDialog = show) }
    }

    fun addConsoleLog(level: LogLevel, message: String) {
        viewModelScope.launch {
            _uiState.update {
                val newLog = ConsoleLog(level = level, message = message)
                val trimmed = (it.consoleLogs + newLog).takeLast(200)
                it.copy(consoleLogs = trimmed)
            }
        }
    }

    fun clearConsoleLogs() {
        _uiState.update { it.copy(consoleLogs = emptyList()) }
    }

    fun requestPermissionFromJs(permissionKey: String) {
        _uiState.update { it.copy(pendingPermissionRequest = permissionKey) }
    }

    fun clearPendingPermissionRequest() {
        _uiState.update { it.copy(pendingPermissionRequest = null) }
    }
}
