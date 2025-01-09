package com.example.testapplication

import android.content.Context
import android.widget.Toast

open class Utils {
    companion object {
        private var toast: Toast? = null
        fun showToast(context: Context, message: String?)
        {
            if (toast != null) toast?.cancel()
            toast = Toast.makeText(context, message, Toast.LENGTH_LONG)
            toast?.show()
        }
    }


}