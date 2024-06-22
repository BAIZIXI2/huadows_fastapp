package top.niunaijun.blackbox.http.controllers;

import com.yanzhenjie.andserver.annotation.GetMapping;
import com.yanzhenjie.andserver.annotation.RestController;

@RestController
public class TestController {
    @GetMapping("/test")
    public String test()
    {
        return "Hello World";
    }
}
