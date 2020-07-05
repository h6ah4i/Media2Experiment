package com.h6ah4i.example.media2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.h6ah4i.example.media2.databinding.PlaylistItemBinding

class PlaylistAdapter(private val songFiles: Array<String>, val listener: PlaylistAdapterEventListener): RecyclerView.Adapter<PlaylistAdapter.PlaylistItemVH>(), View.OnClickListener {
    private var currentItem = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistItemVH {
        val binding =
            PlaylistItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        return PlaylistItemVH(binding)
    }

    override fun getItemCount(): Int {
        return songFiles.size
    }

    override fun onBindViewHolder(holder: PlaylistItemVH, position: Int) {
        holder.binding.songName = if (position == currentItem) {
            "[$position] * ${songFiles[position]}"
        } else {
            "[$position] ${songFiles[position]}"
        }
        holder.binding.root.setOnClickListener(this)
    }

    class PlaylistItemVH(val binding: PlaylistItemBinding): RecyclerView.ViewHolder(binding.root) {
    }

    override fun onClick(v: View) {
        val viewHolder = (v.parent as RecyclerView).findContainingViewHolder(v) ?: return
        listener.onClickPlaylistItem(viewHolder.adapterPosition, songFiles[viewHolder.adapterPosition])
    }

    fun setCurrentItem(position: Int) {
        if (position == currentItem) return

        val prev = currentItem
        currentItem = position

        notifyItemChanged(prev)
        notifyItemChanged(position)
    }

    interface PlaylistAdapterEventListener {
        fun onClickPlaylistItem(position: Int, file: String)
    }
}
