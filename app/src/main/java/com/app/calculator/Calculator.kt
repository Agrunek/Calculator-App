package com.app.calculator

import android.os.Bundle

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

import com.app.calculator.Enums.DisplayMode
import com.app.calculator.Enums.UnaryOperation
import com.app.calculator.Enums.BinaryOperation

import com.app.calculator.Constants.MAX_DISPLAY_SIZE
import com.app.calculator.Constants.SN_LOWER_BOUND
import com.app.calculator.Constants.SN_UPPER_BOUND
import com.app.calculator.Constants.STATE_0
import com.app.calculator.Constants.STATE_1
import com.app.calculator.Constants.STATE_2
import com.app.calculator.Constants.STATE_3
import com.app.calculator.Constants.STATE_4
import com.app.calculator.Constants.STATE_5
import com.app.calculator.Constants.STATE_6
import com.app.calculator.Constants.STATE_7
import com.app.calculator.Constants.STATE_8

class Calculator(private val callback: () -> Unit) {

    private val scientificNotation: DecimalFormat = DecimalFormat("0.###E0")
    private val pseudoEmptyValueRegex: Regex = Regex("^-?0$")
    private val standardNotationRange: OpenEndRange<Double> = SN_LOWER_BOUND..<SN_UPPER_BOUND

    private val first: StringBuilder = StringBuilder()
    private val second: StringBuilder = StringBuilder()

    private var displayMode: DisplayMode = DisplayMode.FIRST
    private var operationIntent: BinaryOperation = BinaryOperation.NONE
    private var operationLocked: BinaryOperation = BinaryOperation.NONE

    private var manipulated: Boolean = false
    private var calculated: Boolean = false
    private var block: Boolean = false
    private var error: Boolean = false

    override fun toString(): String {
        if (error) {
            error = false
            return ""
        }

        var string: String = currentNumber().toString().ifEmpty { "0" }

        if (manipulated || calculated) {
            string = if (isInStandardNotationRange(string.toDouble())) {
                val bigDecimal: BigDecimal = string.toBigDecimal().stripTrailingZeros()
                val numberOfDigits: Int = countDigits(bigDecimal.toPlainString())
                val newScale: Int = MAX_DISPLAY_SIZE + bigDecimal.scale() - numberOfDigits
                val newDecimal: BigDecimal = bigDecimal.setScale(newScale, RoundingMode.HALF_UP)

                newDecimal.stripTrailingZeros().toPlainString()
            } else {
                scientificNotation.format(string.toDouble())
            }
        }

        if (manipulated) {
            currentNumber().clear().append(string)
        }

        return string.replace('.', ',')
    }

    fun uploadInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.run {
            getCharSequence(STATE_0).let { first.append(it) }
            getCharSequence(STATE_1).let { second.append(it) }
            getSerializable(STATE_2, DisplayMode::class.java)?.let { displayMode = it }
            getSerializable(STATE_3, BinaryOperation::class.java)?.let { operationIntent = it }
            getSerializable(STATE_4, BinaryOperation::class.java)?.let { operationLocked = it }
            getBoolean(STATE_5).let { manipulated = it }
            getBoolean(STATE_6).let { calculated = it }
            getBoolean(STATE_7).let { block = it }
            getBoolean(STATE_8).let { error = it }
        }

