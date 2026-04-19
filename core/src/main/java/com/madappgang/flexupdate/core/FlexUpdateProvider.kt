package com.madappgang.flexupdate.core

import android.content.Context
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager

interface FlexUpdateProvider {
    fun provide(context: Context): AppUpdateManager
}

class DefaultFlexUpdateProvider : FlexUpdateProvider {
    override fun provide(context: Context): AppUpdateManager = AppUpdateManagerFactory.create(context)
}

class FakeFlexUpdateProvider(
    private val fakeAppUpdateManager: FakeAppUpdateManager,
) : FlexUpdateProvider {
    override fun provide(context: Context): AppUpdateManager = fakeAppUpdateManager
}
