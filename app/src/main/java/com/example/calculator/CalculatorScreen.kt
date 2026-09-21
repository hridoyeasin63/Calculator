package com.example.calculator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
  viewModel: CalculatorViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

  Surface(
    modifier = modifier.fillMaxSize(),
    color = MaterialTheme.colorScheme.background
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding(),
      contentAlignment = Alignment.TopCenter
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 600.dp)
          .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top Bar
        CalculatorHeader(
          layoutMode = uiState.layoutMode,
          isDegreeMode = uiState.isDegreeMode,
          historyCount = uiState.history.size,
          onSelectLayoutMode = { viewModel.setLayoutMode(it) },
          onToggleScientific = { viewModel.toggleScientific() },
          onToggleAngleMode = { viewModel.toggleAngleMode() },
          onOpenHistory = { viewModel.toggleHistory(true) }
        )

        // Display Area (Expression, Preview, Error, Main Result)
        CalculatorDisplay(
          expression = uiState.expression,
          currentInput = uiState.currentInput,
          previewResult = uiState.previewResult,
          errorMessage = uiState.errorMessage,
          onBackspace = { viewModel.onBackspace() },
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        )

        // Keypad Area
        CalculatorKeypad(
          uiState = uiState,
          viewModel = viewModel,
          modifier = Modifier.fillMaxWidth()
        )
      }

      // History Modal Bottom Sheet
      if (uiState.isHistoryVisible) {
        ModalBottomSheet(
          onDismissRequest = { viewModel.toggleHistory(false) },
          sheetState = sheetState,
          containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
          HistorySheetContent(
            history = uiState.history,
            onSelectItem = { item -> viewModel.onSelectHistoryItem(item) },
            onClearHistory = { viewModel.onClearHistory() },
            onClose = { viewModel.toggleHistory(false) }
          )
        }
      }
    }
  }
}

@Composable
private fun CalculatorHeader(
  layoutMode: CalculatorLayoutMode,
  isDegreeMode: Boolean,
  historyCount: Int,
  onSelectLayoutMode: (CalculatorLayoutMode) -> Unit,
  onToggleScientific: () -> Unit,
  onToggleAngleMode: () -> Unit,
  onOpenHistory: () -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    // Mode Switcher Pill: Basic | Scientific
    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(20.dp))
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        .padding(3.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      ModeTab(
        text = "Basic",
        isSelected = layoutMode == CalculatorLayoutMode.BASIC,
        onClick = { onSelectLayoutMode(CalculatorLayoutMode.BASIC) },
        testTag = "button_mode_basic"
      )
      ModeTab(
        text = "Scientific",
        isSelected = layoutMode == CalculatorLayoutMode.SCIENTIFIC,
        onClick = { onSelectLayoutMode(CalculatorLayoutMode.SCIENTIFIC) },
        testTag = "button_mode_scientific"
      )
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // DEG / RAD toggle chip button (visible in scientific mode)
      if (layoutMode == CalculatorLayoutMode.SCIENTIFIC) {
        Surface(
          onClick = onToggleAngleMode,
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("button_toggle_angle_mode")
        ) {
          Text(
            text = if (isDegreeMode) "DEG" else "RAD",
            style = MaterialTheme.typography.labelLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            ),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      }

      // Quick toggle icon (keeps button_toggle_scientific tag accessible)
      FilledTonalIconButton(
        onClick = onToggleScientific,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
          containerColor = if (layoutMode == CalculatorLayoutMode.SCIENTIFIC) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
          contentColor = if (layoutMode == CalculatorLayoutMode.SCIENTIFIC) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("button_toggle_scientific")
      ) {
        Icon(
          imageVector = Icons.Default.Functions,
          contentDescription = "Scientific Functions Toggle"
        )
      }

      // History button with badge
      IconButton(
        onClick = onOpenHistory,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("button_open_history")
      ) {
        if (historyCount > 0) {
          BadgedBox(
            badge = {
              Badge(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
              ) {
                Text(text = if (historyCount > 99) "99+" else historyCount.toString())
              }
            }
          ) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = "Calculation History",
              tint = MaterialTheme.colorScheme.onBackground
            )
          }
        } else {
          Icon(
            imageVector = Icons.Outlined.History,
            contentDescription = "Calculation History",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun ModeTab(
  text: String,
  isSelected: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent
  val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant

  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(16.dp))
      .background(backgroundColor)
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 6.dp)
      .minimumInteractiveComponentSize()
      .testTag(testTag),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelLarge.copy(
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
      ),
      color = contentColor
    )
  }
}

