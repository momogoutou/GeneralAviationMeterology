package wingsby.common;

import org.apache.log4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by wingsby on 2018/4/9.
 */
public class CacheDataFrame {

    private static final Logger logger = Logger.getLogger(CacheDataFrame.class);
    private final Map<String, CalculateMermory> map = new ConcurrentHashMap<>();
    private static long limit = 1024l * 1024 * 1024;
    private volatile long useMem = 0l;
    private float factor = 0.9f;
    private static int clearTimes = 10;
    private static int slotLimit = 20480;
    private static volatile CacheDataFrame instance;

    private CacheDataFrame() {
    }

    public static CacheDataFrame getInstance() {
        if (instance == null) {
            synchronized (CacheDataFrame.class) {
                if (instance == null) {
                    instance = new CacheDataFrame();
                }
            }
        }
        return instance;
    }


    public boolean pushData(CalculateMermory data, String key) {
        int cnt = 0;
        while ((nowSize() > factor * limit || map.size() >= slotLimit)
                && cnt < clearTimes) {
            for (String tmp : map.keySet()) {
                CalculateMermory mem = map.get(tmp);
                if (mem.isExceed()) {
                    map.remove(tmp);
                    synchronized (map) {
                        useMem -= mem.getMemSize();
                    }
                    mem = null;
                }
            }
            cnt++;
            System.gc();
        }
        if (cnt >= clearTimes) return false;
        else {
            if (data.getMemSize() > 1024) {
                map.put(key, data);
                addSize();
                return true;
            }
            logger.error("加载数据小于1k，不予缓存");
            return false;
        }
    }

    public CalculateMermory pullData(String key) {
        if (map.containsKey(key)) {
            return map.get(key);
        }
        return null;
    }

    public long getMemAvail() {
        return limit - nowSize();
    }

    public long getMemTotal() {
        return limit;
    }

    public long getMemUsed() {
        return nowSize();
    }


    private long nowSize() {
        if (useMem > 0) return useMem;
        long tmpSize = 0;
        for (String key : map.keySet()) {
            tmpSize += map.get(key).getMemSize();
        }
        useMem = tmpSize;
        return useMem;
    }

    private void addSize() {
        long tmpSize = 0;
        for (String key : map.keySet()) {
            tmpSize += map.get(key).getMemSize();
        }
        useMem = tmpSize;
//        return useMem;
    }

    public Set<String> getKeys() {
        return map.keySet();
    }


}
