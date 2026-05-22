package com.petunity.utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class PetTimeUtils {
    public static String getTimeAgo(Date date) {
        if (date == null) return "Unknown time";
        
        long time = date.getTime();
        long now = System.currentTimeMillis();
        long diff = now - time;
        
        if (diff < TimeUnit.MINUTES.toMillis(1)) {
            return "Just now";
        } else if (diff < TimeUnit.HOURS.toMillis(1)) {
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        } else if (diff < TimeUnit.DAYS.toMillis(1)) {
            long hours = TimeUnit.MILLISECONDS.toHours(diff);
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        } else {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault());
            return sdf.format(date);
        }
    }
}