@Composable
private fun CalculatorDisplay(
  expression: String,
  currentInput: String,
  previewResult: String?,
  errorMessage: String?,
  onBackspace: () -> Unit,
  modifier: Modifier = Modifier
) {
  val exprScrollState = rememberScrollState()
  val inputScrollState = rememberScrollState()

  LaunchedEffect(expression) {
    exprScrollState.animateScrollTo(exprScrollState.maxValue)
  }
  LaunchedEffect(currentInput) {
    inputScrollState.animateScrollTo(inputScrollState.maxValue)
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.Bottom,
    horizontalAlignment = Alignment.End
  ) {
    // Expression Line
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(exprScrollState),
      horizontalArrangement = Arrangement.End
    ) {
      Text(
        text = expression.ifEmpty { " " },
        style = MaterialTheme.typography.bodyLarge.copy(
          fontSize = 18.sp,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier.testTag("display_expression")
      )
    }

    Spacer(modifier = Modifier.height(2.dp))

    // Real-time calculation preview
    AnimatedVisibility(
      visible = previewResult != null && errorMessage == null,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Text(
        text = previewResult ?: "",
        style = MaterialTheme.typography.titleMedium.copy(
          fontSize = 20.sp,
          fontWeight = FontWeight.Medium
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
          .padding(vertical = 2.dp)
          .testTag("display_preview")
      )
    }

    // Error Message
    AnimatedVisibility(
      visible = errorMessage != null,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      Text(
        text = errorMessage ?: "",
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = FontWeight.SemiBold
        ),
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier
          .padding(vertical = 4.dp)
          .testTag("display_error")
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Main Active Input/Result Line
    val fontSize = when {
      currentInput.length > 15 -> 24.sp
      currentInput.length > 11 -> 32.sp
      currentInput.length > 8 -> 40.sp
      else -> 48.sp
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.End,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier
          .weight(1f, fill = false)
          .horizontalScroll(inputScrollState),
        horizontalArrangement = Arrangement.End
      ) {
        Text(
          text = if (errorMessage != null) "Error" else currentInput.ifEmpty { "0" },
          style = MaterialTheme.typography.displayMedium.copy(
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            fontFamily = FontFamily.SansSerif,
            letterSpacing = (-0.5).sp
          ),
          color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          textAlign = TextAlign.End,
          modifier = Modifier.testTag("display_result")
        )
      }

      if (currentInput.isNotEmpty() && currentInput != "0") {
        IconButton(
          onClick = onBackspace,
          modifier = Modifier
            .padding(start = 6.dp)
            .minimumInteractiveComponentSize()
            .testTag("button_inline_backspace")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.Backspace,
            contentDescription = "Backspace",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}

@Composable
private fun CalculatorKeypad(
  uiState: CalculatorUiState,
  viewModel: CalculatorViewModel,
  modifier: Modifier = Modifier
) {
  AnimatedContent(
    targetState = uiState.layoutMode,
    transitionSpec = {
      fadeIn() togetherWith fadeOut()
    },
    label = "KeypadLayoutModeTransition",
    modifier = modifier
  ) { mode ->
    when (mode) {
      CalculatorLayoutMode.BASIC -> BasicKeypad(uiState, viewModel)
      CalculatorLayoutMode.SCIENTIFIC -> ScientificKeypad(uiState, viewModel)
    }
  }
}

@Composable
private fun BasicKeypad(
  uiState: CalculatorUiState,
  viewModel: CalculatorViewModel,
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current
  val buttonHeight = 66.dp
  val clearLabel = if (uiState.currentInput.isNotEmpty() && uiState.currentInput != "0") "C" else "AC"

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    // Row 1: AC, ±, %, ÷
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      KeypadButton(
        text = clearLabel,
        buttonStyle = ButtonStyle.Action,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          viewModel.onClear()
        },
        testTag = "button_clear",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "±",
        buttonStyle = ButtonStyle.Function,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onToggleSign()
        },
        testTag = "button_toggle_sign",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "%",
        buttonStyle = ButtonStyle.Function,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onPercentage()
        },
        testTag = "button_percent",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "÷",
        buttonStyle = ButtonStyle.Operator,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onOperator("÷")
        },
        testTag = "button_divide",
        modifier = Modifier.weight(1f)
      )
    }

    // Row 2: 7, 8, 9, ×
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("7", "8", "9").forEach { digit ->
        KeypadButton(
          text = digit,
          buttonStyle = ButtonStyle.Number,
          height = buttonHeight,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            viewModel.onDigit(digit)
          },
          testTag = "button_$digit",
          modifier = Modifier.weight(1f)
        )
      }
      KeypadButton(
        text = "×",
        buttonStyle = ButtonStyle.Operator,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onOperator("×")
        },
        testTag = "button_multiply",
        modifier = Modifier.weight(1f)
      )
    }

    // Row 3: 4, 5, 6, −
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("4", "5", "6").forEach { digit ->
        KeypadButton(
          text = digit,
          buttonStyle = ButtonStyle.Number,
          height = buttonHeight,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            viewModel.onDigit(digit)
          },
          testTag = "button_$digit",
          modifier = Modifier.weight(1f)
        )
      }
      KeypadButton(
        text = "−",
        buttonStyle = ButtonStyle.Operator,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onOperator("−")
        },
        testTag = "button_subtract",
        modifier = Modifier.weight(1f)
      )
    }

    // Row 4: 1, 2, 3, +
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      listOf("1", "2", "3").forEach { digit ->
        KeypadButton(
          text = digit,
          buttonStyle = ButtonStyle.Number,
          height = buttonHeight,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            viewModel.onDigit(digit)
          },
          testTag = "button_$digit",
          modifier = Modifier.weight(1f)
        )
      }
      KeypadButton(
        text = "+",
        buttonStyle = ButtonStyle.Operator,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onOperator("+")
        },
        testTag = "button_add",
        modifier = Modifier.weight(1f)
      )
    }

    // Row 5: 0, ., ⌫, =
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      KeypadButton(
        text = "0",
        buttonStyle = ButtonStyle.Number,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDigit("0")
        },
        testTag = "button_0",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = ".",
        buttonStyle = ButtonStyle.Number,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDecimal()
        },
        testTag = "button_decimal",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "⌫",
        buttonStyle = ButtonStyle.Function,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onBackspace()
        },
        testTag = "button_backspace",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "=",
        buttonStyle = ButtonStyle.Equals,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          viewModel.onEquals()
        },
        testTag = "button_equals",
        modifier = Modifier.weight(1f)
      )
    }
  }
}

