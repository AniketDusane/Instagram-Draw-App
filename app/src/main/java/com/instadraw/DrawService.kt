package com.instadraw

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class DrawService : AccessibilityService() {
    companion object {
        var instance: DrawService? = null
    }

    private val h = Handler(Looper.getMainLooper())
    private var gen = 0

    override fun onServiceConnected() { instance = this }
    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
    override fun onDestroy() { instance = null; super.onDestroy() }

    fun cancel() { gen++; h.removeCallbacksAndMessages(null) }

    fun draw(strokes: List<List<FloatArray>>, delayMs: Long) {
        cancel()
        val g = gen
        h.postDelayed({ step(strokes, 0, g) }, delayMs)
    }

    private fun step(strokes: List<List<FloatArray>>, from: Int, g: Int) {
        if (g != gen || from >= strokes.size) return
        val b = GestureDescription.Builder()
        var t = 0L
        var n = 0
        var j = from
        while (j < strokes.size && n < 15 && t < 20000) {
            val s = strokes[j]
            val p = Path()
            p.moveTo(s[0][0], s[0][1])
            for (k in 1 until s.size) p.lineTo(s[k][0], s[k][1])
            val d = maxOf(20L, s.size * 8L)
            b.addStroke(GestureDescription.StrokeDescription(p, t, d))
            t += d + 30
            n++
            j++
        }
        val next = j
        val cb = object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(d: GestureDescription?) { step(strokes, next, g) }
            override fun onCancelled(d: GestureDescription?) { step(strokes, next, g) }
        }
        dispatchGesture(b.build(), cb, h)
    }
}
