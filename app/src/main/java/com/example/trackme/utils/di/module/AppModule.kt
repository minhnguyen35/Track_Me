package com.example.trackme.utils.di.module

import androidx.room.Room
import com.example.trackme.TrackMeApplication
import com.example.trackme.repo.database.TrackMeDatabase
import com.example.trackme.utils.di.ApplicationScope
import dagger.Module
import dagger.Provides
//import dagger.hilt.InstallIn
//import dagger.hilt.android.internal.managers.ApplicationComponentManager
//import dagger.hilt.components.SingletonComponent
//import javax.inject.Singleton


@Module
class AppModule {


    @Provides
    @ApplicationScope
    fun provideDatabase(context: TrackMeApplication): TrackMeDatabase {
        return Room.databaseBuilder(context, TrackMeDatabase::class.java, "track_me_database")
            .build()
    }

}