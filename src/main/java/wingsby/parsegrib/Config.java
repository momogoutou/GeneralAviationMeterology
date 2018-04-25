package wingsby.parsegrib;

import java.awt.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Properties;

/**
 * Created by Administrator on 2018/4/13.
 */
public class Config {
//readGrib.properties    配置文件信息
    static String inPath;
    static String inPath_JBDB;
    static String outPath;
    static String fileStyle;
    public static String[] isobaric;
    public static String[] isobaric_JBDB;
    public static String[] latlonArea;
    public static String[] fTime;
    static   String[] START_PARSE_TIME;
    static  float DTIME;
//weather.properties    配置文件
    static String weather_freezing_rain;
    static String weather_ice_pellets;
    static String weather_rain;
    static String weather_snow;
    static String weather_precipitation;
    static String[] rainLevel;
    static String[] rainName;
    static String[] snowLevel;
    static String[] snowName;
    static String freezingRainName;
    static String icePelletsName;
    static ArrayList<ElementName> elementNames_WCS = new ArrayList<>();
    static ArrayList<ElementName> elementNames_WS = new ArrayList<>();
    static ArrayList<ElementName> elementNames_WD = new ArrayList<>();
    static ArrayList<ElementName> elementNames_WSS = new ArrayList<>();
    static ArrayList<ElementName> elementNames_WDS = new ArrayList<>();
    static ArrayList<ElementName> elementNames_TMP = new ArrayList<>();
    static ArrayList<ElementName> elementNames_TMPS = new ArrayList<>();
    static ArrayList<ArrayList<ElementName>> elementNames;

    public Config() {
    }

    static {
        loadAll();
    }

