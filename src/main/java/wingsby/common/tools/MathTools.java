package wingsby.common.tools;

/**
 * Created by wingsby on 2018/4/10.
 */
public class MathTools {

    public static boolean isFloatEqual(float a,float b){
        return Math.abs(a-b)<1e-8?true:false;
    }

    public static boolean isDoubleEqual(double a,double b){
        return Math.abs(a-b)<1e-12?true:false;
    }


    public static float max(Float[] data){
        float max=Float.MIN_VALUE;
        for(float f:data)
            max=f>max?f:max;
        return max;
    }

    public static float min(Float[] data){
        float min=Float.MAX_VALUE;
        for(float f:data)
            min=f<min?f:min;
        return min;
    }

}
