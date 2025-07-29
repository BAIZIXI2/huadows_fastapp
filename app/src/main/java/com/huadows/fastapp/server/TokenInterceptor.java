// 文件路径: app/src/main/java/com/huadows/fastapp/server/TokenInterceptor.java
package com.huadows.fastapp.server;

import androidx.annotation.NonNull;

import com.yanzhenjie.andserver.annotation.Interceptor; // 重新添加 @Interceptor 注解
import com.yanzhenjie.andserver.error.HttpException;
import com.yanzhenjie.andserver.framework.HandlerInterceptor;
import com.yanzhenjie.andserver.framework.handler.RequestHandler;
import com.yanzhenjie.andserver.http.HttpRequest;
import com.yanzhenjie.andserver.http.HttpResponse;

/**
 * 验证请求头中是否包含有效 Token 的拦截器。
 * 使用 @Interceptor 注解，让 AndServer 自动扫描并全局应用。
 */
@Interceptor
public class TokenInterceptor implements HandlerInterceptor {

    @Override
    public boolean onIntercept(@NonNull HttpRequest request, @NonNull HttpResponse response, @NonNull RequestHandler handler) throws Exception {
        String path = request.getPath();

        // ====================== 关键修复点 ======================
        // 在拦截器内部判断路径。
        // 只对 /app/ 开头的路径执行 Token 验证逻辑。
        if (!path.startsWith("/app/")) {
            // 对于 /icon 等其他路径，直接返回 false 放行。
            return false;
        }
        // =======================================================

        // --- 以下是原来的验证逻辑，现在只对 /app/ 路径生效 ---
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String packageName = TokenManager.get().getPackageNameForToken(token);
            if (packageName != null) {
                request.setAttribute("callerPackageName", packageName);
                return false; // 验证通过，继续执行请求
            }
        }
        
        // 验证失败，抛出401未授权异常
        throw new HttpException(401, "无效访问");
    }
}