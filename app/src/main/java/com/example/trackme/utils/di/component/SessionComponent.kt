package com.example.trackme.utils.di.component

import androidx.lifecycle.ViewModelStoreOwner
import com.example.trackme.utils.di.module.ViewModelModule
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent(modules = [ViewModelModule::class])
interface SessionComponent {

    @Subcomponent.Factory
    interface Factory{
        fun create(
            @BindsInstance @Named("SESSION_OWNER") owner: ViewModelStoreOwner
        ) : SessionComponent
    }
}