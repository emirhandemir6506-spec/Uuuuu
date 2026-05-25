package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.database.CodeFile
import com.example.data.database.TerminalLog
import com.example.ui.editor.CodeSyntaxHighlighter
import com.example.ui.theme.*
import com.example.ui.viewmodel.CodeTerminalViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CodeTerminalScreen(
    viewModel: CodeTerminalViewModel,
    modifier: Modifier = Modifier
) {
    val allFiles by viewModel.allCodeFiles.collectAsState()
    val terminalLogs by viewModel.allTerminalLogs.collectAsState()
    val selectedFile by viewModel.selectedFile.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    val showSettings by viewModel.showSettingsDialog.collectAsState()
    val showAiSheet by viewModel.showAiAssistantSheet.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val aiResponse by viewModel.aiAssistantResponse.collectAsState()
    val aiTitle by viewModel.aiAssistantTitle.collectAsState()

    val focusManager = LocalFocusManager.current

    // Keep track of which text field currently has focus
    var isEditorFocused by remember { mutableStateOf(true) }
    
    val editorFocusRequester = remember { FocusRequester() }
    val terminalFocusRequester = remember { FocusRequester() }

    // Dialog for creating a new file
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var newFileLanguage by remember { mutableStateOf("python") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Editor",
                            style = TextStyle(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { /* Back navigation / Home placeholder */ },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                actions = {
                    // Save Button
                    TextButton(
                        onClick = { viewModel.saveCurrentFile() },
                        modifier = Modifier.testTag("save_button")
                    ) {
                        Text(
                            text = "Save",
                            style = TextStyle(
                                color = AccentCyan,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                    
                    // Settings icon for API Key configuring
                    IconButton(
                        onClick = { viewModel.toggleSettings(true) },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            // Horizontal mobile code keyboard auxiliary bar + native system footer spacer
            Column(modifier = Modifier.background(BackgroundDark)) {
                CodeKeyboardAccessoryBar(
                    onKeySelected = { value ->
                        if (isEditorFocused) {
                            val currentText = viewModel.editorText
                            viewModel.updateEditorText(currentText + value)
                        } else {
                            val currentTerm = viewModel.terminalInputText
                            viewModel.terminalInputText = currentTerm + value
                        }
                    }
                )
                Spacer(modifier = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
            }
        },
        containerColor = BackgroundDark
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                
                // 1. HORIZONTAL WORKSPACE TABS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundDark)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allFiles.forEach { file ->
                            val isSelected = selectedFile?.id == file.id
                            FileTab(
                                file = file,
                                isSelected = isSelected,
                                onClick = { viewModel.selectFile(file) },
                                onDelete = { viewModel.deleteFile(file) }
                            )
                        }
                    }
                    
                    // Add new file button
                    IconButton(
                        onClick = {
                            newFileName = ""
                            newFileLanguage = "python"
                            showNewFileDialog = true
                        },
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(36.dp)
                            .background(SurfaceDark, CircleShape)
                            .testTag("add_file_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New File",
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // 2. THE EDITING SPLITSCREEN PANELS
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    
                    // UPPER PORTION: CODE EDITOR CANVASES
                    Box(
                        modifier = Modifier
                            .weight(1.1f)
                            .fillMaxWidth()
                            .background(BackgroundDark)
                    ) {
                        if (selectedFile != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(vertical = 12.dp)
                            ) {
                                // 1. Line numbers margin
                                val linesCount = viewModel.editorText.split("\n").size.coerceAtLeast(1)
                                val lineNumbersText = (1..linesCount).joinToString("\n") { it.toString() }
                                
                                Text(
                                    text = lineNumbersText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    lineHeight = 20.sp,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier
                                        .width(38.dp)
                                        .padding(end = 8.dp)
                                        .testTag("line_numbers_column")
                                )
                                
                                // Vertical divider line
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .width(1.dp)
                                        .background(SurfaceLighter)
                                )
                                
                                // 2. Dracula Syntax Highlighting Editor
                                BasicTextField(
                                    value = viewModel.editorText,
                                    onValueChange = { viewModel.updateEditorText(it) },
                                    textStyle = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = TextPrimary,
                                        lineHeight = 20.sp
                                    ),
                                    visualTransformation = CodeSyntaxHighlighter(selectedFile?.language ?: "python"),
                                    cursorBrush = SolidColor(AccentCyan),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 12.dp, end = 12.dp)
                                        .focusRequester(editorFocusRequester)
                                        .onFocusChanged { isEditorFocused = it.isFocused }
                                        .testTag("code_editor_field")
                                )
                            }
                        } else {
                            // Empty state
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No files active in sandbox.",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // CENTRAL MID-SEPARATOR WITH FLOATING AI PILL
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(SurfaceLighter)
                    )

                    // LOWER PORTION: TERMINAL LOGGER
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .fillMaxWidth()
                            .background(Color(0xFF030508))
                    ) {
                        TerminalConsoleView(
                            logs = terminalLogs,
                            inputText = viewModel.terminalInputText,
                            onInputChange = { viewModel.terminalInputText = it },
                            onCommandSubmit = { viewModel.handleTerminalCommand(it) },
                            isAnalyzing = isAnalyzing,
                            onClearTerminal = { viewModel.clearTerminalLogs() },
                            onTriggerRun = {
                                selectedFile?.let { viewModel.handleTerminalCommand("run ${it.name}") }
                            },
                            focusRequester = terminalFocusRequester,
                            onFocusChange = { isEditorFocused = !it }
                        )
                    }
                }
            }

            // 3. THE FLOATING "ASK GEMINI 🤖" PILL (exactly resembling Ask Claude screen position)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 120.dp) // Float above terminal controls
            ) {
                FloatingAiPill(
                    isAnalyzing = isAnalyzing,
                    onClick = {
                        viewModel.triggerAiAction("OPTIMIZE")
                    }
                )
            }

            // 4. FLOATING AI ANALYSIS SHEET OVERLAY
            if (showAiSheet) {
                AiAssistantOverlaySheet(
                    title = aiTitle,
                    responseText = aiResponse,
                    isAnalyzing = isAnalyzing,
                    onClose = { viewModel.toggleAiSheet(false) },
                    onActionSelected = { action -> viewModel.triggerAiAction(action) },
                    onApplyCode = { code -> viewModel.applyOptimizedCode(code) }
                )
            }

            // 5. SETTINGS DIALOG
            if (showSettings) {
                ApiKeySettingsDialog(
                    apiKey = customApiKey,
                    onSave = { key ->
                        viewModel.setCustomApiKey(key)
                        viewModel.toggleSettings(false)
                    },
                    onDismiss = { viewModel.toggleSettings(false) }
                )
            }

            // 6. CREATE FILE DIALOG
            if (showNewFileDialog) {
                CreateFileDialog(
                    fileName = newFileName,
                    onNameChange = { newFileName = it },
                    language = newFileLanguage,
                    onLanguageChange = { newFileLanguage = it },
                    onDismiss = { showNewFileDialog = false },
                    onConfirm = {
                        if (newFileName.isNotBlank()) {
                            viewModel.createNewFile(newFileName, newFileLanguage)
                            showNewFileDialog = false
                        }
                    }
                )
            }
        }
    }
}

