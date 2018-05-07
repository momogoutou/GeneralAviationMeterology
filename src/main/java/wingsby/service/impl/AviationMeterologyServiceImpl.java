package wingsby.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import wingsby.common.CacheDataFrame;
import wingsby.common.tools.ConstantVar;
import wingsby.common.LagrangeInterpolation;
import wingsby.common.tools.GFSDateTimeTools;
import wingsby.common.tools.MeteologicalTools;
import wingsby.dao.AviationMeterologyDao;
import wingsby.parsegrib.ElementName;
import wingsby.parsegrib.Grib2dat;
import wingsby.service.AviationMeterologyService;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * Created by wingsby on 2018/3/20.
 * + point
 * ++ hour
 * +++ lev
 */
@Service
public class AviationMeterologyServiceImpl implements AviationMeterologyService {
    @Autowired
    private AviationMeterologyDao dao;

    @Override
    public JSONObject getRecentPredictPoint(float lat, float lon, DateTime useDate, JSONArray heights) {
        JSONObject pointJson = new JSONObject();
        for (int i = 0; i < 5; i++) {
            DateTime curTime = useDate.plusHours(i);
            try {
                JSONObject hourJson = hourForcast(lat, lon, curTime, heights);
                if (hourJson != null) {
                    pointJson.put(String.valueOf(curTime.getHourOfDay()), hourJson);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (pointJson.isEmpty()) throw new ResourceAccessException("数据查询失败");
        return pointJson;
    }

    private JSONObject hourForcast(float lat, float lon, DateTime useDate, JSONArray heights) {
        //起报时间的json
        JSONObject resJson = new JSONObject();
        String timeVTI = GFSDateTimeTools.getGFSDateTimeVTI(useDate, 0);
        List<ElementName> alleles = ElementName.isobaricVal();
        Map<String, List<Float>> map = new HashMap<>();
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
        boolean wswdFlag = false;
        for (ElementName eles : alleles) {
            try {
                List<Float> vals = new ArrayList<>();
                List<Float> hhs = new ArrayList<>();
                if (eles.equals(ElementName.WD) ||
                        eles.equals(ElementName.WS)) {
                    if (!wswdFlag)
                        WSWDhighInterPolation(heights, timeVTI, lat, lon, map);
                    wswdFlag = true;
                    continue;
                }
                for (String lev : Grib2dat.getInstance().isobaric) {
                    float val = dao.getGFSPointData(timeVTI, lev, eles.name(), lat, lon);
                    float hh = dao.getGFSPointData(timeVTI, lev, ElementName.HGT.name(), lat, lon);
                    hhs.add(hh);
                    vals.add(val);
                }
                //添加地面层次
                //地面高空特定要素匹配
                //todo 将要素分类处理
                if (eles.equals(ElementName.VVEL) || eles.equals(ElementName.JB) || eles.equals(ElementName.DB)) {
                    vals.add(0f);
                } else {
                    vals.add(dao.getGFSPointData(timeVTI, "9999", ElementName.getSurfaceMatchHigh(eles), lat, lon));
                }
                hhs.add(0f);
                List<Float[]> formatData = LagrangeInterpolation.formatData(vals, hhs);
                float gh = dao.getGFSPointData(timeVTI, "9999", ElementName.HGT.name(), lat, lon);
                if (formatData != null && formatData.size() > 0) {
                    List<Float> idata = calculateInterpolationData(heights, formatData, gh, lat, lon);
                    map.put(eles.name(), idata);
                }
            } catch (Exception e) {
//                e.printStackTrace();
            }
        }
        //地面
        JSONObject surfaceJson = new JSONObject();
        List<ElementName> surfaceElesSets = ElementName.surfaceVal();
        for (ElementName surfaceEles : surfaceElesSets) {
            try {
                if (surfaceEles.geteName().contains("weather")) {
                    String wstr = dao.getWeatherComment(timeVTI, "9999", surfaceEles.name(), lat, lon);
                    surfaceJson.put(surfaceEles.name(), wstr);
                } else {
                    float val = dao.getGFSPointData(timeVTI, "9999", surfaceEles.name(), lat, lon);
                    surfaceJson.put(surfaceEles.name(), val);
                }
            } catch (Exception e1) {
//                e1.printStackTrace();
            }
        }
        //添加天气情况
        //todo 临时时间
        DateTime time = new DateTime(Calendar.getInstance()).plusHours(24);
        surfaceJson.put("WW", getWeatherFromXinhong(time,
                lat, lon));
        int k = 0;
        //高空
        for (Object hhstr : heights) {
            JSONObject levJson = new JSONObject();
            for (String key : map.keySet()) {
                levJson.put(key, map.get(key).get(k));
            }
            k++;
            resJson.put(String.valueOf(hhstr), levJson);
        }
        resJson.put("0", surfaceJson);
        return resJson;
    }

    private String getWeatherFromXinhong(DateTime dateTime, float lat, float lon) {
        String url = "http://weather.xinhong.net/stationdata_cityfc/datafromlatlng?lat="
                + lat + "&lng=" + lon + "&elem=WW";
        String s = null;
        try {
            while (s == null) {
                URL localURL = new URL(url);
                URLConnection connection = localURL.openConnection();
                HttpURLConnection httpURLConnection = (HttpURLConnection) connection;
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setRequestMethod("GET");
                httpURLConnection.setRequestProperty("Content-type", "application/json;charset=UTF-8");
//                httpURLConnection.setRequestProperty("Accept", "application/json");
//            httpURLConnection.setR("Accept-Charset", "UTF-8");
                connection.connect();
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        connection.getInputStream(), "UTF-8"));
                s = in.readLine();
                in.close();
                httpURLConnection.disconnect();
            }
            JSONObject jsonObject = JSON.parseObject(s);
            Object obj = jsonObject.get("data");
            Object tobj = jsonObject.get("time");
            if (tobj == null) return null;
            String rtime = (String) jsonObject.get("time");
            DateTimeFormatter dateformat = DateTimeFormat.forPattern("yyyyMMddHHmmss");
            DateTime runtime = dateformat.parseDateTime(rtime);
            int difhour = Math.round((dateTime.getMillis() -
                    runtime.getMillis()) / 3600000.f);
            String res = null;
            if (obj != null) {
                if (obj instanceof JSONObject) {
                    if (difhour <= 48)
                        res = (String) ((JSONObject) obj).get(
                                String.valueOf((int) Math.ceil(difhour / 3.) * 3));
                    else if (difhour <= 96)
                        res = (String) ((JSONObject) obj).get(String.valueOf((int) Math.ceil(difhour / 6.) * 6));
                    else if (difhour <= 168)
                        res = (String) ((JSONObject) obj).get(String.valueOf((int) Math.ceil(difhour / 12.) * 12));
                    return res;
                }
            }
        } catch (Exception e) {

        }
        return null;
    }

    public static void main(String[] args) {
        DateTime dateTime = new DateTime(2018, 04, 27, 6, 0);
        DateTime time = new DateTime(Calendar.getInstance()).plusHours(24);
        System.out.println(new AviationMeterologyServiceImpl().
                getWeatherFromXinhong(time, 39.6f, 116f));
    }


    private void WSWDhighInterPolation(JSONArray heights, String timeVTI, float lat, float lon,
                                       Map<String, List<Float>> map) {
        List<Float> us = new ArrayList<>();
        List<Float> vs = new ArrayList<>();
        List<Float> hhs = new ArrayList<>();
        for (String lev : Grib2dat.getInstance().isobaric) {
            float ws = dao.getGFSPointData(timeVTI, lev, ElementName.WS.name(),
                    lat, lon);
            float wd = dao.getGFSPointData(timeVTI, lev, ElementName.WS.name(),
                    lat, lon);
            double[] uv = MeteologicalTools.WSWD2UV(ws, wd);
            if (uv != null) {
                us.add((float) uv[1]);
                vs.add((float) uv[0]);
                float hh = dao.getGFSPointData(timeVTI, lev, ElementName.HGT.name(), lat, lon);
                hhs.add(hh);
            }
        }
        float wss = dao.getGFSPointData(timeVTI, "9999", ElementName.WSS.name(), lat, lon);
        float wds = dao.getGFSPointData(timeVTI, "9999", ElementName.WDS.name(), lat, lon);
        double[] uv = MeteologicalTools.WSWD2UV(wss, wds);
        if (uv != null) {
            us.add((float) uv[1]);
            vs.add((float) uv[0]);
        }
        hhs.add(0f);
        List<Float[]> formatU = LagrangeInterpolation.formatData(us, hhs);
        List<Float[]> formatV = LagrangeInterpolation.formatData(vs, hhs);
        float gh = dao.getGFSPointData(timeVTI, "9999", ElementName.HGT.name(), lat, lon);
        List<Float> interU = calculateInterpolationData(heights, formatU, gh, lat, lon);
        List<Float> interV = calculateInterpolationData(heights, formatV, gh, lat, lon);
        List<Float> interWS = new ArrayList<>();
        List<Float> interWD = new ArrayList<>();
        for (int i = 0; i < interU.size(); i++) {
            double[] wswd = MeteologicalTools.UV2WSWD(interU.get(i), interV.get(i));
            if (wswd != null) {
                interWS.add((float) wswd[0]);
                interWD.add((float) wswd[1]);
            }
        }
        map.put(ElementName.WS.name(), interWS);
        map.put(ElementName.WD.name(), interWD);
    }

    private List<Float> calculateInterpolationData(JSONArray heights, List<Float[]> formatData,
                                                   float gh, float lat, float lon) {
        List<Float> res = new ArrayList<>();
        for (Object hhtmp : heights) {
            String[] str = String.valueOf(hhtmp).split("-");
            float ch = ConstantVar.NullValF;
            if (str != null && str.length == 2) {
                //标高及真高
                if (str[1].equals(1)) {
                    ch = Float.valueOf(str[0]);
                    //获取海拔高度
                    ch += gh / MeteologicalTools.LocalGravity(lat, lon, (float) (gh / 9.8));
                } else {
                    ch = Float.valueOf(str[0]);
                }
                float interpolationData = LagrangeInterpolation.interpolation3(formatData, ch);
                //保留三位小数
                res.add(Math.round(interpolationData * 1000) / 1000f);
            }
        }
        return res;
    }


}


