/*
 * Copyright (c) 2025 Bart Vullings <dev@windkracht8.com>
 *
 * This file is part of FitoTrack
 *
 * FitoTrack is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 *
 * 	FitoTrack is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 *
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.tadris.fitness.ui.record;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;
import androidx.wear.ongoing.OngoingActivity;
import androidx.wear.ongoing.Status;

import de.tadris.fitness.R;

public class RecordService extends Service{
    private static final String NOTIFICATION_CHANNEL_ID = "FitoTrack_Ongoing";
    private NotificationManager notificationManager;
    private OngoingActivity ongoingActivity;

    @Nullable @Override public IBinder onBind(Intent intent){return null;}

    @Override public void onCreate(){
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean exists = false;
        for(NotificationChannel channel : notificationManager.getNotificationChannels()){
            String channelId = channel.getId();
            if(channelId.equals(NOTIFICATION_CHANNEL_ID)){
                exists = true;
            }else{
                notificationManager.deleteNotificationChannel(channelId);
            }
        }
        if(!exists){
            notificationManager.createNotificationChannel(new NotificationChannel(
                    NOTIFICATION_CHANNEL_ID
                    ,getString(R.string.notification_active)
                    ,NotificationManager.IMPORTANCE_HIGH)
            );
        }

    }
    @Override public void onDestroy(){
        super.onDestroy();
        notificationManager.cancelAll();
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId){
        super.onStartCommand(intent, flags, startId);
        int icon = RecordActivity.workoutData.getWorkoutTypeIcon();
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getBaseContext()
                ,0
                ,new Intent(this, RecordActivity.class)
                ,PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE
        );
        Status ongoingActivityStatus = new Status.Builder()
                .addTemplate(getString(R.string.notification_active))
                .build();
        NotificationCompat.Builder notification = new NotificationCompat.Builder(
                getBaseContext()
                ,NOTIFICATION_CHANNEL_ID
        )
                .setSmallIcon(icon)
                .setOnlyAlertOnce(true)
                .setContentIntent(pendingIntent)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true);
        if(ongoingActivity == null){
            ongoingActivity = new OngoingActivity.Builder(
                    getBaseContext()
                    ,1
                    ,notification
            )
                    .setStaticIcon(icon)
                    .setTouchIntent(pendingIntent)
                    .setStatus(ongoingActivityStatus)
                    .build();
            ongoingActivity.apply(getBaseContext());
            ServiceCompat.startForeground(
                    this
                    ,1
                    ,notification.build()
                    ,ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            );
        }else{
            notificationManager.notify(1, notification.build());
            ongoingActivity.update(getBaseContext(), ongoingActivityStatus);
        }
        return START_STICKY;
    }
}
