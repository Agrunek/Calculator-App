package com.app.calculator

object Enums {

    enum class DisplayMode {
        FIRST, SECOND
    }

    enum class UnaryOperation {
        SIGN_FLIP, PERCENT, SINE, COSINE, TANGENT, NATURAL_LOGARITHM, LOGARITHM, SQUARE_ROOT, SQUARE
    }

    enum class BinaryOperation {
        NONE, ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION, POWER
    }
}