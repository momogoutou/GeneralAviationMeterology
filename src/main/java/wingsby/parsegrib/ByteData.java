package wingsby.parsegrib;
/**
 * Created by desktop13 on 2017/12/20.
 */

import wingsby.common.tools.ConstantVar;

import java.util.ArrayList;
import java.util.List;

/**
 * 字节转换为浮点
 * @param字节数组 至少4位
 * @param开始位置
 * @return
 */
public class ByteData {
    public static final float NULLVAL = -9999f;// 表示NULL值
    public static final int[] shortLimit={-32768,32767};
    //天气条件参数
    public static final String[] rainName=Config.getRainName();      //kg/m2*s-1等效于mm
    public static final String[] rainNum=Config.getRainLevel();
    public static final String[] snowName=Config.getSnowName();
    public static final String[] snowNum=Config.getSnowLevel();
    private static final String freezingRainName= Config.getFreezingRainName();
    private static final String icePelletsName= Config.getIcePelletsName();
    private static List<String> WEATHER_CONDITION_NAME = loadConditionName();

    public ByteData() {
        loadConditionName();
    }

    public static float byteToFloat(byte[] b,int index){
        if(index < 0 || (index + 4) > b.length)
            return NULLVAL;
        int l;
        l = b[index + 0];
        l &= 0xff;
        l |= ((long)b[index + 1] << 8);
        l &= 0xffff;
        l |= ((long)b[index + 2] << 16);
        l &= 0xffffff;
        l |= ((long)b[index + 3] << 24);
        return Float.intBitsToFloat(l);
    }
    public static byte[] float2byte(float f) {
        // 把float转换为byte[]
        int fbit = Float.floatToIntBits(f);
        byte[] b = new byte[4];
        for (int i = 0; i < 4; i++) {
            b[i] = (byte) (fbit >> (24 - i * 8));
        }
        int len = b.length;
        byte[] dest = new byte[len];
        System.arraycopy(b, 0, dest, 0, len);
        byte temp;
        for (int i = 0; i < len / 2; ++i) {
            temp = dest[i];
            dest[i] = dest[len - i - 1];
            dest[len - i - 1] = temp;
        }
        return dest;
    }
    //为了保证精度，将数据全部转化为short，注意不能直接转,公式：(short)((data0-offset)*scale)
// 做法：offset:max-(max+min)/2; scale:32767/(max-min)/2
    //要求数据在-32768~32767的short
    public static float[] getOffsetScale(float mindata,float maxdata){
        float[] offsetScale= new float[2];
        offsetScale[0] = maxdata-( maxdata-mindata)/2;
        offsetScale[1] =( ( maxdata-mindata)/2>1)?(float) shortLimit[1]/(maxdata-mindata)/2 : (float) shortLimit[1]-1-offsetScale[0];
        return  offsetScale;
    }
    public static short[][] changeData(float[][][][] data,float offset,float scale){
        int[] latlon_start_num=ReadGrib.getInstance().getStartAndNum();
        int lat_num = latlon_start_num[2];
        int lon_num = latlon_start_num[3];
        short[][] element_data=new short[lat_num][lon_num];
        for (int i = 0; i < lat_num; i++) {
            for (int j = 0; j < lon_num; j++) {
                element_data[lat_num-1-i][j] = ByteData.float2short(data[0][0][i][j], offset, scale);
            }
        }

        return element_data;
    }
    public static short[][] changeData(float[][][] data,float offset,float scale){
        int[] latlon_start_num=ReadGrib.getInstance().getStartAndNum();
        int lat_num = latlon_start_num[2];
        int lon_num = latlon_start_num[3];
        short[][] element_data=new short[lat_num][lon_num];
        for (int i = 0; i < lat_num; i++) {
            for (int j = 0; j < lon_num; j++) {
                element_data[lat_num-1-i][j] = ByteData.float2short(data[0][i][j], offset, scale);
            }
        }

        return element_data;
    }
    ////////////////////////数据存储,对于float2short的nan数据，变成了0，后面交给short2float做
    //short没有nan，所以用-9999代替
    public static short float2short(float data0,float offset,float scale){
        if ( Float.isNaN(data0) || data0 ==ConstantVar.NullValF ) return (short) ConstantVar.NullValF;
        return (short)((data0-offset)*scale);
    }
    public static float short2float(short data0,float offset,float scale){
        if (data0 == ConstantVar.NullValF)return ConstantVar.NullValF;
        float res=data0/scale+offset;
        if(Math.abs(res- ConstantVar.NullValF)>1e-6){
            return res;
        }else{
            if(res>0||res==0||res<0);
            else{
                System.out.println("NAN"+data0+" "+scale+" "+offset);
                return ConstantVar.NANF;
            }
            return ConstantVar.NullValF;
        }

    }
    ///////////////////////天气情况-----（1~10整数）数据
    public static short string2short(String data){
//        loadConditionName();
        for (int i = 0; i < WEATHER_CONDITION_NAME.size(); i++) {
            if(data== WEATHER_CONDITION_NAME.get(i))return (short)(i+ 1);
            if(data=="降水数据异常")return 999;
        }
        return 0;
    }
    //////////////////////（1~10整数）数据-----天气情况
    public static String short2string(short data){
        for (int i = 0; i < WEATHER_CONDITION_NAME.size(); i++) {
            if(data== (i+1))return WEATHER_CONDITION_NAME.get(i);
        }
        return "无降水";
    }
//将数据转化成名称
    public static String float2string(short type, float num){
        if(type==0)return "无降水";
        if(type==1)return freezingRainName;
        if(type==2)return icePelletsName;
        if(type==3) return rank(num,rainNum,rainName);
        if(type==4)return rank(num,snowNum,snowName);
        return "降水数据异常";
    }
//判别分级(数据认为大于0.05才归入降水)
    public static String rank(float data ,String[] levelNums ,String[] levelNames){
        for (int i = 0; i < levelNums.length; i++) {
            if(data<= Float.parseFloat(levelNums[i]))return levelNames[i];
        }
        return levelNames[levelNames.length-1];
    }

    private static List<String> loadConditionName(){
        //顺序： 冻雨、冰丸、各种雨、各种雪
        if(WEATHER_CONDITION_NAME==null) {
            WEATHER_CONDITION_NAME=new ArrayList<>();
            WEATHER_CONDITION_NAME.add(freezingRainName);
            WEATHER_CONDITION_NAME.add(icePelletsName);
            for (String name : rainName) {
                WEATHER_CONDITION_NAME.add(name);
            }
            for (String name : snowName) {
                WEATHER_CONDITION_NAME.add(name);
            }
        }
        return WEATHER_CONDITION_NAME;
    }
}
