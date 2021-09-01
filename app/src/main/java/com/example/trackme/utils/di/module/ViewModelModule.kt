package com.example.trackme.utils.di.module

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.trackme.utils.ViewModelFactory
import com.example.trackme.viewmodel.RecordingViewModel
import com.example.trackme.viewmodel.SessionViewModel
import dagger.Module
import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module

class ViewModelModule {
    @Provides
    fun provideSessionVM(
        @Named("SESSION_OWNER") owner: ViewModelStoreOwner,
        factory: ViewModelFactory
    ) : SessionViewModel {
        return ViewModelProvider(owner, factory).get(SessionViewModel::class.java)
    }

    @Provides
    fun provideMapVM(
        @Named("MAP_OWNER") owner: ViewModelStoreOwner,
        factory: ViewModelFactory
    ): RecordingViewModel{
        return ViewModelProvider(owner,factory).get(RecordingViewModel::class.java)
    }
}