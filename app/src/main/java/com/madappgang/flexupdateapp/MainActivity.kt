package com.madappgang.flexupdateapp

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.madappgang.flexupdate.core.types.DownloadState
import com.madappgang.flexupdate.core.InAppUpdateManager
import com.madappgang.flexupdate.core.types.UpdateOutcome
import com.madappgang.flexupdateapp.BuildConfig.VERSION_CODE
import com.madappgang.flexupdateapp.BuildConfig.VERSION_NAME
import com.madappgang.flexupdateapp.ui.theme.FlexUpdateTheme

class MainActivity : AppCompatActivity() {

    private lateinit var updateManager: InAppUpdateManager
    private var lastOutcome by mutableStateOf<UpdateOutcome?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateManager = InAppUpdateManager.Builder(this)
            .build()

        enableEdgeToEdge()
        setContent {
            FlexUpdateTheme {
                UpdateDemoScreen(
                    updateManager = updateManager,
                    lastOutcome = lastOutcome,
                    onCheckUpdate = ::checkForUpdate,
                    onInstallNow = { updateManager.completeUpdate() }
                )
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateManager.outcome.collect { outcome ->
                    lastOutcome = outcome
                    when (outcome) {
                        is UpdateOutcome.ReadyToInstall ->
                            UpdateSnackbar.show(this@MainActivity) { updateManager.completeUpdate() }

                        is UpdateOutcome.Declined ->
                            Log.d("FlexUpdate", "User declined the update")

                        is UpdateOutcome.Failed ->
                            Log.e(
                                "FlexUpdate",
                                "Update failed with error code: ${outcome.errorCode}"
                            )

                        else -> Unit
                    }
                }
            }
        }

        checkForUpdate()
    }

    private fun checkForUpdate() {
        lastOutcome = null
        updateManager.startUpdate()
    }
}

@Composable
private fun UpdateDemoScreen(
    updateManager: InAppUpdateManager,
    lastOutcome: UpdateOutcome?,
    onCheckUpdate: () -> Unit,
    onInstallNow: () -> Unit
) {
    val downloadState by updateManager.downloadState.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "In-App Update Demo",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            AppVersionCard()
            UpdateOutcomeCard(lastOutcome)
            DownloadStateCard(downloadState)

            Button(
                onClick = onCheckUpdate,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Check for Updates")
            }

            if (downloadState is DownloadState.Completed) {
                OutlinedButton(
                    onClick = onInstallNow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Install Now & Restart")
                }
            }
        }
    }
}

@Composable
private fun AppVersionCard() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Current Version", style = MaterialTheme.typography.labelMedium)
            Text(
                text = "$VERSION_NAME (build $VERSION_CODE)",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun UpdateOutcomeCard(outcome: UpdateOutcome?) {
    val (label, description) = when (outcome) {
        null -> "Checking…" to "Looking for available updates"
        is UpdateOutcome.NotAvailable -> "Up to date" to "No update available in Play Store"
        is UpdateOutcome.Accepted -> "Update accepted" to "Download will start shortly"
        is UpdateOutcome.Declined -> "Declined" to "User dismissed the update dialog"
        is UpdateOutcome.ReadyToInstall -> "Ready to install" to "Tap 'Install Now' or use the snackbar to restart"
        is UpdateOutcome.Failed -> "Failed" to "Error code: ${outcome.errorCode}"
    }

    val containerColor = when (outcome) {
        is UpdateOutcome.ReadyToInstall -> MaterialTheme.colorScheme.primaryContainer
        is UpdateOutcome.Failed -> MaterialTheme.colorScheme.errorContainer
        is UpdateOutcome.Declined -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (outcome == null) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Update Outcome", style = MaterialTheme.typography.labelMedium)
                Text(label, style = MaterialTheme.typography.bodyLarge)
                Text(description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DownloadStateCard(state: DownloadState) {
    if (state is DownloadState.Idle) return

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Download State", style = MaterialTheme.typography.labelMedium)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = when (state) {
                        is DownloadState.InProgress -> "Downloading…"
                        is DownloadState.Completed -> "Download complete"
                        is DownloadState.Installing -> "Installing…"
                        is DownloadState.Failed -> "Download failed (code ${state.errorCode})"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
                if (state is DownloadState.Installing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }

            if (state is DownloadState.InProgress) {
                LinearProgressIndicator(
                    progress = { state.percent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text("${state.percent}%", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
