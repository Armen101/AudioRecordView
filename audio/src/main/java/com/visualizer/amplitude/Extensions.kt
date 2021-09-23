package com.visualizer.amplitude

import android.content.res.Resources

fun Int.dp(): Float {
    return this * Resources.getSystem().displayMetrics.density
}

fun Float.softTransition(compareWith: Float, allowedDiff: Float, scaleFactor: Float): Float {
    if (scaleFactor == 0f) return this //avoid from ArithmeticException (divide by zero)

    var result = this
    if (compareWith > this && compareWith / this > allowedDiff) {
        val diff = coerceAtLeast(compareWith) - coerceAtMost(compareWith)
        result += diff / scaleFactor
    } else if (this > compareWith && this / compareWith > allowedDiff) {
        val diff = coerceAtLeast(compareWith) - coerceAtMost(compareWith)
        result -= diff / scaleFactor
    }
    return result
}