package cn.intret.app.picgo.utils;

import android.content.res.Resources;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import cn.intret.app.picgo.R;

public class DateTimeUtils {

    /**
     * 计算一个日期在今天之前的天数
     * @param date 指定要计算的日期
     * @return 返回一个正数，表示指定的日期与今天的距离天数，0 表示今天，1表示昨天，2表示前天；返回一个负数，表示今天与指定日期的天数距离。
     */
    public static int daysBeforeToday(Date date) {

        DateTime now = DateTime.now();
        if (date.after(now.toDate())) {
            DateTime start = DateTime.now().dayOfYear().roundFloorCopy(); // 今天的开始时间
            DateTime end = new DateTime(date);

            Period p = new Period(start, end, PeriodType.days());
            return -p.getDays();
        } else {
            DateTime start = new DateTime(date);
            DateTime end = now.dayOfYear().roundCeilingCopy(); // 今天的结束时间
            return new Period(start, end, PeriodType.days()).getDays();
        }
    }

    public static int daysBeforeToday(long date) {
        return daysBeforeToday(new Date(date));
    }

    public static String friendlyDayDescription(Resources res, Date date) {
        return friendlyDayDescription(res, date.getTime());
    }

    public static String friendlyDayDescription(Resources res, long date) {
        int days = daysBeforeToday(date);
        if (days == 0) {
            return res.getString(R.string.today);
        } else if (days == 1) {
            return res.getString(R.string.yesterdy);
        } else {
            return new SimpleDateFormat("yyyy-MM-dd").format(new Date(date));
        }
    }
}
