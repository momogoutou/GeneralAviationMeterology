package wingsby.dao.impl;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Repository;
import wingsby.common.CacheDataFrame;
import wingsby.common.tools.ConstantVar;
import wingsby.common.GFSMem;
import wingsby.dao.AviationMeterologyDao;

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
        if(mem.getStrByLonLat(lon,lat).contains("雨"))logger.error("有降水"+key);
        return mem.getStrByLonLat(lon,lat);
    }


}
