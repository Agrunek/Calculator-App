package com.app.calculator

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

import com.app.calculator.Enums.UnaryOperation
import com.app.calculator.Enums.BinaryOperation

class SimpleCalculatorActivity : AppCompatActivity() {

    private lateinit var displayedValueText: TextView
    private lateinit var clearButton: Button
    private lateinit var addButton: Button
    private lateinit var subtractButton: Button
    private lateinit var multiplyButton: Button
    private lateinit var divideButton: Button

    private val calculator: Calculator = Calculator(::drawUI)

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
        calculator.downloadInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        calculator.uploadInstanceState(savedInstanceState)
    }

    private fun bindListeners() {
        val buttonCallbackMap: Map<Int, (View) -> Unit> = mapOf(
            R.id.calculator_clear_button to { calculator.clear() },
            R.id.calculator_sign_flip_button to { calculator.unaryOperation(UnaryOperation.SIGN_FLIP) },
            R.id.calculator_percent_button to { calculator.unaryOperation(UnaryOperation.PERCENT) },
            R.id.calculator_point_button to { calculator.appendPoint() },
            R.id.calculator_add_button to { calculator.binaryOperation(BinaryOperation.ADDITION) },
            R.id.calculator_subtract_button to { calculator.binaryOperation(BinaryOperation.SUBTRACTION) },
            R.id.calculator_multiply_button to { calculator.binaryOperation(BinaryOperation.MULTIPLICATION) },
            R.id.calculator_divide_button to { calculator.binaryOperation(BinaryOperation.DIVISION) },
            R.id.calculator_equals_button to { calculator.calculate() },
            R.id.calculator_0_button to { calculator.appendDigit(0) },
            R.id.calculator_1_button to { calculator.appendDigit(1) },
            R.id.calculator_2_button to { calculator.appendDigit(2) },
            R.id.calculator_3_button to { calculator.appendDigit(3) },
            R.id.calculator_4_button to { calculator.appendDigit(4) },
            R.id.calculator_5_button to { calculator.appendDigit(5) },
            R.id.calculator_6_button to { calculator.appendDigit(6) },
            R.id.calculator_7_button to { calculator.appendDigit(7) },
            R.id.calculator_8_button to { calculator.appendDigit(8) },
            R.id.calculator_9_button to { calculator.appendDigit(9) },
        )

        buttonCallbackMap.forEach {
            findViewById<Button>(it.key).setOnClickListener(it.value)
        }
    }

    private fun drawUI() {
        clearButton.text = if (calculator.isEmpty()) {
            getString(R.string.calculator_clear_button_title_all)
        } else {
            getString(R.string.calculator_clear_button_title_enter)
        }

        addButton.isSelected = calculator.isSelected(BinaryOperation.ADDITION)
        subtractButton.isSelected = calculator.isSelected(BinaryOperation.SUBTRACTION)
        multiplyButton.isSelected = calculator.isSelected(BinaryOperation.MULTIPLICATION)
        divideButton.isSelected = calculator.isSelected(BinaryOperation.DIVISION)

        displayedValueText.text = calculator.toString().ifEmpty {
            getString(R.string.calculator_error_message)
        }
    }
}