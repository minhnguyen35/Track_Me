package com.example.trackme

import com.example.trackme.utils.di.component.AppComponent
import com.example.trackme.utils.di.component.DaggerAppComponent
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication




class TrackMeApplication : DaggerApplication() {
    companion object {
        lateinit var instance: TrackMeApplication
            private set

        const val SHARED_NAME = "TRACK_ME_SHARED"
        const val RECORD_STATE = "RECORD_STATE"
    }

    init {
        instance = this
    }

    private val _appComponent: AppComponent =
            DaggerAppComponent.factory().create(this) as AppComponent

    val appComponent: AppComponent
        get() = _appComponent

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> {
        return appComponent
    }
}