        callback()
    }

    fun downloadInstanceState(outState: Bundle) {
        outState.run {
            putCharSequence(STATE_0, first)
            putCharSequence(STATE_1, second)
            putSerializable(STATE_2, displayMode)
            putSerializable(STATE_3, operationIntent)
            putSerializable(STATE_4, operationLocked)
            putBoolean(STATE_5, manipulated)
            putBoolean(STATE_6, calculated)
            putBoolean(STATE_7, block)
            putBoolean(STATE_8, error)
        }
    }

    fun isEmpty(): Boolean = currentNumber().isEmpty()

    fun isSelected(operation: BinaryOperation): Boolean = operation == operationIntent

    private fun currentNumber() = when (displayMode) {
        DisplayMode.FIRST -> first
        DisplayMode.SECOND -> second
    }

    private fun countDigits(string: CharSequence): Int = string.count { it.isDigit() }

    private fun isInStandardNotationRange(number: Double): Boolean = number == 0.0 || abs(number) in standardNotationRange

    fun clear() {
        if (currentNumber().isEmpty()) {
            first.clear()
            second.clear()
            displayMode = DisplayMode.FIRST
            operationIntent = BinaryOperation.NONE
        } else {
            currentNumber().clear()

            operationIntent = if (operationIntent == BinaryOperation.NONE && !block) {
                operationLocked
            } else {
                BinaryOperation.NONE
            }
        }

        operationLocked = BinaryOperation.NONE
        calculated = false
        manipulated = false
        block = false

        callback()
    }

    fun appendPoint() {
        if (operationIntent != BinaryOperation.NONE) {
            second.clear()
            displayMode = DisplayMode.SECOND
            operationLocked = operationIntent
            operationIntent = BinaryOperation.NONE
        }

        if (manipulated || calculated) {
            currentNumber().clear()
        }

        if (currentNumber().contains('.') || countDigits(currentNumber()) >= MAX_DISPLAY_SIZE) {
            return
        }

        if (currentNumber().isEmpty()) {
            currentNumber().append('0')
        }

        currentNumber().append('.')

        manipulated = false
        calculated = false
        block = false

        callback()
    }

    fun appendDigit(digit: Int) {
        if (operationIntent != BinaryOperation.NONE) {
            second.clear()
            displayMode = DisplayMode.SECOND
            operationLocked = operationIntent
            operationIntent = BinaryOperation.NONE
        }

        if (manipulated || calculated) {
            currentNumber().clear()
        }

        if (countDigits(currentNumber()) >= MAX_DISPLAY_SIZE) {
            return
        }

        if (pseudoEmptyValueRegex matches currentNumber()) {
            currentNumber().clear()
        }

        currentNumber().append(digit)

        manipulated = false
        calculated = false
        block = false

        callback()
    }

    fun unaryOperation(operation: UnaryOperation) {
        if (currentNumber().isEmpty()) {
            return
        }

        val double: Double = currentNumber().toString().ifEmpty { "0" }.toDouble()

        val result: Double = when (operation) {
            UnaryOperation.SIGN_FLIP -> double.times(-1)
            UnaryOperation.PERCENT -> double.div(100)
            UnaryOperation.SINE -> sin(double)
            UnaryOperation.COSINE -> cos(double)
            UnaryOperation.TANGENT -> tan(double)
            UnaryOperation.NATURAL_LOGARITHM -> ln(double)
            UnaryOperation.LOGARITHM -> log10(double)
            UnaryOperation.SQUARE_ROOT -> sqrt(double)
            UnaryOperation.SQUARE -> double.times(double)
        }

        calculated = false

        if (result.isFinite()) {
            currentNumber().clear().append(result)
            manipulated = true
        } else {
            first.clear()
            second.clear()
            displayMode = DisplayMode.FIRST
            operationIntent = BinaryOperation.NONE
            operationLocked = BinaryOperation.NONE
            manipulated = false
            block = false
            error = true
        }

        callback()
    }

    fun binaryOperation(operation: BinaryOperation) {
        if (operation == operationIntent) {
            return
        }

        if (operationLocked != BinaryOperation.NONE && !block) {
            val firstIngredient: Double = first.toString().ifEmpty { "0" }.toDouble()
            val secondIngredient: Double = second.toString().ifEmpty { "0" }.toDouble()

            val result: Double = when (operationLocked) {
                BinaryOperation.NONE -> Double.NaN
                BinaryOperation.ADDITION -> firstIngredient.plus(secondIngredient)
                BinaryOperation.SUBTRACTION -> firstIngredient.minus(secondIngredient)
                BinaryOperation.MULTIPLICATION -> firstIngredient.times(secondIngredient)
                BinaryOperation.DIVISION -> firstIngredient.div(secondIngredient)
                BinaryOperation.POWER -> firstIngredient.pow(secondIngredient)
            }

            first.clear()
            second.clear()
            displayMode = DisplayMode.FIRST
            operationLocked = BinaryOperation.NONE

            if (result.isFinite()) {
                first.append(result)
                calculated = true
            } else {
                operationIntent = BinaryOperation.NONE
                calculated = false
                error = true
            }
        }

        if (!error) {
            operationIntent = operation
        }

        manipulated = false

        callback()
    }

    fun calculate() {
        val firstIngredient: Double = first.toString().ifEmpty { "0" }.toDouble()
        val secondIngredient: Double = second.toString().ifEmpty { "0" }.toDouble()

        val result: Double = when (operationLocked) {
            BinaryOperation.NONE -> firstIngredient
            BinaryOperation.ADDITION -> firstIngredient.plus(secondIngredient)
            BinaryOperation.SUBTRACTION -> firstIngredient.minus(secondIngredient)
            BinaryOperation.MULTIPLICATION -> firstIngredient.times(secondIngredient)
            BinaryOperation.DIVISION -> firstIngredient.div(secondIngredient)
            BinaryOperation.POWER -> firstIngredient.pow(secondIngredient)
        }

        first.clear()
        displayMode = DisplayMode.FIRST
        operationIntent = BinaryOperation.NONE

        if (result.isFinite()) {
            first.append(result)
            calculated = true
            block = true
        } else {
            second.clear()
            operationLocked = BinaryOperation.NONE
            calculated = false
            block = false
            error = true
        }

        manipulated = false

        callback()
    }
}