    private static void loadReadGrib(){
        //通过类加载器加载配置文件
        Properties properties=new Properties();
        InputStream in = null;
        try {
            String springPath=Grib2dat.class.getClassLoader().getResource("").getPath()+"config/readGrib.properties";
            in = new BufferedInputStream(new FileInputStream(springPath));
//            in = new BufferedInputStream(new FileInputStream(System.getProperty("user.dir")+"/config/readGrib.properties"));
            properties.load(in);
            inPath=properties.getProperty("inPath");
            inPath_JBDB=properties.getProperty("inPath_JBDB");
            outPath=properties.getProperty("outPath");
            fileStyle=properties.getProperty("fileStyle");

            String isobaric_temp=properties.getProperty("isobaric").replace("\"","");
            isobaric=isobaric_temp.split(",");
            isobaric_JBDB=properties.getProperty("isobaric_JBDB").split(",");

            latlonArea=properties.getProperty("latlonArea").split(",");
            fTime=properties.getProperty("fTime").split(",");
            START_PARSE_TIME=properties.getProperty("START_PARSE_TIME").split(",");
            DTIME = Float.parseFloat(properties.getProperty("DTIME"));
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    private static void loadWeather(){
        //通过类加载器加载配置文件
        Properties properties=new Properties();
        InputStream in = null;
        try {
            String springPath=Grib2dat.class.getClassLoader().getResource("").getPath()+"config/weather.properties";
            in = new BufferedInputStream(new FileInputStream(springPath));
//            in = new BufferedInputStream(new FileInputStream(System.getProperty("user.dir")+"/config/weather.properties"));
            properties.load(in);
            weather_freezing_rain=properties.getProperty("weather_freezing_rain");
            weather_ice_pellets=properties.getProperty("weather_ice_pellets");
            weather_rain=properties.getProperty("weather_rain");
            weather_snow=properties.getProperty("weather_snow");
            weather_precipitation=properties.getProperty("weather_precipitation");

            icePelletsName=properties.getProperty("icePelletsName");
            freezingRainName=properties.getProperty("freezingRainName");

            rainLevel=properties.getProperty("rainLevel").split(",");
            rainName=properties.getProperty("rainName").split(",");
            snowLevel=properties.getProperty("snowLevel").split(",");
            snowName=properties.getProperty("snowName").split(",");

            loadSpecialElements();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void loadAll(){
        loadReadGrib();
        loadWeather();
    }

    private static void loadSpecialElements(){
        System.out.println();
        if(elementNames==null) {
            elementNames = new ArrayList<>();
            elementNames.add(elementNames_WCS);
            elementNames.add(elementNames_WS);
            elementNames.add(elementNames_WD);
            elementNames.add(elementNames_WSS);
            elementNames.add(elementNames_WDS);
            elementNames.add(elementNames_TMP);
            elementNames.add(elementNames_TMPS);
            ArrayList<String> elementNames_type = new ArrayList<>();
            elementNames_type.add("weather_condition");
            elementNames_type.add("wind_isobaric");
            elementNames_type.add("wind_isobaric");
            elementNames_type.add("wind_surface");
            elementNames_type.add("wind_surface");
            elementNames_type.add("temperature_isobaric");
            elementNames_type.add("temperature_surface");
            for (int i=0 ; i< elementNames.size() ; i++) {
                if (elementNames.get(i).isEmpty()) {
                    for (ElementName e : ElementName.values()) {
                        if (e.getType() == elementNames_type.get(i)) elementNames.get(i).add(e);
                    }
                }
            }
        }
    }
    public static String getInPath() {
        return inPath;
    }

    public static String getOutPath() {
        return outPath;
    }

    public static String getFileStyle() {
        return fileStyle;
    }

    public static String[] getIsobaric() {
        return isobaric;
    }

    public static String[] getLatlonArea() {
        return latlonArea;
    }

    public static String[] getfTime() {
        return fTime;
    }

    public static String[] getStartParseTime() {
        return START_PARSE_TIME;
    }

    public static float getDTIME() {
        return DTIME;
    }

    public static void setInPath(String inPath) {
        Config.inPath = inPath;
    }

    public static void setOutPath(String outPath) {
        Config.outPath = outPath;
    }

    public static void setFileStyle(String fileStyle) {
        Config.fileStyle = fileStyle;
    }

    public static void setIsobaric(String[] isobaric) {
        Config.isobaric = isobaric;
    }

    public static void setLatlonArea(String[] latlonArea) {
        Config.latlonArea = latlonArea;
    }

    public static void setfTime(String[] fTime) {
        Config.fTime = fTime;
    }

    public static void setStartParseTime(String[] startParseTime) {
        START_PARSE_TIME = startParseTime;
    }

    public static void setDTIME(float DTIME) {
        Config.DTIME = DTIME;
    }

    public static String getWeather_freezing_rain() {
        return weather_freezing_rain;
    }

    public static String getWeather_ice_pellets() {
        return weather_ice_pellets;
    }

    public static String getWeather_rain() {
        return weather_rain;
    }

    public static String getWeather_snow() {
        return weather_snow;
    }

    public static String getWeather_precipitation() {
        return weather_precipitation;
    }

    public static String[] getRainLevel() {
        return rainLevel;
    }

    public static String[] getRainName() {
        return rainName;
    }

    public static String[] getSnowLevel() {
        return snowLevel;
    }

    public static String[] getSnowName() {
        return snowName;
    }

    public static void setWeather_freezing_rain(String weather_freezing_rain) {
        Config.weather_freezing_rain = weather_freezing_rain;
    }

    public static void setWeather_ice_pellets(String weather_ice_pellets) {
        Config.weather_ice_pellets = weather_ice_pellets;
    }

    public static void setWeather_rain(String weather_rain) {
        Config.weather_rain = weather_rain;
    }

    public static void setWeather_snow(String weather_snow) {
        Config.weather_snow = weather_snow;
    }

    public static void setWeather_precipitation(String weather_precipitation) {
        Config.weather_precipitation = weather_precipitation;
    }

    public static void setRainLevel(String[] rainLevel) {
        Config.rainLevel = rainLevel;
    }

    public static void setRainName(String[] rainName) {
        Config.rainName = rainName;
    }

    public static void setSnowLevel(String[] snowLevel) {
        Config.snowLevel = snowLevel;
    }

    public static void setSnowName(String[] snowName) {
        Config.snowName = snowName;
    }

    public static String getFreezingRainName() {
        return freezingRainName;
    }

    public static String getIcePelletsName() {
        return icePelletsName;
    }

    public static void setFreezingRainName(String freezingRainName) {
        Config.freezingRainName = freezingRainName;
    }

    public static void setIcePelletsName(String icePelletsName) {
        Config.icePelletsName = icePelletsName;
    }

    public static ArrayList<ElementName> getElementNames_WCS() {
        return elementNames_WCS;
    }

    public static void setElementNames_WCS(ArrayList<ElementName> elementNames_WCS) {
        Config.elementNames_WCS = elementNames_WCS;
    }

    public static ArrayList<ElementName> getElementNames_WS() {
        return elementNames_WS;
    }

    public static void setElementNames_WS(ArrayList<ElementName> elementNames_WS) {
        Config.elementNames_WS = elementNames_WS;
    }

    public static ArrayList<ElementName> getElementNames_WD() {
        return elementNames_WD;
    }

    public static void setElementNames_WD(ArrayList<ElementName> elementNames_WD) {
        Config.elementNames_WD = elementNames_WD;
    }

    public static ArrayList<ElementName> getElementNames_WSS() {
        return elementNames_WSS;
    }

    public static void setElementNames_WSS(ArrayList<ElementName> elementNames_WSS) {
        Config.elementNames_WSS = elementNames_WSS;
    }

    public static ArrayList<ElementName> getElementNames_WDS() {
        return elementNames_WDS;
    }

    public static void setElementNames_WDS(ArrayList<ElementName> elementNames_WDS) {
        Config.elementNames_WDS = elementNames_WDS;
    }

    public static ArrayList<ElementName> getElementNames_TMP() {
        return elementNames_TMP;
    }

    public static void setElementNames_TMP(ArrayList<ElementName> elementNames_TMP) {
        Config.elementNames_TMP = elementNames_TMP;
    }

    public static ArrayList<ElementName> getElementNames_TMPS() {
        return elementNames_TMPS;
    }

    public static void setElementNames_TMPS(ArrayList<ElementName> elementNames_TMPS) {
        Config.elementNames_TMPS = elementNames_TMPS;
    }

    public static String getInPath_JBDB() {
        return inPath_JBDB;
    }

    public static void setInPath_JBDB(String inPath_JBDB) {
        Config.inPath_JBDB = inPath_JBDB;
    }

    public static String[] getIsobaric_JBDB() {
        return isobaric_JBDB;
    }

    public static void setIsobaric_JBDB(String[] isobaric_JBDB) {
        Config.isobaric_JBDB = isobaric_JBDB;
    }
}
