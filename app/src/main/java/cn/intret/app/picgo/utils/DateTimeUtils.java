package cn.intret.app.picgo.utils;

import android.content.res.Resources;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.intret.app.picgo.R;

public class DateTimeUtils {

    public static int weeksBeforeToday(Date date, Date endDate) {

        DateTime now = new DateTime(endDate);
        if (date.after(now.toDate())) {
            DateTime start = DateTime.now().weekOfWeekyear().roundFloorCopy();
            DateTime end = new DateTime(date);

            Period p = new Period(start, end, PeriodType.weeks());
            return p.getWeeks();
        } else {
            DateTime start = new DateTime(date);
            DateTime end = now.weekOfWeekyear().roundCeilingCopy();
            return new Period(start, end, PeriodType.weeks()).getWeeks();
        }
    }

    public static int weeksBeforeCurrentWeek(Date date) {

        DateTime now = new DateTime(new DateTime());
        if (date.after(now.toDate())) {
            DateTime start = DateTime.now().weekOfWeekyear().roundFloorCopy();
            DateTime end = new DateTime(date);

            Period p = new Period(start, end, PeriodType.weeks());
            return p.getWeeks();
        } else {
            DateTime start = new DateTime(date);
            DateTime end = now.weekOfWeekyear().roundCeilingCopy();
            return new Period(start, end, PeriodType.weeks()).getWeeks();
        }
    }

    public static int monthsBeforeCurrentMonth(Date date) {

        DateTime now = new DateTime(new DateTime());
        if (date.after(now.toDate())) {
            DateTime start = DateTime.now().monthOfYear().roundFloorCopy();
            DateTime end = new DateTime(date);

            Period p = new Period(start, end, PeriodType.months());
            return p.getMonths();
        } else {
            DateTime start = new DateTime(date);
            DateTime end = now.monthOfYear().roundCeilingCopy();
            return new Period(start, end, PeriodType.months()).getMonths();
        }
    }

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

    public static String friendlyWeekDescription(Resources res, Date date) {
        int weeks = weeksBeforeCurrentWeek(date);
        if (weeks == 0) {
            return res.getString(R.string.current_week);
        } else if (weeks == 1) {
            return res.getString(R.string.last_week);
        } else {
            DateTime dateTime = new DateTime(date);
            DateTime floor = dateTime.weekOfWeekyear().roundFloorCopy();
            DateTime ceiling = dateTime.weekOfWeekyear().roundCeilingCopy();

            DateFormat df = SimpleDateFormat.getDateInstance();
            String floorStr = df.format(floor.toDate());
            String ceilingStr = df.format(ceiling.toDate());
            return floorStr + " - " + ceilingStr;
        }
    }

    public static String friendlyMonthDescription(Resources res, Date date) {
        int months = monthsBeforeCurrentMonth(date);
        if (months == 0) {
            return res.getString(R.string.current_month);
        } else if (months == 1) {
            return res.getString(R.string.last_month);
        } else {
            DateTime dateTime = new DateTime(date);
            DateTime floor = dateTime.monthOfYear().roundFloorCopy();
            DateTime ceiling = dateTime.monthOfYear().roundCeilingCopy();

            DateFormat df = SimpleDateFormat.getDateInstance();
            String floorStr = df.format(floor.toDate());
            String ceilingStr = df.format(ceiling.toDate());
            return floorStr + " - " + ceilingStr;
        }
    }

    public static String friendlyDayDescription(Resources res, long date) {
        int days = daysBeforeToday(date);
        if (days == 0) {
            return res.getString(R.string.today);
        } else if (days == 1) {
            return res.getString(R.string.yesterday);
        } else {
            return SimpleDateFormat.getDateInstance().format(new Date(date));
        }
    }
}
