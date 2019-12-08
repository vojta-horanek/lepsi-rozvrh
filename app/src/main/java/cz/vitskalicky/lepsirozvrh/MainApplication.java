package cz.vitskalicky.lepsirozvrh;

import android.app.AlarmManager;
import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import org.joda.time.LocalDateTime;

import cz.vitskalicky.lepsirozvrh.bakaAPI.rozvrh.RozvrhAPI;
import cz.vitskalicky.lepsirozvrh.items.Rozvrh;
import cz.vitskalicky.lepsirozvrh.notification.NotiBroadcastReciever;
import cz.vitskalicky.lepsirozvrh.notification.PermanentNotification;
import io.sentry.Sentry;
import io.sentry.android.AndroidSentryClientFactory;


public class MainApplication extends Application {
    private static final String TAG = MainApplication.class.getSimpleName();
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize the Sentry (crash report) client
        if (SharedPrefs.getBooleanPreference(this, R.string.PREFS_SEND_CRASH_REPORTS)){
            enableSentry();
        }else{
            diableSentry();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Register notification channel for the permanent notification
            CharSequence name = getString(R.string.notification_channel_name);
            String description = getString(R.string.notification_detials);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(PermanentNotification.PERMANENT_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setSound(Uri.parse("android.resource://" + BuildConfig.APPLICATION_ID + "/" + R.raw.silence),new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build());
            channel.setShowBadge(false);
            channel.setVibrationPattern(null);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }

        if (SharedPrefs.getBooleanPreference(this, R.string.PREFS_NOTIFICATION, true)){
            enableNotification();
        }else {
            disableNotification();
        }
    }

    private LocalDateTime scheduledNotificationTime = null;

    public LocalDateTime getScheduledNotificationTime() {
        return scheduledNotificationTime;
    }

    /**
     * Schedules AlarmManager for notification update
     * @param triggerTime
     */
    public void scheduleNotificationUpdate(LocalDateTime triggerTime){
        if (triggerTime == null){
            triggerTime = LocalDateTime.now().plusDays(1);
        }
        PendingIntent pendingIntent = getNotiPendingIntent(this);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, triggerTime.toDate().getTime(),60 * 60000,  pendingIntent);

        Log.d(TAG, "Scheduled a notificatio upadate on " + triggerTime.toString("MM-dd HH:mm:ss"));
        scheduledNotificationTime = triggerTime;
    }

    private static PendingIntent getNotiPendingIntent(Context context){
        Intent intent = new Intent(context, NotiBroadcastReciever.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context.getApplicationContext(), NotiBroadcastReciever.REQUEST_CODE, intent, 0);
        return pendingIntent;
    }

    /**
     * Schedules notification update accordingly to the give Rozvrh using
     * {@link Rozvrh#getNextCurrentLessonChangeTime()}. If there is already one scheduled, it is
     * overwritten.
     * @return <code>true</code> if successful or <code>false</code> if not
     * ({@link Rozvrh#getNextCurrentLessonChangeTime()} returned error, because this is an
     * old/permanent schedule).
     */
    public boolean scheduleNotificationUpdate(Rozvrh rozvrh){
        Rozvrh.GetNCLCTreturnValues values = rozvrh.getNextCurrentLessonChangeTime();
        if (values.localDateTime == null){
            return false;
        }
        scheduleNotificationUpdate(values.localDateTime);
        return true;
    }

    /**
     * Same as {@link #scheduleNotificationUpdate(Rozvrh)}, but gets the rozvrh for you.
     * @param onFinished
     */
    public void scheduleNotificationUpdate(onFinishedListener onFinished){
        RozvrhAPI rozvrhAPI = AppSingleton.getInstance(this).getRozvrhAPI();
        rozvrhAPI.getNextNotificationUpdateTime(updateTime -> {
            if (updateTime == null){
                onFinished.onFinished(false);
            }else {
                scheduleNotificationUpdate(updateTime);
                onFinished.onFinished(true);
            }
        });
    }

    public void enableNotification(){
        SharedPrefs.setBoolean(this, getString(R.string.PREFS_NOTIFICATION), true);
        RozvrhAPI rozvrhAPI = AppSingleton.getInstance(this).getRozvrhAPI();
        PermanentNotification.update(this, rozvrhAPI,() -> {});
    }

    public void disableNotification(){
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.cancel(getNotiPendingIntent(this));
        SharedPrefs.setBoolean(this, getString(R.string.PREFS_NOTIFICATION), false);
        PermanentNotification.update(null, this);
    }

    public static interface onFinishedListener {
        public void onFinished(boolean successful);
    }

    public void enableSentry(){
        Sentry.init("https://d13d732d380444f5bed7487cfea65814@sentry.io/1820627", new AndroidSentryClientFactory(this));
    }

    public void diableSentry(){
        Sentry.close();
    }
}