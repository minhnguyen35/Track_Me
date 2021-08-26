package com.example.trackme.utils.di.component

import androidx.lifecycle.ViewModelStoreOwner
import com.example.trackme.utils.di.module.ViewModelModule

import com.example.trackme.view.activity.RecordingActivity
import com.example.trackme.view.fragment.MapsFragment
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent(modules = [ViewModelModule::class])
interface MapComponent {
    fun inject(activity: RecordingActivity)
    fun inject(fragment: MapsFragment)
    @Subcomponent.Factory
    interface Factory{
        fun create(
            @BindsInstance @Named("MAP_OWNER") owner: ViewModelStoreOwner
        ) : MapComponent
    }
}