package wingsby.service;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.JedisCluster;
import wingsby.common.ElemCityFC;
import wingsby.common.tools.GFSDateTimeTools;
import wingsby.stationFC.StationInfoSurfBeanCP;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StationDataFCService {
    private static final Log logger = LogFactory.getLog(StationDataFCService.class);
    private static final String NAMESPACE = "wingsby.stationFC";
    @Autowired
    private SqlSessionTemplate sqlSession;

    @Autowired
    private JedisCluster jedisCluster;

    final static public String STATIONDATA_CITYFC_PREFIX = "stationdata:cityfc";

    public StationInfoSurfBeanCP getNearstStaionFromLatLng(float lat, float lng, String dataType, float delta) {
        if (delta > 10)
            delta = 10;
        else if (delta < 0.01)
            delta = 0.01f;
        Map<String, Object> params = new HashMap<>();
        params.put("slat", lat);
        params.put("slng", lng);
        //默认查询半径0.8度范围站点
        params.put("deltax", delta);
        params.put("deltay", delta);
        params.put("dataType", dataType);
        List<StationInfoSurfBeanCP> staionInfoList = this.sqlSession.selectList(this.NAMESPACE + ".selectNearestbyLatLng", params);
        if (staionInfoList == null || staionInfoList.isEmpty())
            return null;
        return staionInfoList.get(0);
    }


    //    站点 预报时间 世界时存储 不取日数据
    public JSONObject getDataByStationCode(String stationCode, DateTime time) {
        //计算起报时间        延迟时间相差1小时
//        DateTime time1=time.plusHours(3);//CZFC需要向后推3时间(CZFZ来的比较早,比起报时间早3小时来)??
        String strtime = GFSDateTimeTools.getGFSDateTime(time, -1);
        String field = null;
        JSONObject jsonObject = new JSONObject();
        JSONObject dataObject=new JSONObject();
        for (ElemCityFC elem : ElemCityFC.values()) {
            if (elem.isDay()) continue;
            field = stationCode + "_" + strtime + "_" + elem;
            String strData = jedisCluster.hget(STATIONDATA_CITYFC_PREFIX + ":" + strtime.substring(0, 8), field);
            if (strData != null) dataObject.put(elem.getEname(), strData);
        }
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("yyyyMMddHH");
        if (dataObject.isEmpty()) {
            //往前推12小时
            DateTime pretime = DateTime.parse(strtime,dateTimeFormatter).minusHours(12);
            String strpre = pretime.toString("yyyyMMddHH");
            for (ElemCityFC elem : ElemCityFC.values()) {
                field = stationCode + "_" + pretime + "_" + elem;
                String strData = jedisCluster.hget(STATIONDATA_CITYFC_PREFIX + ":" + strpre.substring(0, 8), field);
                if (strData != null) dataObject.put(elem.getEname(), strData);
            }
            jsonObject.put("runtime", strpre);
        } else {
            jsonObject.put("data",dataObject);
            jsonObject.put("runtime", strtime);
        }
        return jsonObject;
    }
}
