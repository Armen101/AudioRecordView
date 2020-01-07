package com.visualizer.amplitude

import android.content.res.Resources

fun Int.dp(): Float {
    return this * Resources.getSystem().displayMetrics.density
}