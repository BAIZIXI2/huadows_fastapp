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
    @PostMapping("/Touch")
    void Touch(@RequestParam("id") long pointerId,
               @RequestParam("x") int x,
               @RequestParam("y") int y,
               @RequestParam("duration") int duration)
    {
        Point point = new Point(x, y);
        while (!subqueue.offer(ControlMessage.createTouchDownEvent(pointerId, point, 1.0f)));
        while (!subqueue.offer(ControlMessage.createWaitEvent(duration)));
        while (!subqueue.offer(ControlMessage.createTouchUpEvent(pointerId)));
        commitSubqueue();
    }
    @PostMapping("/Slide")
    void Slide(@RequestParam("id") long pointerId,
               @RequestParam("x1") int x1,
               @RequestParam("y1") int y1,
               @RequestParam("x2") int x2,
               @RequestParam("y2") int y2,
               @RequestParam("s_duration") int s_duration,
               @RequestParam("m_duration") int m_duration,
               @RequestParam("e_duration") int e_duration,
               @RequestParam("move_count") int move_count)
    {
        Point point = new Point(x1, y1);
        while (!subqueue.offer(ControlMessage.createTouchDownEvent(pointerId, point, 1.0f)));
        if(s_duration > 0)
        {
            while (!subqueue.offer(ControlMessage.createWaitEvent(s_duration)));
        }
        if(move_count == 0)
        {
            if(m_duration < 100)
            {
                move_count = 1;
            }else
            {
                move_count = m_duration / 100;
            }
        }
        int x = (x2 - x1) / move_count;
        int y = (y2 - y1) / move_count;
        int m = m_duration / move_count;
        for (int i = 1;i<=move_count;i++)
        {
            x1 += x;
            y1 += y;
            Point p = new Point(x1, y1);
            while (!subqueue.offer(ControlMessage.createTouchMoveEvent(pointerId, p, 1.0f)));
            if(i < move_count)
            {
                while (!subqueue.offer(ControlMessage.createWaitEvent(m)));
            }
        }
        if(e_duration > 0)
        {
            while (!subqueue.offer(ControlMessage.createWaitEvent(e_duration)));
        }
        while (!subqueue.offer(ControlMessage.createTouchUpEvent(pointerId)));
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
                     @RequestParam("duration") int duration)
    {
        if(duration <= 0)
        {
            while (!subqueue.offer(ControlMessage.createKeyEvent(keycode)));
        }else{
            while (!subqueue.offer(ControlMessage.createKeyDownEvent(keycode, 0, 0)));
            while (!subqueue.offer(ControlMessage.createWaitEvent(duration)));
            while (!subqueue.offer(ControlMessage.createKeyUpEvent(keycode, 0, 0)));
        }
        commitSubqueue();
    }
    @PostMapping("/Text")
    void Text(@RequestParam("text") String text)
    {
        while (!subqueue.offer(ControlMessage.createTextEvent(text)));
    }
}
