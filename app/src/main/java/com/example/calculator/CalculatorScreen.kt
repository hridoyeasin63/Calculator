package com.example.calculator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
          .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.SpaceBetween
      ) {
        // Top Bar
        CalculatorHeader(
          isScientific = uiState.isScientificExpanded,
          historyCount = uiState.history.size,
          onToggleScientific = { viewModel.toggleScientific() },
          onOpenHistory = { viewModel.toggleHistory(true) }
        )

        // Display Area (Expression, Result, Error)
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
  isScientific: Boolean,
  historyCount: Int,
  onToggleScientific: () -> Unit,
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
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Calculate,
        contentDescription = "Calculator Icon",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(28.dp)
      )
      Text(
        text = "Calculator",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        ),
        color = MaterialTheme.colorScheme.onBackground
      )
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(4.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Toggle Scientific functions
      FilledTonalIconButton(
        onClick = onToggleScientific,
        colors = IconButtonDefaults.filledTonalIconButtonColors(
          containerColor = if (isScientific) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
          contentColor = if (isScientific) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
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

  // Auto scroll expression and input to the end
  LaunchedEffect(expression) {
    exprScrollState.animateScrollTo(exprScrollState.maxValue)
  }
  LaunchedEffect(currentInput) {
    inputScrollState.animateScrollTo(inputScrollState.maxValue)
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp, vertical = 12.dp),
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
          fontSize = 20.sp,
          fontFamily = FontFamily.Monospace,
          letterSpacing = 1.sp
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier.testTag("display_expression")
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Real-time calculation preview
    AnimatedVisibility(
      visible = previewResult != null && errorMessage == null,
      enter = fadeIn(),
      exit = fadeOut()
    ) {
      Text(
        text = previewResult ?: "",
        style = MaterialTheme.typography.titleMedium.copy(
          fontSize = 22.sp,
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

    Spacer(modifier = Modifier.height(6.dp))

    // Main Active Input/Result Line
    val fontSize = when {
      currentInput.length > 15 -> 28.sp
      currentInput.length > 11 -> 36.sp
      currentInput.length > 8 -> 44.sp
      else -> 54.sp
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

      // Inline backspace icon button if input has characters
      if (currentInput.isNotEmpty() && currentInput != "0") {
        IconButton(
          onClick = onBackspace,
          modifier = Modifier
            .padding(start = 8.dp)
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
  val haptic = LocalHapticFeedback.current

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    // Scientific Row (Optional expandable)
    AnimatedVisibility(
      visible = uiState.isScientificExpanded,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically()
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        val sciKeys = listOf("(", ")", "√", "^", "π")
        sciKeys.forEach { key ->
          KeypadButton(
            text = key,
            buttonStyle = ButtonStyle.Scientific,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              viewModel.onScientific(key)
            },
            modifier = Modifier.weight(1f)
          )
        }
      }
    }

    // Row 1: Clear, Sign Toggle, Percent, Divide
    val clearLabel = if (uiState.currentInput.isNotEmpty() && uiState.currentInput != "0") "C" else "AC"
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      KeypadButton(
        text = clearLabel,
        buttonStyle = ButtonStyle.Action,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          viewModel.onClear()
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "±",
        buttonStyle = ButtonStyle.Function,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onToggleSign()
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "%",
        buttonStyle = ButtonStyle.Function,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onPercentage()
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "÷",
        buttonStyle = ButtonStyle.Operator,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onOperator("÷")
        },
        modifier = Modifier.weight(1f)
      )
    }

    // Row 2: 7, 8, 9, Multiply
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      KeypadButton(
        text = "7",
        buttonStyle = ButtonStyle.Number,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDigit("7")
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "8",
        buttonStyle = ButtonStyle.Number,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDigit("8")
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "9",
        buttonStyle = ButtonStyle.Number,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDigit("9")
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "×",
        buttonStyle = ButtonStyle.Operator,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onOperator("×")
        },
        modifier = Modifier.weight(1f)
      )
    }

    // Row 3: 4, 5, 6, Subtract
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      KeypadButton(
        text = "4",
        buttonStyle = ButtonStyle.Number,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDigit("4")
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "5",
        buttonStyle = ButtonStyle.Number,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDigit("5")
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "6",
        buttonStyle = ButtonStyle.Number,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDigit("6")
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "−",
        buttonStyle = ButtonStyle.Operator,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onOperator("−")
        },
        modifier = Modifier.weight(1f)
      )
    }

    // Row 4: 1, 2, 3, Add
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      KeypadButton(
        text = "1",
        buttonStyle = ButtonStyle.Number,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDigit("1")
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "2",
        buttonStyle = ButtonStyle.Number,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDigit("2")
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "3",
        buttonStyle = ButtonStyle.Number,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDigit("3")
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "+",
        buttonStyle = ButtonStyle.Operator,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onOperator("+")
        },
        modifier = Modifier.weight(1f)
      )
    }

    // Row 5: 0, ., Backspace, Equals
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      KeypadButton(
        text = "0",
        buttonStyle = ButtonStyle.Number,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDigit("0")
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = ".",
        buttonStyle = ButtonStyle.Number,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onDecimal()
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "⌫",
        buttonStyle = ButtonStyle.Function,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          viewModel.onBackspace()
        },
        modifier = Modifier.weight(1f)
      )
      KeypadButton(
        text = "=",
        buttonStyle = ButtonStyle.Equals,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.LongPress)
          viewModel.onEquals()
        },
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
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val containerColor = when (buttonStyle) {
    ButtonStyle.Number -> MaterialTheme.colorScheme.surfaceContainerHigh
    ButtonStyle.Operator -> MaterialTheme.colorScheme.primaryContainer
    ButtonStyle.Function -> MaterialTheme.colorScheme.secondaryContainer
    ButtonStyle.Action -> MaterialTheme.colorScheme.surfaceVariant
    ButtonStyle.Equals -> MaterialTheme.colorScheme.primary
    ButtonStyle.Scientific -> MaterialTheme.colorScheme.surfaceContainer
  }

  val contentColor = when (buttonStyle) {
    ButtonStyle.Number -> MaterialTheme.colorScheme.onSurface
    ButtonStyle.Operator -> MaterialTheme.colorScheme.onPrimaryContainer
    ButtonStyle.Function -> MaterialTheme.colorScheme.secondary
    ButtonStyle.Action -> MaterialTheme.colorScheme.error
    ButtonStyle.Equals -> MaterialTheme.colorScheme.onPrimary
    ButtonStyle.Scientific -> MaterialTheme.colorScheme.onSurfaceVariant
  }

  val testTagKey = when (text) {
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
    else -> "button_$text"
  }

  val shape = RoundedCornerShape(22.dp)

  Box(
    modifier = modifier
      .height(68.dp)
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
      style = MaterialTheme.typography.titleLarge.copy(
        fontSize = if (buttonStyle == ButtonStyle.Scientific) 20.sp else 26.sp,
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
          .height(350.dp),
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
