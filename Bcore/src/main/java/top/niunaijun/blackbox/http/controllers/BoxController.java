package top.niunaijun.blackbox.http.controllers;

import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestMapping;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.pm.InstallResult;

@RequestMapping("/box")
@RestController
public class BoxController {
    @PostMapping("/launchApk")
    boolean launchApk(@RequestParam("name") String name,
                      @RequestParam(name = "id",  required = false, defaultValue = "0")
                      int id) {
        return BlackBoxCore.get().launchApk(name,id);
    }
    @PostMapping("/isInstalled")
    boolean isInstalled(@RequestParam("name") String name,
                                 @RequestParam(name = "id",  required = false, defaultValue = "0")
                                 int id) {
        return BlackBoxCore.get().isInstalled(name,id);
    }
    @PostMapping("/installPackageAsUser")
    boolean installPackageAsUser(@RequestParam("name") String name,
                      @RequestParam(name = "id",  required = false, defaultValue = "0")
                      int id) {
        if(BlackBoxCore.get().isInstalled(name,id))
            return true;
        InstallResult result = BlackBoxCore.get().installPackageAsUser(name,id);
        return result.success;
    }
    @PostMapping("/uninstallPackageAsUser")
    void uninstallPackageAsUser(@RequestParam("name") String name,
                     @RequestParam(name = "id",  required = false, defaultValue = "0")
                     int id) {
        BlackBoxCore.get().uninstallPackageAsUser(name,id);
    }
    @PostMapping("/stopPackage")
    void stopPackage(@RequestParam("name") String name,
                        @RequestParam(name = "id",  required = false, defaultValue = "0")
                        int id) {
        BlackBoxCore.get().stopPackage(name,id);
    }
}
