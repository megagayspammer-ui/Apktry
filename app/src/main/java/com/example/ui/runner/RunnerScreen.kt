package com.example.ui.runner

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.bridge.PermScriptBridge
import com.example.model.ConsoleLog
import com.example.model.LogLevel
import com.example.model.ScriptProject
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun RunnerScreen(
    currentProject: ScriptProject?,
    htmlCode: String,
    consoleLogs: List<ConsoleLog>,
    onAddLog: (LogLevel, String) -> Unit,
    onClearLogs: () -> Unit,
    onRequestPermissionFromJs: (String) -> Unit,
    onNavigateBackToCode: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var bridgeInstance by remember { mutableStateOf<PermScriptBridge?>(null) }
    var isConsoleExpanded by remember { mutableStateOf(false) }
    var logFilter by remember { mutableStateOf<LogLevel?>(null) }

    val errorCount = remember(consoleLogs) {
        consoleLogs.count { it.level == LogLevel.ERROR }
    }

    val filteredLogs = remember(consoleLogs, logFilter) {
        if (logFilter == null) consoleLogs else consoleLogs.filter { it.level == logFilter }
    }

    DisposableEffect(Unit) {
        onDispose {
            bridgeInstance?.onDestroy()
            webViewInstance?.destroy()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("runner_screen")
    ) {
        // Top Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onNavigateBackToCode,
                        modifier = Modifier.testTag("back_to_code_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Edit Code")
                    }
                    Column {
                        Text(
                            text = currentProject?.name ?: "Live Sandbox",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Native Sandbox",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            onAddLog(LogLevel.INFO, "Reloading sandbox...")
                            webViewInstance?.loadDataWithBaseURL("https://local.app/", htmlCode, "text/html", "UTF-8", null)
                        },
                        modifier = Modifier.testTag("reload_runner_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }

                    IconButton(
                        onClick = { isConsoleExpanded = !isConsoleExpanded },
                        modifier = Modifier.testTag("toggle_console_btn")
                    ) {
                        BadgedBox(
                            badge = {
                                if (errorCount > 0) {
                                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                                        Text("$errorCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Default.Terminal,
                                contentDescription = "Console",
                                tint = if (isConsoleExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Live WebView Execution Sandbox
        Box(
            modifier = Modifier
                .weight(if (isConsoleExpanded) 0.55f else 1f)
                .fillMaxWidth()
                .background(Color.Black)
        ) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("runner_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            allowContentAccess = true
                            allowFileAccess = true
                            setGeolocationEnabled(true)
                            useWideViewPort = true
                            loadWithOverviewMode = true
                        }

                        val bridge = PermScriptBridge(
                            context = ctx,
                            webViewRef = WeakReference(this),
                            onLogReceived = onAddLog,
                            onRequestPermissionFromJs = onRequestPermissionFromJs
                        )
                        bridgeInstance = bridge
                        addJavascriptInterface(bridge, "Android")

                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                if (consoleMessage != null) {
                                    val level = when (consoleMessage.messageLevel()) {
                                        ConsoleMessage.MessageLevel.ERROR -> LogLevel.ERROR
                                        ConsoleMessage.MessageLevel.WARNING -> LogLevel.WARN
                                        ConsoleMessage.MessageLevel.DEBUG -> LogLevel.DEBUG
                                        else -> LogLevel.INFO
                                    }
                                    onAddLog(level, "[JS] ${consoleMessage.message()} (${consoleMessage.lineNumber()})")
                                }
                                return true
                            }

                            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                                callback?.invoke(origin, true, false)
                            }

                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.grant(request.resources)
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                onAddLog(LogLevel.INFO, "Sandboxed app initialized")
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                onAddLog(LogLevel.ERROR, "Render error: ${error?.description}")
                            }
                        }

                        webViewInstance = this
                        loadDataWithBaseURL("https://local.app/", htmlCode, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    webViewInstance = webView
                }
            )
        }

        // Live Console Drawer
        AnimatedVisibility(visible = isConsoleExpanded) {
            Surface(
                color = Color(0xFF070A0F),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Console Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF0E131F))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Console",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "(${consoleLogs.size} events)",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    val text = consoleLogs.joinToString("\n") { "[${it.level}] ${it.message}" }
                                    clipboardManager.setText(AnnotatedString(text))
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", fontSize = 11.sp)
                            }

                            TextButton(onClick = onClearLogs) {
                                Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", fontSize = 11.sp)
                            }
                        }
                    }

                    // Console Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = logFilter == null,
                            onClick = { logFilter = null },
                            label = { Text("All", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = logFilter == LogLevel.INFO,
                            onClick = { logFilter = LogLevel.INFO },
                            label = { Text("Logs", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = logFilter == LogLevel.WARN,
                            onClick = { logFilter = LogLevel.WARN },
                            label = { Text("Warns", fontSize = 10.sp) }
                        )
                        FilterChip(
                            selected = logFilter == LogLevel.ERROR,
                            onClick = { logFilter = LogLevel.ERROR },
                            label = { Text("Errors", fontSize = 10.sp) }
                        )
                    }

                    // Log items
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (filteredLogs.isEmpty()) {
                            item {
                                Text(
                                    "No log events captured yet. Call console.log(...) or Android.log(...)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color.DarkGray,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        } else {
                            items(filteredLogs) { log ->
                                val timeStr = remember(log.timestamp) {
                                    SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(log.timestamp))
                                }
                                val color = when (log.level) {
                                    LogLevel.ERROR -> Color(0xFFEF4444)
                                    LogLevel.WARN -> Color(0xFFFBBF24)
                                    LogLevel.DEBUG -> Color(0xFF38BDF8)
                                    LogLevel.INFO -> Color(0xFFE2E8F0)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = timeStr,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B),
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Text(
                                        text = "[${log.level.name.take(4)}]",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = color,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Text(
                                        text = log.message,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = color,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
