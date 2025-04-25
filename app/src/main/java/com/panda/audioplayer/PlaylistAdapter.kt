package com.panda.audioplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class PlaylistAdapter(
    private var playlist: MutableList<String>,
    private val onRemoveClickListener: (String) -> Unit
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    private var onItemClickListener: ((String) -> Unit)? = null

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
            onItemClickListener?.invoke(audioFilePath)
        }
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
}
