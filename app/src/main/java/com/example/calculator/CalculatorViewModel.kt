package com.example.calculator

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CalculationHistoryItem(
  val id: Long,
  val expression: String,
  val result: String,
  val timestamp: Long = System.currentTimeMillis()
)

data class CalculatorUiState(
  val expression: String = "",
  val currentInput: String = "0",
  val previewResult: String? = null,
  val errorMessage: String? = null,
  val isNewCalculation: Boolean = false,
  val history: List<CalculationHistoryItem> = emptyList(),
  val isScientificExpanded: Boolean = false,
  val isHistoryVisible: Boolean = false
)

class CalculatorViewModel : ViewModel() {

  private val _uiState = MutableStateFlow(CalculatorUiState())
  val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

  fun onDigit(digit: String) {
    _uiState.update { state ->
      val newErrorMessage = null
      val isNew = state.isNewCalculation

      val newInput = if (isNew || state.currentInput == "0") {
        digit
      } else {
        state.currentInput + digit
      }

      val fullExpr = buildFullExpression(state.expression, newInput)
      val preview = calculatePreview(fullExpr)

      state.copy(
        currentInput = newInput,
        errorMessage = newErrorMessage,
        isNewCalculation = false,
        previewResult = preview
      )
    }
  }

  fun onDecimal() {
    _uiState.update { state ->
      val isNew = state.isNewCalculation
      val current = if (isNew) "0" else state.currentInput

      // Check if current token already has decimal point
      val lastNumber = current.split(Regex("[+\\-×÷^√()]")).lastOrNull() ?: ""
      if (lastNumber.contains('.')) {
        return@update state // already has decimal point
      }

      val newInput = if (current.isEmpty()) "0." else "$current."
      val fullExpr = buildFullExpression(state.expression, newInput)
      val preview = calculatePreview(fullExpr)

      state.copy(
        currentInput = newInput,
        errorMessage = null,
        isNewCalculation = false,
        previewResult = preview
      )
    }
  }

  fun onOperator(op: String) {
    _uiState.update { state ->
      val current = state.currentInput
      var expr = state.expression

      if (state.errorMessage != null) {
        return@update state.copy(errorMessage = null)
      }

      // If there is current input, commit it into expression with the operator
      if (current.isNotEmpty()) {
        expr = if (expr.isEmpty()) {
          "$current $op "
        } else {
          "$expr$current $op "
        }
      } else if (expr.isNotEmpty()) {
        // Replace last operator if expression ends with an operator
        val trimmed = expr.trimEnd()
        val tokens = trimmed.split(" ")
        if (tokens.isNotEmpty() && isOperator(tokens.last())) {
          expr = tokens.dropLast(1).joinToString(" ").let {
            if (it.isEmpty()) "$op " else "$it $op "
          }
        } else {
          expr = "$expr $op "
        }
      } else {
        expr = "0 $op "
      }

      val preview = calculatePreview(expr.trimEnd())
      state.copy(
        expression = expr,
        currentInput = "",
        previewResult = preview,
        errorMessage = null,
        isNewCalculation = false
      )
    }
  }

  fun onPercentage() {
    _uiState.update { state ->
      val current = state.currentInput
      if (current.isNotEmpty()) {
        val fullExpr = buildFullExpression(state.expression, "$current%")
        val preview = calculatePreview(fullExpr)
        state.copy(
          currentInput = "$current%",
          previewResult = preview
        )
      } else {
        state
      }
    }
  }

  fun onToggleSign() {
    _uiState.update { state ->
      val current = state.currentInput
      if (current.isEmpty() || current == "0") return@update state

      val newInput = if (current.startsWith("-")) {
        current.substring(1)
      } else {
        "-$current"
      }

      val fullExpr = buildFullExpression(state.expression, newInput)
      val preview = calculatePreview(fullExpr)

      state.copy(
        currentInput = newInput,
        previewResult = preview
      )
    }
  }

  fun onBackspace() {
    _uiState.update { state ->
      if (state.errorMessage != null) {
        return@update state.copy(errorMessage = null, currentInput = "0")
      }

      if (state.isNewCalculation) {
        return@update state.copy(currentInput = "0", isNewCalculation = false, previewResult = null)
      }

      val current = state.currentInput
      if (current.isNotEmpty()) {
        val updated = current.dropLast(1)
        val newInput = if (updated.isEmpty() && state.expression.isEmpty()) "0" else updated
        val fullExpr = buildFullExpression(state.expression, newInput)
        val preview = calculatePreview(fullExpr)
        state.copy(currentInput = newInput, previewResult = preview)
      } else if (state.expression.isNotEmpty()) {
        // Step back into expression
        val trimmed = state.expression.trimEnd()
        val parts = trimmed.split(" ")
        if (parts.size > 1) {
          // drop last operator and make previous number the current input
          val newExpr = parts.dropLast(2).joinToString(" ").let { if (it.isNotEmpty()) "$it " else "" }
          val prevInput = parts[parts.size - 2]
          val fullExpr = buildFullExpression(newExpr, prevInput)
          val preview = calculatePreview(fullExpr)
          state.copy(
            expression = newExpr,
            currentInput = prevInput,
            previewResult = preview
          )
        } else {
          state.copy(expression = "", currentInput = parts.firstOrNull() ?: "0", previewResult = null)
        }
      } else {
        state
      }
    }
  }

