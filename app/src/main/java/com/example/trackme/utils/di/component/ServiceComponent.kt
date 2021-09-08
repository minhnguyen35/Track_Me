package com.example.trackme.utils.di.component

import com.example.trackme.utils.di.module.ServiceModule
import com.example.trackme.service.MapService
import dagger.Subcomponent

@Subcomponent(modules = [ServiceModule::class])
interface ServiceComponent {
    fun inject(service: MapService)


    @Subcomponent.Factory
    interface Factory{
        fun create() : ServiceComponent
    }
}