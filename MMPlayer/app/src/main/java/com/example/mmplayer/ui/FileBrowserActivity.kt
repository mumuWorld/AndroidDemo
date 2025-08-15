package com.example.mmplayer.ui

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.MenuItem
import android.view.View
import android.widget.PopupMenu
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mmplayer.R
import com.example.mmplayer.adapter.FileAdapter
import com.example.mmplayer.databinding.ActivityFileBrowserBinding
import com.example.mmplayer.model.FileItem
import com.example.mmplayer.model.SortOption
import com.example.mmplayer.model.ViewMode
import com.example.mmplayer.service.FileManager
import com.example.mmplayer.utils.PermissionManager
import com.example.mmplayer.viewmodel.FileBrowserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class FileBrowserActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFileBrowserBinding
    private lateinit var fileAdapter: FileAdapter
    private lateinit var permissionManager: PermissionManager
    private val viewModel: FileBrowserViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFileBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupPermissions()
        setupRecyclerView()
        setupObservers()
        setupClickListeners()
        
        // 检查权限并初始化
        checkPermissionsAndInit()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupPermissions() {
        permissionManager = PermissionManager(this)
    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter(
            viewMode = ViewMode.LIST,
            onItemClick = { fileItem ->
                if (fileItem.isDirectory) {
                    viewModel.navigateToDirectory(fileItem.path)
                } else {
                    handleFileClick(fileItem)
                }
            },
            onItemLongClick = { fileItem ->
                showFileOptionsDialog(fileItem)
            },
            onMoreClick = { fileItem, view ->
                showFilePopupMenu(fileItem, view)
            }
        )

        binding.recyclerView.adapter = fileAdapter
        updateRecyclerViewLayoutManager()
    }

    private fun setupObservers() {
        viewModel.files.observe(this) { files ->
            fileAdapter.submitList(files)
            binding.swipeRefresh.isRefreshing = false
        }

        viewModel.currentPath.observe(this) { path ->
            updateBreadcrumb(path)
        }

        viewModel.viewMode.observe(this) { viewMode ->
            fileAdapter.updateViewMode(viewMode)
            updateRecyclerViewLayoutManager()
            updateViewModeButton()
        }

        viewModel.loading.observe(this) { loading ->
            binding.swipeRefresh.isRefreshing = loading
        }
    }

    private fun setupClickListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.refreshCurrentDirectory()
        }

        binding.btnViewMode.setOnClickListener {
            viewModel.toggleViewMode()
        }

        binding.btnSort.setOnClickListener {
            showSortDialog()
        }

        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        binding.btnNewFolder.setOnClickListener {
            showCreateFolderDialog()
        }
    }

    private fun checkPermissionsAndInit() {
        if (permissionManager.hasStoragePermissions()) {
            initializeFileManager()
        } else {
            permissionManager.requestStoragePermissions(this)
        }
    }

    private fun initializeFileManager() {
        val initialPath = Environment.getExternalStorageDirectory().absolutePath
        viewModel.navigateToDirectory(initialPath)
    }

    private fun updateRecyclerViewLayoutManager() {
        val layoutManager = when (viewModel.viewMode.value) {
            ViewMode.LIST -> LinearLayoutManager(this)
            ViewMode.GRID -> GridLayoutManager(this, 2)
            else -> LinearLayoutManager(this)
        }
        binding.recyclerView.layoutManager = layoutManager
    }

    private fun updateViewModeButton() {
        val iconRes = when (viewModel.viewMode.value) {
            ViewMode.LIST -> R.drawable.ic_launcher_foreground // Grid icon
            ViewMode.GRID -> R.drawable.ic_launcher_foreground // List icon
            else -> R.drawable.ic_launcher_foreground
        }
        binding.btnViewMode.setIconResource(iconRes)
    }

    private fun updateBreadcrumb(path: String) {
        binding.llBreadcrumb.removeAllViews()
        
        val pathParts = path.split("/").filter { it.isNotEmpty() }
        var currentPath = ""
        
        // Root
        addBreadcrumbItem("/", "/")
        
        // Path parts
        pathParts.forEach { part ->
            currentPath += "/$part"
            addBreadcrumbItem(part, currentPath)
        }
    }

    private fun addBreadcrumbItem(name: String, path: String) {
        val textView = TextView(this).apply {
            text = name
            setPadding(16, 8, 16, 8)
            setTextColor(getColor(android.R.color.black))
            setOnClickListener {
                viewModel.navigateToDirectory(path)
            }
            background = getDrawable(android.R.drawable.btn_default)
        }
        binding.llBreadcrumb.addView(textView)
        
        // Add separator
        if (binding.llBreadcrumb.childCount > 1) {
            val separator = TextView(this).apply {
                text = "/"
                setPadding(8, 8, 8, 8)
                setTextColor(getColor(android.R.color.darker_gray))
            }
            binding.llBreadcrumb.addView(separator, binding.llBreadcrumb.childCount - 1)
        }
    }

    private fun handleFileClick(fileItem: FileItem) {
        when {
            fileItem.isVideo -> {
                val intent = Intent(this, VideoPlayerActivity::class.java).apply {
                    putExtra("video_path", fileItem.path)
                    putExtra("video_title", fileItem.name)
                }
                startActivity(intent)
            }
            fileItem.isAudio -> {
                val intent = Intent(this, AudioPlayerActivity::class.java).apply {
                    putExtra("audio_path", fileItem.path)
                    putExtra("audio_title", fileItem.name)
                }
                startActivity(intent)
            }
            fileItem.isImage -> {
                // 可以添加图片查看器
            }
            else -> {
                // 尝试打开文件
            }
        }
    }

    private fun showFileOptionsDialog(fileItem: FileItem) {
        val options = mutableListOf<String>().apply {
            add("重命名")
            add("删除")
            add("复制")
            add("移动")
            if (fileItem.isVideo || fileItem.isAudio) {
                add("播放")
            }
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(fileItem.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "重命名" -> showRenameDialog(fileItem)
                    "删除" -> showDeleteConfirmDialog(fileItem)
                    "复制" -> {
                        // TODO: 实现复制功能
                    }
                    "移动" -> {
                        // TODO: 实现移动功能
                    }
                    "播放" -> handleFileClick(fileItem)
                }
            }
            .show()
    }

    private fun showFilePopupMenu(fileItem: FileItem, view: View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.file_popup_menu, popup.menu)
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_rename -> {
                    showRenameDialog(fileItem)
                    true
                }
                R.id.action_delete -> {
                    showDeleteConfirmDialog(fileItem)
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    private fun showSortDialog() {
        val sortOptions = SortOption.values().map { it.displayName }.toTypedArray()
        val currentSortIndex = SortOption.values().indexOf(viewModel.sortOption.value)
        
        MaterialAlertDialogBuilder(this)
            .setTitle("排序方式")
            .setSingleChoiceItems(sortOptions, currentSortIndex) { dialog, which ->
                viewModel.setSortOption(SortOption.values()[which])
                dialog.dismiss()
            }
            .show()
    }

    private fun showFilterDialog() {
        val filterOptions = arrayOf("全部文件", "视频", "音频", "图片", "文档")
        
        MaterialAlertDialogBuilder(this)
            .setTitle("文件筛选")
            .setItems(filterOptions) { _, which ->
                when (which) {
                    0 -> viewModel.setFilter(null)
                    1 -> viewModel.setFilter { it.isVideo }
                    2 -> viewModel.setFilter { it.isAudio }
                    3 -> viewModel.setFilter { it.isImage }
                    4 -> viewModel.setFilter { it.isDocument }
                }
            }
            .show()
    }

    private fun showCreateFolderDialog() {
        val input = TextInputEditText(this)
        val inputLayout = TextInputLayout(this).apply {
            hint = "文件夹名称"
            addView(input)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("新建文件夹")
            .setView(inputLayout)
            .setPositiveButton("创建") { _, _ ->
                val folderName = input.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    viewModel.createFolder(folderName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showRenameDialog(fileItem: FileItem) {
        val input = TextInputEditText(this).apply {
            setText(fileItem.name)
            selectAll()
        }
        val inputLayout = TextInputLayout(this).apply {
            hint = "文件名"
            addView(input)
        }
        
        MaterialAlertDialogBuilder(this)
            .setTitle("重命名")
            .setView(inputLayout)
            .setPositiveButton("确认") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != fileItem.name) {
                    viewModel.renameFile(fileItem, newName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteConfirmDialog(fileItem: FileItem) {
        MaterialAlertDialogBuilder(this)
            .setTitle("删除确认")
            .setMessage("确定要删除 \"${fileItem.name}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteFile(fileItem)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    override fun onBackPressed() {
        if (viewModel.canNavigateBack()) {
            viewModel.navigateBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults) { granted ->
            if (granted) {
                initializeFileManager()
            } else {
                finish()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        permissionManager.onActivityResult(requestCode, resultCode, data) { granted ->
            if (granted) {
                initializeFileManager()
            } else {
                finish()
            }
        }
    }
}