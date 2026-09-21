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

enum class CalculatorLayoutMode {
  BASIC,
  SCIENTIFIC
}

data class CalculatorUiState(
  val expression: String = "",
  val currentInput: String = "0",
  val previewResult: String? = null,
  val errorMessage: String? = null,
  val isNewCalculation: Boolean = false,
  val isDegreeMode: Boolean = true, // DEG vs RAD
  val history: List<CalculationHistoryItem> = emptyList(),
  val layoutMode: CalculatorLayoutMode = CalculatorLayoutMode.BASIC,
  val isInverseTrig: Boolean = false, // When true in scientific mode, toggles sin⁻¹, cos⁻¹, tan⁻¹
  val isHistoryVisible: Boolean = false
) {
  val isScientificExpanded: Boolean
    get() = layoutMode == CalculatorLayoutMode.SCIENTIFIC
}

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
      val preview = calculatePreview(fullExpr, state.isDegreeMode)

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
      val lastNumber = current.split(Regex("[+\\-×÷^√()!]")).lastOrNull() ?: ""
      if (lastNumber.contains('.')) {
        return@update state
      }

      val newInput = if (current.isEmpty()) "0." else "$current."
      val fullExpr = buildFullExpression(state.expression, newInput)
      val preview = calculatePreview(fullExpr, state.isDegreeMode)

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

      if (current.isNotEmpty()) {
        expr = if (expr.isEmpty()) {
          "$current $op "
        } else {
          "$expr$current $op "
        }
      } else if (expr.isNotEmpty()) {
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

      val preview = calculatePreview(expr.trimEnd(), state.isDegreeMode)
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
        val preview = calculatePreview(fullExpr, state.isDegreeMode)
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
      val preview = calculatePreview(fullExpr, state.isDegreeMode)

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
        val preview = calculatePreview(fullExpr, state.isDegreeMode)
        state.copy(currentInput = newInput, previewResult = preview)
      } else if (state.expression.isNotEmpty()) {
        val trimmed = state.expression.trimEnd()
        val parts = trimmed.split(" ")
        if (parts.size > 1) {
          val newExpr = parts.dropLast(2).joinToString(" ").let { if (it.isNotEmpty()) "$it " else "" }
          val prevInput = parts[parts.size - 2]
          val fullExpr = buildFullExpression(newExpr, prevInput)
          val preview = calculatePreview(fullExpr, state.isDegreeMode)
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
        val fullExpr = state.expression.trimEnd()
        val preview = calculatePreview(fullExpr, state.isDegreeMode)
        state.copy(currentInput = "0", errorMessage = null, previewResult = preview)
      } else {
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

      val result = CalculatorEngine.evaluate(fullExpr, state.isDegreeMode)
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
            history = listOf(historyItem) + state.history.take(49)
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
      val current = state.currentInput
      val isNew = state.isNewCalculation || current == "0"

      when (fn) {
        "sin", "cos", "tan", "log", "ln", "asin", "acos", "atan", "sin⁻¹", "cos⁻¹", "tan⁻¹" -> {
          val formattedFn = when (fn) {
            "asin" -> "sin⁻¹"
            "acos" -> "cos⁻¹"
            "atan" -> "tan⁻¹"
            else -> fn
          }
          val newInput = if (isNew) "$formattedFn(" else "$current$formattedFn("
          val fullExpr = buildFullExpression(state.expression, newInput)
          state.copy(currentInput = newInput, isNewCalculation = false, previewResult = calculatePreview(fullExpr, state.isDegreeMode))
        }
        "√" -> {
          val newInput = if (isNew) "√(" else "$current√("
          val fullExpr = buildFullExpression(state.expression, newInput)
          state.copy(currentInput = newInput, isNewCalculation = false, previewResult = calculatePreview(fullExpr, state.isDegreeMode))
        }
        "^" -> {
          onOperator("^")
          return@update _uiState.value
        }
        "x²" -> {
          // Square current number/expression
          if (current.isNotEmpty() && current != "0") {
            val newInput = "$current^2"
            val fullExpr = buildFullExpression(state.expression, newInput)
            state.copy(currentInput = newInput, isNewCalculation = false, previewResult = calculatePreview(fullExpr, state.isDegreeMode))
          } else {
            state
          }
        }
        "!" -> {
          if (current.isNotEmpty() && current != "0") {
            val newInput = "$current!"
            val fullExpr = buildFullExpression(state.expression, newInput)
            state.copy(currentInput = newInput, isNewCalculation = false, previewResult = calculatePreview(fullExpr, state.isDegreeMode))
          } else {
            state
          }
        }
        "(" -> {
          val newInput = if (isNew) "(" else "$current("
          val fullExpr = buildFullExpression(state.expression, newInput)
          state.copy(currentInput = newInput, isNewCalculation = false, previewResult = calculatePreview(fullExpr, state.isDegreeMode))
        }
        ")" -> {
          val newInput = "$current)"
          val fullExpr = buildFullExpression(state.expression, newInput)
          state.copy(currentInput = newInput, previewResult = calculatePreview(fullExpr, state.isDegreeMode))
        }
        "π" -> {
          val newInput = if (isNew) "π" else "${current}π"
          val fullExpr = buildFullExpression(state.expression, newInput)
          state.copy(currentInput = newInput, isNewCalculation = false, previewResult = calculatePreview(fullExpr, state.isDegreeMode))
        }
        "e" -> {
          val newInput = if (isNew) "e" else "${current}e"
          val fullExpr = buildFullExpression(state.expression, newInput)
          state.copy(currentInput = newInput, isNewCalculation = false, previewResult = calculatePreview(fullExpr, state.isDegreeMode))
        }
        else -> state
      }
    }
  }

  fun toggleAngleMode() {
    _uiState.update { state ->
      val newMode = !state.isDegreeMode
      val fullExpr = buildFullExpression(state.expression, state.currentInput)
      val preview = calculatePreview(fullExpr, newMode)
      state.copy(isDegreeMode = newMode, previewResult = preview)
    }
  }

  fun setLayoutMode(mode: CalculatorLayoutMode) {
    _uiState.update { it.copy(layoutMode = mode) }
  }

  fun toggleScientific() {
    _uiState.update {
      val next = if (it.layoutMode == CalculatorLayoutMode.BASIC) CalculatorLayoutMode.SCIENTIFIC else CalculatorLayoutMode.BASIC
      it.copy(layoutMode = next)
    }
  }

  fun toggleInverseTrig() {
    _uiState.update { it.copy(isInverseTrig = !it.isInverseTrig) }
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

  private fun calculatePreview(expr: String, isDegreeMode: Boolean): String? {
    if (expr.isBlank() || isOperator(expr.takeLast(1).trim())) return null
    if (!expr.any { it in "+-×÷^√%!sincostanlog" } &&
      !expr.contains("asin") && !expr.contains("acos") && !expr.contains("atan") &&
      !expr.contains("sin⁻¹") && !expr.contains("cos⁻¹") && !expr.contains("tan⁻¹")
    ) return null

    val res = CalculatorEngine.evaluate(expr, isDegreeMode)
    return res.getOrNull()?.let { "= ${CalculatorEngine.formatResult(it)}" }
  }

  private fun isOperator(str: String): Boolean {
    val trimmed = str.trim()
    return trimmed in listOf("+", "-", "−", "×", "*", "÷", "/", "^")
  }
}