// HORIZONTAL TAB CARDS FOR THE DESKTOP Workspace
@Composable
fun FileTab(
    file: CodeFile,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val tabBackground = if (isSelected) SurfaceLighter else SurfaceDark
    val textColor = if (isSelected) AccentGreen else TextPrimary
    val borderColor = if (isSelected) AccentGreen else Color.Transparent

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tabBackground)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val icon = when (file.language.lowercase()) {
            "python" -> Icons.Default.Settings
            "javascript", "js" -> Icons.Default.Info
            "sql" -> Icons.Default.List
            else -> Icons.Default.Edit
        }
        
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isSelected) AccentGreen else AccentCyan,
            modifier = Modifier.size(16.dp)
        )
        
        Text(
            text = file.name,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        if (!file.isReadOnly) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete File",
                tint = TextSecondary,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onDelete() }
            )
        }
    }
}

// THE FLOATING GLOWING GRADIENT PILL
@Composable
fun FloatingAiPill(
    isAnalyzing: Boolean,
    onClick: () -> Unit
) {
    val gradientBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF50FA7B), // Cyber Green
            Color(0xFF8BE9FD), // Cyan
            Color(0xFFBD93F9)  // Violet purple
        )
    )

    Box(
        modifier = Modifier
            .clickable { onClick() }
            .background(gradientBrush, RoundedCornerShape(24.dp))
            .border(1.5.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isAnalyzing) {
                CircularProgressIndicator(
                    color = BackgroundDark,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp)
                )
            }
            
            Text(
                text = "Ask Gemini 🤖",
                style = TextStyle(
                    color = BackgroundDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

// THE TERMINAL AND CONSOLE VIEW
@Composable
fun TerminalConsoleView(
    logs: List<TerminalLog>,
    inputText: String,
    onInputChange: (String) -> Unit,
    onCommandSubmit: (String) -> Unit,
    isAnalyzing: Boolean,
    onClearTerminal: () -> Unit,
    onTriggerRun: () -> Unit,
    focusRequester: FocusRequester,
    onFocusChange: (Boolean) -> Unit
) {
    val listState = rememberLazyListState()

    // Auto-scroll terminal to bottom when new logs stream in
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Console Header Control Panel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Terminal Console",
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentOrange
                    )
                )
                if (isAnalyzing) {
                    CircularProgressIndicator(
                        color = AccentOrange,
                        strokeWidth = 1.5.dp,
                        modifier = Modifier.size(10.dp)
                    )
                }
            }

            // Prompt clear & Quick play simulation buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play code simulation button
                IconButton(
                    onClick = onTriggerRun,
                    modifier = Modifier.size(28.dp).background(SurfaceDark, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Simulate Running Code",
                        tint = AccentGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Clear Buffer logs
                IconButton(
                    onClick = onClearTerminal,
                    modifier = Modifier.size(28.dp).background(SurfaceDark, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear logs",
                        tint = TextError,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Output Display Log list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF020305), RoundedCornerShape(4.dp))
                .border(0.5.dp, SurfaceLighter, RoundedCornerShape(4.dp))
                .padding(8.dp)
        ) {
            items(logs) { log ->
                val logColor = when (log.type) {
                    "INPUT" -> TextPrimary
                    "OUTPUT" -> AccentGreen
                    "SYSTEM" -> AccentCyan
                    "ERROR" -> TextError
                    else -> TextPrimary
                }
                Text(
                    text = log.text,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = logColor,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Input Command Prompt Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF020305), RoundedCornerShape(4.dp))
                .border(0.5.dp, SurfaceLighter, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$ ",
                style = TextStyle(
                    color = AccentPink,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                )
            )
            
            BasicTextField(
                value = inputText,
                onValueChange = onInputChange,
                textStyle = TextStyle(
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                cursorBrush = SolidColor(AccentOrange),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = {
                        onCommandSubmit(inputText)
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { onFocusChange(it.isFocused) }
                    .testTag("terminal_input_field")
            )

            if (inputText.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = AccentOrange,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onCommandSubmit(inputText) }
                )
            }
        }
    }
}

