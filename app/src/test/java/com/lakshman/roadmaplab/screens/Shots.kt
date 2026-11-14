package com.lakshman.roadmaplab.screens

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import java.io.File
import java.io.FileOutputStream

/** Saves what an activity shows as a PNG in app/build/screens (CI copies them to the ci-output branch). */
object Shots {
    fun save(activity: Activity, name: String) {
        val view = activity.window.decorView.rootView
        val bmp = Bitmap.createBitmap(view.width.coerceAtLeast(1), view.height.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        val dir = File("build/screens").apply { mkdirs() }
        FileOutputStream(File(dir, "$name.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
