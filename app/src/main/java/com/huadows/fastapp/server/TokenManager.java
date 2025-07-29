// app/src/main/java/com/huadows/fastapp/server/TokenManager.java
package com.huadows.fastapp.server;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负责生成、存储和验证一次性 Token 的管理器。
 */
public class TokenManager {
    private static final TokenManager sInstance = new TokenManager();
    // 使用 ConcurrentHashMap 保证线程安全
    private final ConcurrentHashMap<String, String> mTokenMap = new ConcurrentHashMap<>();

    private TokenManager() {}

    public static TokenManager get() {
        return sInstance;
    }

    /**
     * 为合法的包名生成一个新的 Token。
     * @param packageName 合法的包名。
     * @return 生成的 Token。
     */
    public String generateToken(String packageName) {
        String token = UUID.randomUUID().toString();
        mTokenMap.put(token, packageName);
        return token;
    }

    /**
     * 验证 Token 是否有效。
     * @param token 待验证的 Token。
     * @return 如果有效，返回对应的包名；否则返回 null。
     */
    public String getPackageNameForToken(String token) {
        if (token == null) {
            return null;
        }
        return mTokenMap.get(token);
    }
}