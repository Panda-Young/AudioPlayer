package com.panda.audioplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class EffectManager(private val exoPlayerManager: ExoPlayerManager) {
    private val effects = mutableMapOf(
        "Gain" to false,
        "Equalizer" to false,
        "3D Audio" to false
    )

    fun toggleEffect(effectName: String) {
        effects[effectName] = effects[effectName]?.not() ?: false
        applyEffect(effectName)
    }

    private fun applyEffect(effectName: String) {
        when (effectName) {
            "Gain" -> exoPlayerManager.setGainEffect(effects[effectName] == true)
            "Equalizer" -> exoPlayerManager.setEqualizerEffect(effects[effectName] == true)
            "3D Audio" -> exoPlayerManager.set3DEffect(effects[effectName] == true)
        }
    }

    fun getEffectState() = effects.toMap()
}

class EffectAdapter(
    private val effectManager: EffectManager,
    private val onEffectToggled: (String, Boolean) -> Unit
) : RecyclerView.Adapter<EffectAdapter.EffectViewHolder>() {

    private val effectItems by lazy { 
        effectManager.getEffectState().keys.toList() 
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
        val isEnabled = effectManager.getEffectState()[effectName] == true

        holder.effectName.text = effectName

        holder.effectSwitch.setImageResource(
            if (isEnabled) R.drawable.ic_switch_on 
            else R.drawable.ic_switch_off
        )

        holder.effectSwitch.setOnClickListener {
            effectManager.toggleEffect(effectName)
            notifyItemChanged(position)
            onEffectToggled(effectName, !isEnabled)
        }
    }

    override fun getItemCount() = effectItems.size
}
