package wingsby.parsegrib;

import ucar.ma2.InvalidRangeException;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import wingsby.common.ConstantVar;
import wingsby.common.GFSMem;
import wingsby.common.tools.MeteologicalTools;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by desktop13 on 2017/12/20.
 * 功能：四维数据[time,isobaric,lat,lon]，将后三维输出为一个文件，其中，isobaric需要按照配置文件readGrib筛选
 * 三维数据[time lat lon]，将后两纬输出为一个文件
 * <p>
 * 遇到问题：要求数据分为地面和多层高空，遇到地面有多维（一般3维，特殊4维），高空维度不统一问题，
 * 理想解决办法：读出某要素的维数和名称，再进行解析；目前的做法，人为地先确定好维度，进行解析
 * <p>
 * 读取分类：几种非直接输出的要素+其他直接读取的要素
 */
public class ReadGrib {
    private static final DecimalFormat decimalFormat = new DecimalFormat("0000");  //用于格式化数据的类
    float width;
    float height;
    float slat;
    float elat;
    float slon;
    float elon;
    float resolution;

    float mindata;
    float maxdata;
    float offset;
    float scale;
    short[][] element_data = null;
    int lat_num;
    int lon_num;
    GFSMem gfsMem;

    List<ElementName> elementNames_WCS = Config.elementNames_WCS;
    List<ElementName> elementNames_WS = Config.elementNames_WS;
    List<ElementName> elementNames_WD = Config.elementNames_WD;
    List<ElementName> elementNames_WSS = Config.elementNames_WSS;
    List<ElementName> elementNames_WDS = Config.elementNames_WDS;
    List<ElementName> elementNames_TMP = Config.elementNames_TMP;
    List<ElementName> elementNames_TMPS = Config.elementNames_TMPS;

    public static ReadGrib instance;

    private ReadGrib() {

    }

    public static ReadGrib getInstance() {
        if (instance == null) {
            synchronized (ReadGrib.class) {
                if (instance == null) {
                    instance = new ReadGrib();
                }
            }
        }
        return instance;
    }

