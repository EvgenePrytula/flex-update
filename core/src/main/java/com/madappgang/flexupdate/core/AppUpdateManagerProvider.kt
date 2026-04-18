package com.madappgang.flexupdate.core

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager

interface AppUpdateManagerProvider {
    fun provide(context: Context): AppUpdateManager
}

class DefaultAppUpdateManagerProvider : AppUpdateManagerProvider {
    override fun provide(context: Context): AppUpdateManager =
        AppUpdateManagerFactory.create(context)
}

class FakeAppUpdateManagerProvider(
    private val fakeAppUpdateManager: FakeAppUpdateManager
) : AppUpdateManagerProvider {
    override fun provide(context: Context): AppUpdateManager = fakeAppUpdateManager
}
