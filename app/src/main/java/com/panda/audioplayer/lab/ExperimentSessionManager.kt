package com.panda.audioplayer.lab

import android.content.Context
import com.panda.audioplayer.utils.Logger
import org.json.JSONArray
import org.json.JSONObject

data class ExperimentEvent(
    val pluginId: String,
    val enabled: Boolean,
    val timestamp: Long
)

data class ExperimentReport(
    val sessionId: String,
    val source: String,
    val startedAt: Long,
    val endedAt: Long,
    val events: List<ExperimentEvent>
)

class ExperimentSessionManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private var sessionId: String? = null
    private var source: String? = null
    private var startedAt: Long = 0L
    private val events = mutableListOf<ExperimentEvent>()

    fun startSession(source: String?) {
        sessionId = "session_${System.currentTimeMillis()}"
        this.source = source ?: "unknown"
        startedAt = System.currentTimeMillis()
        events.clear()
        Logger.logi("Experiment session started: $sessionId source=${this.source}")
    }

    fun recordToggle(pluginId: String, enabled: Boolean) {
        if (sessionId == null) return
        events += ExperimentEvent(pluginId, enabled, System.currentTimeMillis())
    }

    fun finishSession() {
        val id = sessionId ?: return
        val report = ExperimentReport(
            sessionId = id,
            source = source ?: "unknown",
            startedAt = startedAt,
            endedAt = System.currentTimeMillis(),
            events = events.toList()
        )
        appendReport(report)
        Logger.logi("Experiment session finished: $id, events=${events.size}")
        sessionId = null
        source = null
        startedAt = 0L
        events.clear()
    }

    fun recentReports(limit: Int = 10): List<ExperimentReport> {
        val raw = prefs.getString(KEY_REPORTS, "[]") ?: "[]"
        val array = JSONArray(raw)
        val result = mutableListOf<ExperimentReport>()
        for (i in array.length() - 1 downTo 0) {
            if (result.size >= limit) break
            val item = array.optJSONObject(i) ?: continue
            result += fromJson(item)
        }
        return result
    }

    private fun appendReport(report: ExperimentReport) {
        val raw = prefs.getString(KEY_REPORTS, "[]") ?: "[]"
        val array = JSONArray(raw)
        array.put(toJson(report))
        prefs.edit().putString(KEY_REPORTS, array.toString()).apply()
    }

    private fun toJson(report: ExperimentReport): JSONObject {
        val eventsArray = JSONArray()
        report.events.forEach {
            eventsArray.put(
                JSONObject()
                    .put("pluginId", it.pluginId)
                    .put("enabled", it.enabled)
                    .put("timestamp", it.timestamp)
            )
        }
        return JSONObject()
            .put("sessionId", report.sessionId)
            .put("source", report.source)
            .put("startedAt", report.startedAt)
            .put("endedAt", report.endedAt)
            .put("events", eventsArray)
    }

    private fun fromJson(json: JSONObject): ExperimentReport {
        val eventsArray = json.optJSONArray("events") ?: JSONArray()
        val events = mutableListOf<ExperimentEvent>()
        for (i in 0 until eventsArray.length()) {
            val item = eventsArray.optJSONObject(i) ?: continue
            events += ExperimentEvent(
                pluginId = item.optString("pluginId", "unknown"),
                enabled = item.optBoolean("enabled", false),
                timestamp = item.optLong("timestamp", 0L)
            )
        }
        return ExperimentReport(
            sessionId = json.optString("sessionId", "unknown"),
            source = json.optString("source", "unknown"),
            startedAt = json.optLong("startedAt", 0L),
            endedAt = json.optLong("endedAt", 0L),
            events = events
        )
    }

    companion object {
        private const val PREF_NAME = "experiment_sessions"
        private const val KEY_REPORTS = "reports"
    }
}
