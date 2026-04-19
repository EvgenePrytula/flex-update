package com.madappgang.flexupdate.core

import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.madappgang.flexupdate.core.types.UpdateDownloadState
import com.madappgang.flexupdate.core.types.UpdateOutcome
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FlexUpdateManagerTest {
    private lateinit var controller: ActivityController<AppCompatActivity>
    private lateinit var fakeAppUpdateManager: FakeAppUpdateManager

    @Before
    fun setUp() {
        controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        // Theme must be applied before create() — AppCompatActivity requires an AppCompat descendant
        controller.get().setTheme(androidx.appcompat.R.style.Theme_AppCompat_Light)
        controller.create()
        fakeAppUpdateManager = FakeAppUpdateManager(controller.get())
    }

    private fun buildManager(config: UpdateConfig = UpdateConfig()): FlexUpdateManager {
        // Manager must be built before start() — registerForActivityResult requires pre-onStart
        val manager =
            FlexUpdateManager
                .Builder(controller.get())
                .managerProvider(FakeFlexUpdateProvider(fakeAppUpdateManager))
                .config(config)
                .build()
        controller.start().resume()
        return manager
    }

    @Test
    fun `initial downloadState is Idle`() {
        val manager = buildManager()
        assertEquals(UpdateDownloadState.Idle, manager.downloadState.value)
    }

    @Test
    fun `startUpdate emits NotAvailable when no update available`() =
        runTest {
            val manager = buildManager()
            manager.startUpdate()
            shadowOf(Looper.getMainLooper()).idle() // drain Play API tasks queued on the main looper
            assertEquals(UpdateOutcome.NotAvailable, manager.outcome.first())
        }
}
