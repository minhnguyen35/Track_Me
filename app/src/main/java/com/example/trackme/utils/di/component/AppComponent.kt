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

    //fun sessionComponent(): FragmentComponent.SessionComponent.Factory

    @Component.Factory
    abstract class Factory : AndroidInjector.Factory<TrackMeApplication>
}