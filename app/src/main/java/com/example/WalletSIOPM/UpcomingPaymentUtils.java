package com.example.WalletSIOPM;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class UpcomingPaymentUtils {

    private UpcomingPaymentUtils() {
    }

    public static final class DuePayment {
        private final Abonelik subscription;
        private final Date date;

        DuePayment(Abonelik subscription, Date date) {
            this.subscription = subscription;
            this.date = date;
        }

        public Abonelik getSubscription() {
            return subscription;
        }

        public Date getDate() {
            return new Date(date.getTime());
        }
    }

    public static Date parseDate(String value) {
        if (value == null) {
            return null;
        }
        String[] formats = {"dd/MM/yyyy", "d/M/yyyy", "yyyy-MM-dd"};
        for (String format : formats) {
            SimpleDateFormat parser = new SimpleDateFormat(format, Locale.US);
            parser.setLenient(false);
            ParsePosition position = new ParsePosition(0);
            Date parsed = parser.parse(value, position);
            if (parsed != null && position.getIndex() == value.length()) {
                return parsed;
            }
        }
        return null;
    }

    public static Date getNextPaymentDate(Abonelik subscription, Date today) {
        Date start = parseDate(subscription.getDate());
        if (start == null) {
            return null;
        }
        Calendar current = Calendar.getInstance();
        current.setTime(start);
        int anchorDay = current.get(Calendar.DAY_OF_MONTH);
        Calendar limit = Calendar.getInstance();
        limit.setTime(today);
        limit.set(Calendar.HOUR_OF_DAY, 0);
        limit.set(Calendar.MINUTE, 0);
        limit.set(Calendar.SECOND, 0);
        limit.set(Calendar.MILLISECOND, 0);

        int iterations = 0;
        while (current.before(limit) && iterations++ < 12000) {
            advance(current, subscription.getBillingCycle(), anchorDay);
        }
        return iterations >= 12000 ? null : current.getTime();
    }

    public static List<DuePayment> getUpcomingPayments(List<Abonelik> subscriptions, Date today) {
        List<DuePayment> payments = new ArrayList<>();
        if (subscriptions == null) {
            return payments;
        }
        for (Abonelik subscription : subscriptions) {
            Date dueDate = getNextPaymentDate(subscription, today);
            if (dueDate != null) {
                payments.add(new DuePayment(subscription, dueDate));
            }
        }
        Collections.sort(payments, Comparator.comparing(DuePayment::getDate));
        return payments;
    }

    public static List<DuePayment> getPaymentsInPeriod(List<Abonelik> subscriptions, Date periodStart, Date periodEnd) {
        List<DuePayment> payments = new ArrayList<>();
        if (subscriptions == null) {
            return payments;
        }
        for (Abonelik subscription : subscriptions) {
            Date startDate = parseDate(subscription.getDate());
            if (startDate == null || startDate.after(periodEnd)) {
                continue;
            }

            Calendar current = Calendar.getInstance();
            current.setTime(startDate);
            int anchorDay = current.get(Calendar.DAY_OF_MONTH);
            int iterations = 0;
            while (!current.getTime().after(periodEnd) && iterations++ < 12000) {
                if (!current.getTime().before(periodStart)) {
                    payments.add(new DuePayment(subscription, current.getTime()));
                }
                advance(current, subscription.getBillingCycle(), anchorDay);
            }
        }
        Collections.sort(payments, Comparator.comparing(DuePayment::getDate));
        return payments;
    }

    public static Calendar monthStart(Calendar month) {
        Calendar result = (Calendar) month.clone();
        result.set(Calendar.DAY_OF_MONTH, 1);
        result.set(Calendar.HOUR_OF_DAY, 0);
        result.set(Calendar.MINUTE, 0);
        result.set(Calendar.SECOND, 0);
        result.set(Calendar.MILLISECOND, 0);
        return result;
    }

    private static void advance(Calendar date, String billingCycle, int anchorDay) {
        if ("7 Days".equalsIgnoreCase(billingCycle)) {
            date.add(Calendar.DAY_OF_MONTH, 7);
        } else if ("14 Days".equalsIgnoreCase(billingCycle)) {
            date.add(Calendar.DAY_OF_MONTH, 14);
        } else {
            int months;
            if ("3 Months".equalsIgnoreCase(billingCycle)) {
                months = 3;
            } else if ("6 Months".equalsIgnoreCase(billingCycle)) {
                months = 6;
            } else if ("Yearly".equalsIgnoreCase(billingCycle)) {
                months = 12;
            } else {
                months = 1;
            }
            date.set(Calendar.DAY_OF_MONTH, 1);
            date.add(Calendar.MONTH, months);
            int maxDay = date.getActualMaximum(Calendar.DAY_OF_MONTH);
            date.set(Calendar.DAY_OF_MONTH, Math.min(anchorDay, maxDay));
        }
    }
}
