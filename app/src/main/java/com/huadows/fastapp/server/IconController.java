// 文件路径: app/src/main/java/com/huadows/fastapp/server/IconController.java
package com.huadows.fastapp.server;

import android.graphics.Bitmap;
import com.google.gson.Gson;
import com.huadows.fastapp.util.ImageUtils;
import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;
import com.yanzhenjie.andserver.framework.body.JsonBody;
import com.yanzhenjie.andserver.http.ResponseBody;

import java.util.HashMap;
import java.util.Map;

@RestController
public class IconController {

    private final Gson gson = new Gson();

    /**
     * 根据 token，返回一个包含 Base64 编码图标的 JSON 对象。
     * @param token 访问令牌
     * @return 包含图标数据的 JsonBody
     */
    // ====================== 关键修复点 ======================
    // 路径保持不变，但返回值改为 ResponseBody
    @GetMapping(path = "/icon")
    public ResponseBody getIcon(@RequestParam("token") String token) {
        Map<String, String> response = new HashMap<>();
        Bitmap bitmap = IconCacheManager.get().getBitmapByToken(token);

        if (bitmap != null) {
            // 1. 将 Bitmap 转换为 Base64 字符串
            String base64Icon = ImageUtils.bitmapToBase64(bitmap);
            
            // 2. 将 Base64 字符串放入一个 Map 中
            response.put("icon", base64Icon);
            response.put("error", null);

            // 3. 将 Map 序列化为 JSON 并通过 JsonBody 返回。
            //    这与 AppController 的做法完全一致，保证了编译通过。
            return new JsonBody(gson.toJson(response));
        } else {
            response.put("icon", null);
            response.put("error", "Icon not found or expired.");
            return new JsonBody(gson.toJson(response));
        }
    }
    // =======================================================
}