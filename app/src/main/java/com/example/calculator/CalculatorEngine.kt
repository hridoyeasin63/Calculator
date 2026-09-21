package com.example.calculator

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Stack
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sqrt

object CalculatorEngine {

  private val mathContext = MathContext(12, RoundingMode.HALF_UP)

  /**
   * Evaluates an arithmetic expression string.
   * Symbols: + (add), − or - (subtract), × or * (multiply), ÷ or / (divide),
   * % (percentage), ^ (power), √ (square root), ( and ) (parentheses).
   */
  fun evaluate(rawExpr: String): Result<BigDecimal> {
    if (rawExpr.isBlank()) return Result.failure(IllegalArgumentException("Empty expression"))

    try {
      // Normalize expression
      val expr = normalizeExpression(rawExpr)
      val tokens = tokenize(expr)
      if (tokens.isEmpty()) return Result.failure(IllegalArgumentException("No tokens"))

      val rpn = infixToRPN(tokens)
      val result = evaluateRPN(rpn)
      return Result.success(result)
    } catch (e: ArithmeticException) {
      return Result.failure(e)
    } catch (e: Exception) {
      return Result.failure(IllegalArgumentException("Invalid expression", e))
    }
  }

  /**
   * Formats a BigDecimal cleanly for display:
   * Strips unnecessary trailing decimal zeros, handles scientific notation for extremely large/small values.
   */
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
      .replace("π", PI.toString())
      .replace(" ", "")
  }

  private sealed class Token {
    data class Number(val value: BigDecimal) : Token()
    data class Op(val char: Char, val precedence: Int, val isRightAssociative: Boolean = false) : Token()
    object LeftParen : Token()
    object RightParen : Token()
    object Sqrt : Token()
  }

  private fun tokenize(expr: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    var lastTokenWasOpOrLeftParen = true

    while (i < expr.length) {
      val c = expr[i]

      when {
        c.isDigit() || c == '.' -> {
          val sb = StringBuilder()
          while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
            sb.append(expr[i])
            i++
          }
          tokens.add(Token.Number(BigDecimal(sb.toString())))
          lastTokenWasOpOrLeftParen = false
          continue
        }
        c == '√' -> {
          tokens.add(Token.Sqrt)
          lastTokenWasOpOrLeftParen = true
        }
        c == '(' -> {
          // Implicit multiplication e.g., 5(3) -> 5 * (3)
          if (!lastTokenWasOpOrLeftParen) {
            tokens.add(Token.Op('*', 2))
          }
          tokens.add(Token.LeftParen)
          lastTokenWasOpOrLeftParen = true
        }
        c == ')' -> {
          tokens.add(Token.RightParen)
          lastTokenWasOpOrLeftParen = false
        }
        c == '+' -> {
          tokens.add(Token.Op('+', 1))
          lastTokenWasOpOrLeftParen = true
        }
        c == '-' -> {
          if (lastTokenWasOpOrLeftParen) {
            // Unary minus: treat as 0 - ... or negative number
            // Lookahead number
            var j = i + 1
            if (j < expr.length && (expr[j].isDigit() || expr[j] == '.')) {
              val sb = StringBuilder("-")
              while (j < expr.length && (expr[j].isDigit() || expr[j] == '.')) {
                sb.append(expr[j])
                j++
              }
              tokens.add(Token.Number(BigDecimal(sb.toString())))
              i = j
              lastTokenWasOpOrLeftParen = false
              continue
            } else {
              tokens.add(Token.Number(BigDecimal.ZERO))
              tokens.add(Token.Op('-', 1))
              lastTokenWasOpOrLeftParen = true
            }
          } else {
            tokens.add(Token.Op('-', 1))
            lastTokenWasOpOrLeftParen = true
          }
        }
        c == '*' -> {
          tokens.add(Token.Op('*', 2))
          lastTokenWasOpOrLeftParen = true
        }
        c == '/' -> {
          tokens.add(Token.Op('/', 2))
          lastTokenWasOpOrLeftParen = true
        }
        c == '%' -> {
          tokens.add(Token.Op('%', 2))
          lastTokenWasOpOrLeftParen = false
        }
        c == '^' -> {
          tokens.add(Token.Op('^', 3, isRightAssociative = true))
          lastTokenWasOpOrLeftParen = true
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
        is Token.Sqrt -> stack.push(token)
        is Token.Op -> {
          while (stack.isNotEmpty()) {
            val top = stack.peek()
            val shouldPop = when (top) {
              is Token.Sqrt -> true
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
          if (stack.isNotEmpty() && stack.peek() is Token.Sqrt) {
            output.add(stack.pop())
          }
          if (!foundLeft) {
            // Mismatched paren; silently ignore or continue
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

  private fun evaluateRPN(rpn: List<Token>): BigDecimal {
    val stack = Stack<BigDecimal>()

    for (token in rpn) {
      when (token) {
        is Token.Number -> stack.push(token.value)
        is Token.Sqrt -> {
          if (stack.isEmpty()) throw IllegalArgumentException("Missing operand for √")
          val operand = stack.pop()
          if (operand < BigDecimal.ZERO) throw ArithmeticException("Invalid input for square root")
          val result = BigDecimal(sqrt(operand.toDouble()), mathContext)
          stack.push(result)
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