  fun onClear() {
    _uiState.update { state ->
      if (state.currentInput.isNotEmpty() && state.currentInput != "0") {
        // First clear clears active input
        val fullExpr = state.expression.trimEnd()
        val preview = calculatePreview(fullExpr)
        state.copy(currentInput = "0", errorMessage = null, previewResult = preview)
      } else {
        // All clear (AC)
        state.copy(
          expression = "",
          currentInput = "0",
          previewResult = null,
          errorMessage = null,
          isNewCalculation = false
        )
      }
    }
  }

  fun onEquals() {
    _uiState.update { state ->
      val fullExpr = buildFullExpression(state.expression, state.currentInput).trim()
      if (fullExpr.isBlank()) return@update state

      val result = CalculatorEngine.evaluate(fullExpr)
      result.fold(
        onSuccess = { value ->
          val formatted = CalculatorEngine.formatResult(value)
          val historyItem = CalculationHistoryItem(
            id = System.currentTimeMillis(),
            expression = fullExpr,
            result = formatted
          )
          state.copy(
            expression = "$fullExpr =",
            currentInput = formatted,
            previewResult = null,
            errorMessage = null,
            isNewCalculation = true,
            history = listOf(historyItem) + state.history.take(29)
          )
        },
        onFailure = { error ->
          val errorMsg = error.message ?: "Error"
          state.copy(
            errorMessage = errorMsg,
            previewResult = null
          )
        }
      )
    }
  }

  fun onScientific(fn: String) {
    _uiState.update { state ->
      when (fn) {
        "√" -> {
          val newInput = if (state.currentInput == "0" || state.isNewCalculation) "√(" else state.currentInput + "√("
          val fullExpr = buildFullExpression(state.expression, newInput)
          state.copy(currentInput = newInput, isNewCalculation = false, previewResult = calculatePreview(fullExpr))
        }
        "(" -> {
          val newInput = if (state.currentInput == "0" || state.isNewCalculation) "(" else state.currentInput + "("
          val fullExpr = buildFullExpression(state.expression, newInput)
          state.copy(currentInput = newInput, isNewCalculation = false, previewResult = calculatePreview(fullExpr))
        }
        ")" -> {
          val newInput = state.currentInput + ")"
          val fullExpr = buildFullExpression(state.expression, newInput)
          state.copy(currentInput = newInput, previewResult = calculatePreview(fullExpr))
        }
        "^" -> {
          onOperator("^")
          return@update _uiState.value
        }
        "π" -> {
          val newInput = if (state.currentInput == "0" || state.isNewCalculation) "π" else state.currentInput + "π"
          val fullExpr = buildFullExpression(state.expression, newInput)
          state.copy(currentInput = newInput, isNewCalculation = false, previewResult = calculatePreview(fullExpr))
        }
        else -> state
      }
    }
  }

  fun toggleScientific() {
    _uiState.update { it.copy(isScientificExpanded = !it.isScientificExpanded) }
  }

  fun toggleHistory(show: Boolean? = null) {
    _uiState.update { it.copy(isHistoryVisible = show ?: !it.isHistoryVisible) }
  }

  fun onSelectHistoryItem(item: CalculationHistoryItem) {
    _uiState.update { state ->
      state.copy(
        expression = "",
        currentInput = item.result,
        previewResult = null,
        errorMessage = null,
        isNewCalculation = true,
        isHistoryVisible = false
      )
    }
  }

  fun onClearHistory() {
    _uiState.update { it.copy(history = emptyList()) }
  }

  private fun buildFullExpression(expr: String, input: String): String {
    val cleanExpr = expr.trimEnd()
    val cleanInput = input.trim()
    return when {
      cleanExpr.isEmpty() -> cleanInput
      cleanInput.isEmpty() -> cleanExpr
      cleanExpr.endsWith("=") -> cleanInput
      else -> "$cleanExpr $cleanInput"
    }
  }

  private fun calculatePreview(expr: String): String? {
    if (expr.isBlank() || isOperator(expr.takeLast(1).trim())) return null
    // If expression doesn't contain operators, no need to show preview
    if (!expr.any { it in "+-×÷^√%" }) return null

    val res = CalculatorEngine.evaluate(expr)
    return res.getOrNull()?.let { "= ${CalculatorEngine.formatResult(it)}" }
  }

  private fun isOperator(str: String): Boolean {
    val trimmed = str.trim()
    return trimmed in listOf("+", "-", "−", "×", "*", "÷", "/", "^")
  }
}
