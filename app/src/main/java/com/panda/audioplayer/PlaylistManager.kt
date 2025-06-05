package com.panda.audioplayer

import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import com.panda.audioplayer.utils.Logger

class PlaylistManager(
    private val context: Context,
    private val adapter: PlaylistAdapter,
    initialLoopMode: LoopMode = LoopMode.REPEAT_ALL
) {
    enum class LoopMode { REPEAT_ONE, REPEAT_ALL, SHUFFLE }
    
    private var currentLoopMode: LoopMode = initialLoopMode
    private val playlist = mutableListOf<String>()
    
    fun getPlaylist(): List<String> = playlist.toList()

    fun getNextPosition(currentPosition: Int): Int {
        return when (currentLoopMode) {
            LoopMode.REPEAT_ONE -> currentPosition
            LoopMode.REPEAT_ALL -> (currentPosition + 1) % playlist.size
            LoopMode.SHUFFLE -> (0 until playlist.size).random()
        }
    }

    fun getPreviousPosition(currentPosition: Int): Int {
        return when (currentLoopMode) {
            LoopMode.REPEAT_ONE -> currentPosition
            LoopMode.REPEAT_ALL -> if (currentPosition - 1 < 0) playlist.size - 1 else currentPosition - 1
            LoopMode.SHUFFLE -> (0 until playlist.size).random()
        }
    }

    fun toggleLoopMode() {
        currentLoopMode = when (currentLoopMode) {
            LoopMode.REPEAT_ONE -> LoopMode.REPEAT_ALL
            LoopMode.REPEAT_ALL -> LoopMode.SHUFFLE
            LoopMode.SHUFFLE -> LoopMode.REPEAT_ONE
        }
    }

    fun getCurrentLoopMode() = currentLoopMode

    fun refreshPlaylist(newFiles: List<String>) {
        playlist.clear()
        playlist.addAll(newFiles)
        adapter.updateData(playlist.toMutableList())
        adapter.setSelectedPosition(0)
    }
}

class PlaylistAdapter(
    private var playlist: MutableList<String>,
    private val onRemoveClickListener: (String) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    private var onItemClickListener: ((String) -> Unit)? = null
    private var selectedPosition = RecyclerView.NO_POSITION

    class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val audioFileName: TextView = itemView.findViewById(R.id.audioFileName)
        val removeIcon: ImageView = itemView.findViewById(R.id.removeIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.playlist_item, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val audioFilePath = playlist[position]
        val fileName = File(audioFilePath).name
        holder.audioFileName.text = fileName
        holder.removeIcon.setOnClickListener {
            onRemoveClickListener(audioFilePath)
        }
        holder.itemView.setOnClickListener {
            Logger.logi("Selected file path: $audioFilePath")
            onItemClickListener?.invoke(audioFilePath)
            setSelectedPosition(position)
        }

        // Highlight the selected item
        holder.itemView.isSelected = selectedPosition == position
        // Change text color based on selection using theme attributes
        if (selectedPosition == position) {
            holder.audioFileName.setTextColor(ContextCompat.getColor(holder.itemView.context, R.color.highlight_color))
        } else {
            val textColorPrimary = resolveColorAttribute(holder.itemView.context, R.attr.text_color_primary)
            holder.audioFileName.setTextColor(textColorPrimary)
        }
    }

    // Helper function to resolve theme attribute colors
    private fun resolveColorAttribute(context: Context, attr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }

    override fun getItemCount(): Int {
        return playlist.size
    }

    fun removeItem(filePath: String) {
        val index = playlist.indexOf(filePath)
        if (index != -1) {
            playlist.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun setOnItemClickListener(listener: (String) -> Unit) {
        onItemClickListener = listener
    }

    fun setSelectedPosition(position: Int) {
        val previousSelected = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousSelected)
        notifyItemChanged(selectedPosition)
    }

    fun getSelectedPosition(): Int {
        return selectedPosition
    }

    fun updateData(newList: MutableList<String>) {
        playlist = newList
        notifyDataSetChanged()
    }
}
