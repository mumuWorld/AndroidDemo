package com.example.mmplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mmplayer.R
import com.example.mmplayer.model.FileItem
import com.example.mmplayer.model.ViewMode
import java.text.SimpleDateFormat
import java.util.*

class FileAdapter(
    private var viewMode: ViewMode,
    private val onItemClick: (FileItem) -> Unit,
    private val onItemLongClick: (FileItem) -> Unit,
    private val onMoreClick: (FileItem, View) -> Unit
) : ListAdapter<FileItem, FileAdapter.FileViewHolder>(DiffCallback()) {

    override fun getItemViewType(position: Int): Int {
        return when (viewMode) {
            ViewMode.LIST -> VIEW_TYPE_LIST
            ViewMode.GRID -> VIEW_TYPE_GRID
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val layoutId = when (viewType) {
            VIEW_TYPE_LIST -> R.layout.item_file_list
            VIEW_TYPE_GRID -> R.layout.item_file_grid
            else -> R.layout.item_file_list
        }
        
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return FileViewHolder(view, viewType)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateViewMode(newViewMode: ViewMode) {
        if (viewMode != newViewMode) {
            viewMode = newViewMode
            notifyDataSetChanged()
        }
    }

    inner class FileViewHolder(
        itemView: View,
        private val viewType: Int
    ) : RecyclerView.ViewHolder(itemView) {

        private val ivFileIcon: ImageView = itemView.findViewById(R.id.ivFileIcon)
        private val tvFileName: TextView = itemView.findViewById(R.id.tvFileName)
        private val tvFileSize: TextView = itemView.findViewById(R.id.tvFileSize)
        private val tvFileDate: TextView? = itemView.findViewById(R.id.tvFileDate)
        private val ivMore: ImageView = itemView.findViewById(R.id.ivMore)

        fun bind(fileItem: FileItem) {
            tvFileName.text = fileItem.name
            tvFileSize.text = fileItem.displaySize
            
            tvFileDate?.text = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
                .format(fileItem.lastModified)

            // 设置文件图标
            loadFileIcon(fileItem)

            // 设置点击事件
            itemView.setOnClickListener { onItemClick(fileItem) }
            itemView.setOnLongClickListener { 
                onItemLongClick(fileItem)
                true
            }
            ivMore.setOnClickListener { onMoreClick(fileItem, it) }
        }

        private fun loadFileIcon(fileItem: FileItem) {
            val context = itemView.context
            
            when {
                fileItem.isDirectory -> {
                    Glide.with(context)
                        .load(R.drawable.ic_launcher_foreground) // 文件夹图标
                        .into(ivFileIcon)
                }
                fileItem.isVideo -> {
                    // 对于视频文件，尝试加载缩略图
                    Glide.with(context)
                        .load(fileItem.file)
                        .placeholder(R.drawable.ic_launcher_foreground) // 视频图标占位符
                        .error(R.drawable.ic_launcher_foreground) // 错误时显示的图标
                        .into(ivFileIcon)
                }
                fileItem.isAudio -> {
                    Glide.with(context)
                        .load(R.drawable.ic_launcher_foreground) // 音频图标
                        .into(ivFileIcon)
                }
                fileItem.isImage -> {
                    Glide.with(context)
                        .load(fileItem.file)
                        .placeholder(R.drawable.ic_launcher_foreground) // 图片图标占位符
                        .error(R.drawable.ic_launcher_foreground) // 错误时显示的图标
                        .into(ivFileIcon)
                }
                fileItem.isDocument -> {
                    Glide.with(context)
                        .load(R.drawable.ic_launcher_foreground) // 文档图标
                        .into(ivFileIcon)
                }
                fileItem.isArchive -> {
                    Glide.with(context)
                        .load(R.drawable.ic_launcher_foreground) // 压缩包图标
                        .into(ivFileIcon)
                }
                else -> {
                    Glide.with(context)
                        .load(R.drawable.ic_launcher_foreground) // 默认文件图标
                        .into(ivFileIcon)
                }
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

    companion object {
        private const val VIEW_TYPE_LIST = 1
        private const val VIEW_TYPE_GRID = 2
    }
}