package com.example.trackme.utils.di.component

import com.example.trackme.viewmodel.MapService
import dagger.Subcomponent

@Subcomponent
interface ServiceComponent {
    fun inject(service: MapService)

    @Subcomponent.Factory
    interface Factory{
        fun create() : ServiceComponent
    }
}