// File path: app/src/main/java/com/huadows/fastapp/server/IconCacheManager.java
package com.huadows.fastapp.server;

import android.graphics.Bitmap;
import android.util.LruCache;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class IconCacheManager {
    private static final IconCacheManager sInstance = new IconCacheManager();

    private final LruCache<String, Bitmap> mBitmapCache = new LruCache<>(4 * 1024 * 1024);
    private final ConcurrentHashMap<String, String> mTokenToKeyMap = new ConcurrentHashMap<>();

    private IconCacheManager() {}

    public static IconCacheManager get() {
        return sInstance;
    }

    // vvvvvvvvvvvv 关键修复 vvvvvvvvvvvv
    /**
     * 生成图标的访问URL
     * @param baseUrl 服务器的基础URL (例如 http://localhost:12345)
     * @param packageName 应用包名
     * @param userId 用户ID
     * @param bitmap 图标位图
     * @return 完整的、可访问的图标URL
     */
    public String generateIconUrl(String baseUrl, String packageName, int userId, Bitmap bitmap) {
    // ^^^^^^^^^^^^ 修复结束 ^^^^^^^^^^^^
        String cacheKey = packageName + ":" + userId;
        mBitmapCache.put(cacheKey, bitmap);
        String token = UUID.randomUUID().toString();
        mTokenToKeyMap.put(token, cacheKey);

        // vvvvvvvvvvvv 关键修复 vvvvvvvvvvvv
        // 使用传入的 baseUrl 构建动态 URL
        return baseUrl + "/icon?token=" + token;
        // ^^^^^^^^^^^^ 修复结束 ^^^^^^^^^^^^
    }

    public Bitmap getBitmapByToken(String token) {
        String cacheKey = mTokenToKeyMap.get(token);
        if (cacheKey != null) {
            return mBitmapCache.get(cacheKey);
        }
        return null;
    }
}