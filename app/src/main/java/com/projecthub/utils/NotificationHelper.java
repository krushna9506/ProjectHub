package com.projecthub.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.projecthub.models.AppNotification;
import com.projecthub.ui.MainActivity;
import com.projecthub.ui.ProjectDetailsActivity;

import java.util.Random;

public class NotificationHelper {

    public static final String CHANNEL_ID = "projecthub_channel";
    public static final String CHANNEL_NAME = "ProjectHub Alerts";
    public static final String CHANNEL_DESC = "Notifications for tasks, mentions, comments, join requests, and deadlines";
    private static final String PREFS_NAME = "projecthub_notified_keys";

    public static boolean shouldNotifyOnce(Context context, String key) {
        if (context == null || key == null || key.isEmpty()) return false;
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(key, false)) {
            return false;
        }
        prefs.edit().putBoolean(key, true).apply();
        return true;
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(CHANNEL_DESC);
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    public static void showSystemNotification(Context context, String title, String body, 
                                             String projectId, String type) {
        createNotificationChannel(context);

        Intent intent;
        if (projectId != null && !projectId.isEmpty()) {
            intent = new Intent(context, ProjectDetailsActivity.class);
            intent.putExtra("PROJECT_ID", projectId);
        } else {
            intent = new Intent(context, MainActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingFlags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            new Random().nextInt(100000),
            intent,
            pendingFlags
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title != null ? title : "ProjectHub Alert")
            .setContentText(body != null ? body : "")
            .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);

        try {
            NotificationManagerCompat managerCompat = NotificationManagerCompat.from(context);
            managerCompat.notify(new Random().nextInt(100000), builder.build());
        } catch (SecurityException ignored) {
            // Handled when user has not granted notification permission
        }
    }

    public static void dispatchNotification(Context context, String recipientUserId, 
                                            String title, String message, String type, 
                                            String projectId, String projectTitle) {
        if (recipientUserId == null || recipientUserId.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("users").document(recipientUserId)
            .collection("notifications").document();

        AppNotification notification = new AppNotification(
            docRef.getId(),
            recipientUserId,
            title,
            message,
            type,
            projectId,
            projectTitle,
            System.currentTimeMillis()
        );

        docRef.set(notification);

        // Show immediate on-device notification if targeted or testing on same device
        if (context != null) {
            String currentUid = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
            if (recipientUserId.equals(currentUid) && shouldNotifyOnce(context, "notif_" + docRef.getId())) {
                showSystemNotification(context, title, message, projectId, type);
            }
        }
    }
}
