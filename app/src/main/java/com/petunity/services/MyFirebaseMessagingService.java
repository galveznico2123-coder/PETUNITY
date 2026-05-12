package com.petunity.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.petunity.R;
import com.petunity.activities.MainActivity;

import java.util.Map;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "FCMService";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        
        Log.d(TAG, "Message received from: " + remoteMessage.getFrom());

        // 1. Handle Notification Payload (Automatic when app is in background, but this handles it in foreground)
        if (remoteMessage.getNotification() != null) {
            showNotification(remoteMessage.getNotification().getTitle(), 
                           remoteMessage.getNotification().getBody());
        }

        // 2. Handle Data Payload (Crucial for background/closed state custom notifications)
        if (remoteMessage.getData().size() > 0) {
            Map<String, String> data = remoteMessage.getData();
            String title = data.get("title");
            String body = data.get("body");
            showNotification(title, body);
        }
    }

    private void showNotification(String title, String body) {
        if (title == null) title = "PetUnity Alert";
        
        String channelId = "petunity_alerts";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create Notification Channel for Android Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "PetUnity Alerts", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications for urgent pet alerts and messages");
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        // Optional: Pass extra to open the Chat or Alert screen directly
        intent.putExtra("open_fragment", "chat"); 

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 
                (int) System.currentTimeMillis(), 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.logo_pet)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(pendingIntent);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "New Token: " + token);
        // In a real app, you would send this token to your Firestore 'users' document
    }
}
