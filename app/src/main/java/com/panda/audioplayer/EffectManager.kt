package com.panda.audioplayer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.panda.audioplayer.lab.ExperimentSessionManager
import com.panda.audioplayer.plugins.AudioAlgorithmPlugin
import com.panda.audioplayer.plugins.PluginRegistry

data class PluginUiModel(
    val id: String,
    val displayName: String,
    val description: String,
    val version: String,
    val enabled: Boolean
)

class EffectManager(
    context: Context,
    private val exoPlayerManager: ExoPlayerManager,
    private val sessionManager: ExperimentSessionManager
) {
    private val plugins: List<AudioAlgorithmPlugin> = PluginRegistry(exoPlayerManager).defaultPlugins()
    private val prefs = context.applicationContext.getSharedPreferences("plugin_states", Context.MODE_PRIVATE)

    init {
        restorePluginState()
    }

    fun getPlugins(): List<PluginUiModel> {
        return plugins.map {
            PluginUiModel(
                id = it.id,
                displayName = it.displayName,
                description = it.description,
                version = it.version,
                enabled = it.isEnabled()
            )
        }
    }

    fun startExperimentSession(source: String?) {
        sessionManager.startSession(source)
    }

    fun endExperimentSession() {
        sessionManager.finishSession()
    }

    fun recentSessionSummary(limit: Int = 3): String {
        val reports = sessionManager.recentReports(limit)
        if (reports.isEmpty()) return "暂无实验会话记录"
        return reports.joinToString(separator = "\n") {
            "${it.sessionId.takeLast(6)} | events=${it.events.size} | source=${it.source.take(18)}"
        }
    }

    fun toggleEffectByName(effectName: String): Boolean {
        val plugin = plugins.firstOrNull { it.displayName == effectName } ?: return false
        val newState = !plugin.isEnabled()
        plugin.setEnabled(newState)
        persistPluginState(plugin.id, newState)
        sessionManager.recordToggle(plugin.id, newState)
        return newState
    }

    fun isEnabledByName(effectName: String): Boolean {
        return plugins.firstOrNull { it.displayName == effectName }?.isEnabled() ?: false
    }

    private fun restorePluginState() {
        plugins.forEach { plugin ->
            val enabled = prefs.getBoolean(plugin.id, false)
            plugin.setEnabled(enabled)
        }
    }

    private fun persistPluginState(pluginId: String, enabled: Boolean) {
        prefs.edit().putBoolean(pluginId, enabled).apply()
    }
}

class EffectAdapter(
    private val effectManager: EffectManager,
    private val onEffectToggled: (String, Boolean) -> Unit
) : RecyclerView.Adapter<EffectAdapter.EffectViewHolder>() {

    private val effectItems by lazy {
        effectManager.getPlugins().map { it.displayName }
    }

    inner class EffectViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val effectName: TextView = view.findViewById(R.id.effect_name)
        val effectSwitch: ImageView = view.findViewById(R.id.effect_switch)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EffectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.effect_list_item, parent, false)
        return EffectViewHolder(view)
    }

    override fun onBindViewHolder(holder: EffectViewHolder, position: Int) {
        val effectName = effectItems[position]
        val isEnabled = effectManager.isEnabledByName(effectName)

        holder.effectName.text = effectName

        holder.effectSwitch.setImageResource(
            if (isEnabled) R.drawable.ic_switch_on 
            else R.drawable.ic_switch_off
        )

        holder.effectSwitch.setOnClickListener {
            val newState = effectManager.toggleEffectByName(effectName)
            notifyItemChanged(position)
            onEffectToggled(effectName, newState)
        }
    }

    override fun getItemCount() = effectItems.size
}
