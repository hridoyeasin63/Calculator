package com.example

import com.example.calculator.CalculatorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CalculatorViewModelTest {

  @Test
  fun testSequentialCalculation() {
    val vm = CalculatorViewModel()

    // 1 2 + 8 = -> 20
    vm.onDigit("1")
    vm.onDigit("2")
    assertEquals("12", vm.uiState.value.currentInput)

    vm.onOperator("+")
    vm.onDigit("8")
    assertEquals("8", vm.uiState.value.currentInput)
    assertEquals("= 20", vm.uiState.value.previewResult)

    vm.onEquals()
    assertEquals("20", vm.uiState.value.currentInput)
    assertEquals(1, vm.uiState.value.history.size)
    assertEquals("20", vm.uiState.value.history.first().result)
  }

  @Test
  fun testClearAndBackspace() {
    val vm = CalculatorViewModel()

    vm.onDigit("9")
    vm.onDigit("5")
    assertEquals("95", vm.uiState.value.currentInput)

    vm.onBackspace()
    assertEquals("9", vm.uiState.value.currentInput)

    vm.onClear()
    assertEquals("0", vm.uiState.value.currentInput)
  }

  @Test
  fun testToggleSign() {
    val vm = CalculatorViewModel()

    vm.onDigit("7")
    vm.onToggleSign()
    assertEquals("-7", vm.uiState.value.currentInput)

    vm.onToggleSign()
    assertEquals("7", vm.uiState.value.currentInput)
  }

  @Test
  fun testDivisionByZeroHandled() {
    val vm = CalculatorViewModel()

    vm.onDigit("5")
    vm.onOperator("÷")
    vm.onDigit("0")
    vm.onEquals()

    assertEquals("Cannot divide by 0", vm.uiState.value.errorMessage)
  }
}
