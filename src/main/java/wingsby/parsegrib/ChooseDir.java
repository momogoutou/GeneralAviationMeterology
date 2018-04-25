package wingsby.parsegrib;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by desktop13 on 2017/12/22.
 * function：文件夹和固定结尾文件  目录读取
 */


public class ChooseDir {
    public ChooseDir() {
    }
    private static final DecimalFormat decimalFormat = new DecimalFormat("000");
    //文件夹和固定结尾文件,或者是包含某个标记的后缀  目录读取
    public static List<String> getFileList(String inPath,String fileStyle ){
        File dir=new File(inPath);
        List<String> fileNames = new ArrayList<>();
        if(!dir.exists()){
            System.out.println("读取路径不存在");
            return null;
        }
        //*****
        if(dir.isDirectory()) {         //文件夹部分
            File[] files = dir.listFiles();
            int condition=0;
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    String fileName = files[i].getName();
                    if (files[i].isDirectory()) {
                        fileNames.add(files[i].toString());
//                        getFileList(inPath,fileStyle);
                    } else if (fileName.endsWith(fileStyle)||fileName.substring(fileName.length()-4).contains(fileStyle)) {
                        String strFileName = files[i].getAbsolutePath();
                        fileNames.add(strFileName);
                        condition=1;
                    }else {

                    }
                }
                if(condition!=1){
//                    System.out.println("文件夹中没有符合要求的文件");
                }
            }
        }else {        //文件部分
            String fileName = dir.getName();
            if (fileName.endsWith(fileStyle)||fileName.substring(fileName.length()-4).contains(fileStyle)) {
                String strFileName = dir.getAbsolutePath();
                fileNames.add(strFileName);
            }else {
                System.out.println("不是符合后缀的文件");
            }
        }
        return fileNames;
    }
    //只读取文件，固定结尾文件,或者是包含某个标记的后缀  目录读取
    public static List<String> getFileListOnly(String inPath,String fileStyle ){
        File dir=new File(inPath);
        List<String> fileNames = new ArrayList<>();
        if(!dir.exists()){
            System.out.println("读取路径不存在");
            return null;
        }
        //*****
        if(dir.isDirectory()) {         //文件夹部分
            File[] files = dir.listFiles();
            int condition=0;
            if (files != null) {
                for (int i = 0; i < files.length; i++) {
                    String fileName = files[i].getName();
                    if (files[i].isDirectory()) {
//                        fileNames.add(files[i].toString());
//                        getFileList(inPath,fileStyle);
                    } else if (fileName.endsWith(fileStyle)||fileName.substring(fileName.length()-4).contains(fileStyle)) {
                        String strFileName = files[i].getAbsolutePath();
                        fileNames.add(strFileName);
                        condition=1;
                    }else {

                    }
                }
                if(condition!=1){
//                    System.out.println("文件夹中没有符合要求的文件");
                }
            }
        }else {        //文件部分
            String fileName = dir.getName();
            if (fileName.endsWith(fileStyle)||fileName.substring(fileName.length()-4).contains(fileStyle)) {
                String strFileName = dir.getAbsolutePath();
                fileNames.add(strFileName);
            }else {
                System.out.println("不是符合后缀的文件");
            }
        }
        return fileNames;
    }
    //筛选出文件末尾带f的，且不为000分析场
    public static List<String> getFilterFileList(String inPath,String fileStyle){
        List<String> fileNames=ChooseDir.getFileListOnly(inPath,fileStyle);
        List<String> fileName_m=new ArrayList<>();
        if(fileNames==null)return fileName_m;
        for(String filenName:fileNames){
            int hour = Integer.parseInt(filenName.substring(filenName.length()-3));
            if( hour <48  && hour >0 )fileName_m.add(filenName);   //且不为000分析场
        }
        return  fileName_m;
    }
    //筛选出大JBDB文件，且不为000分析场
    public static List<String> getFilterFileList_JBDB(String inPath,String fileStyle){
        File dir=new File(inPath);
        String date = dir.getName();
        String[] JBDB = new String[]{ "XHGFS_G_JB_","XHGFS_G_DB_"};
        List<String> fileNames = new ArrayList<>();
        if(!dir.exists()){
            System.out.println("读取路径不存在");
            return null;
        }
        //*****
        if(dir.isDirectory()) {         //文件夹部分
            for (String eleName:JBDB) {
                for (int i = 0; i < 999; i++) {  //对于预报时间，三位
                    String path_temp = inPath+ "/" +eleName+ date + decimalFormat.format(i) +".dat";
                    if(new File(path_temp).exists()){
                        fileNames.add(path_temp);
                    }
                    if( i>  Integer.parseInt(Config.getfTime()[1]))break;
                }
            }
        }
        return fileNames;
    }
}
