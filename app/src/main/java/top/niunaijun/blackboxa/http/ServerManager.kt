package top.niunaijun.blackboxa.http

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.yanzhenjie.andserver.AndServer
import com.yanzhenjie.andserver.Server
import com.yanzhenjie.andserver.Server.ServerListener
import java.util.concurrent.TimeUnit


class ServerManager(context: Context?) {
    private val mServer: Server = AndServer.webServer(context!!)
        .port(8080)
        .timeout(10, TimeUnit.SECONDS)
        .listener(object : ServerListener {
            override fun onStarted() {
                // TODO The server started successfully.
                Toast.makeText(context, "web start", Toast.LENGTH_LONG).show()
            }

            override fun onStopped() {
                // TODO The server has stopped.
                Toast.makeText(context, "web stop", Toast.LENGTH_LONG).show()
            }

            override fun onException(e: Exception) {
                // TODO An exception occurred while the server was starting.
            }
        })
        .build()

    /**
     * Start server.
     */
    fun startServer() {
        if (mServer.isRunning) {
            // TODO The server is already up.
        } else {
            mServer.startup()
        }
    }

    /**
     * Stop server.
     */
    fun stopServer() {
        if (mServer.isRunning) {
            mServer.shutdown()
        } else {
            Log.w("AndServer", "The server has not started yet.")
        }
    }
}