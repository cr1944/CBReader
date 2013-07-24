package cheng.app.cnbeta.util;

import cheng.app.cnbeta.R;

import android.content.Context;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TimeUtil {
    static final String TAG = "TimeUtil";
    private static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static final long WEEK = 24 * 60 * 60 * 7;
    static final long DAY = 24 * 60 * 60;
    static final long HOUR = 60 * 60;
    static final long MINUTE = 60;

    public static String formatTime(Context context, String time) {
        long timestamp = 0;
        try {
            Date date = dateFormat.parse(time);
            timestamp = date.getTime();
        } catch (ParseException e) {
            Log.w(TAG, "can't parse time!");
            return time;
        }
        long currentSeconds = System.currentTimeMillis();
        long timeGap = (currentSeconds - timestamp) / 1000;
        StringBuilder timeStr = new StringBuilder();
        if (timeGap < 0) {
            return time;
        } else if (timeGap < MINUTE) {
            timeStr.append(context.getString(R.string.second_ago));
        } else if (timeGap < HOUR) {
            timeStr.append(context.getString(R.string.minute_ago, timeGap / MINUTE));
        } else if (timeGap < DAY) {
            timeStr.append(context.getString(R.string.hour_ago, timeGap / HOUR));
        } else if (timeGap < WEEK) {
            timeStr.append(context.getString(R.string.day_ago, timeGap / DAY));
        } else {
            return time;
        }
        return timeStr.toString();
    }

}
