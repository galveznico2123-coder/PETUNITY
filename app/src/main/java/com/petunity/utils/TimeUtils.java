package com.petunity.utils;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TimeUtils {

    public static String getTimeAgo(Date date) {
        if (date == null) {
            return "";
        }

        long time = date.getTime();
        long now = System.currentTimeMillis();
        if (time > now || time <= 0) {
            return "just now";
        }

        long diff = now - time;
        long seconds = TimeUnit.MILLISECONDS.toSeconds(diff);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
        long hours = TimeUnit.MILLISECONDS.toHours(diff);
        long days = TimeUnit.MILLISECONDS.toDays(diff);

        if (seconds < 60) {
            return "just now";
        } else if (minutes < 60) {
            return minutes + "m ago";
        } else if (hours < 24) {
            return hours + "h ago";
        } else if (days < 7) {
            return days + "d ago";
        } else {
            return days / 7 + "w ago";
        }
    }
}
