package wingsby.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import wingsby.common.*;
import wingsby.common.tools.GFSDateTimeTools;
import wingsby.parsegrib.Grib2dat;
import wingsby.service.AviationMeterologyService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Calendar;
import java.util.Set;

/**
 * Created by wingsby on 2018/4/16.
 */
@Controller
@RequestMapping
public class AviationMeterologyController {
    private static final Logger logger = Logger.getLogger(AviationMeterologyController.class);

    private static Grib2dat grib2dat = Grib2dat.getInstance();


    static {
        Grib2dat.getInstance().parseGrib();
        Grib2dat.getInstance().parseDat();
        GFSTimeManger manger =
                new GFSTimeManger(GFSTimeManger.TIME_PER_DAY,
                        new String[]{"991800", "990200"},
                        grib2dat);
    }

    @Autowired
    private AviationMeterologyService iService;

    @RequestMapping(value = "/forecast/test", method = RequestMethod.GET)
    @ResponseBody
    public JSONObject forcastPointTest(HttpServletRequest request, HttpServletResponse response) {
        JSONArray heights = new JSONArray();
//        GFSMem mem=(GFSMem) CacheDataFrame.getInstance().
//                pullData("2018050612013_9999_TMPS");
//        float a=mem.getByLonLat(116.8f,39.6f);
//        System.out.println(a);



        heights.add("500-2");
        heights.add("200-1");
        DateTime time = new DateTime(Calendar.getInstance().getTimeInMillis());
//        time=new DateTime(2018,4,25,17,0);
        JSONObject dataJSON = new JSONObject();
        try {
            JSONObject pointJson = iService.getRecentPredictPoint(39.6f, 116f,
                    time, heights);
            dataJSON.put("1", pointJson);
            DateTime time2 = time.minusDays(1);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        JSONObject resJSON = new JSONObject();
        try {
            JSONObject pointJson2 = iService.getRecentPredictPoint(40f, 120f,
                    time, heights);
            dataJSON.put("0", pointJson2);
            resJSON.put("code", ResStatus.SUCCESSFUL.getStatusCode());
            resJSON.put("data", dataJSON);
            resJSON.put("runtime",getTimeStr(time));
        } catch (Exception e) {
            e.printStackTrace();
            resJSON.put("code", ResStatus.SEARCH_ERROR.getStatusCode());
        }
//        resJSON.put("runtime",getTimeStr(time));
        return resJSON;
    }

    @RequestMapping(value = "/forecast/test2", method = RequestMethod.POST)
    @ResponseBody
    public JSONObject forcastPointTest2(HttpServletRequest request, HttpServletResponse response) {
        JSONArray heights = new JSONArray();
        heights.add("500-2");
        heights.add("200-1");
        DateTime time = new DateTime(Calendar.getInstance().getTimeInMillis());
        JSONObject pointJson = iService.getRecentPredictPoint(40f, 120f,
                time, heights);
        JSONObject dataJSON = new JSONObject();
        dataJSON.put("1", pointJson);
        DateTime time2 = new DateTime(2018, 8, 23, 20, 0, 0);
        JSONObject pointJson2 = iService.getRecentPredictPoint(40f, 120f,
                time, heights);
        dataJSON.put("0", pointJson2);
        JSONObject resJSON = new JSONObject();
        resJSON.put("code", ResStatus.SUCCESSFUL.getStatusCode());
        resJSON.put("data", dataJSON);
        resJSON.put("runtime",getTimeStr(time));
        return resJSON;
    }


    @RequestMapping(value = "/forecast/point", method = RequestMethod.POST)
    @ResponseBody
    public JSONObject forcastPoint(@RequestBody JSONArray array) {
        long tt = System.currentTimeMillis();
        JSONObject resJSON = new JSONObject();
        DateTime dateTime = new DateTime(System.currentTimeMillis());
        try {
            JSONObject dataJSON = new JSONObject();
            for (Object obj : array) {
                JSONObject requestJson = (JSONObject) obj;
                int id = (int) requestJson.get("id");
                float lat = Float.valueOf(requestJson.get("lat").toString());
                float lon = Float.valueOf(requestJson.get("lng").toString());
                JSONArray heights = (JSONArray) requestJson.get("heights");
//                dateTime=new DateTime(2018,4,25,17,0);
                JSONObject pointJson = iService.getRecentPredictPoint(lat, lon, dateTime, heights);
                dataJSON.put(String.valueOf(id), pointJson);
            }
            logger.debug("查询耗时:" + (System.currentTimeMillis() - tt));
            resJSON.put("delay", (System.currentTimeMillis() - tt));
            resJSON.put("code", ResStatus.SUCCESSFUL.getStatusCode());
            resJSON.put("data", dataJSON);
            resJSON.put("runtime",getTimeStr(dateTime));
//            JSONUtil.writeJSONToResponse(response, resJSON);
            return resJSON;
        } catch (Exception e) {
            logger.error("日志信息查询:" + e);
            resJSON.put("code", ResStatus.SEARCH_ERROR.getStatusCode());
//            JSONUtil.writeJSONToResponse(response, resJSON);
            return resJSON;
        }

    }

    private String getTimeStr(DateTime useDate){
        String timeVTI = GFSDateTimeTools.getGFSDateTimeVTI(useDate, 0);
        Set<String> keys = CacheDataFrame.getInstance().getKeys();
        boolean timeExists = false;
        for (String key : keys) {
            if (key.contains(timeVTI)) {
                timeExists = true;
                break;
            }
        }
        if (!timeExists)             //往前推12个小时
            timeVTI = GFSDateTimeTools.getGFSDateTimeVTI(useDate, 12);
        DateTimeFormatter dateformat = DateTimeFormat.forPattern("yyyyMMddHH");
        if(timeVTI!=null&&timeVTI.length()>10){
            DateTime time=dateformat.parseDateTime(timeVTI.substring(0,10));
            time=time.plusHours(8);
            return time.toString("yyyyMMddHH");
        }else return null;
    }

}
