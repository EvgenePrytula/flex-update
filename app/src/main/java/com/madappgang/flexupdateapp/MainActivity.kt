package com.madappgang.flexupdateapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.madappgang.flexupdate.core.FlexUpdateManager
import com.madappgang.flexupdate.core.types.UpdatePriority.MEDIUM
import com.madappgang.flexupdate.core.types.UpdateStrategy.Manual
import com.madappgang.flexupdateapp.BuildConfig.VERSION_CODE
import com.madappgang.flexupdateapp.BuildConfig.VERSION_NAME
import com.madappgang.flexupdateapp.ui.theme.FlexUpdateTheme


class MainActivity : ComponentActivity() {

    private val updateManager by lazy {
        FlexUpdateManager.from(
            activity = this,
            updateStrategy = Manual(updatePriority = MEDIUM)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateManager.checkForUpdate()

        enableEdgeToEdge()
        setContent {
            FlexUpdateTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        Text(
                            modifier = Modifier.align(Alignment.Center),
                            text = "Version code ${VERSION_CODE}\nVersion name:$VERSION_NAME"
                        )
                    }
                }
            }
        }
    }
}