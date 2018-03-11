package com.faltenreich.skeletonview

import android.graphics.*
import android.support.annotation.ColorInt
import android.view.View
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import java.util.*

class SkeletonShimmer(
        private val view: View,
        @ColorInt private val shimmerColor: Int,
        private val shimmerDurationInMillis: Long,
        private val shimmerAngle: Float,
        private val shimmerWidth: Float
) {

    var bitmap: Bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ALPHA_8)
    var canvas: Canvas = Canvas(bitmap)
    var paint: Paint = Paint().apply {
        shader = LinearGradient(0f, 0f, shimmerWidth, shimmerAngle, Color.argb(255, 255, 255, 255), shimmerColor, Shader.TileMode.MIRROR)
        isAntiAlias = true
    }

    private var coroutine: Job? = null
    private val shimmerRefreshInterval by lazy { (1000f / view.context.refreshRateInSeconds()).toInt() }

    fun start() {
        if (!isShimmering()) {
            coroutine = launch(UI) {
                while (isActive) {
                    updateShimmer()
                    delay(shimmerRefreshInterval)
                }
            }
        }
    }

    fun stop() {
        if (isShimmering()) {
            coroutine?.cancel()
            coroutine = null
        }
    }

    private fun isShimmering() = coroutine != null

    // Offset is time-dependent to support synchronization between uncoupled views
    private fun shimmerOffset(): Float {
        val now = Calendar.getInstance()
        val millis = (now.timeInMillis - now.withTimeAtStartOfDay().timeInMillis)

        val current = millis.toDouble()
        val interval = shimmerDurationInMillis
        val divisor = Math.floor(current / interval)
        val start = interval * divisor
        val end = start + interval
        val percentage = (current - start) / (end - start)

        return (shimmerWidth * percentage).toFloat()
    }

    private fun updateShimmer() {
        val offset = shimmerOffset()
        paint.shader = LinearGradient(offset, 0f, offset + shimmerWidth, shimmerAngle, shimmerColor, Color.TRANSPARENT, Shader.TileMode.REPEAT)
        view.invalidate()
    }
}