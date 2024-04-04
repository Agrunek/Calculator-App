package com.app.calculator

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_menu)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.menu)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindListeners()
    }

    private fun bindListeners() {
        val buttonCallbackMap: Map<Int, (View) -> Unit> = mapOf(
            R.id.menu_calculator_simple_start_button to { calculatorSimpleStartButtonOnClick() },
            R.id.menu_calculator_advanced_start_button to { calculatorAdvancedStartButtonOnClick() },
            R.id.menu_about_start_button to { aboutStartButtonOnClick() },
            R.id.menu_exit_button to { exitButtonOnClick() }
        )

        buttonCallbackMap.forEach {
            findViewById<Button>(it.key).setOnClickListener(it.value)
        }
    }

    private fun calculatorSimpleStartButtonOnClick() {
        val intent = Intent(this, SimpleCalculatorActivity::class.java)
        startActivity(intent)
    }

    private fun calculatorAdvancedStartButtonOnClick() {
        val intent = Intent(this, AdvancedCalculatorActivity::class.java)
        startActivity(intent)
    }

    private fun aboutStartButtonOnClick() {
        val intent = Intent(this, AboutActivity::class.java)
        startActivity(intent)
    }

    private fun exitButtonOnClick() {
        finish()
    }
}