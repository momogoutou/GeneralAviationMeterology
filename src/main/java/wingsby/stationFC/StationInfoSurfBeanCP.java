package wingsby.stationFC;

/**
 * Description: 地面常规气象站点信息<br>
 * Company: <a href=www.xinhong.net>新宏高科</a><br>
 *
 * @author 作者 <a href=mailto:liusofttech@sina.com>刘晓昌</a>
 * @version 创建时间：2016/3/7.
 * 拷贝过来的wingsby
 */
public class StationInfoSurfBeanCP {

    private Integer ID;
    private String stationCode;
    private Float lat;
    private Float lng;
    private String cname;
    private String ename;
    private String py;
    private String level;
    private String countryCode;
    private int types;
    private int typep;
    private int  typet;
    private String cityCode;

    public String getCityCode() {
        return cityCode;
    }
    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getStationCode() {
        return stationCode;
    }

    public void setStationCode(String stationCode) {
        this.stationCode = stationCode;
    }

    public Float getLat() {
        return lat;
    }

    public void setLat(Float lat) {
        this.lat = lat;
    }

    public Float getLng() {
        return lng;
    }

    public void setLng(Float lng) {
        this.lng = lng;
    }

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        this.cname = cname;
    }

    public String getEname() {
        return ename;
    }

    public void setEname(String ename) {
        this.ename = ename;
    }

    public String getPy() {
        return py;
    }

    public void setPy(String py) {
        this.py = py;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    @Override
    public String toString() {
        return "StationInfoSurfBeanCP{" +
                "stationCode='" + stationCode + '\'' +
                ", lat=" + lat +
                ", lng=" + lng +
                ", cname='" + cname + '\'' +
                ", ename='" + ename + '\'' +
                ", py='" + py + '\'' +
                ", level='" + level + '\'' +
                ", countryCode='" + countryCode + '\'' +
                '}';
    }

    public int getTypes() {
        return types;
    }

    public void setTypes(int types) {
        this.types = types;
    }

    public int getTypep() {
        return typep;
    }

    public void setTypep(int typep) {
        this.typep = typep;
    }

    public int getTypet() {
        return typet;
    }

    public void setTypet(int typet) {
        this.typet = typet;
    }

    public Integer getID() {
        return ID;
    }

    public void setID(Integer ID) {
        this.ID = ID;
    }
}
