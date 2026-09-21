package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.model.PermissionRegistry
import com.example.ui.PermScriptViewModel
import com.example.ui.StudioTab
import com.example.ui.docs.ApiDocsDialog
import com.example.ui.editor.EditorScreen
import com.example.ui.permissions.PermissionsScreen
import com.example.ui.runner.RunnerScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: PermScriptViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                PermScriptApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshPermissions()
    }
}

@Composable
fun PermScriptApp(viewModel: PermScriptViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    // Permission launchers
    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.refreshPermissions()
    }

    val multiplePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        viewModel.refreshPermissions()
    }

    // Handle JS-initiated permission requests
    LaunchedEffect(uiState.pendingPermissionRequest) {
        val perm = uiState.pendingPermissionRequest
        if (perm != null) {
            singlePermissionLauncher.launch(perm)
            viewModel.clearPendingPermissionRequest()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.testTag("main_navigation_bar")
            ) {
                NavigationBarItem(
                    selected = uiState.activeTab == StudioTab.PERMISSIONS,
                    onClick = { viewModel.setActiveTab(StudioTab.PERMISSIONS) },
                    icon = { Icon(Icons.Default.Security, contentDescription = "Permissions") },
                    label = { Text("Permissions") },
                    modifier = Modifier.testTag("nav_tab_permissions"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )

                NavigationBarItem(
                    selected = uiState.activeTab == StudioTab.EDITOR,
                    onClick = { viewModel.setActiveTab(StudioTab.EDITOR) },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Code Studio") },
                    label = { Text("Studio") },
                    modifier = Modifier.testTag("nav_tab_editor"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )

                NavigationBarItem(
                    selected = uiState.activeTab == StudioTab.RUNNER,
                    onClick = { viewModel.setActiveTab(StudioTab.RUNNER) },
                    icon = { Icon(Icons.Default.PlayCircle, contentDescription = "Live Runner") },
                    label = { Text("Runner") },
                    modifier = Modifier.testTag("nav_tab_runner"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (uiState.activeTab) {
                StudioTab.PERMISSIONS -> {
                    PermissionsScreen(
                        permissionsGrantedMap = uiState.permissionsGrantedMap,
                        onRequestSinglePermission = { perm ->
                            singlePermissionLauncher.launch(perm)
                        },
                        onRequestAllPermissions = {
                            val permsToRequest = PermissionRegistry.getRuntimePermissionsToRequest()
                            multiplePermissionsLauncher.launch(permsToRequest.toTypedArray())
                        },
                        onRefreshPermissions = {
                            viewModel.refreshPermissions()
                        }
                    )
                }

                StudioTab.EDITOR -> {
                    EditorScreen(
                        projects = uiState.projects,
                        currentProject = uiState.currentProject,
                        code = uiState.editorCode,
                        onCodeChange = { viewModel.updateEditorCode(it) },
                        onSelectProject = { viewModel.selectProject(it) },
                        onCreateProject = { name, desc -> viewModel.createProject(name, desc) },
                        onDuplicateProject = { viewModel.duplicateCurrentProject() },
                        onDeleteProject = { viewModel.deleteCurrentProject() },
                        onResetAllProjects = { viewModel.resetAllProjects() },
                        onRunProject = { viewModel.setActiveTab(StudioTab.RUNNER) },
                        onOpenDocs = { viewModel.setDocsDialogVisible(true) }
                    )
                }

                StudioTab.RUNNER -> {
                    RunnerScreen(
                        currentProject = uiState.currentProject,
                        htmlCode = uiState.editorCode,
                        consoleLogs = uiState.consoleLogs,
                        onAddLog = { level, msg -> viewModel.addConsoleLog(level, msg) },
                        onClearLogs = { viewModel.clearConsoleLogs() },
                        onRequestPermissionFromJs = { perm ->
                            viewModel.requestPermissionFromJs(perm)
                        },
                        onNavigateBackToCode = {
                            viewModel.setActiveTab(StudioTab.EDITOR)
                        }
                    )
                }
            }

            // Docs Sheet / Dialog
            if (uiState.showDocsDialog) {
                ApiDocsDialog(
                    onDismiss = { viewModel.setDocsDialogVisible(false) },
                    onInsertSnippet = { snippet ->
                        viewModel.updateEditorCode(uiState.editorCode + "\n\n" + snippet)
                    }
                )
            }
        }
    }
}

