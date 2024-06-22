package top.niunaijun.blackboxa.view.main

import android.app.Application
import android.content.Context
import android.util.Log
import top.niunaijun.blackbox.BlackBoxCore
import top.niunaijun.blackbox.app.BActivityThread
import top.niunaijun.blackbox.app.configuration.AppLifecycleCallback
import top.niunaijun.blackbox.app.configuration.ClientConfiguration
import top.niunaijun.blackboxa.app.App
import top.niunaijun.blackboxa.biz.cache.AppSharedPreferenceDelegate
import java.io.File

/**
 *
 * @Description:
 * @Author: wukaicheng
 * @CreateDate: 2021/5/6 23:38
 */
class BlackBoxLoader {


    private var mHideRoot by AppSharedPreferenceDelegate(App.getContext(), true)
    private var mHideXposed by AppSharedPreferenceDelegate(App.getContext(), true)
    private var mDaemonEnable by AppSharedPreferenceDelegate(App.getContext(), true)
    private var mFridaEnable by AppSharedPreferenceDelegate(App.getContext(), true)
    private var mShowShortcutPermissionDialog by AppSharedPreferenceDelegate(App.getContext(), true)


    fun hideRoot(): Boolean {
        return mHideRoot
    }

    fun invalidHideRoot(hideRoot: Boolean) {
        this.mHideRoot = hideRoot
    }

    fun hideXposed(): Boolean {
        return mHideXposed
    }

    fun invalidHideXposed(hideXposed: Boolean) {
        this.mHideXposed = hideXposed
    }

    fun daemonEnable(): Boolean {
        return mDaemonEnable
    }

    fun invalidDaemonEnable(enable: Boolean) {
        this.mDaemonEnable = enable
    }

    fun fridaEnable(): Boolean {
        return mFridaEnable
    }

    fun invalidFridaEnable(enable: Boolean) {
        this.mFridaEnable = enable
    }

    fun showShortcutPermissionDialog(): Boolean {
        return mShowShortcutPermissionDialog
    }

    fun invalidShortcutPermissionDialog(show: Boolean) {
        this.mShowShortcutPermissionDialog = show
    }

    fun getBlackBoxCore(): BlackBoxCore {
        return BlackBoxCore.get()
    }

    fun addLifecycleCallback() {
        Log.d("nfh", TAG + ".addLifecycleCallback")
        BlackBoxCore.get().addAppLifecycleCallback(object : AppLifecycleCallback() {
            override fun beforeCreateApplication(
                packageName: String?,
                processName: String?,
                context: Context?,
                userId: Int
            ) {
                Log.d("nfh", TAG + ".override.beforeCreateApplication")
                Log.d(
                    "nfh",
                    "beforeCreateApplication: pkg $packageName, processName $processName,userID:${BActivityThread.getUserId()}"
                )
                Log.d(
                    TAG,
                    "beforeCreateApplication: pkg $packageName, processName $processName,userID:${BActivityThread.getUserId()}"
                )
            }


            override fun beforeApplicationOnCreate(
                packageName: String?,
                processName: String?,
                application: Application?,
                userId: Int
            ) {
                Log.d("nfh", TAG + ".override.beforeApplicationOnCreate")
                Log.d("nfh", "beforeApplicationOnCreate: pkg $packageName, processName $processName")
                Log.d(TAG, "beforeApplicationOnCreate: pkg $packageName, processName $processName")
            }

            override fun afterApplicationOnCreate(
                packageName: String?,
                processName: String?,
                application: Application?,
                userId: Int
            ) {
                Log.d("nfh", TAG + ".override.afterApplicationOnCreate")
                Log.d("nfh", "afterApplicationOnCreate: pkg $packageName, processName $processName")
//                RockerManager.init(application,userId)
                Log.d(TAG, "afterApplicationOnCreate: pkg $packageName, processName $processName")
//                RockerManager.init(application,userId)
            }
        })
    }

    fun attachBaseContext(context: Context) {
        Log.d("nfh", "BlackBoxLoader.attachBaseContext")
        BlackBoxCore.get().doAttachBaseContext(context, object : ClientConfiguration() {
            override fun getHostPackageName(): String {
                return context.packageName
            }

            override fun isHideRoot(): Boolean {
                return mHideRoot
            }

            override fun isHideXposed(): Boolean {
                return mHideXposed
            }

            override fun isEnableDaemonService(): Boolean {
                return mDaemonEnable
            }
            override fun isEnableFrida(): Boolean {
                return mFridaEnable
            }

            override fun requestInstallPackage(file: File?, userId: Int): Boolean {
                val packageInfo =
                    context.packageManager.getPackageArchiveInfo(file!!.absolutePath, 0)
                return false
            }
        })
    }

    fun doOnCreate(context: Context) {
        Log.d("nfh", TAG + ".doOnCreate")
        BlackBoxCore.get().doCreate()

    }


    companion object {

        val TAG: String = BlackBoxLoader::class.java.simpleName

    }

}