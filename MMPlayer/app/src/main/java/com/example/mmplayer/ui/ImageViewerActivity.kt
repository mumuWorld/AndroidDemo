package com.example.mmplayer.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.PopupMenu
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.mmplayer.R
import com.example.mmplayer.adapter.ImagePagerAdapter
import com.example.mmplayer.databinding.ActivityImageViewerBinding
import com.example.mmplayer.model.FileItem
import com.example.mmplayer.service.FileManager
import com.example.mmplayer.utils.FileUtils
import kotlinx.coroutines.*
import java.io.File

class ImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageViewerBinding
    private lateinit var imagePagerAdapter: ImagePagerAdapter
    private val fileManager = FileManager()
    
    private var images = listOf<FileItem>()
    private var currentImagePath: String? = null
    private var isControlsVisible = true
    
    private val hideControlsHandler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupClickListeners()
        
        val imagePath = intent.getStringExtra("image_path")
        val folderPath = intent.getStringExtra("folder_path")
        
        if (imagePath != null && folderPath != null) {
            currentImagePath = imagePath
            loadImagesFromFolder(folderPath, imagePath)
        }
    }

    private fun setupUI() {
        // 设置全屏
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
        
        imagePagerAdapter = ImagePagerAdapter { 
            // 点击图片时切换控制栏显示
            if (isControlsVisible) hideControls() else showControls()
        }
        
        binding.viewPager.adapter = imagePagerAdapter
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateImageInfo(position)
                scheduleControlsHide()
            }
        })
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener { finish() }
        
        binding.ivShare.setOnClickListener {
            if (images.isNotEmpty()) {
                val currentPosition = binding.viewPager.currentItem
                val currentImage = images[currentPosition]
                FileUtils.shareFile(this, currentImage.file)
            }
        }
        
        binding.ivMore.setOnClickListener { showMoreOptionsMenu(it) }
        
        // 点击控制栏区域时防止隐藏
        binding.llTopControls.setOnClickListener { scheduleControlsHide() }
        binding.llBottomControls.setOnClickListener { scheduleControlsHide() }
    }

    private fun loadImagesFromFolder(folderPath: String, currentImagePath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                
                val allFiles = fileManager.getFiles(folderPath)
                val imageFiles = allFiles.filter { it.isImage }
                
                withContext(Dispatchers.Main) {
                    images = imageFiles
                    imagePagerAdapter.submitList(images)
                    
                    // 定位到当前图片
                    val currentIndex = images.indexOfFirst { it.path == currentImagePath }
                    if (currentIndex != -1) {
                        binding.viewPager.setCurrentItem(currentIndex, false)
                        updateImageInfo(currentIndex)
                    }
                    
                    binding.progressBar.visibility = View.GONE
                    scheduleControlsHide()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun updateImageInfo(position: Int) {
        if (images.isNotEmpty()) {
            val currentImage = images[position]
            binding.tvTitle.text = currentImage.name
            binding.tvImageInfo.text = "${position + 1} / ${images.size}"
        }
    }

    private fun showControls() {
        isControlsVisible = true
        binding.llTopControls.visibility = View.VISIBLE
        binding.llBottomControls.visibility = View.VISIBLE
        scheduleControlsHide()
    }

    private fun hideControls() {
        isControlsVisible = false
        binding.llTopControls.visibility = View.GONE
        binding.llBottomControls.visibility = View.GONE
        cancelControlsHide()
    }

    private fun scheduleControlsHide() {
        cancelControlsHide()
        hideControlsHandler.postDelayed(hideControlsRunnable, 3000)
    }

    private fun cancelControlsHide() {
        hideControlsHandler.removeCallbacks(hideControlsRunnable)
    }

    private fun showMoreOptionsMenu(view: View) {
        if (images.isEmpty()) return
        
        val currentPosition = binding.viewPager.currentItem
        val currentImage = images[currentPosition]
        
        PopupMenu(this, view).apply {
            menu.add("设为壁纸")
            menu.add("图片信息")
            menu.add("删除")
            
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.title) {
                    "设为壁纸" -> {
                        // TODO: 实现设为壁纸功能
                        true
                    }
                    "图片信息" -> {
                        showImageDetails(currentImage)
                        true
                    }
                    "删除" -> {
                        deleteCurrentImage()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun showImageDetails(imageFile: FileItem) {
        val details = buildString {
            append("文件名：${imageFile.name}\n")
            append("路径：${imageFile.path}\n")
            append("大小：${imageFile.displaySize}\n")
            append("修改时间：${imageFile.lastModified}")
        }
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("图片信息")
            .setMessage(details)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun deleteCurrentImage() {
        if (images.isEmpty()) return
        
        val currentPosition = binding.viewPager.currentItem
        val currentImage = images[currentPosition]
        
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("删除确认")
            .setMessage("确定要删除这张图片吗？")
            .setPositiveButton("删除") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    val success = fileManager.deleteFile(currentImage.path)
                    withContext(Dispatchers.Main) {
                        if (success) {
                            val newImages = images.toMutableList()
                            newImages.removeAt(currentPosition)
                            images = newImages
                            
                            if (images.isEmpty()) {
                                finish()
                            } else {
                                imagePagerAdapter.submitList(images)
                                val newPosition = if (currentPosition >= images.size) {
                                    images.size - 1
                                } else {
                                    currentPosition
                                }
                                binding.viewPager.setCurrentItem(newPosition, false)
                                updateImageInfo(newPosition)
                            }
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onBackPressed() {
        if (isControlsVisible) {
            hideControls()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelControlsHide()
    }
}