package com.tweetlanes.android.core;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.support.v4.app.NotificationCompat;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v4.app.TaskStackBuilder;

import com.tweetlanes.android.core.view.AlarmReceiver;
import com.tweetlanes.android.core.view.DeleteNotificationsReceiver;
import com.tweetlanes.android.core.view.HomeActivity;

public class Notifier {

    public static void notify(String title, String text, String bigText, Boolean autoCancel, int id,
                              String accountKey, String type, long postId, Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setTicker(text)
                .setSmallIcon(R.drawable.notification_default)
                .setAutoCancel(autoCancel)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(bigText));

        Uri ringtone = AppSettings.get().getRingtoneUri();
        if (ringtone != null) {
            builder.setSound(ringtone);
        }

        Intent resultIntent = new Intent(context, HomeActivity.class);
        resultIntent.putExtra("account_key", accountKey);
        resultIntent.putExtra("notification_type", type);
        resultIntent.putExtra("post_id", postId);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(HomeActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        int requestCode = (int) (Math.random() * Integer.MAX_VALUE);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(requestCode,
                PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);

        Intent deleteIntent = new Intent(context, DeleteNotificationsReceiver.class);
        deleteIntent.putExtra("account_key", accountKey);
        deleteIntent.putExtra("post_id", postId);
        deleteIntent.putExtra("notification_type", type);
        requestCode = (int) (Math.random() * Integer.MAX_VALUE);
        PendingIntent deletePendingIntent = PendingIntent.getBroadcast(context, requestCode, deleteIntent, 0);

        builder.setDeleteIntent(deletePendingIntent);

        saveLastNotificationDisplayed(context, accountKey, type, postId);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context
                .NOTIFICATION_SERVICE);
        notificationManager.notify(id, builder.build());
    }

    public static void cancel(Context context, String accountKey, String type) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context
                .NOTIFICATION_SERVICE);
        notificationManager.cancel((accountKey + type).hashCode());

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putInt(SharedPreferencesConstants.NOTIFICATION_COUNT + accountKey + type, 0);
        edit.putString(SharedPreferencesConstants.NOTIFICATION_SUMMARY + accountKey + type, "");
        edit.commit();

        Notifier.setDashclockValues(context, accountKey, type, 0, "");
    }

    public static void setNotificationAlarm(Context context) {
        if (AppSettings.get().isShowNotificationsEnabled()) {
            setupNotificationAlarm(context);
        } else {
            cancelNotificationAlarm(context);
        }
    }

    private static void setupNotificationAlarm(Context context) {
        //Create a new PendingIntent and add it to the AlarmManager
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 12345, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);

        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                AppSettings.get().getNotificationTime(), pendingIntent);
    }

    private static void cancelNotificationAlarm(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 12345, intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Activity.ALARM_SERVICE);
        am.cancel(pendingIntent);
    }

    public static void saveLastNotificationActioned(Context context, String accountKey, String type, long postId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = preferences.edit();

        String pref = type == SharedPreferencesConstants.NOTIFICATION_TYPE_MENTION ? SharedPreferencesConstants.NOTIFICATION_LAST_ACTIONED_MENTION_ID : SharedPreferencesConstants.NOTIFICATION_LAST_ACTIONED_DIRECT_MESSAGE_ID;
        edit.putLong(pref + accountKey, postId);
        edit.commit();

        Notifier.setDashclockValues(context, accountKey, type, 0, "");
    }

    public static void saveLastNotificationDisplayed(Context context, String accountKey, String type, long postId) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = preferences.edit();

        String pref = type == SharedPreferencesConstants.NOTIFICATION_TYPE_MENTION ? SharedPreferencesConstants.NOTIFICATION_LAST_DISPLAYED_MENTION_ID : SharedPreferencesConstants.NOTIFICATION_LAST_DISPLAYED_DIRECT_MESSAGE_ID;
        edit.putLong(pref + accountKey, postId);
        edit.commit();
    }

    public static void setDashclockValues(Context context, String accountKey, String type,  int count, String detail) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putInt(SharedPreferencesConstants.NOTIFICATION_COUNT + accountKey + type, count);
        edit.putString(SharedPreferencesConstants.NOTIFICATION_SUMMARY + accountKey + type, detail);
        edit.commit();
    }
}
