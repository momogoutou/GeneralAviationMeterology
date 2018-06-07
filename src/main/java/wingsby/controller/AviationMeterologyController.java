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
import wingsby.common.tools.ConstantVar;
import wingsby.common.tools.GFSDateTimeTools;
import wingsby.parsegrib.ByteData;
import wingsby.parsegrib.Grib2dat;
import wingsby.service.AviationMeterologyService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by wingsby on 2018/4/16.
 */
@Controller
@RequestMapping
public class AviationMeterologyController {
    private static final Logger logger = Logger.getLogger(AviationMeterologyController.class);
    private static Grib2dat grib2dat = Grib2dat.getInstance();
    private static Map<String,Integer> uidCache=new HashMap<String,Integer>();

    static {
        uidCache.put("968e9bfd-c4b3-4519-a613-2277969086d6",10000);
        uidCache.put("f07b4db9-4ab5-4bc9-8d6e-7be590606f82",10000);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Grib2dat.getInstance().parseGrib();
                Grib2dat.getInstance().parseDat();
            }
        }).start();
        GFSTimeManger manger =
                new GFSTimeManger(GFSTimeManger.TIME_PER_DAY,
                        new String[]{"991800", "992200","990200","990600","991100"},
                        grib2dat);

    }

    @Autowired
    private AviationMeterologyService iService;

    @RequestMapping(value = "/forecast/test", method = RequestMethod.GET)
    @ResponseBody
    public JSONObject forcastPointTest(HttpServletRequest request, HttpServletResponse response) {
        JSONArray heights = new JSONArray();
        heights.add("500-2");
        heights.add("200-1");
        DateTime time = new DateTime(Calendar.getInstance().getTimeInMillis());
//        time=new DateTime(2018,4,25,17,0);
        JSONObject dataJSON = new JSONObject();
        try {
            JSONObject pointJson = iService.getRecentPredictPoint(39.88f, 116.42f,
                    time, heights);
            dataJSON.put("1", pointJson);
            DateTime time2 = time.minusDays(1);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        JSONObject resJSON = new JSONObject();
        try {
            JSONObject pointJson2 = iService.getRecentPredictPoint(30f, 120f,
                    time, heights);
            dataJSON.put("0", pointJson2);
            resJSON.put("code", ResStatus.SUCCESSFUL.getStatusCode());
            resJSON.put("data", dataJSON);
            resJSON.put("runtime", getTimeStr(time));
        } catch (Exception e) {
            e.printStackTrace();
            resJSON.put("code", ResStatus.SEARCH_ERROR.getStatusCode());
        }
//        resJSON.put("runtime",getTimeStr(time));
        return resJSON;
    }

    @RequestMapping(value = "/testData", method = RequestMethod.GET)
    @ResponseBody
    public JSONObject forcastPointTest2(HttpServletRequest request, HttpServletResponse response) {
        Object datestr= request.getParameter("time");
        String lev=request.getParameter("lev");
        String eles=request.getParameter("eles");
        String max=request.getParameter("max");
        String min=request.getParameter("min");
        DateTime dateTime=DateTime.now();
        if(datestr!=null){
            DateTimeFormatter dateformat = DateTimeFormat.forPattern("yyyyMMddHH");
            try {
                dateTime = dateformat.parseDateTime(datestr.toString());
            }catch (Exception e1){
                logger.error("输入时间参数不正确");
            }
        }
        CacheDataFrame frame=CacheDataFrame.getInstance();
        String key=null;
        key=GFSDateTimeTools.getGFSDateTimeVTI(dateTime, 0)+"_"+String.format("%04d",Integer.valueOf(lev))+"_"+eles;
        GFSMem mem= (GFSMem) frame.pullData(key);
        JSONObject resJSON=new JSONObject();
        JSONArray array=new JSONArray();
        for(int yidx=0;yidx<mem.getHeight();yidx++){
            for(int xidx=0;xidx<mem.getWidth();xidx++) {
                short t = mem.getData()[yidx][xidx];
                float res = ByteData.short2float(t, mem.getOffset(), mem.getScale());
                if(res>Float.valueOf(max)||res<Float.valueOf(min))array.add(res);
            }
        }
        resJSON.put("wrong data",array);
        return resJSON;
    }


    @RequestMapping(value = "/forecast/point", method = RequestMethod.POST)
    @ResponseBody
    public JSONObject forcastPoint(@RequestBody JSONArray array,HttpServletRequest request) {
        long tt = System.currentTimeMillis();
        JSONObject resJSON = new JSONObject();
        String uid=request.getParameter("uuid");
        if(uidCache.containsKey(uid)&&uidCache.get(uid)>0)
            uidCache.put(uid,uidCache.get(uid)-1);
        else{
            resJSON.put("code", 303);
            return resJSON;
        }
        DateTime dateTime = new DateTime(System.currentTimeMillis());
        try {
            JSONObject dataJSON = new JSONObject();
            for (Object obj : array) {
                JSONObject requestJson = (JSONObject) obj;
                int id = (int) requestJson.get("id");
                String strLat = requestJson.get("lat").toString();
                String strLon = requestJson.get("lng").toString();
                Object datestr= requestJson.get("time");
                if(datestr!=null){
                    DateTimeFormatter dateformat = DateTimeFormat.forPattern("yyyyMMddHH");
                    try {
                        dateTime = dateformat.parseDateTime(datestr.toString());
                    }catch (Exception e1){
                        logger.error("输入时间参数不正确");
                    }
                }
                if(!strLat.matches("\\d+?.?(\\d+)?")&&strLon.matches("\\d+?.?(\\d+)?")){
                    dataJSON.put(String.valueOf(id),"参数错误，经纬度范围为15~55,80~130");
                    logger.error("参数输入：北纬"+strLat+" 东经"+strLon+"参数错误，经纬度范围为15~55N,80~130E");
                    continue;
                }
                float lat = Float.valueOf(strLat);
                float lon = Float.valueOf(strLon);
                JSONArray heights = (JSONArray) requestJson.get("heights");
                JSONObject pointJson = iService.getRecentPredictPoint(lat, lon, dateTime, heights);
                dataJSON.put(String.valueOf(id), pointJson);
            }
            logger.debug("查询耗时:" + (System.currentTimeMillis() - tt));
            resJSON.put("delay", (System.currentTimeMillis() - tt));
            resJSON.put("code", ResStatus.SUCCESSFUL.getStatusCode());
            resJSON.put("data", dataJSON);
            resJSON.put("runtime", getTimeStr(dateTime));
            return resJSON;
        } catch (Exception e) {
            logger.error("数据查询失败:" + e);
            resJSON.put("code", ResStatus.SEARCH_ERROR.getStatusCode());
            return resJSON;
        }

    }



    @RequestMapping(value = "/meminfo", method = RequestMethod.GET)
    @ResponseBody
    public JSONObject meminfo() {
        long tt = System.currentTimeMillis();
        JSONObject resJSON = new JSONObject();
        DateTime dateTime = new DateTime(System.currentTimeMillis());
        try {
            CacheDataFrame frame=CacheDataFrame.getInstance();
            JSONObject dataJson = new JSONObject();
            dataJson.put("used memory",frame.getMemUsed()/1024./1024.+"MB");
            dataJson.put("total memory",frame.getMemTotal()/1024./1024.+"MB");
            dataJson.put("Avail memory",frame.getMemAvail()/1024./1024.+"MB");
            dataJson.put("keys",frame.getKeys());
            resJSON.put("data",dataJson);
            return resJSON;
        } catch (Exception e) {
            logger.error("内存查询:" + e);
            resJSON.put("code", ResStatus.SEARCH_ERROR.getStatusCode());
            return resJSON;
        }

    }



    private String getTimeStr(DateTime useDate) {
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
        if (timeVTI != null && timeVTI.length() > 10) {
            DateTime time = dateformat.parseDateTime(timeVTI.substring(0, 10));
            time = time.plusHours(8);
            return time.toString("yyyyMMddHH");
        } else return null;
    }



}
