package com.madappgang.flexupdate.core

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.madappgang.flexupdate.core.FlexUpdateState.Canceled
import com.madappgang.flexupdate.core.FlexUpdateState.Checking
import com.madappgang.flexupdate.core.FlexUpdateState.Downloaded
import com.madappgang.flexupdate.core.FlexUpdateState.Downloading
import com.madappgang.flexupdate.core.FlexUpdateState.Error
import com.madappgang.flexupdate.core.FlexUpdateState.Idle
import com.madappgang.flexupdate.core.FlexUpdateState.UpdateAvailable
import com.madappgang.flexupdate.core.types.UpdatePriority
import com.madappgang.flexupdate.core.types.UpdateStrategy.Auto
import com.madappgang.flexupdate.core.types.UpdateStrategy.Manual
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ActivityController

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FlexUpdateManagerTest {

    private lateinit var context: Context
    private lateinit var activityController: ActivityController<ComponentActivity>
    private lateinit var activity: ComponentActivity
    private lateinit var fakeAppUpdateManager: FakeAppUpdateManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        activityController = Robolectric.buildActivity(ComponentActivity::class.java)
        activity = activityController.get()
        fakeAppUpdateManager = FakeAppUpdateManager(context)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `when update is available with high priority, state should transition to UpdateAvailable with flexible type`() = runTest {
        // Given
        fakeAppUpdateManager.setUpdateAvailable(UpdateAvailability.UPDATE_AVAILABLE)
        fakeAppUpdateManager.setUpdatePriority(4)
        
        val manager = FlexUpdateManager.from(activity, Manual(UpdatePriority.HIGH), fakeAppUpdateManager)
        activityController.setup()
        
        // When
        manager.checkForUpdate()
        
        // Then
        manager.updateState.test {
            // Initial state from from() or collect() might be Idle
            val firstState = awaitItem()
            if (firstState is Idle) {
                assertEquals(Checking, awaitItem())
            } else {
                assertEquals(Checking, firstState)
            }
            
            val nextState = awaitItem()
            assertTrue(nextState is UpdateAvailable)
            val updateAvailable = nextState as UpdateAvailable
            assertEquals(UpdatePriority.HIGH, updateAvailable.priority)
            assertEquals(false, updateAvailable.isImmediate)
        }
    }

    @Test
    fun `when update is available with critical priority, state should transition to UpdateAvailable with immediate type`() = runTest {
        // Given
        fakeAppUpdateManager.setUpdateAvailable(UpdateAvailability.UPDATE_AVAILABLE)
        fakeAppUpdateManager.setUpdatePriority(5)
        
        val manager = FlexUpdateManager.from(activity, Manual(UpdatePriority.CRITICAL), fakeAppUpdateManager)
        activityController.setup()
        
        // When
        manager.checkForUpdate()
        
        // Then
        manager.updateState.test {
            val state = awaitItem()
            if (state is Idle) {
                assertEquals(Checking, awaitItem())
            } else {
                assertEquals(Checking, state)
            }
            
            val nextState = awaitItem()
            assertTrue(nextState is UpdateAvailable)
            val updateAvailable = nextState as UpdateAvailable
            assertEquals(UpdatePriority.CRITICAL, updateAvailable.priority)
            assertEquals(true, updateAvailable.isImmediate)
        }
    }

    @Test
    fun `when no update is available, state should transition to Idle`() = runTest {
        // Given
        val fakeAppUpdateManager = FakeAppUpdateManager(context)
        // Manual strategy with NONE priority should also result in no update
        val manager = FlexUpdateManager.from(activity, Manual(UpdatePriority.NONE), fakeAppUpdateManager)
        activityController.setup()
        
        // When
        manager.checkForUpdate()
        
        // Then
        manager.updateState.test {
            val state = awaitItem()
            if (state is Idle) {
                val checking = awaitItem()
                assertEquals(Checking, checking)
            } else {
                assertEquals(Checking, state)
            }
            
            // Advance time to ensure coroutine finishes
            testDispatcher.scheduler.advanceUntilIdle()
            
            val finalState = awaitItem()
            assertEquals(Idle, finalState)
        }
    }

    @Test
    fun `when app update info fails, state should transition to Error`() = runTest {
        // Given
        val exception = RuntimeException("Network error")
        val mockAppUpdateManager = mockk<AppUpdateManager>(relaxed = true)
        every { mockAppUpdateManager.appUpdateInfo } returns Tasks.forException(exception)

        val manager = FlexUpdateManager.from(activity, Auto, mockAppUpdateManager)
        activityController.setup()

        // When
        manager.checkForUpdate()

        // Then
        manager.updateState.test {
            val firstState = awaitItem()
            if (firstState is Idle) {
                assertEquals(Checking, awaitItem())
            } else {
                assertEquals(Checking, firstState)
            }

            val errorState = awaitItem()
            assertTrue(errorState is Error)
            assertEquals(exception, (errorState as Error).exception)
        }
    }

    @Test
    fun `when flexible update is downloading, state should transition to Downloading with progress`() = runTest {
        // Given
        val mockAppUpdateManager = mockk<AppUpdateManager>(relaxed = true)
        val listenerSlot = slot<InstallStateUpdatedListener>()
        every { mockAppUpdateManager.registerListener(capture(listenerSlot)) } returns Unit
        
        val manager = FlexUpdateManager.from(activity, Manual(UpdatePriority.HIGH), mockAppUpdateManager)
        activityController.setup()

        // When
        val installState = mockk<InstallState>()
        every { installState.installStatus() } returns InstallStatus.DOWNLOADING
        every { installState.totalBytesToDownload() } returns 100L
        every { installState.bytesDownloaded() } returns 50L
        
        // Then
        manager.updateState.test {
            awaitItem() // Idle
            listenerSlot.captured.onStateUpdate(installState)
            val downloadingState = awaitItem()
            assertTrue(downloadingState is Downloading)
            assertEquals(50, (downloadingState as Downloading).progress)

            // When downloaded
            val downloadedState = mockk<InstallState>()
            every { downloadedState.installStatus() } returns InstallStatus.DOWNLOADED
            listenerSlot.captured.onStateUpdate(downloadedState)
            assertEquals(Downloaded, awaitItem())
        }
    }

    @Test
    fun `when user cancels update, state should transition to Canceled`() = runTest {
        // Given
        val mockAppUpdateManager = mockk<AppUpdateManager>(relaxed = true)
        val listenerSlot = slot<InstallStateUpdatedListener>()
        every { mockAppUpdateManager.registerListener(capture(listenerSlot)) } returns Unit
        
        val manager = FlexUpdateManager.from(activity, Manual(UpdatePriority.HIGH), mockAppUpdateManager)
        activityController.setup()

        // When (Cancellation via listener)
        val installState = mockk<InstallState>()
        every { installState.installStatus() } returns InstallStatus.CANCELED
        
        // Then
        manager.updateState.test {
            awaitItem() // Idle
            listenerSlot.captured.onStateUpdate(installState)
            assertEquals(Canceled, awaitItem())
        }
    }


    @Test
    fun `when update is available but type is not allowed, state should transition to Idle`() = runTest {
        // Given
        val mockAppUpdateManager = mockk<AppUpdateManager>(relaxed = true)
        val info = mockk<com.google.android.play.core.appupdate.AppUpdateInfo>()
        every { info.updateAvailability() } returns UpdateAvailability.UPDATE_AVAILABLE
        every { info.updatePriority() } returns UpdatePriority.HIGH.priority
        every { info.isUpdateTypeAllowed(any<Int>()) } returns false
        every { mockAppUpdateManager.appUpdateInfo } returns Tasks.forResult(info)
        
        val manager = FlexUpdateManager.from(activity, Auto, mockAppUpdateManager)
        activityController.setup()
        
        // When
        manager.checkForUpdate()
        
        // Then
        manager.updateState.test {
            val state = awaitItem()
            if (state is Idle) {
                assertEquals(Checking, awaitItem())
            } else {
                assertEquals(Checking, state)
            }
            assertEquals(Idle, awaitItem())
        }
    }

    @Test
    fun `when using Auto strategy, priority should be taken from Play Store`() = runTest {
        // Given
        fakeAppUpdateManager.setUpdateAvailable(UpdateAvailability.UPDATE_AVAILABLE)
        fakeAppUpdateManager.setUpdatePriority(5)

        val manager = FlexUpdateManager.from(activity, Auto, fakeAppUpdateManager)
        activityController.setup()
        
        // When
        manager.checkForUpdate()
        
        // Then
        manager.updateState.test {
            skipItems(2) // Idle/Checking
            val state = awaitItem() as UpdateAvailable
            assertEquals(UpdatePriority.CRITICAL, state.priority)
            assertTrue(state.isImmediate)
        }
    }

    @Test
    fun `when resumeCheck finds downloaded update, state should transition to Downloaded`() = runTest {
        // Given
        val mockAppUpdateManager = mockk<AppUpdateManager>(relaxed = true)
        val appUpdateInfo = mockk<com.google.android.play.core.appupdate.AppUpdateInfo>()
        every { appUpdateInfo.installStatus() } returns InstallStatus.DOWNLOADED
        every { mockAppUpdateManager.appUpdateInfo } returns Tasks.forResult(appUpdateInfo)
        
        val manager = FlexUpdateManager.from(activity, Auto, mockAppUpdateManager)
        
        // When
        activityController.setup() 
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        manager.updateState.test {
            var state = awaitItem()
            while (state != Downloaded) {
                state = awaitItem()
            }
            assertEquals(Downloaded, state)
        }
    }
}
