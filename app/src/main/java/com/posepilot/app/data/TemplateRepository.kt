package com.posepilot.app.data

import android.content.Context
import com.posepilot.app.models.FramingType
import com.posepilot.app.models.LandmarkType
import com.posepilot.app.pose.landmarks.NormalizedPose
import com.posepilot.app.targetpose.PoseCategory
import com.posepilot.app.targetpose.PoseLibrary
import com.posepilot.app.targetpose.PoseTemplate
import com.posepilot.app.targetpose.TemplateFactory
import com.posepilot.app.targetpose.TemplateSource
import com.posepilot.app.utilities.Vec2
import com.posepilot.app.utilities.Vec3
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Library poses + reference poses the user saved.
 * Privacy: only the normalized skeleton of a reference is stored, never the reference photo itself.
 */
class TemplateRepository(context: Context) {
    private val dir = File(context.filesDir, "references").apply { mkdirs() }
    private val _references = MutableStateFlow<List<PoseTemplate>>(emptyList())
    val references: StateFlow<List<PoseTemplate>> = _references

    /** Unsaved reference currently being coached (e.g. before the user saves it). */
    @Volatile private var transient: PoseTemplate? = null

    fun all(): List<PoseTemplate> = PoseLibrary.all + _references.value

    fun byId(id: String?): PoseTemplate? {
        if (id == null) return null
        transient?.let { if (it.id == id) return it }
        return PoseLibrary.byId(id) ?: _references.value.firstOrNull { it.id == id }
    }

    fun setTransient(template: PoseTemplate) { transient = template }

    suspend fun load() = withContext(Dispatchers.IO) {
        val loaded = dir.listFiles { f -> f.extension == "json" }.orEmpty()
            .sortedByDescending { it.lastModified() }
            .mapNotNull { runCatching { fromJson(JSONObject(it.readText())) }.getOrNull() }
        _references.value = loaded
    }

    suspend fun save(template: PoseTemplate) = withContext(Dispatchers.IO) {
        File(dir, "${template.id}.json").writeText(toJson(template).toString())
        _references.value = listOf(template) + _references.value.filterNot { it.id == template.id }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        File(dir, "$id.json").delete()
        _references.value = _references.value.filterNot { it.id == id }
    }

    private fun toJson(t: PoseTemplate): JSONObject = JSONObject().apply {
        put("id", t.id)
        put("name", t.name)
        put("framing", t.framing.name)
        put("features", JSONArray(t.rules.map { it.feature.name }))
        put("points", JSONObject().apply {
            t.targetPose.points.forEach { (type, p) ->
                put(type.name, JSONArray(listOf(p.x, p.y, p.z)))
            }
        })
        put("visible", JSONArray(t.targetPose.visible.map { it.name }))
    }

    private fun fromJson(o: JSONObject): PoseTemplate {
        val pointsObj = o.getJSONObject("points")
        val points = HashMap<LandmarkType, Vec3>()
        pointsObj.keys().forEach { key ->
            val arr = pointsObj.getJSONArray(key)
            points[LandmarkType.valueOf(key)] = Vec3(arr.getDouble(0), arr.getDouble(1), arr.getDouble(2))
        }
        val visibleArr = o.getJSONArray("visible")
        val visible = (0 until visibleArr.length()).map { LandmarkType.valueOf(visibleArr.getString(it)) }.toSet()
        val featuresArr = o.getJSONArray("features")
        val features = (0 until featuresArr.length())
            .map { com.posepilot.app.pose.analysis.PoseFeature.valueOf(featuresArr.getString(it)) }.toSet()
        val framing = FramingType.valueOf(o.getString("framing"))
        return TemplateFactory.create(
            id = o.getString("id"), name = o.getString("name"), category = PoseCategory.REFERENCE,
            description = "Pose extracted from your reference photo.", framing = framing,
            pose = NormalizedPose(points, visible, Vec2.ZERO, 1.0), features = features,
            source = TemplateSource.REFERENCE_PHOTO, toleranceScale = 1.2,
        )
    }
}
