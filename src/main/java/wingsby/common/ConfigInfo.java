package wingsby.common;

import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component("configInfo")

public class ConfigInfo {
    @Resource
    private Map<String, String> properties;

    @Resource
    private Map<String, String> gribProperties;



    public String getLogPath(String type) {
        Set<String> keys = properties.keySet();
        for (String str : keys) {
            if (str.contains(type) && str.contains(".log"))
                return properties.get(str);
        }
        return null;
    }

}