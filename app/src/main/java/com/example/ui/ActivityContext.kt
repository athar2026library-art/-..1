package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * Compose can expose an Activity through one or more ContextWrapper layers.
 * Unwrap safely so Google Credential Manager buttons are not disabled by a cast.
 */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
