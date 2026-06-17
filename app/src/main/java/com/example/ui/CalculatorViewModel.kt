package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CalculationHistory
import com.example.data.CalculationRepository
import com.example.util.MathEvaluator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CalculatorViewModel(private val repository: CalculationRepository) : ViewModel() {

    private val _expression = MutableStateFlow("")
    val expression: StateFlow<String> = _expression.asStateFlow()

    private val _realtimeResult = MutableStateFlow("")
    val realtimeResult: StateFlow<String> = _realtimeResult.asStateFlow()

    val historyList: StateFlow<List<CalculationHistory>> = repository.historyList
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onButtonClick(btn: String) {
        when (btn) {
            "AC" -> {
                _expression.value = ""
                _realtimeResult.value = ""
            }
            "⌫" -> {
                val current = _expression.value
                if (current.isNotEmpty()) {
                    val nextExpr = if (current.endsWith(" ")) {
                        // Deletes operator with its pre-and-post spaces, e.g., " + " -> delete 3 chars
                        if (current.length >= 3) {
                            current.substring(0, current.length - 3)
                        } else {
                            ""
                        }
                    } else {
                        current.substring(0, current.length - 1)
                    }
                    _expression.value = nextExpr
                    updateRealtimeResult(nextExpr)
                }
            }
            "=" -> {
                evaluateFinal()
            }
            else -> {
                val current = _expression.value
                val nextExpr = appendSmart(current, btn)
                _expression.value = nextExpr
                updateRealtimeResult(nextExpr)
            }
        }
    }

    fun onHistoryItemClick(history: CalculationHistory) {
         _expression.value = history.expression
         _realtimeResult.value = history.result
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAll()
        }
    }

    fun deleteHistoryItem(id: Int) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    private fun appendSmart(current: String, btn: String): String {
        return when (btn) {
            "+", "-", "×", "÷" -> {
                val op = " $btn "
                if (current.isEmpty()) {
                    if (btn == "-") op else "" // Allow negative prefix
                } else if (current.endsWith(" + ") || current.endsWith(" - ") || 
                           current.endsWith(" × ") || current.endsWith(" ÷ ")) {
                    // Replace operator
                    current.substring(0, current.length - 3) + op
                } else if (current.endsWith("(")) {
                    if (btn == "-") current + "-" else current // allow negative number inside paren
                } else {
                    current + op
                }
            }
            "." -> {
                if (current.isEmpty() || current.endsWith(" ") || current.endsWith("(")) {
                    current + "0."
                } else {
                    // Find active number token (from last space/bracket)
                    val lastToken = current.split(" ", "(", ")").lastOrNull() ?: ""
                    if (!lastToken.contains(".")) {
                        current + "."
                    } else {
                        current // Avoid double decimals in single number
                    }
                }
            }
            "%" -> {
                if (current.isNotEmpty() && (current.last().isDigit() || current.endsWith(")"))) {
                    current + "%"
                } else {
                    current
                }
            }
            "(" -> {
                if (current.isEmpty() || current.endsWith(" ") || current.endsWith("(")) {
                    current + "("
                } else if (current.last().isDigit() || current.endsWith(")") || current.endsWith("%")) {
                    current + " × (" // Implicit multiplication representation
                } else {
                    current + "("
                }
            }
            ")" -> {
                val openCount = current.count { it == '(' }
                val closeCount = current.count { it == ')' }
                if (openCount > closeCount && current.isNotEmpty() && 
                    !current.endsWith(" ") && !current.endsWith("(")) {
                    current + ")"
                } else {
                    current
                }
            }
            else -> { // Digits 0-9
                if (current.endsWith(")")) {
                    current + " × $btn" // Implicit multiplication representation
                } else {
                    current + btn
                }
            }
        }
    }

    private fun updateRealtimeResult(expr: String) {
        var sanitized = expr.trim()
        if (sanitized.isEmpty()) {
            _realtimeResult.value = ""
            return
        }

        // Only evaluate if there's actual calculation potential
        // (has at least one operator, percent, or parenthesis)
        val hasOperators = sanitized.any { it in "+-×÷%()" }
        if (!hasOperators) {
            _realtimeResult.value = ""
            return
        }

        // Strip operators at the very end for visual ease
        while (sanitized.endsWith("+") || sanitized.endsWith("-") || 
               sanitized.endsWith("×") || sanitized.endsWith("÷")) {
            sanitized = sanitized.substring(0, sanitized.length - 1).trim()
        }

        if (sanitized.isEmpty()) {
            _realtimeResult.value = ""
            return
        }

        // Balance unclosed brackets for real-time preview
        val openCount = sanitized.count { it == '(' }
        val closeCount = sanitized.count { it == ')' }
        if (openCount > closeCount) {
            sanitized += ")".repeat(openCount - closeCount)
        }

        try {
            val eval = MathEvaluator.evaluate(sanitized)
            _realtimeResult.value = MathEvaluator.formatResult(eval)
        } catch (e: Exception) {
            // Faintly malformed currently, swallow error during typing preview
            _realtimeResult.value = ""
        }
    }

    private fun evaluateFinal() {
        var expr = _expression.value.trim()
        if (expr.isEmpty()) return

        // Strip trailing operator for completeness
        while (expr.endsWith("+") || expr.endsWith("-") || 
               expr.endsWith("×") || expr.endsWith("÷")) {
            expr = expr.substring(0, expr.length - 1).trim()
        }

        // Balance brackets or reject
        val openCount = expr.count { it == '(' }
        val closeCount = expr.count { it == ')' }
        if (openCount > closeCount) {
            expr += ")".repeat(openCount - closeCount)
        }

        try {
            val eval = MathEvaluator.evaluate(expr)
            val formatted = MathEvaluator.formatResult(eval)
            
            // Save to history helper
            val finalExpr = _expression.value // Keep raw presentation for history entries
            viewModelScope.launch {
                repository.insert(CalculationHistory(expression = finalExpr, result = formatted))
            }

            _expression.value = formatted
            _realtimeResult.value = "" // Clean preview after locking in final answer
        } catch (e: Exception) {
            _realtimeResult.value = "Error"
        }
    }
}

class CalculatorViewModelFactory(private val repository: CalculationRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CalculatorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CalculatorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
