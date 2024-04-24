package top.niunaijun.blackboxa.http.controllers

import com.yanzhenjie.andserver.annotation.GetMapping
import com.yanzhenjie.andserver.annotation.PostMapping
import com.yanzhenjie.andserver.annotation.RequestParam
import com.yanzhenjie.andserver.annotation.RestController

@RestController
class TestController {
    @GetMapping("/test")
    fun login(): String {
        return "Hello World"
    }
}