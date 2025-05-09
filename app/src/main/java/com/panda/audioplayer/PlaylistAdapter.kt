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
}
