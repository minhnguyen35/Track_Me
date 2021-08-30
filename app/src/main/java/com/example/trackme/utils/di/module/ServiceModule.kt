package com.example.trackme.utils.di.module

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.trackme.R
import com.example.trackme.TrackMeApplication
import com.example.trackme.utils.Constants
import com.example.trackme.utils.di.ApplicationScope
import com.example.trackme.view.activity.RecordingActivity
import com.google.android.gms.location.FusedLocationProviderClient
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

//import dagger.hilt.InstallIn
//import dagger.hilt.android.components.ServiceComponent
//import dagger.hilt.android.qualifiers.ApplicationContext
//import dagger.hilt.android.scopes.ServiceScoped
//import javax.inject.Named

@Module
class ServiceModule {


    @Provides

    fun provideFusedLocation() =
            FusedLocationProviderClient(TrackMeApplication.instance.applicationContext)

    @Provides

    fun providePending(): PendingIntent{
        val intent = Intent(TrackMeApplication.instance.applicationContext
                , RecordingActivity::class.java)
                .apply {
                    action = Constants.ACTION_FOREGROUND
                }
//        val pending = TaskStackBuilder.create(this).run{
//            addNextIntentWithParentStack(intent)
//            getPendingIntent(0,PendingIntent.FLAG_UPDATE_CURRENT)
//        }
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