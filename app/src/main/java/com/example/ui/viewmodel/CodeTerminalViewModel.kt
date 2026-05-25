package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.database.CodeFile
import com.example.data.database.CodeTerminalDatabase
import com.example.data.database.TerminalLog
import com.example.data.repository.CodeTerminalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CodeTerminalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CodeTerminalRepository

    val allCodeFiles: StateFlow<List<CodeFile>>
    val allTerminalLogs: StateFlow<List<TerminalLog>>

    private val _selectedFile = MutableStateFlow<CodeFile?>(null)
    val selectedFile: StateFlow<CodeFile?> = _selectedFile.asStateFlow()

    var editorText by mutableStateOf("")
        private set

    var terminalInputText by mutableStateOf("")
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _showSettingsDialog = MutableStateFlow(false)
    val showSettingsDialog: StateFlow<Boolean> = _showSettingsDialog.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _showAiAssistantSheet = MutableStateFlow(false)
    val showAiAssistantSheet: StateFlow<Boolean> = _showAiAssistantSheet.asStateFlow()

    private val _aiAssistantResponse = MutableStateFlow("")
    val aiAssistantResponse: StateFlow<String> = _aiAssistantResponse.asStateFlow()

    private val _aiAssistantTitle = MutableStateFlow("")
    val aiAssistantTitle: StateFlow<String> = _aiAssistantTitle.asStateFlow()

    init {
        val database = CodeTerminalDatabase.getDatabase(application)
        repository = CodeTerminalRepository(database.codeTerminalDao())
        
        allCodeFiles = repository.allCodeFiles.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allTerminalLogs = repository.allTerminalLogs.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            repository.checkAndSeedDatabase()
            // Auto select the first file (usually analysis.py) if available
            allCodeFiles.collect { files ->
                if (_selectedFile.value == null && files.isNotEmpty()) {
                    selectFile(files.first())
                }
            }
        }
    }

    fun selectFile(file: CodeFile) {
        _selectedFile.value = file
        editorText = file.content
    }

    fun updateEditorText(newText: String) {
        editorText = newText
    }

    fun saveCurrentFile() {
        val file = _selectedFile.value ?: return
        viewModelScope.launch {
            val updated = file.copy(content = editorText, updatedAt = System.currentTimeMillis())
            repository.updateCodeFile(updated)
            _selectedFile.value = updated
            
            // Add a terminal notification
            addLog("SYSTEM", "File '${file.name}' saved successfully in local workspace.")
        }
    }

    fun createNewFile(name: String, language: String) {
        viewModelScope.launch {
            val defaultContent = when (language.lowercase()) {
                "python" -> "# Python module: $name\nprint('Hello from $name!')\n"
                "javascript" -> "// JavaScript module: $name\nconsole.log('Hello from $name!');\n"
                "sql" -> "-- SQL Query: $name\nSELECT * FROM data LIMIT 10;\n"
                else -> "# Script module: $name\necho 'Running $name'\n"
            }
            val newFile = CodeFile(
                name = name,
                language = language,
                content = defaultContent
            )
            val id = repository.insertCodeFile(newFile)
            val created = newFile.copy(id = id.toInt())
            selectFile(created)
            addLog("SYSTEM", "Created file '${name}' in workspace.")
        }
    }

    fun deleteFile(file: CodeFile) {
        viewModelScope.launch {
            repository.deleteCodeFile(file)
            addLog("SYSTEM", "Deleted file '${file.name}' from workspace.")
            // Reset selected file if deleted
            if (_selectedFile.value?.id == file.id) {
                _selectedFile.value = null
                editorText = ""
                // Try select first remaining
                allCodeFiles.value.firstOrNull { it.id != file.id }?.let {
                    selectFile(it)
                }
            }
        }
    }

    fun clearTerminalLogs() {
        viewModelScope.launch {
            repository.clearTerminalLogs()
            addLog("SYSTEM", "Terminal display cleared. Current session refreshed.")
        }
    }

    fun setCustomApiKey(key: String) {
        _customApiKey.value = key
    }

    fun toggleSettings(show: Boolean) {
        _showSettingsDialog.value = show
    }

    fun toggleAiSheet(show: Boolean) {
        _showAiAssistantSheet.value = show
    }

    private fun getActiveApiKey(): String {
        return _customApiKey.value.trim().ifEmpty {
            BuildConfig.GEMINI_API_KEY.trim().ifEmpty {
                ""
            }
        }
    }

    private suspend fun addLog(type: String, text: String) {
        repository.insertTerminalLog(TerminalLog(type = type, text = text))
    }

    fun handleTerminalCommand(rawCommand: String) {
        val cmd = rawCommand.trim()
        if (cmd.isEmpty()) return

        terminalInputText = ""
        viewModelScope.launch {
            addLog("INPUT", "$ cmd")

            val parts = cmd.split(" ").filter { it.isNotEmpty() }
            if (parts.isEmpty()) return@launch

            val primaryCommand = parts[0].lowercase()

            when (primaryCommand) {
                "help" -> {
                    addLog("SYSTEM", "--- GEMINI SANDBOX TERMINAL HELP ---")
                    addLog("SYSTEM", "Available commands:")
                    addLog("SYSTEM", "  help                   Show this help menu")
                    addLog("SYSTEM", "  ls / files             List all files in workspace")
                    addLog("SYSTEM", "  cat <filename>         Print file contents")
                    addLog("SYSTEM", "  run <filename>         Simulate code execution using Gemini AI")
                    addLog("SYSTEM", "  python <filename>      Alias to run python code")
                    addLog("SYSTEM", "  node <filename>        Alias to run javascript code")
                    addLog("SYSTEM", "  gemini <prompt>        Prompt Gemini AI directly from command line")
                    addLog("SYSTEM", "  clear                  Clear terminal buffer logs")
                }
                "clear" -> {
                    clearTerminalLogs()
                }
                "ls", "files" -> {
                    val files = allCodeFiles.value
                    if (files.isEmpty()) {
                        addLog("OUTPUT", "Workspace is completely empty.")
                    } else {
                        addLog("OUTPUT", "Files in workspace:")
                        files.forEach { file ->
                            val size = file.content.toByteArray().size
                            addLog("OUTPUT", "  -  ${file.name}  (${file.language})  [${size} bytes]")
                        }
                    }
                }
                "cat" -> {
                    if (parts.size < 2) {
                        addLog("ERROR", "ls: missing file operand. Usage: cat <filename>")
                        return@launch
                    }
                    val targetName = parts[1]
                    val file = allCodeFiles.value.firstOrNull { it.name.lowercase() == targetName.lowercase() }
                    if (file == null) {
                        addLog("ERROR", "cat: ${targetName}: No such file or directory in workspace")
                    } else {
                        addLog("OUTPUT", "--- ${file.name} ---")
                        addLog("OUTPUT", file.content)
                    }
                }
                "run", "python", "node", "js", "sh" -> {
                    val targetName = if (parts.size >= 2) {
                        parts[1]
                    } else {
                        // Dev mode default to current selected file
                        _selectedFile.value?.name
                    }

                    if (targetName == null) {
                        addLog("ERROR", "Execution failed: No active file selected to run.")
                        return@launch
                    }

                    val file = allCodeFiles.value.firstOrNull { it.name.lowercase() == targetName.lowercase() }
                    if (file == null) {
                        addLog("ERROR", "sh: execution command failed. '${targetName}' not found.")
                        return@launch
                    }

                    runCodeSimulation(file)
                }
                "gemini", "ai" -> {
                    if (parts.size < 2) {
                        addLog("ERROR", "AI prompt is empty. Usage: gemini <query>")
                        return@launch
                    }
                    val prompt = parts.drop(1).joinToString(" ")
                    queryTerminalGemini(prompt)
                }
                else -> {
                    addLog("ERROR", "sh: command not found: $primaryCommand. Type 'help' for suggestions.")
                }
            }
        }
    }

    private fun runCodeSimulation(file: CodeFile) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            addLog("SYSTEM", "Launching '${file.name}' virtual sandbox runtime...")
            addLog("SYSTEM", "Gemini Code: Executing isolated syntax evaluation...")

            val apiKey = getActiveApiKey()
            if (apiKey.isEmpty()) {
                // Return offline simulated data for speed/integrity if key is not configured
                // Or inform the user how to configure it
                simulateOfflineRun(file)
            } else {
                try {
                    val sysInstruction = "You are a highly advanced isolated execution environment of a mobile terminal built on the Gemini SDK. " +
                            "Please simulate running the code provided in the user's prompt. Output ONLY what would be printed in the console (stdout/stderr outputs). " +
                            "Do not place markdown blocks or descriptions. If it queries mock databases or utilizes variables, resolve them cleanly and print final output results. " +
                            "Make it look exactly like terminal output logs."

                    val userPrompt = "Execute the following file:\nFilename: ${file.name}\nLanguage: ${file.language}\nContent:\n${file.content}"

                    val request = GenerateContentRequest(
                        contents = listOf(Content(parts = listOf(Part(text = userPrompt)))),
                        systemInstruction = Content(parts = listOf(Part(text = sysInstruction))),
                        generationConfig = GenerationConfig(temperature = 0.5f)
                    )

                    val response = RetrofitClient.service.generateContent(apiKey, request)
                    val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    
                    if (resultText != null) {
                        addLog("OUTPUT", resultText.trim())
                    } else {
                        addLog("ERROR", "Simulation completed with empty output buffer.")
                    }
                } catch (e: Exception) {
                    addLog("ERROR", "Sandbox network exception: ${e.localizedMessage}")
                    addLog("SYSTEM", "Falling back to local isolated engine execution:")
                    simulateOfflineRun(file)
                }
            }
            _isAnalyzing.value = false
        }
    }

    private suspend fun simulateOfflineRun(file: CodeFile) {
        withContext(Dispatchers.Default) {
            // High quality mock running output tailored directly to files
            when (file.language.lowercase()) {
                "python" -> {
                    if (file.name.contains("analysis")) {
                        addLog("OUTPUT", "Analyzing 4 rows of developer metrics...\n\n" +
                                "--- Summary Statistics ---\n" +
                                "       build_time       errors\n" +
                                "count    4.000000     4.000000\n" +
                                "mean    67.000000     1.000000\n" +
                                "std     39.251327     1.414214\n" +
                                "min     30.000000     0.000000\n" +
                                "25%     41.250000     0.000000\n" +
                                "50%     59.000000     0.500000\n" +
                                "75%     84.750000     1.500000\n" +
                                "max    120.000000     3.000000\n\n" +
                                "Execution Complete: {\"rows\": 4, \"status\": \"ready\"}")
                    } else {
                        addLog("OUTPUT", "Python 3.11 virtual simulator running ${file.name}...\n" +
                                "Output: Process completed with exit code 0.")
                    }
                }
                "javascript" -> {
                    if (file.name.contains("calculator")) {
                        addLog("OUTPUT", "Starting server calculation...\n\n" +
                                "--- SIMULATION RESULTS ---\n" +
                                "Requests processed successfully: 13864.08\n" +
                                "Node Health Score: 97.6%")
                    } else {
                        addLog("OUTPUT", "Node.js JS Sandbox: running ${file.name}...\n" +
                                "Output: Simulated run success.")
                    }
                }
                "sql" -> {
                    addLog("OUTPUT", "Executing Query against SQL virtual database...\n" +
                            "Table 'app_orders' analyzed (4 rows fetched):\n" +
                            "----------------------------------------------------\n" +
                            "category    | purchases | gross_rev | satisfaction\n" +
                            "----------------------------------------------------\n" +
                            "SmartPhones |     4210  | 840,240   | 4.6\n" +
                            "Audio       |     1050  |  84,000   | 4.1\n" +
                            "Laptops     |      920  | 552,000   | 4.8\n" +
                            "----------------------------------------------------\n" +
                            "Query runtime: 0.045 seconds")
                }
                "sh" -> {
                    if (file.name.contains("diagnostic")) {
                        addLog("OUTPUT", "===========================================\n" +
                                "   GEMINI DEVELOPMENT SANDBOX INITIALIZED  \n" +
                                "===========================================\n" +
                                "System OS  : AI Studio Android VirtIO Cluster\n" +
                                "Cores      : 4-Core ARM64\n" +
                                "Free Memory: 68% OK\n" +
                                "Gemini SDK : ACTIVE Version 3.5-Flash\n" +
                                "===========================================\n" +
                                "Verifying environment integrity...\n" +
                                "STATUS [COMPLETED]: All sandbox environments healthy.")
                    } else {
                        addLog("OUTPUT", "Running command script shell...\n" +
                                "Workspace environment validated successfully.")
                    }
                }
                else -> {
                    addLog("OUTPUT", "Simulating execution success for metadata source.")
                }
            }
        }
    }

    private fun queryTerminalGemini(prompt: String) {
        viewModelScope.launch {
            _isAnalyzing.value = true
            addLog("SYSTEM", "Connecting to Gemini AI Gateway...")
            
            val apiKey = getActiveApiKey()
            if (apiKey.isEmpty()) {
                addLog("ERROR", "API Key setup missing. Please tap settings icon on the top right to configure your API key.")
                _isAnalyzing.value = false
                return@launch
            }

            try {
                val sysInstruction = "You are a witty, extremely knowledgeable terminal AI developer companion called Gemini Developer. " +
                        "A developer is interacting with you inside a UNIX-style coding terminal. Give extremely direct, concise, " +
                        "no-fluff, actionable technical answers, with clean terminal presentation. Limit answers to fits in a visual console screen (1-2 paragraphs)."

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    systemInstruction = Content(parts = listOf(Part(text = sysInstruction))),
                    generationConfig = GenerationConfig(temperature = 0.7f)
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (resultText != null) {
                    addLog("OUTPUT", "Gemini Console response:\n" + resultText.trim())
                } else {
                    addLog("ERROR", "Empty response received from Gemini Gateway.")
                }
            } catch (e: Exception) {
                addLog("ERROR", "Gemini integration failed: ${e.localizedMessage}")
            }
            _isAnalyzing.value = false
        }
    }

    fun triggerAiAction(action: String) {
        val file = _selectedFile.value ?: return
        val currentCode = editorText
        _aiAssistantTitle.value = when (action) {
            "OPTIMIZE" -> "Optimize Workspace Code"
            "EXPLAIN" -> "Explain Code Logic"
            "FIND_BUGS" -> "Find Code Defects & Bugs"
            else -> "Assistant Quick Prompt"
        }

        viewModelScope.launch {
            _isAnalyzing.value = true
            _aiAssistantResponse.value = "AI Engine working, please hold..."
            _showAiAssistantSheet.value = true

            // Log action in terminal
            addLog("SYSTEM", "Requesting AI developer action '$action' on ${file.name}...")

            val apiKey = getActiveApiKey()
            if (apiKey.isEmpty()) {
                _aiAssistantResponse.value = "⚠️ Gemini API Key is missing!\n\n" +
                        "To use advanced AI analysis, please click the gear gear icon \"⚙️\" in the top right to paste your Gemini API Key.\n\n" +
                        "Or, enter your GEMINI_API_KEY inside the Secrets Panel of Google AI Studio securely and restart the application."
                _isAnalyzing.value = false
                return@launch
            }

            try {
                val sysInstruction = "You are an elite, highly professional AI Software Architect. " +
                        "Explain, evaluate, or optimize code written in the mobile sandbox editor. " +
                        "Always be concise and direct. Keep formatting highly structured with clear titles."

                val prompt = when (action) {
                    "OPTIMIZE" -> "Analyze and optimize the following ${file.language} code for clean styling, runtime efficiency, and modern syntax. " +
                            "Output your suggested code first in a markdown block, followed by 3 short bullet points summarizing the changes:\n\n$currentCode"
                    "EXPLAIN" -> "Offer a highly readable explanation of what this ${file.language} code does, breaking it down logically in 3 simple phases:\n\n$currentCode"
                    "FIND_BUGS" -> "Examine this ${file.language} code for logical bugs, runtime exceptions, typing errors, or edge cases. " +
                            "Highlight any problems and suggest corrective actions:\n\n$currentCode"
                    else -> "Analyze the following code:\n\n$currentCode"
                }

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    systemInstruction = Content(parts = listOf(Part(text = sysInstruction))),
                    generationConfig = GenerationConfig(temperature = 0.2f)
                )

                val response = RetrofitClient.service.generateContent(apiKey, request)
                val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (responseText != null) {
                    _aiAssistantResponse.value = responseText.trim()
                    addLog("SYSTEM", "AI review generated successfully. Read details in the overlay sheet.")
                } else {
                    _aiAssistantResponse.value = "⚠️ Error: Empty response parsed from AI gateway."
                }
            } catch (e: Exception) {
                _aiAssistantResponse.value = "⚠️ Gemini Client Exception:\n\n${e.localizedMessage}\n\nPlease check your internet connection or API keys config."
                addLog("ERROR", "API action failed: ${e.localizedMessage}")
            }
            _isAnalyzing.value = false
        }
    }

    fun applyOptimizedCode(codeBlock: String) {
        // Simple extraction block if markdown wrapper exists
        var cleaned = codeBlock
        if (cleaned.contains("```")) {
            val lines = cleaned.split("\n")
            val codeLines = mutableListOf<String>()
            var inCodeBlock = false
            for (line in lines) {
                if (line.trim().startsWith("```")) {
                    inCodeBlock = !inCodeBlock
                    continue
                }
                if (inCodeBlock) {
                    codeLines.add(line)
                }
            }
            if (codeLines.isNotEmpty()) {
                cleaned = codeLines.joinToString("\n")
            }
        }
        
        editorText = cleaned
        saveCurrentFile()
        _showAiAssistantSheet.value = false
        viewModelScope.launch {
            addLog("SYSTEM", "AI Suggested code successfully applied to the current editor session.")
        }
    }
}
