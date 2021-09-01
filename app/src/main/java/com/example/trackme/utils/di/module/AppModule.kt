package com.example.trackme.utils.di.module

import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.room.Room
import com.example.trackme.TrackMeApplication
import com.example.trackme.repo.database.TrackMeDatabase
import com.example.trackme.utils.di.ApplicationScope
import dagger.Module
import dagger.Provides

@Module
class AppModule {


    @Provides
    @ApplicationScope
    fun provideDatabase(context: TrackMeApplication): TrackMeDatabase {
        return Room.databaseBuilder(context, TrackMeDatabase::class.java, "track_me_database")
            .build()
    }

    @Provides
    @ApplicationScope
    fun provideSharedPreference(context: TrackMeApplication): SharedPreferences {
        return context.getSharedPreferences(TrackMeApplication.SHARED_NAME, MODE_PRIVATE)
    }

}