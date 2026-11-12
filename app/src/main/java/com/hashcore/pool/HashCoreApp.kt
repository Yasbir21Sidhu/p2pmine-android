package com.hashcore.pool

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.hashcore.pool.domain.repository.LiveDataRepository
import com.hashcore.pool.util.NotificationHelper
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HashCoreApp : Application() {

    @Inject lateinit var liveDataRepository: LiveDataRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper.ensureChannels()
        // Close WebSocket when app goes to background (battery)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                // Screens that need live data call start() themselves
            }

            override fun onStop(owner: LifecycleOwner) {
                liveDataRepository.stop()
            }
        })
    }
}
