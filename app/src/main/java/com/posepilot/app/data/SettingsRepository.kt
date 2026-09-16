package com.posepilot.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.posepilot.app.models.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val VOICE = booleanPreferencesKey("voice")
        val SKELETON = booleanPreferencesKey("skeleton")
        val TARGET = booleanPreferencesKey("target")
        val PERCENT = booleanPreferencesKey("percent")
        val AUTO = booleanPreferencesKey("auto_capture")
        val COUNTDOWN = intPreferencesKey("countdown")
        val INTERVAL = longPreferencesKey("interval")
        val MIRROR = booleanPreferencesKey("mirror")
        val FRONT = booleanPreferencesKey("front")
    }

    val settings: Flow<UserSettings> = context.settingsStore.data.map { read(it) }

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        context.settingsStore.edit { p ->
            val s = transform(read(p))
            p[Keys.VOICE] = s.voiceGuidance
            p[Keys.SKELETON] = s.showSkeleton
            p[Keys.TARGET] = s.showTargetPose
            p[Keys.PERCENT] = s.showMatchPercentage
            p[Keys.AUTO] = s.autoCapture
            p[Keys.COUNTDOWN] = s.countdownSeconds
            p[Keys.INTERVAL] = s.instructionIntervalMs
            p[Keys.MIRROR] = s.mirrorFrontCamera
            p[Keys.FRONT] = s.useFrontCamera
        }
    }

    private fun read(p: Preferences): UserSettings {
        val d = UserSettings()
        return UserSettings(
            voiceGuidance = p[Keys.VOICE] ?: d.voiceGuidance,
            showSkeleton = p[Keys.SKELETON] ?: d.showSkeleton,
            showTargetPose = p[Keys.TARGET] ?: d.showTargetPose,
            showMatchPercentage = p[Keys.PERCENT] ?: d.showMatchPercentage,
            autoCapture = p[Keys.AUTO] ?: d.autoCapture,
            countdownSeconds = p[Keys.COUNTDOWN] ?: d.countdownSeconds,
            instructionIntervalMs = p[Keys.INTERVAL] ?: d.instructionIntervalMs,
            mirrorFrontCamera = p[Keys.MIRROR] ?: d.mirrorFrontCamera,
            useFrontCamera = p[Keys.FRONT] ?: d.useFrontCamera,
        )
    }
}
