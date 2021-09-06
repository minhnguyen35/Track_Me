package com.example.trackme.utils.di.component

import androidx.lifecycle.ViewModelStoreOwner
import com.example.trackme.utils.di.module.ViewModelModule
import com.example.trackme.view.activity.SessionActivity
import dagger.BindsInstance
import dagger.Subcomponent
import javax.inject.Named

@Subcomponent(modules = [ViewModelModule::class])
interface SessionComponent {

    fun inject(activity: SessionActivity)

    @Subcomponent.Factory
    interface Factory{
        fun create(
            @BindsInstance @Named("SESSION_OWNER") owner: ViewModelStoreOwner
            ) : SessionComponent
    }


}