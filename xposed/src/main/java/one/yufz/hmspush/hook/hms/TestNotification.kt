package one.yufz.hmspush.hook.hms


import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.huawei.android.app.NotificationManagerEx
import one.yufz.hmspush.hook.XLog

object TestNotification {
    private const val TAG = "TestNotification"
    private const val TEST_ACTION = "one.yufz.hmspush.TEST_NOTIFICATION"
    private const val TEST_CHANNEL_ID = "push"

    private var registered = false

    fun register() {
        if (registered) return
        registered = true

        XLog.d(TAG, "register() called, scheduling receiver registration")

        // Post to main looper to ensure Application is ready
        Handler(Looper.getMainLooper()).post {
            try {
                val context = App.current()
                val filter = IntentFilter(TEST_ACTION)
                context.registerReceiver(object : BroadcastReceiver() {
                    override fun onReceive(ctx: Context, intent: Intent) {
                        val targetPackage = intent.getStringExtra("package")
                            ?: "com.ss.android.ugc.aweme"
                        val title = intent.getStringExtra("title")
                            ?: "HMSPush 测试通知"
                        val content = intent.getStringExtra("content")
                            ?: "如果你看到这条通知，说明推送链路正常 ✅"

                        sendTestNotification(targetPackage, title, content)
                    }
                }, filter)

                XLog.d(TAG, "test notification receiver registered (action=$TEST_ACTION)")
            } catch (t: Throwable) {
                XLog.e(TAG, "register failed, will retry", t)
                registered = false
                // Retry after 3s
                Handler(Looper.getMainLooper()).postDelayed({ register() }, 3000)
            }
        }
    }

    private fun sendTestNotification(packageName: String, title: String, content: String) {
        XLog.d(TAG, "sendTestNotification() called: package=$packageName, title=$title")

        try {
            val context = App.current()

            // Ensure test channel exists
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                TEST_CHANNEL_ID,
                "HMSPush 测试",
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(channel)

            // Build test notification
            val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Notification.Builder(context, TEST_CHANNEL_ID)
            } else {
                @Suppress("DEPRECATION")
                Notification.Builder(context)
            }

            builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(content)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_MESSAGE)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                builder.setPriority(Notification.PRIORITY_HIGH)
            }

            val notification = builder.build()
            val id = System.currentTimeMillis().toInt()

            // Send through HMSPush's notification pipeline
            NotificationManagerEx.notify(context, packageName, id, notification)

            XLog.d(TAG, "test notification sent to $packageName (id=$id)")
        } catch (t: Throwable) {
            XLog.e(TAG, "sendTestNotification failed", t)
        }
    }
}
