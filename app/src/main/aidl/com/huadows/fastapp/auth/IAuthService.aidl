// app/src/main/aidl/com/huadows/fastapp/auth/IAuthService.aidl
package com.huadows.fastapp.auth;

import android.os.Bundle;

interface IAuthService {
    /**
     * 与快应用服务进行握手。
     * 客户端传递自己的包名以供验证。
     * @param packageName 调用方的包名。
     * @return 如果验证通过，返回一个包含端口号(KEY_PORT)和Token(KEY_TOKEN)的 Bundle；否则返回 null。
     */
    Bundle performHandshake(String packageName);

    /**
     * 通过 Binder 启动一个虚拟应用。
     * @param packageName 要启动的应用包名。
     * @param userId 用户ID，通常为 0。
     * @return 启动是否成功。
     */
    boolean launchApp(String packageName, int userId);
}