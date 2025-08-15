package com.example.mmplayer.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mmplayer.databinding.ItemPlaylistBinding
import com.example.mmplayer.model.FileItem

class PlaylistAdapter(
    private val onItemClick: (FileItem) -> Unit
) : ListAdapter<FileItem, PlaylistAdapter.PlaylistViewHolder>(DiffCallback()) {

    private var currentPlayingPath: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val binding = ItemPlaylistBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlaylistViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun setCurrentPlaying(path: String?) {
        val oldPlayingPosition = currentList.indexOfFirst { it.path == currentPlayingPath }
        val newPlayingPosition = currentList.indexOfFirst { it.path == path }
        
        currentPlayingPath = path
        
        if (oldPlayingPosition != -1) {
            notifyItemChanged(oldPlayingPosition)
        }
        if (newPlayingPosition != -1) {
            notifyItemChanged(newPlayingPosition)
        }
    }

    inner class PlaylistViewHolder(
        private val binding: ItemPlaylistBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(fileItem: FileItem) {
            binding.tvTitle.text = fileItem.name
            binding.tvDuration.text = "00:00" // TODO: 获取视频时长
            
            // 加载缩略图
            Glide.with(binding.ivThumbnail.context)
                .load(fileItem.file)
                .into(binding.ivThumbnail)
            
            // 显示当前播放指示器
            binding.ivPlaying.visibility = if (fileItem.path == currentPlayingPath) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
            
            // 设置点击事件
            binding.root.setOnClickListener {
                onItemClick(fileItem)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FileItem>() {
        override fun areItemsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: FileItem, newItem: FileItem): Boolean {
            return oldItem == newItem
        }
    }
}