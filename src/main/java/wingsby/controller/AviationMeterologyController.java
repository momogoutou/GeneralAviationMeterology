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
import redis.clients.jedis.JedisCluster;
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
    private static Map<String, Integer> uidCache = new HashMap<String, Integer>();
    private static final long expireTime = 3600 * 1000l;
//    private static Map<String,String> tokenMap=new HashMap<>();
    // 过期代码 310 303无权限

    static {
        uidCache.put("968e9bfd-c4b3-4519-a613-2277969086d6", 100000);
        uidCache.put("f07b4db9-4ab5-4bc9-8d6e-7be590606f82", 100000);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Grib2dat.getInstance().parseGrib();
                Grib2dat.getInstance().parseDat();
            }
        }).start();
        GFSTimeManger manger =
                new GFSTimeManger(GFSTimeManger.TIME_PER_DAY,
                        new String[]{"991800", "992200", "990200", "990600", "991100"},
                        grib2dat);
    }

    @Autowired
    private AviationMeterologyService iService;

    @Autowired
    private JedisCluster cluster;

    @RequestMapping(value = "/forecast/mem", method = RequestMethod.GET)
    @ResponseBody
    public JSONObject forcastPointTest(HttpServletRequest request, HttpServletResponse response) {
        JSONObject resJSON = new JSONObject();

        String username = request.getParameter("user");
        String token = request.getParameter("token");
        int code = checkToken(username, token);
        if (code > 0) {
            resJSON.put("code", code);
            return resJSON;
        }
        //内存监控
        resJSON.put("total", Runtime.getRuntime().totalMemory() / 1024. / 1024. + "MB");
        resJSON.put("max", Runtime.getRuntime().maxMemory() / 1024. / 1024. + "MB");
        resJSON.put("free", Runtime.getRuntime().freeMemory() / 1024. / 1024. + "MB");
        return resJSON;
    }

    @RequestMapping(value = "/testData", method = RequestMethod.GET)
    @ResponseBody
    public JSONObject forcastPointTest2(HttpServletRequest request, HttpServletResponse response) {
        Object datestr = request.getParameter("time");
        String lev = request.getParameter("lev");
        String eles = request.getParameter("eles");
        String max = request.getParameter("max");
        String min = request.getParameter("min");
        DateTime dateTime = DateTime.now();
        if (datestr != null) {
            DateTimeFormatter dateformat = DateTimeFormat.forPattern("yyyyMMddHH");
            try {
                dateTime = dateformat.parseDateTime(datestr.toString());
            } catch (Exception e1) {
                logger.error("输入时间参数不正确");
            }
        }
        CacheDataFrame frame = CacheDataFrame.getInstance();
        String key = null;
        key = GFSDateTimeTools.getGFSDateTimeVTI(dateTime, 0) + "_" + String.format("%04d", Integer.valueOf(lev)) + "_" + eles;
        GFSMem mem = (GFSMem) frame.pullData(key);
        JSONObject resJSON = new JSONObject();
        JSONArray array = new JSONArray();
        for (int yidx = 0; yidx < mem.getHeight(); yidx++) {
            for (int xidx = 0; xidx < mem.getWidth(); xidx++) {
                short t = mem.getData()[yidx][xidx];
                float res = ByteData.short2float(t, mem.getOffset(), mem.getScale());
                if (res > Float.valueOf(max) || res < Float.valueOf(min)) array.add(res);
            }
        }
        resJSON.put("wrong data", array);
        return resJSON;
    }


    public int checkToken(String name, String token) {
        String cacheToken = cluster.hget("XHMS_USERS", name);
        if (cacheToken.equals(token)) {
            String[] strs = token.split("_");
            if (strs != null && strs.length == 2) {
                DateTime time = DateTime.now();
                DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyyMMddHHmmss");
                DateTime stime = DateTime.parse(strs[1], formatter);
                if (time.getMillis() > (stime.getMillis() + expireTime))
                    return 310;
                else return 0;
            }
        }
        return 303;
    }


    @RequestMapping(value = "/forecast/point", method = RequestMethod.POST)
    @ResponseBody
    public JSONObject forcastPoint(@RequestBody JSONArray array, HttpServletRequest request) {
        long tt = System.currentTimeMillis();
        JSONObject resJSON = new JSONObject();
        String uid = request.getParameter("uuid");
        if(uid==null){
            String username = request.getParameter("user");
            String token = request.getParameter("token");
            int code = checkToken(username, token);
            if (code > 0) {
                resJSON.put("code", code);
                return resJSON;
            }
        }
        //保留对老版id的保存
        if (uidCache.containsKey(uid) && uidCache.get(uid) > 0)
            uidCache.put(uid, uidCache.get(uid) - 1);
        else {
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
                Object datestr = requestJson.get("time");
                if (datestr != null) {
                    DateTimeFormatter dateformat = DateTimeFormat.forPattern("yyyyMMddHH");
                    try {
                        dateTime = dateformat.parseDateTime(datestr.toString());
                    } catch (Exception e1) {
                        logger.error("输入时间参数不正确");
                    }
                }
                if (!strLat.matches("\\d+?.?(\\d+)?") && strLon.matches("\\d+?.?(\\d+)?")) {
                    dataJSON.put(String.valueOf(id), "参数错误，经纬度范围为15~55,80~130");
                    logger.error("参数输入：北纬" + strLat + " 东经" + strLon + "参数错误，经纬度范围为15~55N,80~130E");
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
            CacheDataFrame frame = CacheDataFrame.getInstance();
            JSONObject dataJson = new JSONObject();
            dataJson.put("used memory", frame.getMemUsed() / 1024. / 1024. + "MB");
            dataJson.put("total memory", frame.getMemTotal() / 1024. / 1024. + "MB");
            dataJson.put("Avail memory", frame.getMemAvail() / 1024. / 1024. + "MB");
            dataJson.put("keys", frame.getKeys());
            resJSON.put("data", dataJson);
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
