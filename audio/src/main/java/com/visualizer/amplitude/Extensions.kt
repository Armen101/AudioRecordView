package com.visualizer.amplitude

import android.content.res.Resources

fun Int.dp(): Float {
    return this * Resources.getSystem().displayMetrics.density
}

fun Float.softTransition(compareWith: Float, allowedDiff: Float, scaleFactor: Float): Float {
    if (scaleFactor == 0f) return this //avoid from ArithmeticException (divide by zero)

    var result = this
    if (compareWith > this) {
        if (compareWith / this > allowedDiff) {
            val diff = this.coerceAtLeast(compareWith) - this.coerceAtMost(compareWith)
            result += diff / scaleFactor
        }
    } else if (this > compareWith) {
        if (this / compareWith > allowedDiff) {
            val diff = this.coerceAtLeast(compareWith) - this.coerceAtMost(compareWith)
            result -= diff / scaleFactor
        }
    }
    return result
}