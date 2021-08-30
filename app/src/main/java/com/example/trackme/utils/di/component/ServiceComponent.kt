package com.example.trackme.utils.di.component

import android.content.Context
import com.example.trackme.utils.di.module.ServiceModule
import com.example.trackme.viewmodel.MapService
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent(modules = [ServiceModule::class])
interface ServiceComponent {
    fun inject(service: MapService)


    @Subcomponent.Factory
    interface Factory{
        fun create() : ServiceComponent
    }
}