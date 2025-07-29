// IBDisplayManagerService.aidl
package top.niunaijun.blackbox.core.system.display;

interface IBDisplayManagerService {
    void setVirtualDPI(String packageName, int dpi, int userId);
    int getVirtualDPI(String packageName, int userId);
}