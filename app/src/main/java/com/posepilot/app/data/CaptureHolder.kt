package com.posepilot.app.data

import android.graphics.Bitmap

/**
 * Keeps the last captured photo in memory between the camera and review screens.
 * Nothing touches storage until the user explicitly saves.
 *
 * Bitmaps are deliberately NOT recycled here: the review screen may still be drawing or analysing the
 * previous capture on a background thread, and recycling under it would crash. On API 29+ bitmap pixel
 * memory is released by the GC as soon as the last reference is dropped.
 */
class CaptureHolder {
    data class Capture(val bitmap: Bitmap, val templateId: String?)

    @Volatile var current: Capture? = null
        private set

    fun set(bitmap: Bitmap, templateId: String?) {
        current = Capture(bitmap, templateId)
    }

    fun clear() {
        current = null
    }
}
