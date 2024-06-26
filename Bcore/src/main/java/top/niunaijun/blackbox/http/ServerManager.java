package top.niunaijun.blackbox.http;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.yanzhenjie.andserver.AndServer;
import com.yanzhenjie.andserver.Server;

import java.util.concurrent.TimeUnit;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.touch.ControlThread;

public class ServerManager {

    private static Server mServer;
    public static Server get()
    {
        if(mServer == null)
        {
            Context context = BlackBoxCore.getContext();
            mServer = AndServer.webServer(context)
                    .port(8080)
                    .timeout(10, TimeUnit.SECONDS)
                    .listener(new Server.ServerListener() {
                        @Override
                        public void onStarted() {
                            // TODO The server started successfully.
                            ControlThread.get().start();
                            Toast.makeText(context, "web start", Toast.LENGTH_LONG).show();
                        }

                        @Override
                        public void onStopped() {
                            // TODO The server has stopped.
                        }

                        @Override
                        public void onException(Exception e) {
                            // TODO An exception occurred while the server was starting.
                        }
                    })
                    .build();
        }
        return mServer;
    }

    /**
     * Start server.
     */
    public static void startServer() {
        Server server = get();
        if (server.isRunning()) {
            // TODO The server is already up.
        } else {
            server.startup();
        }
    }

    /**
     * Stop server.
     */
    public static void stopServer() {
        Server server = get();
        if (server.isRunning()) {
            server.shutdown();
        } else {
            Log.w("AndServer", "The server has not started yet.");
        }
    }
}
