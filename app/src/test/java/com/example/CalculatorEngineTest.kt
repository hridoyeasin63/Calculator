package com.example

import com.example.calculator.CalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CalculatorEngineTest {

  @Test
  fun testAddition() {
    val result = CalculatorEngine.evaluate("15 + 27")
    assertTrue(result.isSuccess)
    assertEquals("42", CalculatorEngine.formatResult(result.getOrThrow()))
  }

  @Test
  fun testDecimals() {
    val result = CalculatorEngine.evaluate("0.1 + 0.2")
    assertTrue(result.isSuccess)
    assertEquals("0.3", CalculatorEngine.formatResult(result.getOrThrow()))
  }

  @Test
  fun testOperatorPrecedence() {
    val result = CalculatorEngine.evaluate("2 + 3 × 4")
    assertTrue(result.isSuccess)
    assertEquals("14", CalculatorEngine.formatResult(result.getOrThrow()))
  }

  @Test
  fun testParentheses() {
    val result = CalculatorEngine.evaluate("(2 + 3) × 4")
    assertTrue(result.isSuccess)
    assertEquals("20", CalculatorEngine.formatResult(result.getOrThrow()))
  }

  @Test
  fun testDivision() {
    val result = CalculatorEngine.evaluate("100 ÷ 4")
    assertTrue(result.isSuccess)
    assertEquals("25", CalculatorEngine.formatResult(result.getOrThrow()))
  }

  @Test
  fun testDivisionByZero() {
    val result = CalculatorEngine.evaluate("42 ÷ 0")
    assertTrue(result.isFailure)
    assertEquals("Cannot divide by 0", result.exceptionOrNull()?.message)
  }

  @Test
  fun testPercentage() {
    val result = CalculatorEngine.evaluate("50%")
    assertTrue(result.isSuccess)
    assertEquals("0.5", CalculatorEngine.formatResult(result.getOrThrow()))
  }

  @Test
  fun testSquareRoot() {
    val result = CalculatorEngine.evaluate("√(49)")
    assertTrue(result.isSuccess)
    assertEquals("7", CalculatorEngine.formatResult(result.getOrThrow()))
  }

  @Test
  fun testPower() {
    val result = CalculatorEngine.evaluate("2 ^ 8")
    assertTrue(result.isSuccess)
    assertEquals("256", CalculatorEngine.formatResult(result.getOrThrow()))
  }

  @Test
  fun testLargeNumberFormatting() {
    val value = BigDecimal("1234567.89")
    val formatted = CalculatorEngine.formatResult(value)
    assertEquals("1,234,567.89", formatted)
  }
}
