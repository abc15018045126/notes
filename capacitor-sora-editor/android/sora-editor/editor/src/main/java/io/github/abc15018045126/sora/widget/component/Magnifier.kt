package io.github.abc15018045126.sora.widget.component

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.PixelCopy
import android.view.View
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.annotation.FloatRange
import androidx.annotation.Px
import androidx.annotation.RequiresApi
import io.github.abc15018045126.sora.R
import io.github.abc15018045126.sora.event.ColorSchemeUpdateEvent
import io.github.abc15018045126.sora.widget.CodeEditor
import io.github.abc15018045126.sora.widget.schemes.EditorColorScheme

/**
 * Magnifier specially designed for CodeEditor
 *
 * @author abc15018045126
 */
class Magnifier(private val view: CodeEditor) : EditorBuiltinComponent {

    private val eventManager = view.createSubEventManager()
    private val popup = PopupWindow()
    private val image: ImageView
    private val paint = Paint()
    private var maxTextSize: Float
    private var x = 0
    private var y = 0
    override var isEnabled = true
        set(value) {
            field = value
            if (!value) {
                dismiss()
            }
        }
    private var isWithinEditorForcibly = false
    private var parentView: View = view

    /**
     * Scale factor for regions
     */
    private var scaleFactor = 1.25f

