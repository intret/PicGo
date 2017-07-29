package cn.intret.app.picgo.utils;

import android.content.res.Resources;

import org.apache.commons.lang3.time.DateUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Minutes;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import cn.intret.app.picgo.R;

public class DateTimeUtils {

    public static int daysFromToday(Date date) {

        DateTime end = new DateTime(date);

        DateTime now = DateTime.now();
        //计算区间天数
        Period p = new Period(end, now, PeriodType.days());
        return p.getDays()+1;
    }

    public static int daysFromToday(long date) {
        return daysFromToday(new Date(date));
    }

    public static String friendlyDayDescription(Resources res, Date date) {
        return friendlyDayDescription(res, date.getTime());
    }

    public static String friendlyDayDescription(Resources res, long date) {
        int i = daysFromToday(date);
        if (i == 0) {
            return res.getString(R.string.today);
        } else if (i == 1) {
            return res.getString(R.string.yesterdy);
        } else {
            return new SimpleDateFormat("yyyy-MM-dd").format(new Date(date));
        }
    }
}
