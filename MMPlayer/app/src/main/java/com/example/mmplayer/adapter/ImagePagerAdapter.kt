package com.example.mmplayer.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.mmplayer.R
import com.example.mmplayer.model.FileItem

class ImagePagerAdapter(
    private val onImageClick: () -> Unit
) : ListAdapter<FileItem, ImagePagerAdapter.ImageViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_page, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val photoView: ImageView = itemView.findViewById(R.id.photoView)
        private val llError: LinearLayout = itemView.findViewById(R.id.llError)

        fun bind(fileItem: FileItem) {
            llError.visibility = View.GONE
            photoView.visibility = View.VISIBLE
            
            Glide.with(itemView.context)
                .load(fileItem.file)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .error { 
                    photoView.visibility = View.GONE
                    llError.visibility = View.VISIBLE
                }
                .into(photoView)
            
            photoView.setOnClickListener { onImageClick() }
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