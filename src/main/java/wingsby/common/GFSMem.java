package wingsby.common;

import wingsby.common.tools.ConstantVar;
import wingsby.parsegrib.ByteData;

/**
 * Created by wingsby on 2018/4/9.
 */
public class GFSMem implements CalculateMermory{

    long startTime=0l;

    long excced=3600l*24*1000l;

    float offset=0;
    float scale=1;

    short[][] data;
    int width;
    int height;
    float slat;
    float elat;
    float slon;
    float elon;
    float resolution;

    String key;
    String unit;



    public GFSMem(long startTime, float offset, float scale, short[][] data) {
        this.startTime = startTime;
        this.offset = offset;
        this.scale = scale;
        this.data = data;
    }

    @Override
    public long getMemSize() {
        if(data==null)
            return 0;
        else{
            // header
            int headersz=32+8;
            if(key!=null)headersz+=key.length()+30;
            return headersz+width*8+Short.BYTES*width*height;
        }
    }

    @Override
    public boolean isExceed() {
        return (System.currentTimeMillis()-startTime)>excced?true:false;
    }


    public short[][] getData(){
        return data;
    }


    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public float getSlat() {
        return slat;
    }

    public void setSlat(float slat) {
        this.slat = slat;
    }

    public float getElat() {
        return elat;
    }

    public void setElat(float elat) {
        this.elat = elat;
    }

    public float getSlon() {
        return slon;
    }

    public void setSlon(float slon) {
        this.slon = slon;
    }

    public float getElon() {
        return elon;
    }

    public void setElon(float elon) {
        this.elon = elon;
    }

    public float getResolution() {
        return resolution;
    }

    public void setResolution(float resolution) {
        this.resolution = resolution;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public float getOffset() {
        return offset;
    }

    public void setOffset(float offset) {
        this.offset = offset;
    }

    public float getScale() {
        return scale;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public void setData(short[][] data) {
        this.data = data;
    }

//    //通过经纬度、要素获取数据，返回高度及数据
    public float getByLonLat(float lon,float lat){
        int yidx= (int) (((lat+90)/getResolution()-(getSlat()+90)/getResolution()));
        int xidx= (int) ((lon/getResolution()-getSlon()/getResolution()));
        if(xidx<width&&yidx<height&&xidx>=0&&yidx>=0){
            if(data!=null){
                short t=data[yidx][xidx];
                float res=ByteData.short2float(t,offset,scale);
                return res;
            }
        }
        return ConstantVar.NullValF;
    }

    public String getStrByLonLat(float lon,float lat){
        int yidx= (int) ((lat/getResolution()-getSlat()/getResolution()));
        int xidx= (int) ((lon/getResolution()-getSlon()/getResolution()));
        if(xidx<width&&yidx<height&&xidx>=0&&yidx>=0){
            if(data!=null){
                short t=data[yidx][xidx];
                String res= ByteData.short2string(t);
                return res;
            }
        }
        return null;
    }
}
