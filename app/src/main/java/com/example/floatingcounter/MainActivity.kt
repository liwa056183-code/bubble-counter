package com.example.floatingcounter

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val OVERLAY_PERMISSION_REQ_CODE = 1234
    private var currentSizeDp = 70

    private lateinit var tvStatus: TextView
    private lateinit var tvSizeLabel: TextView
    private lateinit var sbSize: SeekBar
    private lateinit var btnToggle: Button
    private lateinit var btnReset: Button

    private var isServiceRunning = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvSizeLabel = findViewById(R.id.tvSizeLabel)
        sbSize = findViewById(R.id.sbSize)
        btnToggle = findViewById(R.id.btnToggle)
        btnReset = findViewById(R.id.btnReset)

        checkOverlayPermission()
        setupListeners()
    }

    private fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                tvStatus.text = "يرجى منح إذن الظهور فوق التطبيقات لتفعيل الدائرة الصفراء"
                false
            } else {
                tvStatus.text = "تم منح الإذن بنجاح! يمكنك تشغيل الدائرة الآن"
                true
            }
        } else {
            true
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        }
    }

    private fun setupListeners() {
        btnToggle.setOnClickListener {
            if (!checkOverlayPermission()) {
                requestOverlayPermission()
                return@setOnClickListener
            }

            if (!isServiceRunning) {
                startFloatingService()
            } else {
                stopFloatingService()
            }
        }

        btnReset.setOnClickListener {
            val intent = Intent(this, FloatingBubbleService::class.java).apply {
                action = FloatingBubbleService.ACTION_RESET
            }
            startService(intent)
            Toast.makeText(this, "تمت إعادة تعيين العداد إلى 0", Toast.LENGTH_SHORT).show()
        }

        sbSize.progress = currentSizeDp
        tvSizeLabel.text = "حجم الدائرة: $currentSizeDp dp"

        // التحكم بحجم الدائرة فقط
        sbSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                currentSizeDp = progress.coerceIn(40, 180)
                tvSizeLabel.text = "حجم الدائرة: $currentSizeDp dp"

                if (isServiceRunning) {
                    val intent = Intent(this@MainActivity, FloatingBubbleService::class.java).apply {
                        action = FloatingBubbleService.ACTION_RESIZE
                        putExtra(FloatingBubbleService.EXTRA_SIZE_DP, currentSizeDp)
                    }
                    startService(intent)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingBubbleService::class.java).apply {
            action = FloatingBubbleService.ACTION_START
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isServiceRunning = true
        btnToggle.text = "إيقاف الدائرة المنبثقة"
        tvStatus.text = "الدائرة الصفراء ظاهرة الآن فوق جميع التطبيقات!"
    }

    private fun stopFloatingService() {
        val intent = Intent(this, FloatingBubbleService::class.java).apply {
            action = FloatingBubbleService.ACTION_STOP
        }
        startService(intent)
        isServiceRunning = false
        btnToggle.text = "تشغيل الدائرة المنبثقة"
        tvStatus.text = "تم إيقاف الدائرة المنبثقة"
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (checkOverlayPermission()) {
                Toast.makeText(this, "تم منح إذن الظهور فوق التطبيقات!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
