package com.posepilot.app.data

import android.content.Context
import com.posepilot.app.gallery.PhotoStore
import com.posepilot.app.pose.detection.StillImagePoseDetector

/** Manual dependency container — enough for an MVP without a DI framework. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val settings = SettingsRepository(appContext)
    val templates = TemplateRepository(appContext)
    val captures = CaptureHolder()
    val photos = PhotoStore(appContext)
    val stillDetector: StillImagePoseDetector by lazy { StillImagePoseDetector() }
}