@Composable
private fun ScientificKeypad(
  uiState: CalculatorUiState,
  viewModel: CalculatorViewModel,
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current
  val buttonHeight = 50.dp
  val clearLabel = if (uiState.currentInput.isNotEmpty() && uiState.currentInput != "0") "C" else "AC"

  val isInv = uiState.isInverseTrig
  val sinLabel = if (isInv) "sin⁻¹" else "sin"
  val cosLabel = if (isInv) "cos⁻¹" else "cos"
  val tanLabel = if (isInv) "tan⁻¹" else "tan"

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    // Row 1: INV, sin/sin⁻¹, cos/cos⁻¹, tan/tan⁻¹, ln
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      KeypadButton(
        text = "INV",
        buttonStyle = ButtonStyle.Scientific,
        isActive = isInv,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.toggleInverseTrig()
        },
        testTag = "button_inv",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = sinLabel,
        buttonStyle = ButtonStyle.Scientific,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onScientific(sinLabel)
        },
        testTag = if (isInv) "button_sin_inv" else "button_sin",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = cosLabel,
        buttonStyle = ButtonStyle.Scientific,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onScientific(cosLabel)
        },
        testTag = if (isInv) "button_cos_inv" else "button_cos",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = tanLabel,
        buttonStyle = ButtonStyle.Scientific,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onScientific(tanLabel)
        },
        testTag = if (isInv) "button_tan_inv" else "button_tan",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "ln",
        buttonStyle = ButtonStyle.Scientific,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onScientific("ln")
        },
        testTag = "button_ln",
        modifier = Modifier.weight(1f)
      )
    }

    // Row 2: log, √, ^, (, )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      KeypadButton(
        text = "log",
        buttonStyle = ButtonStyle.Scientific,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onScientific("log")
        },
        testTag = "button_log",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "√",
        buttonStyle = ButtonStyle.Scientific,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onScientific("√")
        },
        testTag = "button_sqrt",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "^",
        buttonStyle = ButtonStyle.Scientific,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onScientific("^")
        },
        testTag = "button_power",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "(",
        buttonStyle = ButtonStyle.Scientific,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onScientific("(")
        },
        testTag = "button_open_paren",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = ")",
        buttonStyle = ButtonStyle.Scientific,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onScientific(")")
        },
        testTag = "button_close_paren",
        modifier = Modifier.weight(1f)
      )
    }

    // Row 3: x², !, π, e, Clear
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      KeypadButton(
        text = "x²",
        buttonStyle = ButtonStyle.Scientific,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onScientific("x²")
        },
        testTag = "button_square",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "!",
        buttonStyle = ButtonStyle.Scientific,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onScientific("!")
        },
        testTag = "button_factorial",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "π",
        buttonStyle = ButtonStyle.Scientific,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onScientific("π")
        },
        testTag = "button_pi",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "e",
        buttonStyle = ButtonStyle.Scientific,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onScientific("e")
        },
        testTag = "button_e",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = clearLabel,
        buttonStyle = ButtonStyle.Action,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          viewModel.onClear()
        },
        testTag = "button_clear",
        modifier = Modifier.weight(1f)
      )
    }

    // Row 4: 7, 8, 9, ⌫, ÷
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      listOf("7", "8", "9").forEach { digit ->
        KeypadButton(
          text = digit,
          buttonStyle = ButtonStyle.Number,
          height = buttonHeight,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            viewModel.onDigit(digit)
          },
          testTag = "button_$digit",
          modifier = Modifier.weight(1f)
        )
      }
      KeypadButton(
        text = "⌫",
        buttonStyle = ButtonStyle.Function,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onBackspace()
        },
        testTag = "button_backspace",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "÷",
        buttonStyle = ButtonStyle.Operator,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onOperator("÷")
        },
        testTag = "button_divide",
        modifier = Modifier.weight(1f)
      )
    }

    // Row 5: 4, 5, 6, %, ×
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      listOf("4", "5", "6").forEach { digit ->
        KeypadButton(
          text = digit,
          buttonStyle = ButtonStyle.Number,
          height = buttonHeight,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            viewModel.onDigit(digit)
          },
          testTag = "button_$digit",
          modifier = Modifier.weight(1f)
        )
      }
      KeypadButton(
        text = "%",
        buttonStyle = ButtonStyle.Function,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onPercentage()
        },
        testTag = "button_percent",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "×",
        buttonStyle = ButtonStyle.Operator,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onOperator("×")
        },
        testTag = "button_multiply",
        modifier = Modifier.weight(1f)
      )
    }

    // Row 6: 1, 2, 3, ±, −
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      listOf("1", "2", "3").forEach { digit ->
        KeypadButton(
          text = digit,
          buttonStyle = ButtonStyle.Number,
          height = buttonHeight,
          onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            viewModel.onDigit(digit)
          },
          testTag = "button_$digit",
          modifier = Modifier.weight(1f)
        )
      }
      KeypadButton(
        text = "±",
        buttonStyle = ButtonStyle.Function,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onToggleSign()
        },
        testTag = "button_toggle_sign",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "−",
        buttonStyle = ButtonStyle.Operator,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onOperator("−")
        },
        testTag = "button_subtract",
        modifier = Modifier.weight(1f)
      )
    }

    // Row 7: 0 (weight 2f), . (weight 1f), + (weight 1f), = (weight 1f)
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      KeypadButton(
        text = "0",
        buttonStyle = ButtonStyle.Number,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDigit("0")
        },
        testTag = "button_0",
        modifier = Modifier.weight(2f)
      )
      KeypadButton(
        text = ".",
        buttonStyle = ButtonStyle.Number,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDecimal()
        },
        testTag = "button_decimal",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "+",
        buttonStyle = ButtonStyle.Operator,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onOperator("+")
        },
        testTag = "button_add",
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "=",
        buttonStyle = ButtonStyle.Equals,
        height = buttonHeight,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          viewModel.onEquals()
        },
        testTag = "button_equals",
        modifier = Modifier.weight(1f)
      )
    }
  }
}

