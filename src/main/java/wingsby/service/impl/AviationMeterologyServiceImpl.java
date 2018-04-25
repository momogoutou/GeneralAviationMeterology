package wingsby.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import wingsby.common.CacheDataFrame;
import wingsby.common.ConstantVar;
import wingsby.common.LagrangeInterpolation;
import wingsby.common.tools.GFSDateTimeTools;
import wingsby.common.tools.MeteologicalTools;
import wingsby.dao.AviationMeterologyDao;
import wingsby.parsegrib.ElementName;
import wingsby.parsegrib.Grib2dat;
import wingsby.service.AviationMeterologyService;

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
            JSONObject hourJson = hourForcast(lat, lon, useDate, heights);
            if (hourJson != null) {
                pointJson.put(String.valueOf(curTime.getHourOfDay()), hourJson);
            }
        }
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
            if (eles.equals(ElementName.JB) || eles.equals(ElementName.DB)) ;
            else {
                if (eles.equals(ElementName.VVEL)) {
                    vals.add(0f);
                } else {
                    vals.add(dao.getGFSPointData(timeVTI, "9999", ElementName.getSurfaceMatchHigh(eles), lat, lon));
                }
                hhs.add(0f);
                List<Float[]> formatData = LagrangeInterpolation.formatData(vals, hhs);
                float gh = dao.getGFSPointData(timeVTI, "9999", ElementName.HGT.name(), lat, lon);
                try {
                    List<Float> idata = calculateInterpolationData(heights, formatData, gh, lat, lon);
                    map.put(eles.name(), idata);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        //地面
        JSONObject surfaceJson = new JSONObject();
        List<ElementName> surfaceElesSets = ElementName.surfaceVal();
        for (ElementName surfaceEles : surfaceElesSets) {
            if (surfaceEles.geteName().contains("weather")) {
                String wstr = dao.getWeatherComment(timeVTI, "9999", surfaceEles.name(), lat, lon);
                surfaceJson.put(surfaceEles.name(), wstr);
            } else {
                float val = dao.getGFSPointData(timeVTI, "9999", surfaceEles.name(), lat, lon);
                surfaceJson.put(surfaceEles.name(), val);
            }
        }

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


