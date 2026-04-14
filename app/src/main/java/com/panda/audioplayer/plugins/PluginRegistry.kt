package com.panda.audioplayer.plugins

import com.panda.audioplayer.ExoPlayerManager

class PluginRegistry(private val exoPlayerManager: ExoPlayerManager) {

    private abstract class SwitchPlugin : AudioAlgorithmPlugin {
        private var enabled: Boolean = false
        override fun setEnabled(enabled: Boolean) {
            this.enabled = enabled
            onSwitch(enabled)
        }

        override fun isEnabled(): Boolean = enabled
        abstract fun onSwitch(enabled: Boolean)
    }

    fun defaultPlugins(): List<AudioAlgorithmPlugin> {
        return listOf(
            object : SwitchPlugin() {
                override val id = "gain"
                override val displayName = "Gain"
                override val description = "增益插件，用于快速验证响度变化与失真风险。"
                override val version = "0.1.0"
                override fun onSwitch(enabled: Boolean) = exoPlayerManager.setGainEffect(enabled)
            },
            object : SwitchPlugin() {
                override val id = "equalizer"
                override val displayName = "Equalizer"
                override val description = "均衡器插件，用于验证频段调节算法。"
                override val version = "0.1.0"
                override fun onSwitch(enabled: Boolean) = exoPlayerManager.setEqualizerEffect(enabled)
            },
            object : SwitchPlugin() {
                override val id = "spatial3d"
                override val displayName = "3D Audio"
                override val description = "空间音频插件，用于验证3D声场处理效果。"
                override val version = "0.1.0"
                override fun onSwitch(enabled: Boolean) = exoPlayerManager.set3DEffect(enabled)
            }
        )
    }
}
