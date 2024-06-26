package top.niunaijun.blackbox.http.controllers;

import com.yanzhenjie.andserver.annotation.PostMapping;
import com.yanzhenjie.andserver.annotation.RequestMapping;
import com.yanzhenjie.andserver.annotation.RequestParam;
import com.yanzhenjie.andserver.annotation.RestController;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import top.niunaijun.blackbox.touch.ControlMessage;
import top.niunaijun.blackbox.touch.ControlThread;
import top.niunaijun.blackbox.touch.Point;

@RequestMapping("/touch")
@RestController
public class TouchController {
    private Queue<ControlMessage> subqueue = new LinkedList<>();
    private final LinkedBlockingQueue<Queue<ControlMessage>> queue = ControlThread.getQueue();


    private void commitSubqueue() {
        if (!subqueue.isEmpty()) {
            try {
                queue.put(subqueue);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.subqueue = new LinkedList<>();
        }
    }



    @PostMapping("/Commit")
    void Commit()
    {
        commitSubqueue();
    }
    @PostMapping("/Reset")
    void Reset()
    {
        subqueue.clear();
        while (!subqueue.offer(ControlMessage.createEmpty(ControlMessage.TYPE_EVENT_TOUCH_RESET)));
        commitSubqueue();
    }
    @PostMapping("/Down")
    void Down(@RequestParam("id") long pointerId,
                      @RequestParam("x") int x,
                      @RequestParam("y") int y)
    {
        Point point = new Point(x, y);
        while (!subqueue.offer(ControlMessage.createTouchDownEvent(pointerId, point, 1.0f)));
    }
    @PostMapping("/Move")
    void Move(@RequestParam("id") long pointerId,
                      @RequestParam("x") int x,
                      @RequestParam("y") int y)
    {
        Point point = new Point(x, y);
        while (!subqueue.offer(ControlMessage.createTouchMoveEvent(pointerId, point, 1.0f)));
    }
    @PostMapping("/Up")
    void Up(@RequestParam("id") long pointerId)
    {
        while (!subqueue.offer(ControlMessage.createTouchUpEvent(pointerId)));
    }
    @PostMapping("/Wait")
    void Wait(@RequestParam("ms") long milis)
    {
        while (!subqueue.offer(ControlMessage.createWaitEvent(milis)));
    }
    @PostMapping("/Key")
    void Key(@RequestParam("key") int keycode,
                     @RequestParam("type") int type)
    {
        switch (type)
        {
            case 0:
                while (!subqueue.offer(ControlMessage.createKeyEvent(keycode)));
                break;
            case 1:
                while (!subqueue.offer(ControlMessage.createKeyDownEvent(keycode, 0, 0)));
                break;
            case 2:
                while (!subqueue.offer(ControlMessage.createKeyUpEvent(keycode, 0, 0)));
                break;
            default:
                break;
        }
    }
    @PostMapping("/Text")
    void Text(@RequestParam("text") String text)
    {
        while (!subqueue.offer(ControlMessage.createTextEvent(text)));
    }
}
