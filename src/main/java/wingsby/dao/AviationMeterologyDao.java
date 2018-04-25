package wingsby.dao;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by wingsby on 2018/3/20.
 */
public interface AviationMeterologyDao {
    float getGFSPointData(
            String TimeVti,
            String lev,
            String eles,
            float lat,
            float lon
    );

    String getWeatherComment(String TimeVti,
                             String lev,
                             String eles,
                             float lat,
                             float lon);

}
