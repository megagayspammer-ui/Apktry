package com.example.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ScriptProject

@Composable
fun EditorScreen(
    projects: List<ScriptProject>,
    currentProject: ScriptProject?,
    code: String,
    onCodeChange: (String) -> Unit,
    onSelectProject: (String) -> Unit,
    onCreateProject: (String, String) -> Unit,
    onDuplicateProject: () -> Unit,
    onDeleteProject: () -> Unit,
    onResetAllProjects: () -> Unit,
    onRunProject: () -> Unit,
    onOpenDocs: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var showProjectMenu by remember { mutableStateOf(false) }
    var showNewDialog by remember { mutableStateOf(false) }
    var newProjName by remember { mutableStateOf("") }
    var newProjDesc by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }

    val linesCount = remember(code) { code.lines().size }
    val lineNumbersText = remember(linesCount) {
        (1..linesCount).joinToString("\n")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("editor_screen")
    ) {
        // Project selector & actions bar
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box {
                    Button(
                        onClick = { showProjectMenu = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.testTag("select_project_dropdown_btn")
                    ) {
                        Text(
                            text = currentProject?.name ?: "Select Project",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            maxLines = 1
                        )
                    }

                    DropdownMenu(
                        expanded = showProjectMenu,
                        onDismissRequest = { showProjectMenu = false }
                    ) {
                        projects.forEach { p ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(p.name, fontWeight = FontWeight.Bold)
                                        Text(p.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                onClick = {
                                    onSelectProject(p.id)
                                    showProjectMenu = false
                                }
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showNewDialog = true },
                        modifier = Modifier.testTag("new_project_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "New App", tint = MaterialTheme.colorScheme.primary)
                    }

                    IconButton(
                        onClick = onOpenDocs,
                        modifier = Modifier.testTag("open_docs_btn")
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = "API Docs", tint = MaterialTheme.colorScheme.secondary)
                    }

                    IconButton(
                        onClick = onDuplicateProject,
                        modifier = Modifier.testTag("duplicate_project_btn")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate App")
                    }

                    if (projects.size > 1) {
                        IconButton(
                            onClick = onDeleteProject,
                            modifier = Modifier.testTag("delete_project_btn")
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete App", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    Button(
                        onClick = onRunProject,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .testTag("run_code_fab")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("RUN", fontWeight = FontWeight.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Snippets / Symbols Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val snippets = listOf(
                "Android.toast(\"\")" to "Android.toast(\"Hello!\");\n",
                "Android.vibrate()" to "Android.vibrate(150);\n",
                "Android.notify()" to "Android.notify(\"Alert\", \"Message\");\n",
                "Android.speak()" to "Android.speak(\"Hello!\");\n",
                "Android.getLocation()" to "Android.getLocation();\nwindow.onLocationResult = (lat, lng) => {\n  console.log(lat, lng);\n};\n",
                "console.log()" to "console.log(\"Debug: \");\n",
                "<button>" to "<button onclick=\"\">Click</button>\n",
                "{ }" to "{\n  \n}",
                "( )" to "( )"
            )

            snippets.forEach { (label, snippet) ->
                FilterChip(
                    selected = false,
                    onClick = {
                        onCodeChange(code + "\n" + snippet)
                    },
                    label = {
                        Text(label, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Code Editor Box with Line Numbers
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF090D14),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                // Line numbers gutter
                Box(
                    modifier = Modifier
                        .width(38.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF070A0F))
                        .padding(top = 16.dp, end = 6.dp)
                ) {
                    Text(
                        text = lineNumbersText,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFF475569),
                        lineHeight = 18.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Code Input
                OutlinedTextField(
                    value = code,
                    onValueChange = onCodeChange,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("code_editor_input"),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = Color(0xFFE2E8F0),
                        lineHeight = 18.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(0.dp)
                )
            }
        }

        // Status footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${code.length} chars • $linesCount lines • Zero compile • Live sandbox",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )

            TextButton(
                onClick = { showResetConfirm = true },
                modifier = Modifier.testTag("reset_presets_btn")
            ) {
                Text("Reset Presets", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            }
        }
    }

    // New Project Dialog
    if (showNewDialog) {
        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            title = { Text("Create New Mini-App") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newProjName,
                        onValueChange = { newProjName = it },
                        label = { Text("App Name") },
                        placeholder = { Text("e.g. Compass & Sensor HUD") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newProjDesc,
                        onValueChange = { newProjDesc = it },
                        label = { Text("Description") },
                        placeholder = { Text("What does this app do?") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onCreateProject(newProjName, newProjDesc)
                        newProjName = ""
                        newProjDesc = ""
                        showNewDialog = false
                    },
                    modifier = Modifier.testTag("confirm_create_project_btn")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Reset Confirm Dialog
    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset All Project Templates?") },
            text = { Text("This will restore the original sample templates (Hardware Toolkit, GPS Navigator, Sound Meter, etc.).") },
            confirmButton = {
                Button(
                    onClick = {
                        onResetAllProjects()
                        showResetConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
