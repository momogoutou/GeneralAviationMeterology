package wingsby.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import sun.security.x509.Extension;
import wingsby.TimeMangerJob;
import wingsby.common.GFSTimeManger;
import wingsby.common.JSONUtil;
import wingsby.common.ResStatus;
import wingsby.parsegrib.Grib2dat;
import wingsby.service.AviationMeterologyService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Calendar;

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
        heights.add("500-2");
        heights.add("200-1");
        DateTime time = new DateTime(Calendar.getInstance().getTimeInMillis());
        JSONObject dataJSON = new JSONObject();
        try {
            JSONObject pointJson = iService.getRecentPredictPoint(40f, 120f,
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
        } catch (Exception e) {
            e.printStackTrace();
            resJSON.put("code", ResStatus.SEARCH_ERROR.getStatusCode());
        }
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
        return null;
    }


    @RequestMapping(value = "/forecast/point", method = RequestMethod.POST)
    @ResponseBody
    public JSONObject forcastPoint(@RequestBody JSONArray array) {
        long tt = System.currentTimeMillis();
        JSONObject resJSON = new JSONObject();
        DateTime dateTime = new DateTime(System.currentTimeMillis());
        try {
            JSONObject dataJSON = new JSONObject();
//            Object[]array=new Object[1];
            for (Object obj : array) {
                JSONObject requestJson = (JSONObject) obj;
                int id = (int) requestJson.get("id");
                float lat = Float.valueOf((String) requestJson.get("lat"));
                float lon = Float.valueOf((String) requestJson.get("lng"));
                JSONArray heights = (JSONArray) requestJson.get("heights");
                JSONObject pointJson = iService.getRecentPredictPoint(lat, lon, dateTime, heights);
                dataJSON.put(String.valueOf(id), pointJson);
            }
            logger.debug("查询耗时:" + (System.currentTimeMillis() - tt));
            resJSON.put("delay", (System.currentTimeMillis() - tt));
            resJSON.put("code", ResStatus.SUCCESSFUL.getStatusCode());
            resJSON.put("data", dataJSON);
//            JSONUtil.writeJSONToResponse(response, resJSON);
            return resJSON;
        } catch (Exception e) {
            logger.error("日志信息查询:" + e);
            resJSON.put("code", ResStatus.SEARCH_ERROR.getStatusCode());
//            JSONUtil.writeJSONToResponse(response, resJSON);
            return resJSON;
        }
    }

}
