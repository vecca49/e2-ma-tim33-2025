package com.example.bossapp.business;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.bossapp.MainActivity;
import com.example.bossapp.R;

public class LocalNotificationHelper {
    private static final String CHANNEL_FRIEND_REQUEST = "friend_requests";
    private static final String CHANNEL_ALLIANCE = "alliance_notifications";
    private static final String CHANNEL_ALLIANCE_CHAT = "alliance_chat";

    private Context context;
    private NotificationManager notificationManager;

    public LocalNotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Friend Requests Channel
            NotificationChannel friendChannel = new NotificationChannel(
                    CHANNEL_FRIEND_REQUEST,
                    "Friend Requests",
                    NotificationManager.IMPORTANCE_HIGH
            );
            friendChannel.setDescription("Notifications for friend requests");
            notificationManager.createNotificationChannel(friendChannel);

            // Alliance Channel
            NotificationChannel allianceChannel = new NotificationChannel(
                    CHANNEL_ALLIANCE,
                    "Alliance Invitations",
                    NotificationManager.IMPORTANCE_HIGH
            );
            allianceChannel.setDescription("Notifications for alliance invitations");
            notificationManager.createNotificationChannel(allianceChannel);

            // Alliance Chat Channel
            NotificationChannel chatChannel = new NotificationChannel(
                    CHANNEL_ALLIANCE_CHAT,
                    "Alliance Chat",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            chatChannel.setDescription("Messages from your alliance");
            notificationManager.createNotificationChannel(chatChannel);
        }
    }

    public void showFriendRequestNotification(String senderUsername, String senderId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("openFragment", "notifications");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 1001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_FRIEND_REQUEST)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("New Friend Request")
                .setContentText(senderUsername + " wants to be your friend")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(1001, builder.build());
    }

    public void showFriendAcceptedNotification(String username) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("openFragment", "profile");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 1002, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_FRIEND_REQUEST)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Friend Request Accepted!")
                .setContentText(username + " accepted your friend request")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(1002, builder.build());
    }

    public void showAllianceInvitationNotification(String senderUsername, String allianceName, String allianceId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("openFragment", "notifications");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 2001, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ALLIANCE)
                .setSmallIcon(R.drawable.ic_alliance)
                .setContentTitle("Alliance Invitation")
                .setContentText(senderUsername + " invited you to " + allianceName)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(2001, builder.build());
    }

    public void showAllianceAcceptedNotification(String username, String allianceName) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("openFragment", "alliance");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 2002, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ALLIANCE)
                .setSmallIcon(R.drawable.ic_alliance)
                .setContentTitle("Alliance Member Joined!")
                .setContentText(username + " joined " + allianceName)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(2002, builder.build());
    }

    public void showAllianceDeclinedNotification(String username, String allianceName) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 2003, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ALLIANCE)
                .setSmallIcon(R.drawable.ic_alliance)
                .setContentTitle("Alliance Invitation Declined")
                .setContentText(username + " declined your invitation to " + allianceName)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(2003, builder.build());
    }

    public void showAllianceMessageNotification(String senderUsername, String messageText, String allianceId) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("openFragment", "alliance_chat");
        intent.putExtra("allianceId", allianceId);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 3000 + allianceId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        String displayText = messageText.length() > 50 ?
                messageText.substring(0, 50) + "..." : messageText;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ALLIANCE_CHAT)
                .setSmallIcon(R.drawable.ic_alliance)
                .setContentTitle(senderUsername)
                .setContentText(displayText)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify(3000 + allianceId.hashCode(), builder.build());
    }
}