package top.niunaijun.blackbox.fake.frameworks;

import android.os.RemoteException;

import top.niunaijun.blackbox.core.system.ServiceManager;
import top.niunaijun.blackbox.core.system.display.IBDisplayManagerService;

public class BDisplayManager extends BlackManager<IBDisplayManagerService> {
    private static final BDisplayManager sDisplayManager = new BDisplayManager();

    public static BDisplayManager get() {
        return sDisplayManager;
    }

    @Override
    protected String getServiceName() {
        return ServiceManager.DISPLAY_MANAGER;
    }

    public void setVirtualDPI(String packageName, int dpi, int userId) {
        try {
            getService().setVirtualDPI(packageName, dpi, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public int getVirtualDPI(String packageName, int userId) {
        try {
            return getService().getVirtualDPI(packageName, userId);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return 0;
    }
}