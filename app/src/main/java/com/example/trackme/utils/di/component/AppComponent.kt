package com.example.trackme.utils.di.component

import com.example.trackme.TrackMeApplication
import com.example.trackme.utils.di.ApplicationScope
import com.example.trackme.utils.di.module.AppModule
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule

@ApplicationScope
@Component(
    modules = [
        AndroidSupportInjectionModule::class,
        AppModule::class
    ]
)
interface AppComponent : AndroidInjector<TrackMeApplication> {

    fun sessionComponent(): SessionComponent.Factory
    fun mapComponent(): MapComponent.Factory
    fun serviceComponent(): ServiceComponent.Factory
    @Component.Factory
    interface Factory : AndroidInjector.Factory<TrackMeApplication>{
    }
}