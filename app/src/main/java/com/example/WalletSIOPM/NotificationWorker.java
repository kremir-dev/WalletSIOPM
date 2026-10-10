package com.example.WalletSIOPM;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

public class NotificationWorker extends Worker {

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        AboneDatabase db = AboneDatabase.getInstance(context);
        List<Abonelik> subs = db.aboneDao().tumunuGetir();

        if (subs != null && !subs.isEmpty()) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "abonelik_kanal")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Subscription Tracker Reminder")
                    .setContentText("You have active subscriptions. Check your budget and upcoming payments!")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationManager.notify(1001, builder.build());
            }
        }

        return Result.success();
    }
}