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
  fun testTrigonometricSineInDegrees() {
    val result = CalculatorEngine.evaluate("sin(90)", isDegreeMode = true)
    assertTrue(result.isSuccess)
    assertEquals("1", CalculatorEngine.formatResult(result.getOrThrow()))
  }

  @Test
  fun testTrigonometricCosineInDegrees() {
    val result = CalculatorEngine.evaluate("cos(0)", isDegreeMode = true)
    assertTrue(result.isSuccess)
    assertEquals("1", CalculatorEngine.formatResult(result.getOrThrow()))
  }

  @Test
  fun testTrigonometricTangentInDegrees() {
    val result = CalculatorEngine.evaluate("tan(45)", isDegreeMode = true)
    assertTrue(result.isSuccess)
    assertEquals("1", CalculatorEngine.formatResult(result.getOrThrow()))
  }

  @Test
  fun testInverseTrigonometricSineInDegrees() {
    val result1 = CalculatorEngine.evaluate("asin(1)", isDegreeMode = true)
    assertTrue(result1.isSuccess)
    assertEquals("90", CalculatorEngine.formatResult(result1.getOrThrow()))

    val result2 = CalculatorEngine.evaluate("asin(0.5)", isDegreeMode = true)
    assertTrue(result2.isSuccess)
    assertEquals("30", CalculatorEngine.formatResult(result2.getOrThrow()))
  }

  @Test
  fun testInverseTrigonometricCosineInDegrees() {
    val result1 = CalculatorEngine.evaluate("acos(1)", isDegreeMode = true)
    assertTrue(result1.isSuccess)
    assertEquals("0", CalculatorEngine.formatResult(result1.getOrThrow()))

    val result2 = CalculatorEngine.evaluate("acos(0.5)", isDegreeMode = true)
    assertTrue(result2.isSuccess)
    assertEquals("60", CalculatorEngine.formatResult(result2.getOrThrow()))
  }

  @Test
  fun testInverseTrigonometricTangentInDegrees() {
    val result1 = CalculatorEngine.evaluate("atan(1)", isDegreeMode = true)
    assertTrue(result1.isSuccess)
    assertEquals("45", CalculatorEngine.formatResult(result1.getOrThrow()))

    val result2 = CalculatorEngine.evaluate("atan(0)", isDegreeMode = true)
    assertTrue(result2.isSuccess)
    assertEquals("0", CalculatorEngine.formatResult(result2.getOrThrow()))
  }

  @Test
  fun testLogarithms() {
    val log10Result = CalculatorEngine.evaluate("log(100)")
    assertTrue(log10Result.isSuccess)
    assertEquals("2", CalculatorEngine.formatResult(log10Result.getOrThrow()))

    val lnResult = CalculatorEngine.evaluate("ln(e)")
    assertTrue(lnResult.isSuccess)
    assertEquals("1", CalculatorEngine.formatResult(lnResult.getOrThrow()))
  }

  @Test
  fun testFactorial() {
    val result = CalculatorEngine.evaluate("5!")
    assertTrue(result.isSuccess)
    assertEquals("120", CalculatorEngine.formatResult(result.getOrThrow()))
  }

  @Test
  fun testLargeNumberFormatting() {
    val value = BigDecimal("1234567.89")
    val formatted = CalculatorEngine.formatResult(value)
    assertEquals("1,234,567.89", formatted)
  }
}
