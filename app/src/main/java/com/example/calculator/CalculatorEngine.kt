package com.example.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Stack
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object CalculatorEngine {

  private val mathContext = MathContext(12, RoundingMode.HALF_UP)

  /**
   * Evaluates an arithmetic and scientific expression string.
   * Supports:
   * - Operators: +, − (or -), × (or *), ÷ (or /), ^ (power), % (percent)
   * - Functions: sin, cos, tan, ln, log, √ (square root), ! (factorial)
   * - Constants: π (pi), e
   * - Parentheses: ( and )
   * - Angle unit: Degrees (default) or Radians
   */
  fun evaluate(rawExpr: String, isDegreeMode: Boolean = true): Result<BigDecimal> {
    if (rawExpr.isBlank()) return Result.failure(IllegalArgumentException("Empty expression"))

    try {
      val expr = normalizeExpression(rawExpr)
      val tokens = tokenize(expr)
      if (tokens.isEmpty()) return Result.failure(IllegalArgumentException("No tokens"))

      val rpn = infixToRPN(tokens)
      val result = evaluateRPN(rpn, isDegreeMode)
      return Result.success(result)
    } catch (e: ArithmeticException) {
      return Result.failure(e)
    } catch (e: Exception) {
      return Result.failure(IllegalArgumentException(e.message ?: "Invalid expression", e))
    }
  }

  fun formatResult(value: BigDecimal): String {
    val stripped = value.stripTrailingZeros()
    // For extreme values, use scientific notation
    if (stripped.abs() >= BigDecimal("1000000000000") || (stripped.abs() > BigDecimal.ZERO && stripped.abs() < BigDecimal("0.0000001"))) {
      val formatter = DecimalFormat("0.######E0", DecimalFormatSymbols(Locale.US))
      return formatter.format(stripped)
    }

    val symbols = DecimalFormatSymbols(Locale.US).apply {
      groupingSeparator = ','
      decimalSeparator = '.'
    }
    val pattern = if (stripped.scale() > 0) "#,##0.##########" else "#,##0"
    val formatter = DecimalFormat(pattern, symbols)
    return formatter.format(stripped)
  }

  private fun normalizeExpression(expr: String): String {
    return expr
      .replace("×", "*")
      .replace("÷", "/")
      .replace("−", "-")
      .replace("sin⁻¹", "asin")
      .replace("cos⁻¹", "acos")
      .replace("tan⁻¹", "atan")
      .replace(" ", "")
  }

  private sealed class Token {
    data class Number(val value: BigDecimal) : Token()
    data class Op(val char: Char, val precedence: Int, val isRightAssociative: Boolean = false) : Token()
    data class Func(val name: String) : Token()
    object Factorial : Token() // Postfix unary
    object LeftParen : Token()
    object RightParen : Token()
  }

  private fun tokenize(expr: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    var lastTokenWasOpOrLeftParenOrFunc = true

    while (i < expr.length) {
      val c = expr[i]

      when {
        // Digits / decimals
        c.isDigit() || c == '.' -> {
          val sb = StringBuilder()
          while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
            sb.append(expr[i])
            i++
          }
          tokens.add(Token.Number(BigDecimal(sb.toString())))
          lastTokenWasOpOrLeftParenOrFunc = false
          continue
        }

        // Functions and constants identified by words / symbols
        expr.startsWith("asin", i) -> {
          if (!lastTokenWasOpOrLeftParenOrFunc) tokens.add(Token.Op('*', 2))
          tokens.add(Token.Func("asin"))
          i += 4
          lastTokenWasOpOrLeftParenOrFunc = true
          continue
        }
        expr.startsWith("acos", i) -> {
          if (!lastTokenWasOpOrLeftParenOrFunc) tokens.add(Token.Op('*', 2))
          tokens.add(Token.Func("acos"))
          i += 4
          lastTokenWasOpOrLeftParenOrFunc = true
          continue
        }
        expr.startsWith("atan", i) -> {
          if (!lastTokenWasOpOrLeftParenOrFunc) tokens.add(Token.Op('*', 2))
          tokens.add(Token.Func("atan"))
          i += 4
          lastTokenWasOpOrLeftParenOrFunc = true
          continue
        }
        expr.startsWith("sin", i) -> {
          if (!lastTokenWasOpOrLeftParenOrFunc) tokens.add(Token.Op('*', 2))
          tokens.add(Token.Func("sin"))
          i += 3
          lastTokenWasOpOrLeftParenOrFunc = true
          continue
        }
        expr.startsWith("cos", i) -> {
          if (!lastTokenWasOpOrLeftParenOrFunc) tokens.add(Token.Op('*', 2))
          tokens.add(Token.Func("cos"))
          i += 3
          lastTokenWasOpOrLeftParenOrFunc = true
          continue
        }
        expr.startsWith("tan", i) -> {
          if (!lastTokenWasOpOrLeftParenOrFunc) tokens.add(Token.Op('*', 2))
          tokens.add(Token.Func("tan"))
          i += 3
          lastTokenWasOpOrLeftParenOrFunc = true
          continue
        }
        expr.startsWith("log", i) -> {
          if (!lastTokenWasOpOrLeftParenOrFunc) tokens.add(Token.Op('*', 2))
          tokens.add(Token.Func("log"))
          i += 3
          lastTokenWasOpOrLeftParenOrFunc = true
          continue
        }
        expr.startsWith("ln", i) -> {
          if (!lastTokenWasOpOrLeftParenOrFunc) tokens.add(Token.Op('*', 2))
          tokens.add(Token.Func("ln"))
          i += 2
          lastTokenWasOpOrLeftParenOrFunc = true
          continue
        }
        c == '√' -> {
          if (!lastTokenWasOpOrLeftParenOrFunc) tokens.add(Token.Op('*', 2))
          tokens.add(Token.Func("sqrt"))
          lastTokenWasOpOrLeftParenOrFunc = true
        }
        c == 'π' -> {
          if (!lastTokenWasOpOrLeftParenOrFunc) tokens.add(Token.Op('*', 2))
          tokens.add(Token.Number(BigDecimal(PI, mathContext)))
          lastTokenWasOpOrLeftParenOrFunc = false
        }
        c == 'e' -> {
          if (!lastTokenWasOpOrLeftParenOrFunc) tokens.add(Token.Op('*', 2))
          tokens.add(Token.Number(BigDecimal(E, mathContext)))
          lastTokenWasOpOrLeftParenOrFunc = false
        }
        c == '!' -> {
          tokens.add(Token.Factorial)
          lastTokenWasOpOrLeftParenOrFunc = false
        }
        c == '(' -> {
          if (!lastTokenWasOpOrLeftParenOrFunc) {
            tokens.add(Token.Op('*', 2))
          }
          tokens.add(Token.LeftParen)
          lastTokenWasOpOrLeftParenOrFunc = true
        }
        c == ')' -> {
          tokens.add(Token.RightParen)
          lastTokenWasOpOrLeftParenOrFunc = false
        }
        c == '+' -> {
          tokens.add(Token.Op('+', 1))
          lastTokenWasOpOrLeftParenOrFunc = true
        }
        c == '-' -> {
          if (lastTokenWasOpOrLeftParenOrFunc) {
            // Unary minus: check if immediately followed by number
            var j = i + 1
            if (j < expr.length && (expr[j].isDigit() || expr[j] == '.')) {
              val sb = StringBuilder("-")
              while (j < expr.length && (expr[j].isDigit() || expr[j] == '.')) {
                sb.append(expr[j])
                j++
              }
              tokens.add(Token.Number(BigDecimal(sb.toString())))
              i = j
              lastTokenWasOpOrLeftParenOrFunc = false
              continue
            } else {
              // Unary minus as 0 -
              tokens.add(Token.Number(BigDecimal.ZERO))
              tokens.add(Token.Op('-', 1))
              lastTokenWasOpOrLeftParenOrFunc = true
            }
          } else {
            tokens.add(Token.Op('-', 1))
            lastTokenWasOpOrLeftParenOrFunc = true
          }
        }
        c == '*' -> {
          tokens.add(Token.Op('*', 2))
          lastTokenWasOpOrLeftParenOrFunc = true
        }
        c == '/' -> {
          tokens.add(Token.Op('/', 2))
          lastTokenWasOpOrLeftParenOrFunc = true
        }
        c == '%' -> {
          tokens.add(Token.Op('%', 2))
          lastTokenWasOpOrLeftParenOrFunc = false
        }
        c == '^' -> {
          tokens.add(Token.Op('^', 3, isRightAssociative = true))
          lastTokenWasOpOrLeftParenOrFunc = true
        }
      }
      i++
    }

    return tokens
  }

  private fun infixToRPN(tokens: List<Token>): List<Token> {
    val output = mutableListOf<Token>()
    val stack = Stack<Token>()

    for (token in tokens) {
      when (token) {
        is Token.Number -> output.add(token)
        is Token.Factorial -> output.add(token) // Postfix operator attaches directly
        is Token.Func -> stack.push(token)
        is Token.Op -> {
          while (stack.isNotEmpty()) {
            val top = stack.peek()
            val shouldPop = when (top) {
              is Token.Func -> true
              is Token.Op -> {
                if (token.isRightAssociative) {
                  token.precedence < top.precedence
                } else {
                  token.precedence <= top.precedence
                }
              }
              else -> false
            }
            if (shouldPop) {
              output.add(stack.pop())
            } else {
              break
            }
          }
          stack.push(token)
        }
        is Token.LeftParen -> stack.push(token)
        is Token.RightParen -> {
          var foundLeft = false
          while (stack.isNotEmpty()) {
            val top = stack.pop()
            if (top is Token.LeftParen) {
              foundLeft = true
              break
            } else {
              output.add(top)
            }
          }
          if (stack.isNotEmpty() && stack.peek() is Token.Func) {
            output.add(stack.pop())
          }
        }
      }
    }

    while (stack.isNotEmpty()) {
      val top = stack.pop()
      if (top !is Token.LeftParen && top !is Token.RightParen) {
        output.add(top)
      }
    }

    return output
  }

  private fun evaluateRPN(rpn: List<Token>, isDegreeMode: Boolean): BigDecimal {
    val stack = Stack<BigDecimal>()

    for (token in rpn) {
      when (token) {
        is Token.Number -> stack.push(token.value)
        is Token.Factorial -> {
          if (stack.isEmpty()) throw IllegalArgumentException("Missing operand for factorial")
          val operand = stack.pop()
          val intVal = operand.toInt()
          if (operand.scale() > 0 && operand.stripTrailingZeros().scale() > 0) {
            throw ArithmeticException("Factorial only defined for integers")
          }
          if (intVal < 0) throw ArithmeticException("Factorial undefined for negative numbers")
          if (intVal > 100) throw ArithmeticException("Factorial overflow (>100!)")

          var fact = BigDecimal.ONE
          for (num in 2..intVal) {
            fact = fact.multiply(BigDecimal(num), mathContext)
          }
          stack.push(fact)
        }
        is Token.Func -> {
          if (stack.isEmpty()) throw IllegalArgumentException("Missing operand for ${token.name}")
          val operand = stack.pop()
          val doubleVal = operand.toDouble()

          val resultVal = when (token.name) {
            "sqrt" -> {
              if (operand < BigDecimal.ZERO) throw ArithmeticException("Negative square root")
              BigDecimal(sqrt(doubleVal), mathContext)
            }
            "sin" -> {
              val radians = if (isDegreeMode) Math.toRadians(doubleVal) else doubleVal
              // Correct precision near 0 (e.g. sin(180) in degrees)
              val s = sin(radians)
              BigDecimal(if (kotlin.math.abs(s) < 1e-15) 0.0 else s, mathContext)
            }
            "cos" -> {
              val radians = if (isDegreeMode) Math.toRadians(doubleVal) else doubleVal
              val c = cos(radians)
              BigDecimal(if (kotlin.math.abs(c) < 1e-15) 0.0 else c, mathContext)
            }
            "tan" -> {
              val radians = if (isDegreeMode) Math.toRadians(doubleVal) else doubleVal
              val c = cos(radians)
              if (kotlin.math.abs(c) < 1e-15) {
                throw ArithmeticException("Tangent undefined")
              }
              val t = tan(radians)
              BigDecimal(if (kotlin.math.abs(t) < 1e-15) 0.0 else t, mathContext)
            }
            "asin" -> {
              if (doubleVal < -1.0 || doubleVal > 1.0) {
                throw ArithmeticException("asin domain error: must be between -1 and 1")
              }
              val rad = asin(doubleVal)
              val out = if (isDegreeMode) Math.toDegrees(rad) else rad
              BigDecimal(if (kotlin.math.abs(out) < 1e-15) 0.0 else out, mathContext)
            }
            "acos" -> {
              if (doubleVal < -1.0 || doubleVal > 1.0) {
                throw ArithmeticException("acos domain error: must be between -1 and 1")
              }
              val rad = acos(doubleVal)
              val out = if (isDegreeMode) Math.toDegrees(rad) else rad
              BigDecimal(if (kotlin.math.abs(out) < 1e-15) 0.0 else out, mathContext)
            }
            "atan" -> {
              val rad = atan(doubleVal)
              val out = if (isDegreeMode) Math.toDegrees(rad) else rad
              BigDecimal(if (kotlin.math.abs(out) < 1e-15) 0.0 else out, mathContext)
            }
            "log" -> {
              if (doubleVal <= 0) throw ArithmeticException("Log undefined for non-positive values")
              BigDecimal(log10(doubleVal), mathContext)
            }
            "ln" -> {
              if (doubleVal <= 0) throw ArithmeticException("Ln undefined for non-positive values")
              BigDecimal(ln(doubleVal), mathContext)
            }
            else -> throw IllegalArgumentException("Unknown function: ${token.name}")
          }
          stack.push(resultVal)
        }
        is Token.Op -> {
          if (token.char == '%') {
            if (stack.isEmpty()) throw IllegalArgumentException("Missing operand for %")
            val operand = stack.pop()
            val result = operand.divide(BigDecimal("100"), mathContext)
            stack.push(result)
          } else {
            if (stack.size < 2) throw IllegalArgumentException("Missing operand for ${token.char}")
            val b = stack.pop()
            val a = stack.pop()

            val res = when (token.char) {
              '+' -> a.add(b, mathContext)
              '-' -> a.subtract(b, mathContext)
              '*' -> a.multiply(b, mathContext)
              '/' -> {
                if (b.compareTo(BigDecimal.ZERO) == 0) {
                  throw ArithmeticException("Cannot divide by 0")
                }
                a.divide(b, mathContext)
              }
              '^' -> {
                val doubleRes = a.toDouble().pow(b.toDouble())
                if (doubleRes.isNaN() || doubleRes.isInfinite()) {
                  throw ArithmeticException("Result out of range")
                }
                BigDecimal(doubleRes, mathContext)
              }
              else -> throw IllegalArgumentException("Unknown operator: ${token.char}")
            }
            stack.push(res)
          }
        }
        else -> {}
      }
    }

    if (stack.isEmpty()) return BigDecimal.ZERO
    return stack.pop()
  }
}