enum class ButtonStyle {
  Number,
  Operator,
  Function,
  Action,
  Equals,
  Scientific
}

@Composable
private fun KeypadButton(
  text: String,
  buttonStyle: ButtonStyle,
  height: androidx.compose.ui.unit.Dp,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isActive: Boolean = false,
  testTag: String? = null
) {
  val containerColor = when {
    isActive -> MaterialTheme.colorScheme.primary
    buttonStyle == ButtonStyle.Number -> MaterialTheme.colorScheme.surfaceContainerHigh
    buttonStyle == ButtonStyle.Operator -> MaterialTheme.colorScheme.primaryContainer
    buttonStyle == ButtonStyle.Function -> MaterialTheme.colorScheme.secondaryContainer
    buttonStyle == ButtonStyle.Action -> MaterialTheme.colorScheme.surfaceVariant
    buttonStyle == ButtonStyle.Equals -> MaterialTheme.colorScheme.primary
    buttonStyle == ButtonStyle.Scientific -> MaterialTheme.colorScheme.surfaceContainer
    else -> MaterialTheme.colorScheme.surfaceContainer
  }

  val contentColor = when {
    isActive -> MaterialTheme.colorScheme.onPrimary
    buttonStyle == ButtonStyle.Number -> MaterialTheme.colorScheme.onSurface
    buttonStyle == ButtonStyle.Operator -> MaterialTheme.colorScheme.onPrimaryContainer
    buttonStyle == ButtonStyle.Function -> MaterialTheme.colorScheme.secondary
    buttonStyle == ButtonStyle.Action -> MaterialTheme.colorScheme.error
    buttonStyle == ButtonStyle.Equals -> MaterialTheme.colorScheme.onPrimary
    buttonStyle == ButtonStyle.Scientific -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.onSurface
  }

  val testTagKey = testTag ?: when (text) {
    "+" -> "button_add"
    "−" -> "button_subtract"
    "×" -> "button_multiply"
    "÷" -> "button_divide"
    "=" -> "button_equals"
    "AC", "C" -> "button_clear"
    "±" -> "button_toggle_sign"
    "%" -> "button_percent"
    "⌫" -> "button_backspace"
    "." -> "button_decimal"
    "^" -> "button_power"
    "√" -> "button_sqrt"
    "x²" -> "button_square"
    "!" -> "button_factorial"
    "sin" -> "button_sin"
    "cos" -> "button_cos"
    "tan" -> "button_tan"
    "sin⁻¹" -> "button_sin_inv"
    "cos⁻¹" -> "button_cos_inv"
    "tan⁻¹" -> "button_tan_inv"
    "log" -> "button_log"
    "ln" -> "button_ln"
    "INV" -> "button_inv"
    else -> "button_$text"
  }

  val shape = RoundedCornerShape(16.dp)

  Box(
    modifier = modifier
      .height(height)
      .clip(shape)
      .background(containerColor)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = androidx.compose.material3.ripple(bounded = true),
        onClick = onClick
      )
      .minimumInteractiveComponentSize()
      .testTag(testTagKey),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.titleMedium.copy(
        fontSize = if (buttonStyle == ButtonStyle.Scientific) 16.sp else 22.sp,
        fontWeight = if (buttonStyle == ButtonStyle.Number) FontWeight.Medium else FontWeight.SemiBold
      ),
      color = contentColor,
      textAlign = TextAlign.Center
    )
  }
}

