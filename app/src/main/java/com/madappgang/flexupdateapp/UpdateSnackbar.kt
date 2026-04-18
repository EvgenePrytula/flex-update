package com.madappgang.flexupdateapp

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar

object UpdateSnackbar {
    fun show(activity: AppCompatActivity, onRestart: () -> Unit) {
        Snackbar.make(
            activity.findViewById(android.R.id.content),
            "An update has just been downloaded.",
            Snackbar.LENGTH_INDEFINITE
        ).setAction("Restart") {
            onRestart()
        }.show()
    }
}
