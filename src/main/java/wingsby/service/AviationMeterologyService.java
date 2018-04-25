package wingsby.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.joda.time.DateTime;

import java.util.List;

/**
 * Created by wingsby on 2018/3/19.
 */
public interface AviationMeterologyService {
    JSONObject getRecentPredictPoint(float lat, float lon,DateTime dateTime,JSONArray hh);

}
