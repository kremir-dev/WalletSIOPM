package com.example.WalletSIOPM;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AbonelikHatirlatici extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String name = intent.getStringExtra("isim");
        if (name == null) name = "Subscription";

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "abonelik_kanal")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Subscription Reminder")
                .setContentText("Payment upcoming for " + name + "!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }
}