@Composable
private fun HistorySheetContent(
  history: List<CalculationHistoryItem>,
  onSelectItem: (CalculationHistoryItem) -> Unit,
  onClearHistory: () -> Unit,
  onClose: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
      .padding(bottom = 32.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = "Calculation History",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        if (history.isNotEmpty()) {
          IconButton(
            onClick = onClearHistory,
            modifier = Modifier.testTag("button_clear_history")
          ) {
            Icon(
              imageVector = Icons.Default.DeleteSweep,
              contentDescription = "Clear History",
              tint = MaterialTheme.colorScheme.error
            )
          }
        }
        IconButton(
          onClick = onClose,
          modifier = Modifier.testTag("button_close_history")
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    if (history.isEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.History,
          contentDescription = null,
          modifier = Modifier.size(56.dp),
          tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "No calculations yet",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
          text = "Completed calculations will appear here",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .height(360.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(history, key = { it.id }) { item ->
          Card(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectItem(item) }
              .testTag("history_item_${item.id}"),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(14.dp)
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
              horizontalAlignment = Alignment.End
            ) {
              Text(
                text = item.expression,
                style = MaterialTheme.typography.bodyMedium.copy(
                  fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "= ${item.result}",
                style = MaterialTheme.typography.titleLarge.copy(
                  fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }
    }
  }
}
