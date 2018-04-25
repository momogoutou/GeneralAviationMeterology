package wingsby.parsegrib;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2018/4/9.
 * 要素分类：直接输出要素 + 合成输出要素 + 外部传入
 */
public enum ElementName {
    //直接输出要素
    /*
    大气分层
     */
    HGT("Geopotential_height_isobaric", "位势高度", "isobaric", "gpm"),  //位势米
    RH("Relative_humidity_isobaric", "大气湿度", "isobaric", "%"),
    VVEL("Vertical_velocity_pressure_isobaric", "大气垂直风速", "isobaric", "m/s"),
    /*
    地面
     */
    VIS("Visibility_surface", "能见度", "surface", "m"),
    HGTS("Geopotential_height_surface", "地面位势", "surface", "gpm"),

    RHS("Relative_humidity_height_above_ground", "2m相对湿度", "surface", "%"),   //4维[1][1][721][1440]
    GUST("Wind_speed_gust_surface", "阵风", "surface", "m/s"),
//**************************************************************************************************************************

    //合成输出要素
    //使用雨、雪、冻雨、冰丸分类解析，分析标准：https://baike.baidu.com/item/%E9%99%8D%E6%B0%B4%E9%87%8F/850460
    WCS("weather_condition", "降水天气", "surface"),  //
    /*
    几个天气条件判断天气
     */
    CFR("Categorical_Freezing_Rain_surface_2_Hour_Average", "冻雨", "weather_condition"), //其中的数字2表示2h，这个值会变
    CIP("Categorical_Ice_Pellets_surface_2_Hour_Average", "冰丸", "weather_condition"),
    CR("Categorical_Rain_surface_2_Hour_Average", "雨", "weather_condition"),
    CS("Categorical_Snow_surface_2_Hour_Average", "雪", "weather_condition"),
    TR("Total_precipitation_surface_2_Hour_Accumulation", "降水量", "weather_condition", "kg.m-2"),

    //uv变成speed和direction
    WS("wind_speed", "水平风速", "isobaric", "m/s"),
    WD("wind_direction", "水平风向", "isobaric", "°"),
    /*
    风速风向
     */
    U("u-component_of_wind_isobaric", "大气水平风U", "wind_isobaric", "m/s"),
    V("v-component_of_wind_isobaric", "大气水平风V", "wind_isobaric", "m/s"),

    //地面uv变成speed和direction
    WSS("wind_speed_surface", "水平风速", "surface", "m/s"),
    WDS("wind_direction_surface", "水平风向", "surface", "°"),
    /*
    风速风向
     */
    VS("v-component_of_wind_height_above_ground", "地面风V", "wind_surface", "m/s"),   //4维度，[1][3][721][1440],3维度只取10m的
    US("u-component_of_wind_height_above_ground", "地面风U", "wind_surface", "m/s"),   //4维度，[1][3][721][1440],3维度只取10m的

    //大气温度
    TMP("Temperature_isobaric_C", "大气温度", "isobaric", "°C"),   //单位需要转化k到度
    /*
    大气温度
     */
    TMP0("Temperature_isobaric", "大气温度", "temperature_isobaric", "K"),

    //地面温度
    TMPS("Temperature_surface_C", "地面温度", "surface", "°C"),  //单位需要转化k到度
    /*
    地面温度
     */
    TMPS0("Temperature_surface", "地面温度", "temperature_surface", "K"),

    //本站海压
    MSL("Pressure_reduced_to_MSL_msl_C", "本站海压", "surface", "Pa"),
    /*
    本站海压
     */
    MSL0("Pressure_reduced_to_MSL_msl", "本站海压", "MSL_msl", "Pa"),
    //本站气压=地面气压
    PRES("Pressure_surface_C", "本站气压", "surface", "Pa"),
    PRES0("Pressure_surface", "本站气压", "PRES_surface", "Pa"),

//***********************************************************************************************************************

    //外部传入
    JB("JB", "积冰", "isobaric"),
    DB("DB", "颠簸", "isobaric"),;

    private String eName;//英文描述
    private String type;
    private String cName;//中文描述
    private String units;//单位

    /**
     * 私有构造,防止被外部调用
     *
     * @param eName
     */
    private ElementName(String eName, String cName, String type, String units) {
        this.eName = eName;
        this.cName = cName;
        this.type = type;
        this.units = units;
    }

    private ElementName(String eName, String cName, String type) {
        this.eName = eName;
        this.cName = cName;
        this.type = type;
    }

    /**
     * 定义方法,返回描述,跟常规类的定义没区别
     *
     * @return
     */
    public String geteName() {
        return eName;
    }

    public String getType() {
        return type;
    }

    public String getcName() {
        return cName;
    }

    public String getUnit() {
        return units;
    }

    //    @Test
    public static void main(String[] args) {
        for (ElementName day : ElementName.values()) {
            System.out.println("name:" + day.name() +
                    ",desc:" + day.geteName());
            System.out.println(ElementName.HGT.geteName());
        }
    }

    /**
     * 输出结果:
     * name:MONDAY,desc:星期一
     */

    public static List<ElementName> surfaceVal() {
        List<ElementName> res = new ArrayList<>();
        for (ElementName ele : ElementName.values()) {
            if (ele.type.equals("surface")) {
                res.add(ele);
            }
        }
        return res;
    }

    public static List<ElementName> isobaricVal() {
        List<ElementName> res = new ArrayList<>();
        for (ElementName ele : ElementName.values()) {
            if (ele.type.equals("isobaric")) {
                res.add(ele);
            }
        }
        return res;
    }

    public static String getSurfaceMatchHigh(ElementName eles) {
        List<ElementName> res = new ArrayList<>();
        for (ElementName ele : ElementName.values()) {
            if (ele.type.equals("surface") &&
                    ele.name().contains(eles.name())) {
                return ele.name();
            }
        }
        return null;
    }
}