    /**
     * @param fileName      路径
     * @param elementName   元素
     * @param isobaricName0 气压层 isobaric="0700","0750","0800","0850","0900","0925","0950","0975","1000"
     * @return
     */
    public GFSMem readGrib(String fileName, ElementName elementName, String isobaricName0) {
        NetcdfFile ncfile = null;
        try {
            String isobaricName;
            ncfile = NetcdfDataset.open(fileName, null);
            //可能用到的一维数据
            Variable time = ncfile.findVariable("time");
            Variable isobaric = ncfile.findVariable("isobaric");
            double[] myTime = null;
            float[] myIsobaric = null;
            if (time != null) {
                myTime = (double[]) time.read().copyTo1DJavaArray();
            }
            if (isobaric != null) {
                myIsobaric = (float[]) isobaric.read().copyTo1DJavaArray();
            }
            //*****************************************
            int[] latlon_start_num = getStartAndNum();
            lat_num = latlon_start_num[2];
            lon_num = latlon_start_num[3];
            element_data = new short[lat_num][lon_num];   //要输出的数据结果
/*
    几种合成的要素
    **************************************************************
*/
            //天气判断(冻雨、冰丸、雨、雪 )，尽可能使用了float，输出：0~999的short型格点,0表示无降水，999表示数据异常，正常数据从一开始
            if (elementName.geteName() == ElementName.WCS.geteName()) {
                setGFSMem_WCS(fileName);
                return gfsMem;
            }
            //uv变成speed和direction
            if (elementName.geteName() == ElementName.WS.geteName()) {
                setGFSMem_WS(fileName, isobaricName0);
                return gfsMem;
            }
            if (elementName.geteName() == ElementName.WD.geteName()) {
                setGFSMem_WD(fileName, isobaricName0);
                return gfsMem;
            }
            //地面uv变成speed和direction
            if (elementName.geteName() == ElementName.WSS.geteName()) {
                setGFSMem_WSS(fileName);
                return gfsMem;
            }
            if (elementName.geteName() == ElementName.WDS.geteName()) {
                setGFSMem_WDS(fileName);
                return gfsMem;
            }
            //大气温度
            if (elementName.geteName() == ElementName.TMP.geteName()) {
                setGFSMem_TMP(fileName, isobaricName0);
                return gfsMem;
            }
            //地面温度
            if (elementName.geteName() == ElementName.TMPS.geteName()) {
                setGFSMem_TMPS(fileName);
                return gfsMem;
            }
            //本站海压
            if (elementName.geteName() == ElementName.MSL.geteName()) {
                setGFSMem_MSL(fileName);
                return gfsMem;
            }
            //本站气压
            if (elementName.geteName() == ElementName.PRES.geteName()) {
                setGFSMem_PRES(fileName);
                return gfsMem;
            }
/*
    直接输出的要素
    **************************************************************
*/
            int hour = Integer.parseInt(fileName.substring(fileName.length() - 3));//读取非天气条件要素
            Variable element = getVariable(ncfile, elementName.geteName(), hour);
            if (element == null) {   //要素有的文件有，有的没
                pack_false();
                return gfsMem;
            }
            int var_size = element.getDimensions().size();  //得到元素的  纬度个数
            //************************四维数据循环（认为是 time isobaric lat lon）[element][isobaric][lat][lon]
            if (var_size == 4) {
                isobaricName = changeIsobaricName(isobaricName0);
                String myIsobaricString = null;
                if (elementName.geteName() == ElementName.VVEL.geteName()) {   //对垂直风的气压层不同于其他气压层的情况
                    float[] myIsobaric_temp = getIsobaricFloat(getIsobaricName(element), ncfile);
                    for (float f : myIsobaric_temp) {
                        myIsobaricString = myIsobaricString + "," + decimalFormat.format(f);
                    }
                } else {
                    for (float f : myIsobaric) {
                        myIsobaricString = myIsobaricString + "," + decimalFormat.format(f);
                    }
                }
                for (int time_i = 0; time_i < myTime.length; time_i++) {
                    //输出路径设置
                    try {
                        String myIsobaricStringSub = myIsobaricString.substring(5, myIsobaricString.length());
                        if (!myIsobaricStringSub.contains(isobaricName)) {
                            pack_false();
                            return gfsMem;
                        }
                        int isobaric_iii = 0;
                        for (String string : myIsobaricStringSub.split(",")) {
                            if (string.equals(isobaricName)) break;
                            isobaric_iii++;
                        }
                        //对4纬数据的读取,由于需要筛选第二纬度的数据，因此采用此读法
                        int[] org = {time_i, isobaric_iii, latlon_start_num[0], latlon_start_num[1]};           //起点
                        int[] sha = {1, 1, latlon_start_num[2], latlon_start_num[3]};             //个数
                        float[][][][] data1 = (float[][][][]) element.read(org, sha).copyToNDJavaArray();//用于获取数组
                        setOffsetScale(data1);
                        element_data = ByteData.changeData(data1, offset, scale);//数据转化short
                        pack();
                    } catch (InvalidRangeException e) {
                        e.printStackTrace();
                    }
                }
            }
            //************************ 3维数据循环（认为是 time lat lon）
            if (var_size == 3) {
                String myIsobaricString = null;
                for (float f : myIsobaric) {
                    myIsobaricString = myIsobaricString + "," + decimalFormat.format(f);
                }
                for (int time_i = 0; time_i < myTime.length; time_i++) {
                    //输出路径设置
                    try {
                        int[] org = {time_i, latlon_start_num[0], latlon_start_num[1]};           //起点
                        int[] sha = {1, latlon_start_num[2], latlon_start_num[3]};             //个数
                        float[][][] data1 = (float[][][]) element.read(org, sha).copyToNDJavaArray();//用于获取数组
                        setOffsetScale(data1);
                        element_data = ByteData.changeData(data1, offset, scale);//数据转化short
                        pack();
                    } catch (InvalidRangeException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                ncfile.close();
                ncfile.empty();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return gfsMem;
    }

    //遇到问题：大文件无头信息，按照小文件去读取；头（lon0 lat0 lon1 lat1 resolution）
    public GFSMem readDat(String fileName0, ElementName elementName, String isobaricName0) {
        RandomAccessFile rf = null;
        String fileName = getFileName(fileName0, elementName, isobaricName0);
        File file = new File(fileName);
//        System.out.println( "正在解析：：："+fileName);/////////////////////////////////////////////
        if (!file.exists()) {
            pack_false();
            return gfsMem;
        }
        try {
            rf = new RandomAccessFile(file, "r");
            //读取头
            ByteBuffer byteBuffer = ByteBuffer.allocate(5 * 1 * 4);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            rf.read(byteBuffer.array());
            float value;
            int ii = 0;
            String[] latlon_name = new String[]{"lon1", "lat1", "lon2", "lat2", "resolution"};
            Map<String, Float> latlon_dat = new HashMap<>();
            for (int i = 0; i < 5; i++) {
                latlon_dat.put(latlon_name[i], byteBuffer.getFloat());
            }
            //用两个矩形取小矩形
            int lat_num_dat = (int) ((latlon_dat.get(latlon_name[3]) - latlon_dat.get(latlon_name[1])) / latlon_dat.get(latlon_name[4]));
            int lon_num_dat = (int) ((latlon_dat.get(latlon_name[2]) - latlon_dat.get(latlon_name[0])) / latlon_dat.get(latlon_name[4]));
            int[] latlon_start_num = getStartAndNum();
            lat_num = latlon_start_num[2];
            lon_num = latlon_start_num[3];
            int blank_left = (int) Math.abs((Float.parseFloat(Config.getLatlonArea()[1]) - latlon_dat.get(latlon_name[0])) / latlon_dat.get(latlon_name[4]));
            int blank_right = (int) Math.abs((Float.parseFloat(Config.getLatlonArea()[3]) - latlon_dat.get(latlon_name[2])) / latlon_dat.get(latlon_name[4]));
            //读取数据
            int blank_down = (int) (Float.parseFloat(Config.getLatlonArea()[0]) - latlon_dat.get(latlon_name[1]) / latlon_dat.get(latlon_name[4]));
            int blank_temp = (int) (Float.parseFloat(Config.getLatlonArea()[0]) - latlon_dat.get(latlon_name[1]) / latlon_dat.get(latlon_name[4]) * lon_num_dat);
            rf.seek(4 * (5 + blank_temp));  //跳过下面空缺
            byteBuffer = ByteBuffer.allocate(lat_num_dat * lon_num_dat * 1 * 4);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            rf.read(byteBuffer.array());
            float[][][] element_data0 = new float[1][lat_num][lon_num];
            mark:
            for (int i = 0; i < lat_num_dat; i++) {
                for (int j = 0; j < lon_num_dat; j++) {
                    float value_temp = byteBuffer.getFloat();
                    if (j >= blank_left && j < lon_num_dat - blank_right && i <= lat_num) {
                        element_data0[0][i][j - blank_left] = value_temp;
                    }
                    if (i >= lat_num) break mark;
                }
            }
            setOffsetScale(element_data0);
            element_data = ByteData.changeData(element_data0, offset, scale);//数据转化short
            pack();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (rf != null)
                    rf.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return gfsMem;
    }

    public String getFileName(String fileName0, ElementName elementName, String isobaricName0) {
        String name1 = null;
        String name2 = null;
        String name = null;

        Pattern pattern = Pattern.compile("(?<=\\_DB).*?(?=\\.)");
        Matcher matcher = pattern.matcher(fileName0);
        while (matcher.find()) {
            name2 = matcher.group();
        }
        pattern = Pattern.compile("(?<=X).*?(?<=\\_DB)");
        matcher = pattern.matcher(fileName0);
        while (matcher.find()) {
            name1 = matcher.group();
        }
        pattern = Pattern.compile("(?<=\\_JB).*?(?=\\.)");
        matcher = pattern.matcher(fileName0);
        while (matcher.find()) {
            name2 = matcher.group();
        }
        pattern = Pattern.compile("(?<=X).*?(?<=\\_JB)");
        matcher = pattern.matcher(fileName0);
        while (matcher.find()) {
            name1 = matcher.group();
        }

        name = "X" + name1 + "_" + isobaricName0 + name2 + ".dat";
        String s = fileName0.replace(new File(fileName0).getName(), name);
//        String s = fileName0.replace( new File(fileName0).getName(),name ).replace( "/","\\\\");

        return s;
    }

    //输出文件夹路径（由于文件流的时候，如果没有文件，会自动创建文件，所以，只需要保证对应文件夹存在就好）
    public static void judeDirExists(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            if (file.isDirectory()) {
            } else {
            }
        } else {
            file.mkdirs();
        }
    }

    //jar包要求：读取开始值，以及读取的个数    -90~90,-180~180
    public int[] getStartAndNum() {
        int[] start_num = new int[4];
        float d = Float.parseFloat(Grib2dat.latlonArea[4]);
        start_num[0] = (int) ((Float.parseFloat(Grib2dat.latlonArea[0]) + 90) / d);
        start_num[1] = (int) ((Float.parseFloat(Grib2dat.latlonArea[1]) + 180) / d);
        start_num[2] = (int) ((Float.parseFloat(Grib2dat.latlonArea[2]) + 90) / d) - start_num[0];
        start_num[3] = (int) ((Float.parseFloat(Grib2dat.latlonArea[3]) + 180) / d) - start_num[1];
        return start_num;
    }

    //排序，取前后1%的最大最小，结果加减方差，超出结论最值数据后期考虑剔除
    private float[] getMinMax(float[][][][] data) {
        float sum = 0;
        double sn = 0;
        float average = 0;
        float standardDeviation = 0;
        float[] minMax = new float[2];
        int data_num = data[0][0].length * data[0][0][0].length;
        float[] data1 = new float[data_num];
        for (int i = 0; i < data[0][0].length; i++) {
            for (int ii = 0; ii < data[0][0][0].length; ii++) {
                sum = sum + data[0][0][i][ii];
                data1[i * data[0][0][0].length + ii] = data[0][0][i][ii];
            }
        }
        Arrays.sort(data1);  //排序
        average = sum / data_num;
        for (int i = 0; i < data[0][0].length; i++) {
            for (int ii = 0; ii < data[0][0][0].length; ii++) {
                sn = sn + Math.pow((data[0][0][i][ii] - average), 2);
            }
        }
        standardDeviation = (float) Math.sqrt(sn / data_num);  //计算方差
        minMax[0] = data1[(int) (data_num * 0.01)] - standardDeviation;
        minMax[1] = data1[(int) (data_num * 0.99)] + standardDeviation;
        return minMax;
    }

    private float[] getMinMax(float[][][] data) {
        float sum = 0;
        double sn = 0;
        float average = 0;
        float standardDeviation = 0;
        float[] minMax = new float[2];
        int data_num = data[0].length * data[0][0].length;
        float[] data1 = new float[data_num];
        for (int i = 0; i < data[0].length; i++) {
            for (int ii = 0; ii < data[0][0].length; ii++) {
                sum = sum + data[0][i][ii];
                data1[i * data[0][0].length + ii] = data[0][i][ii];
            }
        }
        Arrays.sort(data1);  //排序
        average = sum / data_num;
        for (int i = 0; i < data[0].length; i++) {
            for (int ii = 0; ii < data[0][0].length; ii++) {
                sn = sn + Math.pow((data[0][i][ii] - average), 2);
            }
        }
        standardDeviation = (float) Math.sqrt(sn / data_num);  //计算方差
        minMax[0] = data1[(int) (data_num * 0.01)] - standardDeviation;
        minMax[1] = data1[(int) (data_num * 0.99)] + standardDeviation;
        return minMax;
    }

    private String changeIsobaricName(String isobaricName0) {
        if (isobaricName0 == "surface") return "0100";     //针对surface层出现4维数据情况
        else if (Integer.parseInt(isobaricName0) <= 1000)
            return decimalFormat.format(Integer.parseInt(isobaricName0) * 100);
        else
            return Integer.toString(Integer.parseInt(isobaricName0) * 100);
    }

    //封装GFSMen
    public void pack() {
        gfsMem = new GFSMem(System.currentTimeMillis(), offset, scale, element_data);
        setWidth(lon_num);
        setHeight(lat_num);
        setSlat(Float.parseFloat(Grib2dat.latlonArea[0]));
        setSlon(Float.parseFloat(Grib2dat.latlonArea[1]));
        setElat(Float.parseFloat(Grib2dat.latlonArea[2]));
        setElon(Float.parseFloat(Grib2dat.latlonArea[3]));
        setResolution(Float.parseFloat(Grib2dat.latlonArea[4]));
        gfsMem.setElat(ReadGrib.getInstance().getElat());
        gfsMem.setSlat(ReadGrib.getInstance().getSlat());
        gfsMem.setElon(ReadGrib.getInstance().getElon());
        gfsMem.setSlon(ReadGrib.getInstance().getSlon());
        gfsMem.setResolution(ReadGrib.getInstance().getResolution());
        gfsMem.setHeight((int) ReadGrib.getInstance().getHeight());
        gfsMem.setWidth((int) ReadGrib.getInstance().getWidth());
    }

    private void pack_false() {
        element_data = new short[lat_num][lon_num];
        System.out.println("        要素不存在，数据置为-9999");
        for (int i = 0; i < element_data.length; i++) {
            for (int j = 0; j < element_data[0].length; j++) {
                element_data[i][j] = ConstantVar.NullValSI;
            }
        }
        setOffset(0);
        setScale(1);
        pack();
    }

    //获取对应气压层
    private String getIsobaricName(Variable element) {
        String isobaricName = element.getDimension(1).toString().split("=")[0].trim();
        return isobaricName;
    }

    private float[] getIsobaricFloat(String isobaricName, NetcdfFile ncfile) {
        float[] myIsobaric_temp = null;
        Variable isobaric = ncfile.findVariable(isobaricName);
        if (isobaric != null) {
            try {
                myIsobaric_temp = (float[]) isobaric.read().copyTo1DJavaArray();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return myIsobaric_temp;
    }

    //匹配名称中的整数，用于替换（原因是要素的名称会改变）
    private Variable getVariable(NetcdfFile ncfile, String eName, int realHour0) {
        Variable element = null;
        String elementRealName = null;
        String realHour = (realHour0 % 6 > 0) ? Integer.toString(realHour0 % 6) : "6";
        // 按指定模式在字符串查找
        String line = eName;
        String pattern = "(\\D*)(\\d+)(.*)";
        Pattern r = Pattern.compile(pattern);// 创建 Pattern 对象
        // 现在创建 matcher 对象
        Matcher m = r.matcher(line);
        if (m.find()) {
            elementRealName = line.replace(m.group(2), realHour);
            element = ncfile.findVariable(elementRealName);
        } else {
//            System.out.println("NO MATCH");
            element = ncfile.findVariable(eName);
        }
        return element;
    }

    private void setGFSMem_WCS(String fileName) {
        short[][] weather_condition_type = new short[lat_num][lon_num];      //类型， 由于这部分数据用于标记，因此不需要数据转化
        float[][] weather_condition_num = new float[lat_num][lon_num];        //大小
        for (int i = 0; i < elementNames_WCS.size(); i++) {        ///////////////////////////////////////////
            GFSMem gfsMen_temp = ReadGrib.getInstance().readGrib(fileName, elementNames_WCS.get(i), "surface");
            if (elementNames_WCS.get(i).geteName() == ElementName.TR.geteName()) {  //降水量
                for (int j = 0; j < lat_num; j++) {
                    for (int k = 0; k < lon_num; k++) {
                        weather_condition_num[j][k] = ByteData.short2float(gfsMen_temp.getData()[j][k], gfsMen_temp.getOffset(), gfsMen_temp.getScale());
                    }
                }
                break;
            }
            for (int j = 0; j < lat_num; j++) {   //其他天气条件
                for (int k = 0; k < lon_num; k++) {
                    float type_temp = ByteData.short2float(gfsMen_temp.getData()[j][k], gfsMen_temp.getOffset(), gfsMen_temp.getScale());
                    if (type_temp > 0.1 && type_temp < 9990)
                        weather_condition_type[j][k] = (short) (i + 1);
                }
            }
        }
        for (int i = 0; i < lat_num; i++) {
            for (int j = 0; j < lon_num; j++) {
                element_data[i][j] = ByteData.string2short(ByteData.float2string(weather_condition_type[i][j], weather_condition_num[i][j]));
            }
        }
        setOffset(0);//对gfsmen类的设置
        setScale(1);
        pack();
    }

    private void setGFSMem_WS(String fileName, String isobaricName0) {
        GFSMem gfsMen_u = ReadGrib.getInstance().readGrib(fileName, ElementName.U, isobaricName0);
        GFSMem gfsMen_v = ReadGrib.getInstance().readGrib(fileName, ElementName.V, isobaricName0);
        float[][][] data1 = new float[1][lat_num][lon_num];
        for (int i = 0; i < lat_num; i++) {
            for (int j = 0; j < lon_num; j++) {
                float u = ByteData.short2float(gfsMen_u.getData()[i][j], gfsMen_u.getOffset(), gfsMen_u.getScale());
                float v = ByteData.short2float(gfsMen_v.getData()[i][j], gfsMen_v.getOffset(), gfsMen_v.getScale());
//                data1[0][i][j] = (float) Math.pow(Math.pow(u, 2) + Math.pow(v, 2), 0.5);
                double[] wswd = MeteologicalTools.UV2WSWD(u, v);
                if (wswd != null)
                    data1[0][i][j] = (float) (wswd[0]);
            }
        }
        setOffsetScale(data1);
        element_data = ByteData.changeData(data1, offset, scale);//数据转化short
        pack();
    }

    private void setGFSMem_WD(String fileName, String isobaricName0) {
        GFSMem gfsMen_u = ReadGrib.getInstance().readGrib(fileName, ElementName.U, isobaricName0);
        GFSMem gfsMen_v = ReadGrib.getInstance().readGrib(fileName, ElementName.V, isobaricName0);
        float[][][] data1 = new float[1][lat_num][lon_num];
        for (int i = 0; i < lat_num; i++) {
            for (int j = 0; j < lon_num; j++) {
                float u = ByteData.short2float(gfsMen_u.getData()[i][j], gfsMen_u.getOffset(), gfsMen_u.getScale());
                float v = ByteData.short2float(gfsMen_v.getData()[i][j], gfsMen_v.getOffset(), gfsMen_v.getScale());
//                data1[0][i][j]= (float) (180+ Math.atan2(u,v)*180/Math.PI);
                double[] wswd = MeteologicalTools.UV2WSWD(u, v);
                if (wswd != null)
                    data1[0][i][j] = (float) (wswd[1]);
            }
        }
        setOffsetScale(data1);
        element_data = ByteData.changeData(data1, offset, scale);//数据转化short
        pack();
    }

    private void setGFSMem_WSS(String fileName) {
        GFSMem gfsMen_u = ReadGrib.getInstance().readGrib(fileName, ElementName.US, "surface");
        GFSMem gfsMen_v = ReadGrib.getInstance().readGrib(fileName, ElementName.VS, "surface");
        float[][][] data1 = new float[1][lat_num][lon_num];
        for (int i = 0; i < lat_num; i++) {
            for (int j = 0; j < lon_num; j++) {
                float u = ByteData.short2float(gfsMen_u.getData()[i][j], gfsMen_u.getOffset(), gfsMen_u.getScale());
                float v = ByteData.short2float(gfsMen_v.getData()[i][j], gfsMen_v.getOffset(), gfsMen_v.getScale());
                double[] wswd = MeteologicalTools.UV2WSWD(u, v);
                if (wswd != null)
                    data1[0][i][j] = (float) (wswd[0]);
            }
        }
        setOffsetScale(data1);
        element_data = ByteData.changeData(data1, offset, scale);//数据转化short
        pack();
    }

    private void setGFSMem_WDS(String fileName) {
        GFSMem gfsMen_u = ReadGrib.getInstance().readGrib(fileName, ElementName.US, "surface");
        GFSMem gfsMen_v = ReadGrib.getInstance().readGrib(fileName, ElementName.VS, "surface");
        float[][][] data1 = new float[1][lat_num][lon_num];
        for (int i = 0; i < lat_num; i++) {
            for (int j = 0; j < lon_num; j++) {
                float u = ByteData.short2float(gfsMen_u.getData()[i][j], gfsMen_u.getOffset(), gfsMen_u.getScale());
                float v = ByteData.short2float(gfsMen_v.getData()[i][j], gfsMen_v.getOffset(), gfsMen_v.getScale());
                double[] wswd = MeteologicalTools.UV2WSWD(u, v);
                if (wswd != null)
                    data1[0][i][j] = (float) (wswd[1]);
            }
        }
        setOffsetScale(data1);
        element_data = ByteData.changeData(data1, offset, scale);//数据转化short
        pack();
    }

    private void setGFSMem_TMP(String fileName, String isobaricName0) {
        GFSMem gfsMen_t = ReadGrib.getInstance().readGrib(fileName, ElementName.TMP0, isobaricName0);
        float[][][] data1 = new float[1][lat_num][lon_num];
        for (int i = 0; i < lat_num; i++) {
            for (int j = 0; j < lon_num; j++) {
                float t = ByteData.short2float(gfsMen_t.getData()[i][j], gfsMen_t.getOffset(), gfsMen_t.getScale());
                data1[0][i][j] = (float) (t - 273.15);
            }
        }
        setOffsetScale(data1);
        element_data = ByteData.changeData(data1, offset, scale);//数据转化short
        pack();
    }

    private void setGFSMem_TMPS(String fileName) {
        GFSMem gfsMen_t = ReadGrib.getInstance().readGrib(fileName, ElementName.TMPS0, "surface");
        float[][][] data1 = new float[1][lat_num][lon_num];
        for (int i = 0; i < lat_num; i++) {
            for (int j = 0; j < lon_num; j++) {
                float t = ByteData.short2float(gfsMen_t.getData()[i][j], gfsMen_t.getOffset(), gfsMen_t.getScale());
                data1[0][i][j] = (float) (t - 273.15);
            }
        }
        setOffsetScale(data1);
        element_data = ByteData.changeData(data1, offset, scale);//数据转化short
        pack();
    }
    private void setGFSMem_MSL(String fileName) {
        GFSMem gfsMen_t = ReadGrib.getInstance().readGrib(fileName, ElementName.MSL0, "surface");
        float[][][] data1 = new float[1][lat_num][lon_num];
        for (int i = 0; i < lat_num; i++) {
            for (int j = 0; j < lon_num; j++) {
                float p = ByteData.short2float(gfsMen_t.getData()[i][j], gfsMen_t.getOffset(), gfsMen_t.getScale());
                data1[0][i][j] = p/100;
            }
        }
        setOffsetScale(data1);
        element_data = ByteData.changeData(data1, offset, scale);//数据转化short
        pack();
    }
    private void setGFSMem_PRES(String fileName) {
        GFSMem gfsMen_t = ReadGrib.getInstance().readGrib(fileName, ElementName.PRES0, "surface");
        float[][][] data1 = new float[1][lat_num][lon_num];
        for (int i = 0; i < lat_num; i++) {
            for (int j = 0; j < lon_num; j++) {
                float p = ByteData.short2float(gfsMen_t.getData()[i][j], gfsMen_t.getOffset(), gfsMen_t.getScale());
                data1[0][i][j] = p/100;
            }
        }
        setOffsetScale(data1);
        element_data = ByteData.changeData(data1, offset, scale);//数据转化short
        pack();
    }
    private void setOffsetScale(float[][][] data1) {
        float[] minmax = getMinMax(data1);
        float[] offsetScale = ByteData.getOffsetScale(minmax[0], minmax[1]);
        float offset = offsetScale[0];
        float scale = offsetScale[1];
        setOffset(offset);//对gfsmen类的设置
        setScale(scale);
    }

    private void setOffsetScale(float[][][][] data1) {
        float[] minmax = getMinMax(data1);
        float[] offsetScale = ByteData.getOffsetScale(minmax[0], minmax[1]);
        float offset = offsetScale[0];
        float scale = offsetScale[1];
        setOffset(offset);//对gfsmen类的设置
        setScale(scale);
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setSlat(float slat) {
        this.slat = slat;
    }

    public void setElat(float elat) {
        this.elat = elat;
    }

    public void setSlon(float slon) {
        this.slon = slon;
    }

    public void setElon(float elon) {
        this.elon = elon;
    }

    public void setResolution(float resolution) {
        this.resolution = resolution;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public float getSlat() {
        return slat;
    }

    public float getElat() {
        return elat;
    }

    public float getSlon() {
        return slon;
    }

    public float getElon() {
        return elon;
    }

    public float getResolution() {
        return resolution;
    }

    public void setMindata(float mindata) {
        this.mindata = mindata;
    }

    public float getMindata() {
        return mindata;
    }

    public float getMaxdata() {
        return maxdata;
    }

    public void setMaxdata(float maxdata) {
        this.maxdata = maxdata;
    }

    public float getOffset() {
        return offset;
    }

    public float getScale() {
        return scale;
    }

    public void setOffset(float offset) {
        this.offset = offset;
    }

    public void setScale(float scale) {
        this.scale = scale;
    }

    public static DecimalFormat getDecimalFormat() {
        return decimalFormat;
    }

    public short[][] getElement_data() {
        return element_data;
    }

    public int getLat_num() {
        return lat_num;
    }

    public int getLon_num() {
        return lon_num;
    }

    public GFSMem getGfsMem() {
        return gfsMem;
    }

    public void setElement_data(short[][] element_data) {
        this.element_data = element_data;
    }

    public void setLat_num(int lat_num) {
        this.lat_num = lat_num;
    }

    public void setLon_num(int lon_num) {
        this.lon_num = lon_num;
    }

    public void setGfsMem(GFSMem gfsMem) {
        this.gfsMem = gfsMem;
    }
}
