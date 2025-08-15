package com.example.mmplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mmplayer.R
import com.example.mmplayer.model.FavoriteFolder
import java.text.SimpleDateFormat
import java.util.*

class FavoritesAdapter(
    private val onItemClick: (FavoriteFolder) -> Unit,
    private val onRemoveClick: (FavoriteFolder) -> Unit
) : ListAdapter<FavoriteFolder, FavoritesAdapter.FavoriteViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_folder, parent, false)
        return FavoriteViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavoriteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FavoriteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivIcon)
        private val tvName: TextView = itemView.findViewById(R.id.tvName)
        private val tvPath: TextView = itemView.findViewById(R.id.tvPath)
        private val tvAddedTime: TextView = itemView.findViewById(R.id.tvAddedTime)
        private val ivRemove: ImageView = itemView.findViewById(R.id.ivRemove)

        fun bind(favoriteFolder: FavoriteFolder) {
            tvName.text = favoriteFolder.displayName
            tvPath.text = favoriteFolder.path
            
            val dateFormat = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.getDefault())
            tvAddedTime.text = "添加于 ${dateFormat.format(favoriteFolder.addedTime)}"
            
            ivIcon.setImageResource(R.drawable.ic_launcher_foreground) // 文件夹图标
            
            itemView.setOnClickListener { onItemClick(favoriteFolder) }
            ivRemove.setOnClickListener { onRemoveClick(favoriteFolder) }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<FavoriteFolder>() {
        override fun areItemsTheSame(oldItem: FavoriteFolder, newItem: FavoriteFolder): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FavoriteFolder, newItem: FavoriteFolder): Boolean {
            return oldItem == newItem
        }
    }
}