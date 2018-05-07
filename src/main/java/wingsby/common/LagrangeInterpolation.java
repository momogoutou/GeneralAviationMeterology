package wingsby.common;

import wingsby.common.tools.ConstantVar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Created by wingsby on 2018/4/10.
 */
public class LagrangeInterpolation {

    //    要求hh从低到高、hh及vals无空值==先使用formatData函数处理
    public static float interpolation3(List<Float[]>input, float ch) {
        if(input==null)return ConstantVar.NullValF;
        Float[]y=input.get(0);
        Float[]x=input.get(1);
        //find nearest 3 points
        int[] idx = new int[3];
        for (int i = 0; i < x.length - 1; i++) {
            if (ch >= x[i] && ch <= x[i + 1]) {
                idx[0]=i;
                idx[1]=i+1;
                if (i > 0 && i < x.length - 2) {
                    if(Math.abs(ch - x[i - 1]) > Math.abs(ch - x[i + 2]))
                        idx[2]=i+2;
                    else idx[2]=i - 1;
                } else {
                    if (i == 0) idx[2]=i+2;
                    if (i == x.length - 2) idx[2]=i - 1;
                }
                break;
            }
        }
        // 开始插值
        float res = (ch - x[idx[1]]) * (ch - x[idx[2]]) / ((x[idx[0]] - x[idx[1]]) * (x[idx[0]] - x[idx[2]])) *
                y[idx[0]] + (ch - x[idx[0]]) * (ch - x[idx[2]]) / ((x[idx[1]] - x[idx[0]]) * (x[idx[1]] - x[idx[2]])) *
                y[idx[1]] + (ch - x[idx[0]]) * (ch - x[idx[1]]) / ((x[idx[2]] - x[idx[0]]) * (x[idx[2]] - x[idx[1]])) *
                y[idx[2]];

        return res;
    }

    public static List<Float[]> formatData(List<Float> vals, List<Float> hh) {
        if (vals == null || hh == null) return null;
        if (vals.size() != hh.size()) return null;
        List<Float[]> res = new ArrayList<>();
        for (int i = 0; i < vals.size(); i++) {
            if (Math.abs(vals.get(i) - ConstantVar.NullValF) < 1e-6 ||
                    Math.abs(hh.get(i) - ConstantVar.NullValF) < 1e-6) ;
            else {
                res.add(new Float[]{vals.get(i), hh.get(i)});
            }
        }
        //排序
        Collections.sort(res, new Comparator<Float[]>() {
            @Override
            public int compare(Float[] o1, Float[] o2) {
                //有问题的
                if (o1 == null || o2 == null) return -1;
                if (o1.length != 2 || o2.length != 2) return -1;
                if (o1[1] > o2[1]) return 1;
                else if (Math.abs(o1[1] - o2[1]) < 1e-6) return 0;
                else return -1;
            }
        });

        Float[]x=new Float[res.size()];
        Float[]y=new Float[res.size()];
        int i=0;
        for(Float[] f:res){
            x[i]=f[1];
            y[i]=f[0];
            i++;
        }
        List<Float[]>fres=new ArrayList<>();
        fres.add(y);
        fres.add(x);
        return fres;
    }
}
