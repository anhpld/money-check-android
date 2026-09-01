package com.example.moneycheck.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.res.ColorStateList
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.Display
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.moneycheck.data.CapturedNotificationEntity
import com.example.moneycheck.data.MoneyCheckRepository
import com.example.moneycheck.llm.NotificationAnalysisScheduler
import com.example.moneycheck.settings.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID
import java.lang.ref.WeakReference
import kotlin.math.abs

class ScreenTransactionCaptureService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val sessionStore by lazy { ScreenCaptureSessionStore(this) }
    private val windowManager by lazy { getSystemService(WindowManager::class.java) }
    private val textRecognizerDelegate = lazy { OnDeviceScreenTextRecognizer() }
    private val textRecognizer by textRecognizerDelegate
    private var captureButton: View? = null
    private var captureButtonParams: WindowManager.LayoutParams? = null
    private var captureInProgress = false
    private var pendingPackageName: String? = null
    private val inspectScreen = Runnable { inspectAutomaticScreen() }
    private var lastForegroundPackage: String? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        connectedService = WeakReference(this)
        sessionStore.active()?.let(::showCaptureButton)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        if (packageName != applicationContext.packageName && packageName != SYSTEM_UI_PACKAGE) {
            lastForegroundPackage = packageName
        }
        val manualSession = sessionStore.active()
        if (manualSession != null) {
            if (!captureInProgress) showCaptureButton(manualSession)
            return
        }

        removeCaptureButton()
        if (packageName != ScreenTransactionExtractor.SHOPEE_PACKAGE) return
        pendingPackageName = packageName
        mainHandler.removeCallbacks(inspectScreen)
        mainHandler.postDelayed(inspectScreen, SCREEN_SETTLE_DELAY_MILLIS)
    }

    override fun onInterrupt() {
        mainHandler.removeCallbacks(inspectScreen)
        removeCaptureButton()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacks(inspectScreen)
        removeCaptureButton()
        if (textRecognizerDelegate.isInitialized()) textRecognizer.close()
        if (connectedService?.get() === this) connectedService = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun showCaptureButton(session: ScreenCaptureSession) {
        if (captureButton != null || captureInProgress) return
        val label = TextView(this).apply {
            text = "Nhận diện"
            setTextColor(Color.WHITE)
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(12), dp(12), dp(12))
        }
        val close = TextView(this).apply {
            text = "×"
            setTextColor(Color.WHITE)
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(dp(10), dp(8), dp(12), dp(10))
            setOnClickListener {
                sessionStore.clear()
                removeCaptureButton()
                showToast("Đã hủy đọc màn hình")
            }
        }
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            elevation = dp(10).toFloat()
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(24).toFloat()
                setColor(Color.rgb(8, 122, 85))
            }
            addView(label)
            addView(close)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (resources.displayMetrics.widthPixels - dp(180)).coerceAtLeast(0)
            y = resources.displayMetrics.heightPixels / 2
        }
        attachDragAndCapture(container, params, session)
        runCatching { windowManager.addView(container, params) }
            .onSuccess {
                captureButton = container
                captureButtonParams = params
            }
    }

    private fun attachDragAndCapture(
        view: View,
        params: WindowManager.LayoutParams,
        session: ScreenCaptureSession,
    ) {
        var downRawX = 0f
        var downRawY = 0f
        var startX = 0
        var startY = 0
        var dragged = false
        view.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = event.rawX
                    downRawY = event.rawY
                    startX = params.x
                    startY = params.y
                    dragged = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - downRawX
                    val dy = event.rawY - downRawY
                    if (abs(dx) > dp(6) || abs(dy) > dp(6)) dragged = true
                    params.x = (startX + dx.toInt()).coerceIn(0, resources.displayMetrics.widthPixels - dp(80))
                    params.y = (startY + dy.toInt()).coerceIn(0, resources.displayMetrics.heightPixels - dp(80))
                    runCatching { windowManager.updateViewLayout(view, params) }
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!dragged) showExtractionModePicker(session)
                    true
                }

                else -> false
            }
        }
    }

    private fun showExtractionModePicker(session: ScreenCaptureSession) {
        val target = detectCurrentTarget(requireAccessibilityRoot = false)
        if (target == null) {
            showToast("Hãy mở màn chi tiết giao dịch của một ứng dụng rồi thử lại")
            return
        }
        removeCaptureButton()

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            elevation = dp(18).toFloat()
            setPadding(dp(16), dp(16), dp(16), dp(14))
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dp(24).toFloat()
                setColor(Color.rgb(20, 29, 38))
                setStroke(dp(1), Color.rgb(55, 72, 84))
            }
            addView(modePickerHeader(target.appName) {
                removeCaptureButton()
                showCaptureButton(session)
            })
            addView(modePickerOption("XML", "Accessibility", "Nhanh · Đọc cấu trúc giao diện") {
                captureManualScreen(session, ScreenExtractionMode.ACCESSIBILITY)
            })
            addView(modePickerOption("OCR", "Nhận dạng chữ", "Chụp màn hình · Xử lý trên máy") {
                captureManualScreen(session, ScreenExtractionMode.OCR)
            })
            addView(modePickerOption("2×", "Accessibility + OCR", "Kết hợp cấu trúc và chữ trên ảnh") {
                captureManualScreen(session, ScreenExtractionMode.ACCESSIBILITY_OCR)
            })
            addView(modePickerHint("Chọn phương thức phù hợp với màn hình hiện tại"))
        }
        val params = overlayLayoutParams(width = dp(336)).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }
        runCatching { windowManager.addView(container, params) }
            .onSuccess {
                captureButton = container
                captureButtonParams = params
            }
            .onFailure { showCaptureButton(session) }
    }

    private fun modePickerHeader(appName: String, onBack: () -> Unit): View =
        LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            addView(
                ImageButton(this@ScreenTransactionCaptureService).apply {
                    contentDescription = "Quay lại"
                    setImageResource(com.example.moneycheck.R.drawable.ic_arrow_back_24)
                    setColorFilter(Color.WHITE)
                    setPadding(dp(9), dp(9), dp(9), dp(9))
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(Color.rgb(39, 53, 64))
                    }
                    setOnClickListener { onBack() }
                },
                LinearLayout.LayoutParams(dp(40), dp(40)),
            )

            addView(
                LinearLayout(this@ScreenTransactionCaptureService).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(dp(12), 0, 0, 0)
                    addView(TextView(this@ScreenTransactionCaptureService).apply {
                        text = "Chọn cách đọc"
                        setTextColor(Color.WHITE)
                        textSize = 17f
                    })
                    addView(TextView(this@ScreenTransactionCaptureService).apply {
                        text = appName
                        setTextColor(Color.rgb(157, 176, 188))
                        textSize = 12f
                    })
                },
                LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
            )
        }

    private fun modePickerOption(
        badge: String,
        title: String,
        subtitle: String,
        onClick: () -> Unit,
    ): View = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        minimumHeight = dp(72)
        setPadding(dp(12), dp(10), dp(12), dp(10))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(10)
        }

        val card = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(16).toFloat()
            setColor(Color.rgb(40, 54, 66))
            setStroke(dp(1), Color.rgb(58, 76, 89))
        }
        background = RippleDrawable(
            ColorStateList.valueOf(Color.argb(55, 255, 255, 255)),
            card,
            null,
        )

        addView(
            TextView(this@ScreenTransactionCaptureService).apply {
                text = badge
                gravity = Gravity.CENTER
                setTextColor(Color.rgb(139, 233, 199))
                textSize = 12f
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(12).toFloat()
                    setColor(Color.rgb(18, 91, 70))
                }
            },
            LinearLayout.LayoutParams(dp(46), dp(46)),
        )

        addView(
            LinearLayout(this@ScreenTransactionCaptureService).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), 0, dp(8), 0)
                addView(TextView(this@ScreenTransactionCaptureService).apply {
                    text = title
                    setTextColor(Color.WHITE)
                    textSize = 15f
                })
                addView(TextView(this@ScreenTransactionCaptureService).apply {
                    text = subtitle
                    setTextColor(Color.rgb(171, 187, 197))
                    textSize = 12f
                    setPadding(0, dp(2), 0, 0)
                })
            },
            LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f),
        )

        addView(TextView(this@ScreenTransactionCaptureService).apply {
            text = "›"
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(151, 171, 183))
            textSize = 26f
        }, LinearLayout.LayoutParams(dp(24), dp(46)))

        setOnClickListener { onClick() }
    }

    private fun modePickerHint(value: String): TextView = TextView(this).apply {
        text = value
        gravity = Gravity.CENTER
        setTextColor(Color.rgb(125, 146, 159))
        textSize = 11f
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
        ).apply {
            topMargin = dp(12)
        }
    }

    private data class DetectedTarget(
        val packageName: String,
        val appName: String,
        val root: AccessibilityNodeInfo?,
    )

    private fun detectCurrentTarget(requireAccessibilityRoot: Boolean): DetectedTarget? {
        val root = rootInActiveWindow
        val rootPackage = root?.packageName?.toString()?.takeIf(String::isNotBlank)
        val packageName = rootPackage ?: lastForegroundPackage ?: return null
        if (packageName == applicationContext.packageName || packageName == SYSTEM_UI_PACKAGE ||
            packageName == SETTINGS_PACKAGE || packageName == homePackageName()
        ) return null
        if (requireAccessibilityRoot && (root == null || rootPackage != packageName)) return null
        val appName = runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
        return DetectedTarget(packageName = packageName, appName = appName, root = root)
    }

    private fun homePackageName(): String? = packageManager.resolveActivity(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
        0,
    )?.activityInfo?.packageName

    private fun captureManualScreen(session: ScreenCaptureSession, mode: ScreenExtractionMode) {
        val target = detectCurrentTarget(requireAccessibilityRoot = mode != ScreenExtractionMode.OCR)
        if (target == null) {
            showToast("Không nhận diện được ứng dụng hoặc XML của màn hình hiện tại")
            removeCaptureButton()
            showCaptureButton(session)
            return
        }
        val cleanedXml = if (mode != ScreenExtractionMode.OCR) {
            target.root?.let { AccessibilityScreenXmlCleaner.clean(it, target.appName).xml }
        } else {
            null
        }
        captureInProgress = true
        removeCaptureButton()

        if (mode == ScreenExtractionMode.ACCESSIBILITY) {
            if (cleanedXml.isNullOrBlank()) {
                captureInProgress = false
                showToast("Không đọc được Accessibility XML trên màn hình này")
                showCaptureButton(session)
                return
            }
            val content = ScreenCaptureXmlComposer.compose(
                packageName = target.packageName,
                appName = target.appName,
                mode = mode,
                accessibilityXml = cleanedXml,
            )
            enqueueManualAnalysis(session, target, mode, content)
            return
        }

        showToast("Đang chụp màn hình và nhận dạng chữ…")
        mainHandler.postDelayed(
            { captureScreenshotAndAnalyze(session, target, mode, cleanedXml) },
            SCREENSHOT_OVERLAY_SETTLE_MILLIS,
        )
    }

    private fun captureScreenshotAndAnalyze(
        session: ScreenCaptureSession,
        target: DetectedTarget,
        mode: ScreenExtractionMode,
        accessibilityXml: String?,
    ) {
        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            mainExecutor,
            object : TakeScreenshotCallback {
                override fun onSuccess(screenshot: ScreenshotResult) {
                    val hardwareBuffer = screenshot.hardwareBuffer
                    val bitmap = try {
                        Bitmap.wrapHardwareBuffer(hardwareBuffer, screenshot.colorSpace)
                            ?.copy(Bitmap.Config.ARGB_8888, false)
                    } finally {
                        hardwareBuffer.close()
                    }
                    if (bitmap == null) {
                        restoreAfterCaptureFailure(session, "Không tạo được ảnh màn hình")
                        return
                    }
                    serviceScope.launch {
                        runCatching { textRecognizer.recognize(bitmap) }
                            .also { bitmap.recycle() }
                            .onSuccess { lines ->
                                if (lines.isEmpty()) {
                                    restoreAfterCaptureFailure(session, "OCR không đọc được chữ trên màn hình")
                                    return@onSuccess
                                }
                                val content = ScreenCaptureXmlComposer.compose(
                                    packageName = target.packageName,
                                    appName = target.appName,
                                    mode = mode,
                                    accessibilityXml = accessibilityXml,
                                    ocrLines = lines,
                                )
                                enqueueManualAnalysis(session, target, mode, content)
                            }
                            .onFailure { error ->
                                restoreAfterCaptureFailure(
                                    session,
                                    "OCR thất bại: ${error.message?.take(120) ?: "lỗi không xác định"}",
                                )
                            }
                    }
                }

                override fun onFailure(errorCode: Int) {
                    val message = when (errorCode) {
                        ERROR_TAKE_SCREENSHOT_NO_ACCESSIBILITY_ACCESS -> "Dịch vụ chưa có quyền chụp màn hình"
                        ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT -> "Hãy chờ một chút rồi thử chụp lại"
                        ERROR_TAKE_SCREENSHOT_SECURE_WINDOW -> "Ứng dụng này chặn chụp màn hình bảo mật"
                        else -> "Không thể chụp màn hình (mã lỗi $errorCode)"
                    }
                    restoreAfterCaptureFailure(session, message)
                }
            },
        )
    }

    private fun restoreAfterCaptureFailure(session: ScreenCaptureSession, message: String) {
        mainHandler.post {
            captureInProgress = false
            showToast(message)
            if (sessionStore.active() != null) showCaptureButton(session)
        }
    }

    private fun enqueueManualAnalysis(
        session: ScreenCaptureSession,
        target: DetectedTarget,
        mode: ScreenExtractionMode,
        screenXml: String,
    ) {
        val now = System.currentTimeMillis()
        val settings = AppSettings.get(applicationContext)
        val prompt = settings.screenPrompt(target.packageName)
        val model = settings.model()
        val token = UUID.randomUUID().toString()
        val eventId = "${ScreenCaptureSessionStore.EVENT_PREFIX}$token"
        val rawPayload = JSONObject().apply {
            put("source", ScreenCaptureSessionStore.SOURCE)
            put("package_name", target.packageName)
            put("app_name", target.appName)
            put("extraction_mode", mode.wireValue)
            put("screen_xml", screenXml)
            put("prompt", prompt)
            put("model", model)
            put("captured_at", now)
        }.toString()
        val entity = CapturedNotificationEntity(
            eventId = eventId,
            notificationKey = eventId,
            packageName = target.packageName,
            appName = target.appName,
            title = "Đọc màn hình giao dịch",
            text = target.packageName,
            expandedContent = screenXml,
            rawPayload = rawPayload,
            postedAt = now,
            capturedAt = now,
        )

        mainHandler.post {
            captureInProgress = false
            removeCaptureButton()
            showCaptureButton(session)
            showToast("Đang phân tích nội dung màn hình…")
        }
        serviceScope.launch {
            val id = MoneyCheckRepository.get(applicationContext).capture(entity)
            if (id > 0) {
                NotificationAnalysisScheduler.enqueue(applicationContext, id)
            } else {
                showToast("Không thể tạo dữ liệu phân tích")
            }
        }
    }

    private fun removeCaptureButton() {
        val view = captureButton ?: return
        captureButton = null
        captureButtonParams = null
        runCatching { windowManager.removeView(view) }
    }

    private fun inspectAutomaticScreen() {
        if (sessionStore.active() != null) return
        val packageName = pendingPackageName ?: return
        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != packageName) return

        val visibleTexts = collectVisibleTexts(root)
        val match = ScreenTransactionExtractor.extract(packageName, visibleTexts) ?: return
        val now = System.currentTimeMillis()
        val fingerprint = "$packageName\n${match.expandedContent}".sha256()
        if (isRecentlyCaptured(fingerprint, now)) return
        rememberCapture(fingerprint, now)

        val entity = CapturedNotificationEntity(
            eventId = "screen:$packageName:$fingerprint:${now / DEDUP_WINDOW_MILLIS}",
            notificationKey = "screen:$packageName:$fingerprint",
            packageName = packageName,
            appName = "Shopee",
            title = match.title,
            text = match.text,
            expandedContent = match.expandedContent,
            rawPayload = JSONObject().apply {
                put("source", "accessibility")
                put("packageName", packageName)
                put("title", match.title)
                put("text", match.text)
                put("expandedContent", match.expandedContent)
                put("matchedTexts", JSONArray(listOf(match.title, match.text)))
                put("capturedAt", now)
            }.toString(),
            postedAt = now,
        )

        serviceScope.launch {
            val id = MoneyCheckRepository.get(applicationContext).capture(entity)
            if (id > 0) NotificationAnalysisScheduler.enqueue(applicationContext, id)
        }
    }

    private fun collectVisibleTexts(root: AccessibilityNodeInfo): List<String> {
        val result = LinkedHashSet<String>()
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)
        var visited = 0
        while (pending.isNotEmpty() && visited < MAX_NODES) {
            val node = pending.removeFirst()
            visited++
            node.text?.toString()?.trim()?.takeIf(String::isNotEmpty)?.let(result::add)
            node.contentDescription?.toString()?.trim()?.takeIf(String::isNotEmpty)?.let(result::add)
            for (index in 0 until node.childCount) node.getChild(index)?.let(pending::addLast)
        }
        return result.toList()
    }

    private fun isRecentlyCaptured(fingerprint: String, now: Long): Boolean {
        val preferences = getSharedPreferences(DEDUP_PREFERENCES, MODE_PRIVATE)
        return preferences.getString(KEY_LAST_FINGERPRINT, null) == fingerprint &&
            now - preferences.getLong(KEY_LAST_CAPTURED_AT, 0L) < DEDUP_WINDOW_MILLIS
    }

    private fun rememberCapture(fingerprint: String, capturedAt: Long) {
        getSharedPreferences(DEDUP_PREFERENCES, MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_FINGERPRINT, fingerprint)
            .putLong(KEY_LAST_CAPTURED_AT, capturedAt)
            .apply()
    }

    private fun showToast(message: String) {
        mainHandler.post { Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show() }
    }

    private fun overlayLayoutParams(width: Int): WindowManager.LayoutParams = WindowManager.LayoutParams(
        width,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun String.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(toByteArray(Charsets.UTF_8))
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    companion object {
        private var connectedService: WeakReference<ScreenTransactionCaptureService>? = null
        private const val SCREEN_SETTLE_DELAY_MILLIS = 700L
        private const val SCREENSHOT_OVERLAY_SETTLE_MILLIS = 220L
        private const val DEDUP_WINDOW_MILLIS = 15L * 60L * 1_000L
        private const val MAX_NODES = 1_500
        private const val DEDUP_PREFERENCES = "screen_transaction_capture"
        private const val KEY_LAST_FINGERPRINT = "last_fingerprint"
        private const val KEY_LAST_CAPTURED_AT = "last_captured_at"
        private const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        private const val SETTINGS_PACKAGE = "com.android.settings"

        fun refreshManualSession() {
            val service = connectedService?.get() ?: return
            service.mainHandler.post {
                service.sessionStore.active()?.let(service::showCaptureButton)
            }
        }
    }
}
