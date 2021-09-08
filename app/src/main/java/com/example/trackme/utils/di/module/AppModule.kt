package com.example.trackme.utils.di.module

import android.app.PendingIntent
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import androidx.core.app.NotificationCompat
import androidx.room.Room

import com.example.trackme.R
import com.example.trackme.TrackMeApplication

import com.example.trackme.repo.database.TrackMeDatabase
import com.example.trackme.utils.Constants
import com.example.trackme.utils.di.ApplicationScope
import com.example.trackme.view.activity.RecordingActivity
import com.google.android.gms.location.FusedLocationProviderClient
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
    @Provides
    fun providePending(): PendingIntent {
        val intent = Intent(TrackMeApplication.instance.applicationContext
                , RecordingActivity::class.java)
                .apply {
                    action = Constants.ACTION_FOREGROUND
                }

        val pending = PendingIntent.getActivity(
                TrackMeApplication.instance.applicationContext,
                0, intent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        return pending
    }

    @Provides
    fun provideNotificationBuilder(
            pending: PendingIntent
    ): NotificationCompat.Builder {
        val notificationBuilder = NotificationCompat
                .Builder(TrackMeApplication.instance.applicationContext
                        , Constants.NOTIFICATION_CHANNEL_ID)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(pending)
                .setSmallIcon(R.drawable.ic_run_24)
                .setContentText("Distance")
                .setContentTitle("You Are Running")

        return notificationBuilder
    }

}