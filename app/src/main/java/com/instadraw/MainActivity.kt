package com.instadraw

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.widget.*
import kotlin.math.abs

class Traced(val w: Int, val h: Int, val strokes: List<List<IntArray>>)

class MainActivity : Activity() {
    private var bmp: Bitmap? = null
    private var traced: Traced? = null
    private lateinit var info: TextView
    private lateinit var img: ImageView
    private lateinit var detailBar: SeekBar
    private lateinit var sizeBar: SeekBar

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(40, 40, 40, 40)

        fun btn(t: String, f: () -> Unit) {
            val b = Button(this)
            b.text = t
            b.setOnClickListener { f() }
            root.addView(b)
        }
        fun label(t: String) {
            val v = TextView(this)
            v.text = t
            root.addView(v)
        }

        btn("1. Turn on accessibility") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        btn("2. Pick image") {
            startActivityForResult(Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), 1)
        }
        label("Detail (right = more lines)")
        detailBar = SeekBar(this)
        detailBar.max = 90
        detailBar.progress = 60
        root.addView(detailBar)
        label("Size")
        sizeBar = SeekBar(this)
        sizeBar.max = 70
        sizeBar.progress = 55
        root.addView(sizeBar)
        info = TextView(this)
        root.addView(info)
        img = ImageView(this)
        root.addView(img, LinearLayout.LayoutParams(-1, 700))
        btn("3. Start (draws after 10 s)") { start() }
        btn("Stop") { DrawService.instance?.cancel() }

        detailBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, u: Boolean) {}
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) { refresh() }
        })

        val sv = ScrollView(this)
        sv.addView(root)
        setContentView(sv)
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        val uri = data?.data
        if (res == RESULT_OK && uri != null) {
            bmp = load(uri)
            refresh()
        }
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_LONG).show()

    private fun load(uri: Uri): Bitmap {
        val o = BitmapFactory.Options()
        o.inJustDecodeBounds = true
        contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, o) }
        var s = 1
        while (maxOf(o.outWidth, o.outHeight) / s > 1200) s *= 2
        val o2 = BitmapFactory.Options()
        o2.inSampleSize = s
        return contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, o2)!! }
    }

    private fun refresh() {
        val b = bmp ?: return
        val thr = 120 - detailBar.progress
        info.text = "Working..."
        Thread {
            val t = trace(b, thr)
            val pv = preview(t)
            runOnUiThread {
                traced = t
                img.setImageBitmap(pv)
                val ms = t.strokes.sumOf { it.size * 8L + 30L }
                info.text = "${t.strokes.size} strokes, about ${ms / 1000} s"
            }
        }.start()
    }

    private fun trace(src: Bitmap, thr: Int): Traced {
        val sc = 260f / maxOf(src.width, src.height)
        val w = maxOf(2, (src.width * sc).toInt())
        val h = maxOf(2, (src.height * sc).toInt())
        val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(bm)
        cv.drawColor(Color.WHITE)
        cv.drawBitmap(src, null, Rect(0, 0, w, h), Paint(Paint.FILTER_BITMAP_FLAG))
        val px = IntArray(w * h)
        bm.getPixels(px, 0, w, 0, 0, w, h)
        val g = IntArray(w * h) {
            val c = px[it]
            (Color.red(c) * 3 + Color.green(c) * 6 + Color.blue(c)) / 10
        }
        val edge = BooleanArray(w * h)
        for (y in 0 until h - 1) for (x in 0 until w - 1) {
            val i = y * w + x
            edge[i] = abs(g[i + 1] - g[i]) + abs(g[i + w] - g[i]) > thr
        }
        val vis = BooleanArray(w * h)
        val dx = intArrayOf(1, 0, -1, 0, 1, -1, 1, -1)
        val dy = intArrayOf(0, 1, 0, -1, 1, 1, -1, -1)
        val out = ArrayList<List<IntArray>>()
        for (y in 0 until h) for (x in 0 until w) {
            if (!edge[y * w + x] || vis[y * w + x]) continue
            val path = ArrayList<IntArray>()
            var cx = x
            var cy = y
            vis[cy * w + cx] = true
            path.add(intArrayOf(cx, cy))
            while (true) {
                var moved = false
                for (k in 0 until 8) {
                    val nx = cx + dx[k]
                    val ny = cy + dy[k]
                    if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue
                    val j = ny * w + nx
                    if (edge[j] && !vis[j]) {
                        vis[j] = true
                        cx = nx
                        cy = ny
                        path.add(intArrayOf(cx, cy))
                        moved = true
                        break
                    }
                }
                if (!moved) break
            }
            if (path.size >= 5) {
                val sp = ArrayList<IntArray>()
                for (i in path.indices step 2) sp.add(path[i])
                if ((path.size - 1) % 2 != 0) sp.add(path.last())
                out.add(sp)
            }
        }
        return Traced(w, h, out)
    }

    private fun preview(t: Traced): Bitmap {
        val b = Bitmap.createBitmap(t.w * 3, t.h * 3, Bitmap.Config.ARGB_8888)
        val c = Canvas(b)
        c.drawColor(Color.WHITE)
        val p = Paint()
        p.color = Color.BLACK
        p.strokeWidth = 2f
        p.isAntiAlias = true
        for (s in t.strokes) for (i in 1 until s.size)
            c.drawLine(s[i - 1][0] * 3f, s[i - 1][1] * 3f, s[i][0] * 3f, s[i][1] * 3f, p)
        return b
    }

    private fun start() {
        val t = traced
        val svc = DrawService.instance
        if (t == null) { toast("Pick an image first"); return }
        if (svc == null) { toast("Turn on InstaDraw in Accessibility settings first"); return }
        val dm = DisplayMetrics()
        windowManager.defaultDisplay.getRealMetrics(dm)
        val sw = dm.widthPixels.toFloat()
        val sh = dm.heightPixels.toFloat()
        val maxW = sw * (sizeBar.progress + 30) / 100f
        val maxH = sh * 0.7f
        val sc = minOf(maxW / t.w, maxH / t.h)
        val ox = (sw - t.w * sc) / 2
        val oy = sh * 0.15f + (maxH - t.h * sc) / 2
        val pts = t.strokes.map { s -> s.map { floatArrayOf(ox + it[0] * sc, oy + it[1] * sc) } }
        svc.draw(pts, 10000)
        toast("Open Instagram's draw screen now. Starting in 10 s")
        moveTaskToBack(true)
    }
}
