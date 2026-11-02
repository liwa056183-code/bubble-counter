package com.example.floatingcounter

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlin.math.abs

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleLayout: FrameLayout
    private lateinit var counterTextView: TextView
    private lateinit var layoutParams: WindowManager.LayoutParams

    private var count = 0
    private var bubbleSizeDp = 70 // الحجم الافتراضي بالـ DP

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_RESIZE = "ACTION_RESIZE"
        const val ACTION_RESET = "ACTION_RESET"
        const val EXTRA_SIZE_DP = "EXTRA_SIZE_DP"
        private const val CHANNEL_ID = "FloatingBubbleChannel"
        private const val NOTIFICATION_ID = 1001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForegroundServiceNotification()
        createFloatingBubble()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_RESIZE -> {
                val newSize = intent.getIntExtra(EXTRA_SIZE_DP, bubbleSizeDp)
                updateBubbleSize(newSize)
            }
            ACTION_RESET -> {
                count = 0
                counterTextView.text = count.toString()
            }
        }
        return START_STICKY
    }

    private fun createFloatingBubble() {
        val sizePx = dpToPx(bubbleSizeDp)

        // إعداد مواصفات النافذة المنبثقة فوق التطبيقات
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        // إنشاء حاوية الدائرة الصفراء
        bubbleLayout = FrameLayout(this).apply {
            val drawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#FACC15")) // اللون الأصفر المطلوب
                setStroke(dpToPx(2), Color.parseColor("#EAB308")) // إطار ذهبي أنيق
            }
            background = drawable
            elevation = dpToPx(8).toFloat()
        }

        // إنشاء النص لرقم العداد باللون الأسود داخل الدائرة
        counterTextView = TextView(this).apply {
            text = count.toString()
            setTextColor(Color.BLACK) // لون الرقم أسود كما طُلب
            setTextSize(TypedValue.COMPLEX_UNIT_SP, (bubbleSizeDp * 0.38f))
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            val params = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            layoutParams = params
        }

        bubbleLayout.addView(counterTextView)

        // إعداد حركة السحب (Drag) والنقر لزيادة الرقم
        setupTouchListener()

        windowManager.addView(bubbleLayout, layoutParams)
    }

    private fun setupTouchListener() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false
        val clickThresholdPx = dpToPx(10) // للتفريق بين النقر والسحب

        bubbleLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY

                    // إذا تحرك الإصبع أكثر من الحد الأدنى يعتبر تحريكاً
                    if (abs(deltaX) > clickThresholdPx || abs(deltaY) > clickThresholdPx) {
                        isDragging = true
                    }

                    if (isDragging) {
                        layoutParams.x = initialX + deltaX.toInt()
                        layoutParams.y = initialY + deltaY.toInt()
                        windowManager.updateViewLayout(bubbleLayout, layoutParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    // إذا لم يكن سحباً، يتم تنفيذ النقر وزيادة الرقم
                    if (!isDragging) {
                        onBubbleClicked()
                    }
                    true
                }
                else -> false
            }
        }
    }

    /**
     * عند النقر على الدائرة يزداد الرقم الذي بداخلها
     */
    private fun onBubbleClicked() {
        count++
        counterTextView.text = count.toString()

        // تأثير اهتزاز وتكبير بسيط عند النقر
        bubbleLayout.animate()
            .scaleX(1.15f)
            .scaleY(1.15f)
            .setDuration(80)
            .withEndAction {
                bubbleLayout.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(80)
                    .start()
            }.start()
    }

    /**
     * التحكم بحجم الدائرة الصفراء ديناميكياً
     */
    private fun updateBubbleSize(newSizeDp: Int) {
        bubbleSizeDp = newSizeDp.coerceIn(40, 180)
        val sizePx = dpToPx(bubbleSizeDp)
        layoutParams.width = sizePx
        layoutParams.height = sizePx
        counterTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, (bubbleSizeDp * 0.38f))
        windowManager.updateViewLayout(bubbleLayout, layoutParams)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun startForegroundServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "العداد العائم",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "خدمة تشغيل الدائرة الصفراء العائمة فوق التطبيقات"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("العداد العائم يعمل")
            .setContentText("الدائرة الصفراء تظهر الآن فوق كل التطبيقات")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::bubbleLayout.isInitialized) {
            windowManager.removeView(bubbleLayout)
        }
    }
}
