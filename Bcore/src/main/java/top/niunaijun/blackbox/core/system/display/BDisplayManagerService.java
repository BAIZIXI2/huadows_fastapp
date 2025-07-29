package top.niunaijun.blackbox.core.system.display;

import android.os.Parcel;
import android.util.AtomicFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.core.system.ISystemService;
import top.niunaijun.blackbox.utils.CloseUtils;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.Slog;

public class BDisplayManagerService extends IBDisplayManagerService.Stub implements ISystemService {
    public static final String TAG = "BDisplayManagerService";
    private static final BDisplayManagerService sService = new BDisplayManagerService();

    // Key: userId, Value: Map<PackageName, DPI>
    private final Map<Integer, Map<String, Integer>> mDpiConf = new HashMap<>();
    private final Object mLock = new Object();

    public static BDisplayManagerService get() {
        return sService;
    }

    @Override
    public void systemReady() {
        loadDpiConfig();
    }

    @Override
    public void setVirtualDPI(String packageName, int dpi, int userId) {
        synchronized (mLock) {
            Map<String, Integer> userDpiMap = mDpiConf.get(userId);
            if (userDpiMap == null) {
                userDpiMap = new HashMap<>();
                mDpiConf.put(userId, userDpiMap);
            }
            userDpiMap.put(packageName, dpi);
            saveDpiConfig();
        }
    }

    @Override
    public int getVirtualDPI(String packageName, int userId) {
        synchronized (mLock) {
            Map<String, Integer> userDpiMap = mDpiConf.get(userId);
            if (userDpiMap != null) {
                Integer dpi = userDpiMap.get(packageName);
                return dpi != null ? dpi : 0; // 0 表示使用默认DPI
            }
            return 0;
        }
    }

    private File getDpiConfFile() {
        return new File(BEnvironment.getSystemDir(), "display.conf");
    }

    private void saveDpiConfig() {
        synchronized (mLock) {
            Parcel parcel = Parcel.obtain();
            AtomicFile atomicFile = new AtomicFile(getDpiConfFile());
            FileOutputStream fileOutputStream = null;
            try {
                parcel.writeMap(mDpiConf);
                fileOutputStream = atomicFile.startWrite();
                FileUtils.writeParcelToOutput(parcel, fileOutputStream);
                atomicFile.finishWrite(fileOutputStream);
            } catch (IOException e) {
                Slog.e(TAG, "saveDpiConfig failed", e);
                atomicFile.failWrite(fileOutputStream);
            } finally {
                parcel.recycle();
                CloseUtils.close(fileOutputStream);
            }
        }
    }

    private void loadDpiConfig() {
        synchronized (mLock) {
            mDpiConf.clear();
            File dpiConfFile = getDpiConfFile();
            if (!dpiConfFile.exists()) {
                return;
            }
            Parcel parcel = null;
            try {
                parcel = FileUtils.readToParcel(dpiConfFile);
                Map<Integer, Map<String, Integer>> map = parcel.readHashMap(HashMap.class.getClassLoader());
                if (map != null) {
                    mDpiConf.putAll(map);
                }
            } catch (Exception e) {
                Slog.e(TAG, "loadDpiConfig failed", e);
                dpiConfFile.delete();
            } finally {
                if (parcel != null) {
                    parcel.recycle();
                }
            }
        }
    }
}