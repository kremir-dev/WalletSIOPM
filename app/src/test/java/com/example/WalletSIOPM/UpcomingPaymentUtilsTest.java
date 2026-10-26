package com.example.WalletSIOPM;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UpcomingPaymentUtilsTest {

    @Test
    public void monthlyDateKeepsOriginalDayAfterShortMonth() throws Exception {
        Abonelik subscription = new Abonelik(
                "Service", "10", "31/01/2026", "Other", "", "1 Month");
        Date marchFirst = new SimpleDateFormat("dd/MM/yyyy", Locale.US).parse("01/03/2026");

        Date nextPayment = UpcomingPaymentUtils.getNextPaymentDate(subscription, marchFirst);

        assertNotNull(nextPayment);
        assertEquals("31/03/2026",
                new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(nextPayment));
    }

    @Test
    public void monthlyPeriodIncludesShortMonthRenewal() throws Exception {
        Abonelik subscription = new Abonelik(
                "Service", "10", "31/01/2026", "Other", "", "1 Month");
        Calendar start = Calendar.getInstance();
        start.set(2026, Calendar.FEBRUARY, 1, 0, 0, 0);
        start.set(Calendar.MILLISECOND, 0);
        Calendar end = (Calendar) start.clone();
        end.set(Calendar.DAY_OF_MONTH, end.getActualMaximum(Calendar.DAY_OF_MONTH));
        end.set(Calendar.HOUR_OF_DAY, 23);
        end.set(Calendar.MINUTE, 59);
        end.set(Calendar.SECOND, 59);

        List<UpcomingPaymentUtils.DuePayment> payments =
                UpcomingPaymentUtils.getPaymentsInPeriod(
                        java.util.Collections.singletonList(subscription), start.getTime(), end.getTime());

        assertEquals(1, payments.size());
        assertEquals("28/02/2026",
                new SimpleDateFormat("dd/MM/yyyy", Locale.US).format(payments.get(0).getDate()));
    }
}