    init {
        popup.elevation = view.dpUnit * 4
        @SuppressLint("InflateParams")
        val contentView = LayoutInflater.from(view.context).inflate(R.layout.magnifier_popup, null)
        image = contentView.findViewById(R.id.magnifier_image_view)
        popup.height = (view.dpUnit * 70).toInt()
        popup.width = (view.dpUnit * 100).toInt()
        popup.contentView = contentView
        maxTextSize = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            28f,
            contentView.resources.displayMetrics
        )
        applyBackgroundTint()
        eventManager.subscribeAlways(ColorSchemeUpdateEvent::class.java) {
            applyBackgroundTint()
        }
    }

    private fun applyBackgroundTint() {
        val currentContent = popup.contentView ?: return
        val background = currentContent.background
        if (background != null) {
            @Suppress("DEPRECATION")
            background.setTint(view.colorScheme.getColor(EditorColorScheme.WHOLE_BACKGROUND))
        }
    }

    /**
     * @see setParentView
     */
    fun getParentView(): View {
        return parentView
    }

    /**
     * Set parent view for popup
     */
    fun setParentView(parentView: View) {
        this.parentView = parentView
    }

    /**
     * Get the scale factor of the image to be displayed in magnifier
     */
    @FloatRange(from = 1.0, fromInclusive = false)
    fun getScaleFactor(): Float {
        return scaleFactor
    }

    /**
     * Set the scale factor of the image to be displayed in magnifier
     *
     * @param scaleFactor Scale factor. Must not be under 1.0
     */
    fun setScaleFactor(@FloatRange(from = 1.0, fromInclusive = false) scaleFactor: Float) {
        if (scaleFactor <= 1.0f) {
            throw IllegalArgumentException("factor can not be under 1.0")
        }
        this.scaleFactor = scaleFactor
    }

    /**
     * Set the max text size to show the magnifier
     *
     * @param maxTextSize Text size in px
     */
    fun setMaxTextSize(@Px maxTextSize: Float) {
        this.maxTextSize = maxTextSize
    }

    /**
     * @return Text size in px
     */
    @Px
    fun getMaxTextSize(): Float {
        return maxTextSize
    }

    fun isWithinEditorForcibly(): Boolean {
        return isWithinEditorForcibly
    }

    /**
     * If true, the magnifier will never try to copy pixels by system and create the image by
     * editor.
     * If you are trying to add the view into an activity by WindowManager, this should be enabled.
     * Otherwise, the generated image may be wrong.
     */
    fun setWithinEditorForcibly(withinEditorForcibly: Boolean) {
        this.isWithinEditorForcibly = withinEditorForcibly
    }

    /**
     * Show the magnifier according to the given position.
     * X and Y are relative to the code editor view
     */
    fun show(x: Int, y: Int) {
        if (!isEnabled) {
            return
        }
        if (Math.abs(x - this.x) < 2 && Math.abs(y - this.y) < 2) {
            return
        }
        if (view.textSizePx > maxTextSize) {
            if (isShowing()) {
                dismiss()
            }
            return
        }
        popup.width = Math.min(view.width * 3 / 5, (view.dpUnit * 250).toInt())
        this.x = x
        this.y = y
        val pos = IntArray(2)
        view.getLocationInWindow(pos)
        var left = Math.max(pos[0] + x - popup.width / 2, 0)
        var right = left + popup.width
        if (right > view.width + pos[0]) {
            right = view.width + pos[0]
            left = Math.max(0, right - popup.width)
        }
        val top = Math.max(pos[1] + y - popup.height - view.rowHeight, 0)
        if (popup.isShowing) {
            popup.update(left, top, popup.width, popup.height)
        } else {
            popup.showAtLocation(parentView, Gravity.START or Gravity.TOP, left, top)
        }
        updateDisplay()
    }

    /**
     * Whether the magnifier is showing
     */
    fun isShowing(): Boolean {
        return popup.isShowing
    }

    /**
     * Hide the magnifier
     */
    fun dismiss() {
        popup.dismiss()
    }

    /**
     * Update the display of the magnifier without updating the window's
     * location on screen.
     */
    fun updateDisplay() {
        if (!isShowing()) {
            return
        }
        if (!isWithinEditorForcibly && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPixelCopyApplicable()) {
            updateDisplayOreo(view.context as Activity)
        } else {
            updateDisplayWithinEditor()
        }
    }

    /**
     * Check if [PixelCopy] is applicable in current view context
     */
    private fun isPixelCopyApplicable(): Boolean {
        val ctx = view.context
        if (ctx !is Activity) {
            return false
        }
        val localWndId = view.windowId
        val activityWnd = ctx.window ?: return false
        val activityWndId = activityWnd.decorView.windowId
        return localWndId != null && localWndId == activityWndId
    }

    /**
     * Update display on API 26 or later.
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun updateDisplayOreo(activity: Activity) {
        val requiredWidth = (popup.width / scaleFactor).toInt()
        val requiredHeight = (popup.height / scaleFactor).toInt()

        var left = Math.max(x - requiredWidth / 2, 0)
        var top = Math.max(y - requiredHeight / 2, 0)
        val right = Math.min(left + requiredWidth, view.width)
        val bottom = Math.min(top + requiredHeight, view.height)
        if (right - left < requiredWidth) {
            left = Math.max(0, right - requiredWidth)
        }
        if (bottom - top < requiredHeight) {
            top = Math.max(0, bottom - requiredHeight)
        }
        if (right - left <= 0 || bottom - top <= 0) {
            dismiss()
            return
        }
        val pos = IntArray(2)
        view.getLocationInWindow(pos)
        val clip = Bitmap.createBitmap(right - left, bottom - top, Bitmap.Config.ARGB_8888)
        try {
            PixelCopy.request(
                activity.window,
                Rect(pos[0] + left, pos[1] + top, pos[0] + right, pos[1] + bottom),
                clip,
                { statusCode ->
                    if (statusCode == PixelCopy.SUCCESS) {
                        val dest = Bitmap.createBitmap(popup.width, popup.height, Bitmap.Config.ARGB_8888)
                        val scaled = Bitmap.createScaledBitmap(clip, popup.width, popup.height, true)
                        clip.recycle()

                        val canvas = Canvas(dest)
                        paint.reset()
                        paint.isAntiAlias = true
                        canvas.drawARGB(0, 0, 0, 0)
                        val roundFactor = 6
                        canvas.drawRoundRect(
                            0f,
                            0f,
                            popup.width.toFloat(),
                            popup.height.toFloat(),
                            view.dpUnit * roundFactor,
                            view.dpUnit * roundFactor,
                            paint
                        )
                        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
                        canvas.drawBitmap(scaled, 0f, 0f, paint)
                        scaled.recycle()

                        image.setImageBitmap(dest)
                    } else {
                        Log.w("Magnifier", "Failed to copy pixels, error = $statusCode")
                    }
                },
                view.handler
            )
        } catch (e: IllegalArgumentException) {
            dismiss()
            if (!clip.isRecycled) {
                clip.recycle()
            }
        }
    }

    /**
     * Update display on low API devices
     */
    private fun updateDisplayWithinEditor() {
        if (popup.width <= 0 || popup.height <= 0) {
            dismiss()
            return
        }
        val dest = Bitmap.createBitmap(popup.width, popup.height, Bitmap.Config.ARGB_8888)
        val requiredWidth = (popup.width / scaleFactor).toInt()
        val requiredHeight = (popup.height / scaleFactor).toInt()

        var left = Math.max(x - requiredWidth / 2, 0)
        var top = Math.max(y - requiredHeight / 2, 0)
        val right = Math.min(left + requiredWidth, view.width)
        val bottom = Math.min(top + requiredHeight, view.height)
        if (right - left < requiredWidth) {
            left = Math.max(0, right - requiredWidth)
        }
        if (bottom - top < requiredHeight) {
            top = Math.max(0, bottom - requiredHeight)
        }
        if (right - left <= 0 || bottom - top <= 0) {
            dismiss()
            dest.recycle()
            return
        }
        val clip = Bitmap.createBitmap(requiredWidth, requiredHeight, Bitmap.Config.ARGB_8888)
        val viewCanvas = Canvas(clip)
        viewCanvas.translate(-left.toFloat() - view.offsetX, -top.toFloat() - view.offsetY)
        view.draw(viewCanvas)
        val scaled = Bitmap.createScaledBitmap(clip, popup.width, popup.height, true)
        clip.recycle()

        val canvas = Canvas(dest)
        paint.reset()
        paint.isAntiAlias = true
        canvas.drawARGB(0, 0, 0, 0)
        val roundFactor = 6
        canvas.drawRoundRect(
            0f,
            0f,
            popup.width.toFloat(),
            popup.height.toFloat(),
            view.dpUnit * roundFactor,
            view.dpUnit * roundFactor,
            paint
        )
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        scaled.recycle()

        image.setImageBitmap(dest)
    }

}
