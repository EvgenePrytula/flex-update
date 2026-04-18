package com.madappgang.flexupdate.core.ui

import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.madappgang.flexupdate.core.R

object UpdateSnackbar {
    fun show(activity: AppCompatActivity, onRestart: () -> Unit) {
        Snackbar.make(
            activity.findViewById(android.R.id.content),
            activity.getString(R.string.flex_update_snackbar_message),
            Snackbar.LENGTH_INDEFINITE
        ).setAction(activity.getString(R.string.flex_update_snackbar_action)) {
            onRestart()
        }.show()
    }
}
