// 文件路径: 快应用/Bcore/src/main/java/top/niunaijun/blackbox/utils/GadgetUtils.java

package top.niunaijun.blackbox.utils;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GadgetUtils {
    private static final String TAG = "nfh";

    public static void load() {
        // 关键修复：注释掉对 "libnfh.so" 的加载调用。
        // 此库是用于Frida调试的可选组件，在您的项目中并未提供，因此加载它会导致闪退。
        // 移除后，将禁用该调试功能，但不会影响BlackBox的核心虚拟化能力。
        // System.loadLibrary("nfh");
        Log.d("nfh", TAG + ".loadLibrary gadget - disabled");
    }

    /**
     * 获取全部已加载的SO库
     */
    public static List<String> readProcMaps(){
        // 当前应用的进程ID
        int pid = android.os.Process.myPid();
        String path = "/proc/" + pid + "/maps";
        File file = new File(path);
        List<String> list = new ArrayList<>();
        if(file.exists() && file.isFile()){
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(file));
                String tempString = null;
                // 一次读入一行，直到读入null为文件结束
                while ((tempString = reader.readLine()) != null) {
                    if(tempString.endsWith(".so")) {
                        int index = tempString.indexOf("/");
                        if(index != -1){
                            String str = tempString.substring(index);
                            // 所有so库（包括系统的，即包含/system/目录下的）
                            list.add(str);
                        }
                    }
                }
                reader.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }
        }
        return list;
    }

    /**
     * 获取当前应用已加载的SO库
     */
    public static void getCurrSOLoaded(){
        List<String> list = readProcMaps();
        for (String str : list) {
            if (str.startsWith("/data/") || str.contains("gadget")) {
                Log.d(TAG,"CurrSOLoaded: " + str);
            }
        }
    }

    /**
     * 获取全部已加载的SO库
     */
    public static void getAllSOLoaded(){
        List<String> list = readProcMaps();
        for (String str : list) {
            Log.d(TAG,"AllSOLoaded: " + str);
        }
    }
}