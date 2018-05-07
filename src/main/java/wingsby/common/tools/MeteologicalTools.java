package wingsby.common.tools;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by wingsby on 2018/4/11.
 */
public class MeteologicalTools {

    public static double LocalGravity(float lat, float lon, float h) {
        //依据中国气象常用表
        double g0 = 9.80665 * (1 - 0.00265 * Math.cos(2 * lat * 180 / 3.1415926));
        double gh = g0 * (1 - 1.96 * 1e-7 * h);
        return gh;
    }

    public static double[] UV2WSWD(double u, double v) {
        double ws = Math.sqrt(u * u + v * v);
        double wd = ConstantVar.NullValF;
        if (MathTools.isDoubleEqual(u, 0.)) {
            if (MathTools.isDoubleEqual(v, 0.)) wd = 0.;
            else {
                if (v > 0) wd = 360.;
                if (v < 0) wd = 180.;
            }
        } else {
            if (u > 0)
                wd = 270. - Math.atan(v / u) * 180. / Math.PI;
            if (u < 0)
                wd = 90. - Math.atan(v / u) * 180. / Math.PI;
        }
//        if(wd<0){
//            System.out.println();
//        }
        return new double[]{ws, wd};
    }

    public static double[] WSWD2UV(double ws, double wd) {
        double seta = 0.;
        if (MathTools.isDoubleEqual(wd, 0.)) return new double[]{0., 0.};
        if ((wd < 180 && wd > 0) || MathTools.isDoubleEqual(wd, 360.))
            seta = wd + 180.;
        else if ((wd < 360 && wd >= 180))
            seta = wd - 180.;
        double u = ws * Math.sin(seta * Math.PI / 180.);
        double v = ws * Math.cos(seta * Math.PI / 180.);
        return new double[]{u, v};
    }


    public static void main(String[] args) {
//        double[] wswd=UV2WSWD(0,0);
//        double[] uv=WSWD2UV(wswd[0],wswd[1]);
//        System.out.println(uv[0]+" "+uv[1]);
//        wswd=UV2WSWD(-2,-5);
//        uv=WSWD2UV(wswd[0],wswd[1]);
//        System.out.println(wswd[0]+" "+wswd[1]);
//        System.out.println(uv[0]+" "+uv[1]);
//        System.out.println("=======================");
//        wswd=UV2WSWD(2,-5);
//        uv=WSWD2UV(wswd[0],wswd[1]);
//        System.out.println(wswd[0]+" "+wswd[1]);
//        System.out.println(uv[0]+" "+uv[1]);
//        System.out.println("=======================");
//        wswd=UV2WSWD(-2,2);
//        uv=WSWD2UV(wswd[0],wswd[1]);
//        System.out.println(wswd[0]+" "+wswd[1]);
//        System.out.println(uv[0]+" "+uv[1]);
//        System.out.println("=======================");
//
//        wswd=UV2WSWD(-2,0);
//        uv=WSWD2UV(wswd[0],wswd[1]);
//        System.out.println(wswd[0]+" "+wswd[1]);
//        System.out.println(uv[0]+" "+uv[1]);
//        System.out.println("=======================");
//
//        wswd=UV2WSWD(2,0);
//        uv=WSWD2UV(wswd[0],wswd[1]);
//        System.out.println(wswd[0]+" "+wswd[1]);
//        System.out.println(uv[0]+" "+uv[1]);
//        System.out.println("=======================");
//
//        wswd=UV2WSWD(0,2);
//        uv=WSWD2UV(wswd[0],wswd[1]);
//        System.out.println(uv[0]+" "+uv[1]);
//
//        wswd=UV2WSWD(0,-2);
//        uv=WSWD2UV(wswd[0],wswd[1]);
//        System.out.println(wswd[0]+" "+wswd[1]);
//        System.out.println(uv[0]+" "+uv[1]);
//        System.out.println("=======================");

        Date date=new Date(2018,03,15,12,0);
        SimpleDateFormat df = new SimpleDateFormat("YY-MM-dd-HH");
        String str=df.format(date);

        DateTimeFormatter formatter= DateTimeFormat.forPattern("YY-MM-dd-HH");
        DateTime dateTime=new DateTime(2018,04,15,12,0);
        System.out.println(dateTime.toString("YY-MM-dd-HH"));
        String path="12@YYMMDD@SDF";
        String replace="@YYMMDD@";
        path = path.replace(replace, dateTime.toString("YY-MM-dd-HH"));
        System.out.println(path);
        System.out.println("zsadfasdfaf000".matches(".+?3"));

    }

}