// PROGRAMMING SYMBOLS KEYBOARD ACCESSORY ROW
@Composable
fun CodeKeyboardAccessoryBar(
    onKeySelected: (String) -> Unit
) {
    val listKeys = listOf(
        "{", "}", "[", "]", "(", ")", "/", "|", ";", ":", "<", ">", "=", "+", "*", "tab", " "
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(SurfaceDark)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        listKeys.forEach { symbol ->
            val displaySymbol = if (symbol == "tab") "⇥ tab" else symbol
            val keyStringValue = if (symbol == "tab") "    " else symbol
            
            Box(
                modifier = Modifier
                    .widthIn(min = 34.dp)
                    .height(32.dp)
                    .background(SurfaceLighter, RoundedCornerShape(4.dp))
                    .clickable { onKeySelected(keyStringValue) }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displaySymbol,
                    style = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan
                    )
                )
            }
        }
    }
}

// AI CONSOLE OVERLAY DIALOG MODAL SHEET
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAssistantOverlaySheet(
    title: String,
    responseText: String,
    isAnalyzing: Boolean,
    onClose: () -> Unit,
    onActionSelected: (String) -> Unit,
    onApplyCode: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .background(BackgroundDark)
            .border(1.dp, SurfaceLighter, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = AccentPurple
                    )
                    Text(
                        text = title,
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary
                    )
                }
            }

            // Quick actions buttons rows
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    text = "Refactor Code ⚡",
                    color = AccentPurple,
                    onClick = { onActionSelected("OPTIMIZE") }
                )
                QuickActionButton(
                    text = "Explain Logic 📖",
                    color = AccentCyan,
                    onClick = { onActionSelected("EXPLAIN") }
                )
                QuickActionButton(
                    text = "Find Defects 🔍",
                    color = AccentOrange,
                    onClick = { onActionSelected("FIND_BUGS") }
                )
            }

            HorizontalDivider(color = SurfaceLighter, modifier = Modifier.padding(bottom = 12.dp))

            // Scrollable Output response text area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(BackgroundDark, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                if (isAnalyzing && responseText.startsWith("AI Engine")) {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = AccentPurple)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Consulting Gemini Architect...",
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = AccentPurple
                            )
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = responseText,
                            style = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        )
                    }
                }
            }

            // Apply Code CTA Button if optimization was requested
            if (title.contains("Optimize") && responseText.contains("```")) {
                Button(
                    onClick = { onApplyCode(responseText) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .testTag("apply_optimized_code_btn")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = BackgroundDark)
                        Text(
                            text = "Apply AI Suggestions",
                            color = BackgroundDark,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
            .border(1.dp, color, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
    }
}

// API KEY SETTINGS MODAL DIALOG
@Composable
fun ApiKeySettingsDialog(
    apiKey: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyInput by remember { mutableStateOf(apiKey) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Configure Gemini API",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text = "Provide custom GEMINI_API_KEY for isolated executions. You can obtain this key standard from Google MakerSuite AI platform.",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                TextField(
                    value = keyInput,
                    onValueChange = { keyInput = it },
                    placeholder = { 
                        Text("AI Studio Key (AIzaSy...)", color = TextSecondary, fontSize = 12.sp, fontFamily = FontFamily.Monospace) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_field"),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = SurfaceLighter,
                        focusedLabelColor = AccentGreen
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(keyInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Text("Save Key", color = BackgroundDark, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// CREATE NEW WORKSPACE FILE DIALOG
@Composable
fun CreateFileDialog(
    fileName: String,
    onNameChange: (String) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val languages = listOf("python", "javascript", "sql", "sh")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Create Sandbox File",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = fileName,
                    onValueChange = onNameChange,
                    label = { Text("File Name", fontFamily = FontFamily.Monospace) },
                    placeholder = { Text("e.g. stats.py", color = TextSecondary, fontFamily = FontFamily.Monospace) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_file_name_field"),
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = TextPrimary),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = SurfaceLighter,
                        focusedLabelColor = AccentGreen
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Select Syntax Environment:",
                    fontSize = 11.sp,
                    color = AccentOrange,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    languages.forEach { lang ->
                        val isSelected = lang == language
                        val bg = if (isSelected) AccentOrange.copy(alpha = 0.2f) else SurfaceLighter
                        val border = if (isSelected) AccentOrange else Color.Transparent
                        Box(
                            modifier = Modifier
                                .background(bg, RoundedCornerShape(8.dp))
                                .border(1.dp, border, RoundedCornerShape(8.dp))
                                .clickable { onLanguageChange(lang) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = lang.uppercase(),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSelected) AccentOrange else TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary, fontFamily = FontFamily.Monospace)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                    ) {
                        Text("Create", color = BackgroundDark, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
// End of CodeTerminalScreen component sandbox layout.
