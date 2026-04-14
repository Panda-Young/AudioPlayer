package com.panda.audioplayer

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.panda.audioplayer.lab.ExperimentSessionManager
import com.panda.audioplayer.utils.Logger

class EffectLabActivity : AppCompatActivity() {
    private lateinit var effectRecyclerView: RecyclerView
    private lateinit var sessionSummaryView: TextView
    private lateinit var effectAdapter: EffectAdapter
    private lateinit var effectManager: EffectManager
    private lateinit var exoPlayerManager: ExoPlayerManager
    private lateinit var sessionManager: ExperimentSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_effect_lab)

        val root = findViewById<android.view.View>(R.id.effect_lab_root)
        val baseStart = root.paddingStart
        val baseTop = root.paddingTop
        val baseEnd = root.paddingEnd
        val baseBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                baseStart + systemBars.left,
                baseTop + systemBars.top,
                baseEnd + systemBars.right,
                baseBottom + systemBars.bottom
            )
            insets
        }

        exoPlayerManager = PlaybackEngine.getManager(this)
        sessionManager = ExperimentSessionManager(this)
        effectManager = EffectManager(this, exoPlayerManager, sessionManager)
        effectManager.startExperimentSession(exoPlayerManager.getCurrentSource())

        effectAdapter = EffectAdapter(effectManager) { name, state ->
            Logger.logi("[Lab] Effect $name ${if (state) "enabled" else "disabled"}")
            sessionSummaryView.text = effectManager.recentSessionSummary()
        }

        sessionSummaryView = findViewById(R.id.lab_session_summary)
        sessionSummaryView.text = effectManager.recentSessionSummary()

        effectRecyclerView = findViewById(R.id.effect_recycler_view)
        effectRecyclerView.layoutManager = LinearLayoutManager(this)
        effectRecyclerView.adapter = effectAdapter
    }

    override fun onDestroy() {
        effectManager.endExperimentSession()
        super.onDestroy()
    }
}
