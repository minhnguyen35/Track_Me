package com.example.trackme.utils.di.module

import androidx.room.Room
import com.example.trackme.TrackMeApplication
import com.example.trackme.repo.database.TrackMeDatabase
import com.example.trackme.utils.di.ApplicationScope
import dagger.Module
import dagger.Provides


@Module
class AppModule {

    @ApplicationScope
    @Provides
    fun provideDatabase(context: TrackMeApplication): TrackMeDatabase {
        return Room.databaseBuilder(context, TrackMeDatabase::class.java, "track_me_database")
            .build()
    }

}