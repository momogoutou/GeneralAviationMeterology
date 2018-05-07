package wingsby.common.tools;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Created by wingsby on 2018/4/17.
 */
public class GFSDateTimeTools {

    public static String getGFSDateTimeVTI(DateTime inputDate,int minusHour){
        DateTime useDate=new DateTime(inputDate.getMillis());
        DateTimeFormatter dateformat = DateTimeFormat.forPattern("yyyyMMddHH");
        //转换为世界时
        useDate = useDate.minusHours(8);
        DateTime delayDate = useDate.minusHours(10);
        delayDate=delayDate.minusHours(minusHour);
        int shour = (delayDate.getHourOfDay() / 12) * 12;
        String ftimeStr = delayDate.toString(DateTimeFormat.forPattern("yyyyMMdd"))
                + String.format("%02d", shour);
        long disHour = (useDate.getMillis() - DateTime.parse(ftimeStr, dateformat).getMillis()) / 3600000l;
        String timeVTI = ftimeStr + String.format("%03d", disHour);
        return timeVTI;
    }

    public static String getGFSDateTime(DateTime inputDate,int minusHour){
        DateTime useDate=new DateTime(inputDate.getMillis());
//        useDate=new DateTime(2018,4,25,14,0);
        DateTimeFormatter dateformat = DateTimeFormat.forPattern("yyyyMMddHH");
        //转换为世界时
        useDate = useDate.minusHours(8);
        DateTime delayDate = useDate.minusHours(10);
        delayDate=delayDate.minusHours(minusHour);
        int shour = (delayDate.getHourOfDay() / 12) * 12;
        String ftimeStr = delayDate.toString(DateTimeFormat.forPattern("yyyyMMdd"))
                + String.format("%02d", shour);
        return ftimeStr;
    }
}
