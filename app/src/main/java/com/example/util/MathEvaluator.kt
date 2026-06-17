package com.example.util

import kotlin.math.roundToLong

object MathEvaluator {
    fun evaluate(expression: String): Double {
        val sanitized = expression
            .replace("×", "*")
            .replace("÷", "/")
            .replace(" ", "")
        
        if (sanitized.isEmpty()) return 0.0

        return Parser(sanitized).parse()
    }

    private class Parser(private val input: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < input.length) input[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < input.length) {
                throw RuntimeException("Unexpected character: " + ch.toChar())
            }
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                if (eat('+'.code)) x += parseTerm()
                else if (eat('-'.code)) x -= parseTerm()
                else break
            }
            return x
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                if (eat('*'.code)) x *= parseFactor()
                else if (eat('/'.code)) {
                    val divisor = parseFactor()
                    if (divisor == 0.0) throw ArithmeticException("Divide by zero")
                    x /= divisor
                } else break
            }
            return x
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()

            var x: Double
            val startPos = this.pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) {
                while (ch >= '0'.code && ch <= '9'.code || ch == '.'.code) nextChar()
                val str = input.substring(startPos, this.pos)
                x = str.toDoubleOrNull() ?: throw RuntimeException("Invalid number: $str")
            } else {
                throw RuntimeException("Unexpected character: " + ch.toChar())
            }

            while (eat('%'.code)) {
                x *= 0.01
            }

            return x
        }
    }

    fun formatResult(value: Double): String {
        if (value.isInfinite() || value.isNaN()) return "Error"
        
        return if (value % 1.0 == 0.0) {
            if (value >= Long.MIN_VALUE && value <= Long.MAX_VALUE) {
                value.toLong().toString()
            } else {
                value.toString()
            }
        } else {
            val rounded = (value * 1e8).roundToLong() / 1e8
            if (rounded % 1.0 == 0.0) {
                rounded.toLong().toString()
            } else {
                rounded.toString()
            }
        }
    }
}
