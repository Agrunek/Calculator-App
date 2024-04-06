package com.app.calculator

object Constants {

    const val MAX_DISPLAY_SIZE: Int = 9

    const val SN_LOWER_BOUND: Double = 0.00000001
    const val SN_UPPER_BOUND: Double = 1000000000.0

    const val STATE_0: String = "first"
    const val STATE_1: String = "second"
    const val STATE_2: String = "displayMode"
    const val STATE_3: String = "operationIntent"
    const val STATE_4: String = "operationLocked"
    const val STATE_5: String = "manipulated"
    const val STATE_6: String = "calculated"
    const val STATE_7: String = "block"
    const val STATE_8: String = "error"
}