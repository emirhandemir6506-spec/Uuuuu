package com.example.data.repository

import com.example.data.database.CodeFile
import com.example.data.database.CodeTerminalDao
import com.example.data.database.TerminalLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CodeTerminalRepository(private val dao: CodeTerminalDao) {

    val allCodeFiles: Flow<List<CodeFile>> = dao.getAllCodeFiles()
    val allTerminalLogs: Flow<List<TerminalLog>> = dao.getAllTerminalLogs()

    suspend fun getCodeFileById(id: Int): CodeFile? = withContext(Dispatchers.IO) {
        dao.getCodeFileById(id)
    }

    suspend fun insertCodeFile(codeFile: CodeFile): Long = withContext(Dispatchers.IO) {
        dao.insertCodeFile(codeFile)
    }

    suspend fun updateCodeFile(codeFile: CodeFile) = withContext(Dispatchers.IO) {
        dao.updateCodeFile(codeFile)
    }

    suspend fun deleteCodeFile(codeFile: CodeFile) = withContext(Dispatchers.IO) {
        dao.deleteCodeFile(codeFile)
    }

    suspend fun insertTerminalLog(log: TerminalLog) = withContext(Dispatchers.IO) {
        dao.insertTerminalLog(log)
    }

    suspend fun clearTerminalLogs() = withContext(Dispatchers.IO) {
        dao.clearAllTerminalLogs()
    }

    suspend fun checkAndSeedDatabase() = withContext(Dispatchers.IO) {
        val count = dao.getCodeFilesCount()
        if (count == 0) {
            // Seed sample files
            dao.insertCodeFile(
                CodeFile(
                    name = "analysis.py",
                    language = "python",
                    isReadOnly = false,
                    content = """# Python Data Analysis Sandbox
import pandas as pd

def analyze_data(data):
    # TODO: Implement AI analysis
    df = pd.DataFrame(data)
    print(f"Analyzing {len(df)} rows..." )
    return {
        "rows": len(df),
        "status": "ready"
    }

metrics = [
    {"build_time": 45, "errors": 0},
    {"build_time": 120, "errors": 3}
]

result = analyze_data(metrics)
print("Execution Complete:", result)
"""
                )
            )

            dao.insertCodeFile(
                CodeFile(
                    name = "calculator.js",
                    language = "javascript",
                    isReadOnly = false,
                    content = """// JavaScript Calculator Sandbox
function computeMetrics(requests, errRate) {
    console.log("Starting server calculation...");
    let result = (requests * (1 - errRate)).toFixed(2);
    return {
        totalRequests: requests,
        successfulRequests: parseFloat(result),
        healthScore: ((1 - errRate) * 100).toFixed(1) + "%"
    };
}

const reqCount = 14205;
const errorPercentage = 0.024;
const totalSuccess = (reqCount * (1 - errorPercentage)).toFixed(2);
const score = ((1 - errorPercentage) * 100).toFixed(1) + "%";
console.log("\n--- SIMULATION RESULTS ---");
console.log("Requests processed successfully: " + totalSuccess);
console.log("Node Health Score: " + score);
"""
                )
            )

            dao.insertCodeFile(
                CodeFile(
                    name = "orders_db.sql",
                    language = "sql",
                    isReadOnly = false,
                    content = """-- SQL Query Metrics
SELECT 
    product_category, 
    COUNT(order_id) as total_purchases 
FROM app_orders
WHERE status = 'DELIVERED' 
GROUP BY product_category;
"""
                )
            )

            dao.insertCodeFile(
                CodeFile(
                    name = "system_diagnostic.sh",
                    language = "sh",
                    isReadOnly = false,
                    content = """#!/bin/bash
# Shell Environment Diagnostic Tool
echo "==========================================="
echo "   GEMINI DEVELOPMENT SANDBOX INITIALIZED  "
echo "==========================================="
echo "System OS  : AI Studio Android VirtIO Cluster"
echo "Cores      : 4-Core ARM64"
echo "Free Memory: 68% OK"
echo "Gemini SDK : ACTIVE Version 3.5-Flash"
echo "==========================================="
echo "Verifying environment integrity..."
sleep 1
echo "STATUS [COMPLETED]: All sandbox environments healthy."
"""
                )
            )

            // Seed initial terminal welcoming message
            dao.insertTerminalLog(
                TerminalLog(
                    type = "SYSTEM",
                    text = "Welcome to Gemini AI Sandbox! Write code on the top panel and run commands in the terminal.\n" +
                            "Type '$ python <filename>' or '$ js <filename>' or '$ run <filename>' to execute.\n" +
                            "Or click 'Ask Gemini 🤖' to let AI review or generate your code."
                )
            )
        }
    }
}
