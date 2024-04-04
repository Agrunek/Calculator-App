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
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

import com.app.calculator.Enums.DisplayMode
import com.app.calculator.Enums.ExpandedOperationMode

import com.app.calculator.Constants.MAX_DISPLAY_SIZE
import com.app.calculator.Constants.SN_LOWER_BOUND
import com.app.calculator.Constants.SN_UPPER_BOUND

class AdvancedCalculatorActivity : AppCompatActivity() {

    private lateinit var displayedValueText: TextView
    private lateinit var powerButton: Button
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
    private var operationIntent: ExpandedOperationMode = ExpandedOperationMode.NONE
    private var operationLocked: ExpandedOperationMode = ExpandedOperationMode.NONE

    private var manipulated: Boolean = false
    private var calculated: Boolean = false
    private var block: Boolean = false
    private var error: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_advanced_calculator)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.advanced_calculator)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindListeners()

        displayedValueText = findViewById(R.id.calculator_displayed_value_text)
        powerButton = findViewById(R.id.calculator_power_button)
        clearButton = findViewById(R.id.calculator_clear_button)
        addButton = findViewById(R.id.calculator_add_button)
        subtractButton = findViewById(R.id.calculator_subtract_button)
        multiplyButton = findViewById(R.id.calculator_multiply_button)
        divideButton = findViewById(R.id.calculator_divide_button)

        savedInstanceState?.getCharSequence("first")?.let { first.append(it) }
        savedInstanceState?.getCharSequence("second")?.let { second.append(it) }
        savedInstanceState?.getSerializable("displayMode", DisplayMode::class.java)?.let { displayMode = it }
        savedInstanceState?.getSerializable("operationIntent", ExpandedOperationMode::class.java)?.let { operationIntent = it }
        savedInstanceState?.getSerializable("operationLocked", ExpandedOperationMode::class.java)?.let { operationLocked = it }
        savedInstanceState?.getBoolean("manipulated")?.let { manipulated = it }
        savedInstanceState?.getBoolean("calculated")?.let { calculated = it }
        savedInstanceState?.getBoolean("block")?.let { block = it }
        savedInstanceState?.getBoolean("error")?.let { error = it }

        drawUI()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putCharSequence("first", first)
        outState.putCharSequence("second", second)
        outState.putSerializable("displayMode", displayMode)
        outState.putSerializable("operationIntent", operationIntent)
        outState.putSerializable("operationLocked", operationLocked)
        outState.putBoolean("manipulated", manipulated)
        outState.putBoolean("calculated", calculated)
        outState.putBoolean("block", block)
        outState.putBoolean("error", error)
    }

    private fun bindListeners() {
        val buttonCallbackMap: Map<Int, (View) -> Unit> = mapOf(
            R.id.calculator_square_root_button to { squareRootButtonOnClick() },
            R.id.calculator_sine_button to { sineButtonOnClick() },
            R.id.calculator_cosine_button to { cosineButtonOnClick() },
            R.id.calculator_tangent_button to { tangentButtonOnClick() },
            R.id.calculator_square_button to { squareButtonOnClick() },
            R.id.calculator_power_button to { operationButtonOnClick(ExpandedOperationMode.POWER) },
            R.id.calculator_natural_logarithm_button to { naturalLogarithmButtonOnClick() },
            R.id.calculator_logarithm_button to { logarithmButtonOnClick() },
            R.id.calculator_clear_button to { clearButtonOnClick() },
            R.id.calculator_sign_flip_button to { signFlipButtonOnClick() },
            R.id.calculator_percent_button to { percentButtonOnClick() },
            R.id.calculator_point_button to { pointButtonOnClick() },
            R.id.calculator_add_button to { operationButtonOnClick(ExpandedOperationMode.ADDITION) },
            R.id.calculator_subtract_button to { operationButtonOnClick(ExpandedOperationMode.SUBTRACTION) },
            R.id.calculator_multiply_button to { operationButtonOnClick(ExpandedOperationMode.MULTIPLICATION) },
            R.id.calculator_divide_button to { operationButtonOnClick(ExpandedOperationMode.DIVISION) },
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

        powerButton.isSelected = operationIntent == ExpandedOperationMode.POWER
        addButton.isSelected = operationIntent == ExpandedOperationMode.ADDITION
        subtractButton.isSelected = operationIntent == ExpandedOperationMode.SUBTRACTION
        multiplyButton.isSelected = operationIntent == ExpandedOperationMode.MULTIPLICATION
        divideButton.isSelected = operationIntent == ExpandedOperationMode.DIVISION

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

    private fun squareRootButtonOnClick() {
        if (currentNumber().isEmpty()) {
            return
        }

        val double: Double = currentNumber().toString().ifEmpty { "0" }.toDouble()
        val result: Double = sqrt(double)

        calculated = false

        if (result.isFinite()) {
            currentNumber().clear().append(result)
            manipulated = true
        } else {
            first.clear()
            second.clear()
            displayMode = DisplayMode.FIRST
            operationIntent = ExpandedOperationMode.NONE
            operationLocked = ExpandedOperationMode.NONE
            manipulated = false
            block = false
            error = true
        }

        drawUI()
    }

    private fun sineButtonOnClick() {
        if (currentNumber().isEmpty()) {
            return
        }

        val double: Double = currentNumber().toString().ifEmpty { "0" }.toDouble()
        currentNumber().clear().append(sin(double))

        manipulated = true
        calculated = false

        drawUI()
    }

    private fun cosineButtonOnClick() {
        if (currentNumber().isEmpty()) {
            return
        }

        val double: Double = currentNumber().toString().ifEmpty { "0" }.toDouble()
        currentNumber().clear().append(cos(double))

        manipulated = true
        calculated = false

        drawUI()
    }

    private fun tangentButtonOnClick() {
        if (currentNumber().isEmpty()) {
            return
        }

        val double: Double = currentNumber().toString().ifEmpty { "0" }.toDouble()
        currentNumber().clear().append(tan(double))

        manipulated = true
        calculated = false

        drawUI()
    }

    private fun squareButtonOnClick() {
        if (currentNumber().isEmpty()) {
            return
        }

        val double: Double = currentNumber().toString().ifEmpty { "0" }.toDouble()
        currentNumber().clear().append(double.times(double))

        manipulated = true
        calculated = false

        drawUI()
    }

    private fun naturalLogarithmButtonOnClick() {
        if (currentNumber().isEmpty()) {
            return
        }

        val double: Double = currentNumber().toString().ifEmpty { "0" }.toDouble()
        val result: Double = ln(double)

        calculated = false

        if (result.isFinite()) {
            currentNumber().clear().append(result)
            manipulated = true
        } else {
            first.clear()
            second.clear()
            displayMode = DisplayMode.FIRST
            operationIntent = ExpandedOperationMode.NONE
            operationLocked = ExpandedOperationMode.NONE
            manipulated = false
            block = false
            error = true
        }

        drawUI()
    }

    private fun logarithmButtonOnClick() {
        if (currentNumber().isEmpty()) {
            return
        }

        val double: Double = currentNumber().toString().ifEmpty { "0" }.toDouble()
        val result: Double = log10(double)

        calculated = false

        if (result.isFinite()) {
            currentNumber().clear().append(result)
            manipulated = true
        } else {
            first.clear()
            second.clear()
            displayMode = DisplayMode.FIRST
            operationIntent = ExpandedOperationMode.NONE
            operationLocked = ExpandedOperationMode.NONE
            manipulated = false
            block = false
            error = true
        }

        drawUI()
    }

    private fun clearButtonOnClick() {
        if (currentNumber().isEmpty()) {
            first.clear()
            second.clear()
            displayMode = DisplayMode.FIRST
            operationIntent = ExpandedOperationMode.NONE
        } else {
            currentNumber().clear()

            operationIntent = if (operationIntent == ExpandedOperationMode.NONE && !block) {
                operationLocked
            } else {
                ExpandedOperationMode.NONE
            }
        }

        operationLocked = ExpandedOperationMode.NONE
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
        if (operationIntent != ExpandedOperationMode.NONE) {
            second.clear()
            displayMode = DisplayMode.SECOND
            operationLocked = operationIntent
            operationIntent = ExpandedOperationMode.NONE
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

    private fun operationButtonOnClick(operation: ExpandedOperationMode) {
        if (operation == operationIntent) {
            return
        }

        if (operationLocked != ExpandedOperationMode.NONE && !block) {
            val firstIngredient: Double = first.toString().ifEmpty { "0" }.toDouble()
            val secondIngredient: Double = second.toString().ifEmpty { "0" }.toDouble()

            val result: Double = when (operationLocked) {
                ExpandedOperationMode.NONE -> Double.NaN
                ExpandedOperationMode.ADDITION -> firstIngredient.plus(secondIngredient)
                ExpandedOperationMode.SUBTRACTION -> firstIngredient.minus(secondIngredient)
                ExpandedOperationMode.MULTIPLICATION -> firstIngredient.times(secondIngredient)
                ExpandedOperationMode.DIVISION -> firstIngredient.div(secondIngredient)
                ExpandedOperationMode.POWER -> firstIngredient.pow(secondIngredient)
            }

            first.clear()
            second.clear()
            displayMode = DisplayMode.FIRST
            operationLocked = ExpandedOperationMode.NONE

            if (result.isFinite()) {
                first.append(result)
                calculated = true
            } else {
                operationIntent = ExpandedOperationMode.NONE
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
            ExpandedOperationMode.NONE -> firstIngredient
            ExpandedOperationMode.ADDITION -> firstIngredient.plus(secondIngredient)
            ExpandedOperationMode.SUBTRACTION -> firstIngredient.minus(secondIngredient)
            ExpandedOperationMode.MULTIPLICATION -> firstIngredient.times(secondIngredient)
            ExpandedOperationMode.DIVISION -> firstIngredient.div(secondIngredient)
            ExpandedOperationMode.POWER -> firstIngredient.pow(secondIngredient)
        }

        first.clear()
        displayMode = DisplayMode.FIRST
        operationIntent = ExpandedOperationMode.NONE

        if (result.isFinite()) {
            first.append(result)
            calculated = true
            block = true
        } else {
            second.clear()
            operationLocked = ExpandedOperationMode.NONE
            calculated = false
            block = false
            error = true
        }

        manipulated = false

        drawUI()
    }

    private fun digitButtonOnClick(digit: Int) {
        if (operationIntent != ExpandedOperationMode.NONE) {
            second.clear()
            displayMode = DisplayMode.SECOND
            operationLocked = operationIntent
            operationIntent = ExpandedOperationMode.NONE
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