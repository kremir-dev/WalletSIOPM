package com.example.WalletSIOPM;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SubscriptionWidgetProvider extends AppWidgetProvider {

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        refresh(context);
    }

    public static void refresh(Context context) {
        Context appContext = context.getApplicationContext();
        EXECUTOR.execute(() -> {
            SharedPreferences prefs = appContext.getSharedPreferences("AppSettings", Context.MODE_PRIVATE);
            boolean lockEnabled = prefs.getBoolean(SettingsActivity.PREF_APP_LOCK_ENABLED, false);
            if (lockEnabled) {
                updateWidgets(appContext, null, true);
                return;
            }
            List<Abonelik> subscriptions = AboneDatabase.getInstance(appContext)
                    .aboneDao().tumunuGetir();
            List<UpcomingPaymentUtils.DuePayment> payments =
                    UpcomingPaymentUtils.getUpcomingPayments(subscriptions, new Date());
            updateWidgets(appContext, payments, false);
        });
    }

    private static void updateWidgets(Context appContext,
                                      List<UpcomingPaymentUtils.DuePayment> payments,
                                      boolean lockEnabled) {
        AppWidgetManager manager = AppWidgetManager.getInstance(appContext);
        int[] ids = manager.getAppWidgetIds(new ComponentName(appContext, SubscriptionWidgetProvider.class));
        for (int id : ids) {
            RemoteViews views = new RemoteViews(appContext.getPackageName(), R.layout.widget_upcoming_payments);
            Intent openIntent = new Intent(appContext, UpcomingPaymentsActivity.class);
            views.setOnClickPendingIntent(R.id.widgetUpcomingRoot,
                    android.app.PendingIntent.getActivity(appContext, 0, openIntent,
                            android.app.PendingIntent.FLAG_UPDATE_CURRENT
                                    | android.app.PendingIntent.FLAG_IMMUTABLE));
            views.setTextViewText(R.id.widgetPayment1,
                    lockEnabled ? "Unlock app to view payments" : paymentText(payments, 0));
            views.setTextViewText(R.id.widgetPayment2, lockEnabled ? "" : paymentText(payments, 1));
            views.setTextViewText(R.id.widgetPayment3, lockEnabled ? "" : paymentText(payments, 2));
            manager.updateAppWidget(id, views);
        }
    }

    private static String paymentText(List<UpcomingPaymentUtils.DuePayment> payments, int index) {
        if (index >= payments.size()) {
            return index == 0 && payments.isEmpty() ? "No upcoming payments" : "";
        }
        UpcomingPaymentUtils.DuePayment payment = payments.get(index);
        Abonelik subscription = payment.getSubscription();
        String date = new SimpleDateFormat("d MMM", Locale.getDefault()).format(payment.getDate());
        return String.format(Locale.getDefault(), "%s • %s%s • %s",
                subscription.getName(), subscription.getCurrency(), subscription.getAmount(), date);
    }
}
