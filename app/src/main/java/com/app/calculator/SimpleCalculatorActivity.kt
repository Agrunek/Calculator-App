package com.app.calculator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.abs

import com.app.calculator.Enums.DisplayMode
import com.app.calculator.Enums.OperationMode

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

class SimpleCalculatorActivity : AppCompatActivity() {

    private lateinit var displayedValueText: TextView
    private lateinit var clearButton: Button
    private lateinit var addButton: Button
    private lateinit var subtractButton: Button
    private lateinit var multiplyButton: Button
    private lateinit var divideButton: Button

    private val scientificNotation: DecimalFormat = DecimalFormat("0.###E0")
    private val pseudoEmptyValueRegex: Regex = Regex("^-?0$")
    private val standardNotationRange: OpenEndRange<Double> = SN_LOWER_BOUND..<SN_UPPER_BOUND

    private val first: StringBuilder = StringBuilder()
    private val second: StringBuilder = StringBuilder()

    private var displayMode: DisplayMode = DisplayMode.FIRST
    private var operationIntent: OperationMode = OperationMode.NONE
    private var operationLocked: OperationMode = OperationMode.NONE

    private var manipulated: Boolean = false
    private var calculated: Boolean = false
    private var block: Boolean = false
    private var error: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_simple_calculator)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.simple_calculator)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindListeners()

        displayedValueText = findViewById(R.id.calculator_displayed_value_text)
        clearButton = findViewById(R.id.calculator_clear_button)
        addButton = findViewById(R.id.calculator_add_button)
        subtractButton = findViewById(R.id.calculator_subtract_button)
        multiplyButton = findViewById(R.id.calculator_multiply_button)
        divideButton = findViewById(R.id.calculator_divide_button)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

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

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        savedInstanceState.run {
            getCharSequence(STATE_0).let { first.append(it) }
            getCharSequence(STATE_1).let { second.append(it) }
            getSerializable(STATE_2, DisplayMode::class.java)?.let { displayMode = it }
            getSerializable(STATE_3, OperationMode::class.java)?.let { operationIntent = it }
            getSerializable(STATE_4, OperationMode::class.java)?.let { operationLocked = it }
            getBoolean(STATE_5).let { manipulated = it }
            getBoolean(STATE_6).let { calculated = it }
            getBoolean(STATE_7).let { block = it }
            getBoolean(STATE_8).let { error = it }
        }

        drawUI()
    }

    private fun bindListeners() {
        val buttonCallbackMap: Map<Int, (View) -> Unit> = mapOf(
            R.id.calculator_clear_button to { clearButtonOnClick() },
            R.id.calculator_sign_flip_button to { signFlipButtonOnClick() },
            R.id.calculator_percent_button to { percentButtonOnClick() },
            R.id.calculator_point_button to { pointButtonOnClick() },
            R.id.calculator_add_button to { operationButtonOnClick(OperationMode.ADDITION) },
            R.id.calculator_subtract_button to { operationButtonOnClick(OperationMode.SUBTRACTION) },
            R.id.calculator_multiply_button to { operationButtonOnClick(OperationMode.MULTIPLICATION) },
            R.id.calculator_divide_button to { operationButtonOnClick(OperationMode.DIVISION) },
            R.id.calculator_equals_button to { equalsButtonOnClick() },
            R.id.calculator_0_button to { digitButtonOnClick(0) },
            R.id.calculator_1_button to { digitButtonOnClick(1) },
            R.id.calculator_2_button to { digitButtonOnClick(2) },
            R.id.calculator_3_button to { digitButtonOnClick(3) },
            R.id.calculator_4_button to { digitButtonOnClick(4) },
            R.id.calculator_5_button to { digitButtonOnClick(5) },
            R.id.calculator_6_button to { digitButtonOnClick(6) },
            R.id.calculator_7_button to { digitButtonOnClick(7) },
            R.id.calculator_8_button to { digitButtonOnClick(8) },
            R.id.calculator_9_button to { digitButtonOnClick(9) },
        )

        buttonCallbackMap.forEach {
            findViewById<Button>(it.key).setOnClickListener(it.value)
        }
    }

    private fun currentNumber() = when (displayMode) {
        DisplayMode.FIRST -> first
        DisplayMode.SECOND -> second
    }

    private fun countDigits(string: CharSequence): Int {
        return string.count { it.isDigit() }
    }

    private fun isInStandardNotationRange(number: Double): Boolean {
        return number == 0.0 || abs(number) in standardNotationRange
    }

    private fun drawUI() {
        clearButton.text = if (currentNumber().isEmpty()) {
            getString(R.string.calculator_clear_button_title_all)
        } else {
            getString(R.string.calculator_clear_button_title_enter)
        }

        addButton.isSelected = operationIntent == OperationMode.ADDITION
        subtractButton.isSelected = operationIntent == OperationMode.SUBTRACTION
        multiplyButton.isSelected = operationIntent == OperationMode.MULTIPLICATION
        divideButton.isSelected = operationIntent == OperationMode.DIVISION

        if (error) {
            displayedValueText.text = getString(R.string.calculator_error_message)
            error = false
            return
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

        string = string.replace('.', ',')

        displayedValueText.text = string
    }

    private fun clearButtonOnClick() {
        if (currentNumber().isEmpty()) {
            first.clear()
            second.clear()
            displayMode = DisplayMode.FIRST
            operationIntent = OperationMode.NONE
        } else {
            currentNumber().clear()

            operationIntent = if (operationIntent == OperationMode.NONE && !block) {
                operationLocked
            } else {
                OperationMode.NONE
            }
        }

        operationLocked = OperationMode.NONE
        calculated = false
        manipulated = false
        block = false

        drawUI()
    }

    private fun signFlipButtonOnClick() {
        if (currentNumber().isEmpty()) {
            return
        }

        val double: Double = currentNumber().toString().ifEmpty { "0" }.toDouble()
        currentNumber().clear().append(double.times(-1))

        manipulated = true
        calculated = false

        drawUI()
    }

    private fun percentButtonOnClick() {
        if (currentNumber().isEmpty()) {
            return
        }

        val double: Double = currentNumber().toString().ifEmpty { "0" }.toDouble()
        currentNumber().clear().append(double.div(100))

        manipulated = true
        calculated = false

        drawUI()
    }

    private fun pointButtonOnClick() {
        if (operationIntent != OperationMode.NONE) {
            second.clear()
            displayMode = DisplayMode.SECOND
            operationLocked = operationIntent
            operationIntent = OperationMode.NONE
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

        drawUI()
    }

    private fun operationButtonOnClick(operation: OperationMode) {
        if (operation == operationIntent) {
            return
        }

        if (operationLocked != OperationMode.NONE && !block) {
            val firstIngredient: Double = first.toString().ifEmpty { "0" }.toDouble()
            val secondIngredient: Double = second.toString().ifEmpty { "0" }.toDouble()

            val result: Double = when (operationLocked) {
                OperationMode.NONE -> Double.NaN
                OperationMode.ADDITION -> firstIngredient.plus(secondIngredient)
                OperationMode.SUBTRACTION -> firstIngredient.minus(secondIngredient)
                OperationMode.MULTIPLICATION -> firstIngredient.times(secondIngredient)
                OperationMode.DIVISION -> firstIngredient.div(secondIngredient)
            }

            first.clear()
            second.clear()
            displayMode = DisplayMode.FIRST
            operationLocked = OperationMode.NONE

            if (result.isFinite()) {
                first.append(result)
                calculated = true
            } else {
                operationIntent = OperationMode.NONE
                calculated = false
                error = true
            }
        }

        if (!error) {
            operationIntent = operation
        }

        manipulated = false

        drawUI()
    }

    private fun equalsButtonOnClick() {
        val firstIngredient: Double = first.toString().ifEmpty { "0" }.toDouble()
        val secondIngredient: Double = second.toString().ifEmpty { "0" }.toDouble()

        val result: Double = when (operationLocked) {
            OperationMode.NONE -> firstIngredient
            OperationMode.ADDITION -> firstIngredient.plus(secondIngredient)
            OperationMode.SUBTRACTION -> firstIngredient.minus(secondIngredient)
            OperationMode.MULTIPLICATION -> firstIngredient.times(secondIngredient)
            OperationMode.DIVISION -> firstIngredient.div(secondIngredient)
        }

        first.clear()
        displayMode = DisplayMode.FIRST
        operationIntent = OperationMode.NONE

        if (result.isFinite()) {
            first.append(result)
            calculated = true
            block = true
        } else {
            second.clear()
            operationLocked = OperationMode.NONE
            calculated = false
            block = false
            error = true
        }

        manipulated = false

        drawUI()
    }

    private fun digitButtonOnClick(digit: Int) {
        if (operationIntent != OperationMode.NONE) {
            second.clear()
            displayMode = DisplayMode.SECOND
            operationLocked = operationIntent
            operationIntent = OperationMode.NONE
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

        drawUI()
    }
}