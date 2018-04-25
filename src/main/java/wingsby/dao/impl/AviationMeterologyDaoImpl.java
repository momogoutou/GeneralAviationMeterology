package wingsby.dao.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.sun.corba.se.impl.orbutil.closure.Constant;
import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import wingsby.common.CacheDataFrame;
import wingsby.common.ConstantVar;
import wingsby.common.GFSMem;
import wingsby.dao.AviationMeterologyDao;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wingsby on 2018/3/20.
 */
@Repository
public class AviationMeterologyDaoImpl implements AviationMeterologyDao {
    Logger logger = Logger.getLogger(AviationMeterologyDaoImpl.class);


    @Override
    public float getGFSPointData(String TimeVti, String lev, String eles, float lat, float lon) {
        CacheDataFrame frame=CacheDataFrame.getInstance();
        String key=null;
        key=TimeVti+"_"+String.format("%04d",Integer.valueOf(lev))+"_"+eles;
        GFSMem mem= (GFSMem) frame.pullData(key);
        if(mem==null){
           return ConstantVar.NullValF;
        }
        return mem.getByLonLat(lon,lat);
    }

    @Override
    public String getWeatherComment(String TimeVti, String lev, String eles, float lat, float lon) {
        CacheDataFrame frame=CacheDataFrame.getInstance();
        String key=null;
        key=TimeVti+"_"+String.format("%04d",Integer.valueOf(lev))+"_"+eles;
        GFSMem mem= (GFSMem) frame.pullData(key);
        return mem.getStrByLonLat(lon,lat);
    }


}
