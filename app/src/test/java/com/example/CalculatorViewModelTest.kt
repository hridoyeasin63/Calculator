package com.example

import com.example.calculator.CalculatorLayoutMode
import com.example.calculator.CalculatorViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
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
  fun testTrigonometricCalculation() {
    val vm = CalculatorViewModel()

    // sin(90) in degrees = 1
    vm.onScientific("sin")
    vm.onDigit("9")
    vm.onDigit("0")
    vm.onScientific(")")
    vm.onEquals()

    assertEquals("1", vm.uiState.value.currentInput)
  }

  @Test
  fun testSquareRootCalculation() {
    val vm = CalculatorViewModel()

    // √(144) = 12
    vm.onScientific("√")
    vm.onDigit("1")
    vm.onDigit("4")
    vm.onDigit("4")
    vm.onScientific(")")
    vm.onEquals()

    assertEquals("12", vm.uiState.value.currentInput)
  }

  @Test
  fun testExponentiationCalculation() {
    val vm = CalculatorViewModel()

    // 2 ^ 4 = 16
    vm.onDigit("2")
    vm.onScientific("^")
    vm.onDigit("4")
    vm.onEquals()

    assertEquals("16", vm.uiState.value.currentInput)
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

  @Test
  fun testLayoutModeSwitching() {
    val vm = CalculatorViewModel()

    assertEquals(CalculatorLayoutMode.BASIC, vm.uiState.value.layoutMode)
    assertFalse(vm.uiState.value.isScientificExpanded)

    vm.setLayoutMode(CalculatorLayoutMode.SCIENTIFIC)
    assertEquals(CalculatorLayoutMode.SCIENTIFIC, vm.uiState.value.layoutMode)
    assertTrue(vm.uiState.value.isScientificExpanded)

    vm.toggleScientific()
    assertEquals(CalculatorLayoutMode.BASIC, vm.uiState.value.layoutMode)

    vm.toggleScientific()
    assertEquals(CalculatorLayoutMode.SCIENTIFIC, vm.uiState.value.layoutMode)
  }

  @Test
  fun testInverseTrigonometricViewModel() {
    val vm = CalculatorViewModel()

    assertFalse(vm.uiState.value.isInverseTrig)
    vm.toggleInverseTrig()
    assertTrue(vm.uiState.value.isInverseTrig)

    // asin(1) in degrees = 90
    vm.onScientific("sin⁻¹")
    vm.onDigit("1")
    vm.onScientific(")")
    vm.onEquals()

    assertEquals("90", vm.uiState.value.currentInput)
  }

  @Test
  fun testLogAndLnViewModel() {
    val vm = CalculatorViewModel()

    // log(100) = 2
    vm.onScientific("log")
    vm.onDigit("1")
    vm.onDigit("0")
    vm.onDigit("0")
    vm.onScientific(")")
    vm.onEquals()

    assertEquals("2", vm.uiState.value.currentInput)
  }
}
