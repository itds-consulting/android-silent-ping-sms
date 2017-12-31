package com.itds.sms.ping;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import static com.itds.sms.ping.MainActivity.PREF_DATA_SMS_STORE;

public class PingSmsReceiver extends BroadcastReceiver {
    public static final String TAG = "PingSmsReceiver";
    public final String CHANNEL_ID = "com.itds.sms.ping";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Telephony.Sms.Intents.DATA_SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            Log.d(TAG, "Received intent, not DATA_SMS_RECEIVED_ACTION, but " + intent.getAction());
            return;
        }
        SharedPreferences preferences = context.getSharedPreferences(PREF_DATA_SMS_STORE, Context.MODE_PRIVATE);
        if (!context.getSharedPreferences("MainActivity", Context.MODE_PRIVATE).getBoolean(MainActivity.PREF_RECEIVE_DATA_SMS, false)) {
            return;
        }
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }
        Object[] PDUs = (Object[]) bundle.get("pdus");
        Log.d(TAG, "Received " + (PDUs != null ? PDUs.length : 0) + " messages");

        int counter = 0;
        for (Object pdu : PDUs != null ? PDUs : new Object[0]) {
            StringBuilder sb = new StringBuilder();
            for (byte b : (byte[]) pdu) {
                sb.append(String.format("%02x", b));
            }
            Log.d(TAG, "HEX[" + (counter + 1) + "]: " + sb.toString());
            String storeString = preferences.getString(PREF_DATA_SMS_STORE, "");
            storeString += sb.toString() + ",";
            preferences.edit().putString(PREF_DATA_SMS_STORE, storeString).apply();
            Notification(context, sb.toString());
        }
    }

    public void Notification(Context context, String message) {
        // Create Notification Manager
        NotificationManager notificationmanager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationmanager == null) {
            return;
        }

        Intent intent = new Intent(context, StoreActivity.class);
        // Send data to NotificationView Class
        intent.putExtra("title", "Binary SMS Received");
        intent.putExtra("text", message);
        // Open NotificationView.java Activity
        PendingIntent pIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Create Notification using NotificationCompat.Builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, CHANNEL_ID)
                // Set Icon
                .setSmallIcon(R.mipmap.ic_launcher)
                // Set Ticker Message
                .setTicker(message)
                // Set Title
                .setContentTitle("Binary SMS Received")
                // Set Text
                .setContentText(message)
                // Add an Action Button below Notification
                .addAction(R.mipmap.ic_launcher, "Open SMS Ping", pIntent)
                // Set PendingIntent into Notification
                .setContentIntent(pIntent)
                // Dismiss Notification
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, "com.itds.sms.ping", NotificationManager.IMPORTANCE_HIGH);
            // Configure the notification channel.
            mChannel.setDescription("Data SMS Received Notifications");
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            notificationmanager.createNotificationChannel(mChannel);
        }
        // Build Notification with Notification Manager
        notificationmanager.notify(0, builder.build());

    }
}