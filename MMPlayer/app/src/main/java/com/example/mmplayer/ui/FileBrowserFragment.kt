package com.example.mmplayer.ui

import android.content.Intent
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mmplayer.R
import com.example.mmplayer.adapter.FileAdapter
import com.example.mmplayer.databinding.FragmentFileBrowserBinding
import com.example.mmplayer.model.FileItem
import com.example.mmplayer.model.SortOption
import com.example.mmplayer.model.ViewMode
import com.example.mmplayer.service.FavoriteManager
import com.example.mmplayer.service.FileManager
import com.example.mmplayer.utils.FileUtils
import com.example.mmplayer.utils.PermissionManager
import com.example.mmplayer.viewmodel.FileBrowserViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class FileBrowserFragment : Fragment() {

    private var _binding: FragmentFileBrowserBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var fileAdapter: FileAdapter
    private lateinit var permissionManager: PermissionManager
    private lateinit var favoriteManager: FavoriteManager
    private val fileManager = FileManager()
    
    private var currentPath = Environment.getExternalStorageDirectory().absolutePath
    private var viewMode = ViewMode.LIST
    private var sortOption = SortOption.NAME
    private var currentFilter: ((FileItem) -> Boolean)? = null
    private var currentFilterName: String? = null
    private var allFiles = listOf<FileItem>()
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileBrowserBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        favoriteManager = FavoriteManager(requireContext())
        permissionManager = PermissionManager(requireContext())
        
        setupRecyclerView()
        setupClickListeners()
        
        // 获取传入的初始路径
        arguments?.getString("initial_path")?.let { path ->
            currentPath = path
        }
        
        checkPermissionsAndInit()
    }

    private fun setupRecyclerView() {
        fileAdapter = FileAdapter(
            viewMode = viewMode,
            onItemClick = { fileItem ->
                if (fileItem.isDirectory) {
                    navigateToDirectory(fileItem.path)
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

    private fun setupClickListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            refreshCurrentDirectory()
        }

        binding.btnViewMode.setOnClickListener {
            toggleViewMode()
        }

        binding.btnSort.setOnClickListener {
            showSortDialog()
        }

        binding.btnFilter.setOnClickListener {
            showFilterDialog()
        }

        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        binding.btnNewFolder.setOnClickListener {
            showCreateFolderDialog()
        }
    }

    private fun checkPermissionsAndInit() {
        if (permissionManager.hasStoragePermissions()) {
            loadFiles()
        } else {
            // 在Fragment中需要通过Activity来请求权限
            Toast.makeText(requireContext(), "需要存储权限才能浏览文件", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadFiles() {
        lifecycleScope.launch {
            binding.swipeRefresh.isRefreshing = true
            
            try {
                val files = fileManager.getFiles(currentPath, sortOption)
                allFiles = files
                applyCurrentFilter()
                updateBreadcrumb()
                updateFavoriteButton()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), "加载文件失败", Toast.LENGTH_SHORT).show()
            } finally {
                binding.swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun navigateToDirectory(path: String) {
        currentPath = path
        loadFiles()
    }

    private fun refreshCurrentDirectory() {
        loadFiles()
    }

    private fun applyCurrentFilter() {
        val filteredFiles = if (currentFilter != null) {
            allFiles.filter { currentFilter!!.invoke(it) }
        } else {
            allFiles
        }
        fileAdapter.submitList(filteredFiles)
        
        // 更新筛选状态显示
        updateFilterStatus()
    }

    private fun updateFilterStatus() {
        if (currentFilterName != null) {
            binding.tvFilterStatus.text = currentFilterName
            binding.tvFilterStatus.visibility = View.VISIBLE
        } else {
            binding.tvFilterStatus.visibility = View.GONE
        }
    }

    private fun updateRecyclerViewLayoutManager() {
        val layoutManager = when (viewMode) {
            ViewMode.LIST -> LinearLayoutManager(requireContext())
            ViewMode.GRID -> GridLayoutManager(requireContext(), 2)
        }
        binding.recyclerView.layoutManager = layoutManager
    }

    private fun updateBreadcrumb() {
        binding.llBreadcrumb.removeAllViews()
        
        val pathParts = currentPath.split("/").filter { it.isNotEmpty() }
        var currentPathTemp = ""
        
        // Root
        addBreadcrumbItem("/", "/")
        
        // Path parts
        pathParts.forEach { part ->
            currentPathTemp += "/$part"
            addBreadcrumbItem(part, currentPathTemp)
        }
    }

    private fun addBreadcrumbItem(name: String, path: String) {
        val textView = TextView(requireContext()).apply {
            text = name
            setPadding(16, 8, 16, 8)
            setTextColor(requireContext().getColor(android.R.color.black))
            setOnClickListener {
                navigateToDirectory(path)
            }
            background = requireContext().getDrawable(android.R.drawable.btn_default)
        }
        binding.llBreadcrumb.addView(textView)
        
        // Add separator
        if (binding.llBreadcrumb.childCount > 1) {
            val separator = TextView(requireContext()).apply {
                text = "/"
                setPadding(8, 8, 8, 8)
                setTextColor(requireContext().getColor(android.R.color.darker_gray))
            }
            binding.llBreadcrumb.addView(separator, binding.llBreadcrumb.childCount - 1)
        }
    }

    private fun updateFavoriteButton() {
        lifecycleScope.launch {
            val isFavorite = favoriteManager.isFavorite(currentPath)
            binding.btnFavorite.setIconTintResource(
                if (isFavorite) android.R.color.holo_red_light else android.R.color.darker_gray
            )
        }
    }

    private fun toggleViewMode() {
        viewMode = when (viewMode) {
            ViewMode.LIST -> ViewMode.GRID
            ViewMode.GRID -> ViewMode.LIST
        }
        fileAdapter.updateViewMode(viewMode)
        updateRecyclerViewLayoutManager()
    }

    private fun showSortDialog() {
        val sortOptions = SortOption.values().map { it.displayName }.toTypedArray()
        val currentSortIndex = SortOption.values().indexOf(sortOption)
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("排序方式")
            .setSingleChoiceItems(sortOptions, currentSortIndex) { dialog, which ->
                sortOption = SortOption.values()[which]
                loadFiles()
                dialog.dismiss()
            }
            .show()
    }

    private fun showFilterDialog() {
        val filterOptions = arrayOf("全部文件", "视频", "音频", "图片", "文档")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("文件筛选")
            .setItems(filterOptions) { _, which ->
                when (which) {
                    0 -> {
                        currentFilter = null
                        currentFilterName = null
                    }
                    1 -> {
                        currentFilter = { it.isVideo }
                        currentFilterName = "视频"
                    }
                    2 -> {
                        currentFilter = { it.isAudio }
                        currentFilterName = "音频"
                    }
                    3 -> {
                        currentFilter = { it.isImage }
                        currentFilterName = "图片"
                    }
                    4 -> {
                        currentFilter = { it.isDocument }
                        currentFilterName = "文档"
                    }
                }
                applyCurrentFilter()
            }
            .show()
    }

    private fun toggleFavorite() {
        lifecycleScope.launch {
            val isFavorite = favoriteManager.isFavorite(currentPath)
            
            if (isFavorite) {
                val success = favoriteManager.removeFavorite(currentPath)
                if (success) {
                    Toast.makeText(requireContext(), "已取消收藏", Toast.LENGTH_SHORT).show()
                    updateFavoriteButton()
                }
            } else {
                val folderName = currentPath.substringAfterLast("/")
                val success = favoriteManager.addFavorite(currentPath, folderName)
                if (success) {
                    Toast.makeText(requireContext(), "已添加收藏", Toast.LENGTH_SHORT).show()
                    updateFavoriteButton()
                } else {
                    Toast.makeText(requireContext(), "该文件夹已在收藏中", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleFileClick(fileItem: FileItem) {
        when {
            fileItem.isVideo -> {
                val intent = Intent(requireContext(), VideoPlayerActivity::class.java).apply {
                    putExtra("video_path", fileItem.path)
                    putExtra("video_title", fileItem.name)
                }
                startActivity(intent)
            }
            fileItem.isAudio -> {
                val intent = Intent(requireContext(), AudioPlayerActivity::class.java).apply {
                    putExtra("audio_path", fileItem.path)
                    putExtra("audio_title", fileItem.name)
                }
                startActivity(intent)
            }
            fileItem.isImage -> {
                val intent = Intent(requireContext(), ImageViewerActivity::class.java).apply {
                    putExtra("image_path", fileItem.path)
                    putExtra("folder_path", fileItem.file.parent)
                }
                startActivity(intent)
            }
        }
    }

    private fun showFileOptionsDialog(fileItem: FileItem) {
        val options = mutableListOf<String>().apply {
            add("重命名")
            add("删除")
            add("分享")
            if (fileItem.isVideo || fileItem.isAudio) {
                add("播放")
            }
            if (fileItem.isImage) {
                add("查看")
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(fileItem.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (options[which]) {
                    "重命名" -> showRenameDialog(fileItem)
                    "删除" -> showDeleteConfirmDialog(fileItem)
                    "分享" -> shareFile(fileItem)
                    "播放", "查看" -> handleFileClick(fileItem)
                }
            }
            .show()
    }

    private fun showFilePopupMenu(fileItem: FileItem, view: View) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(R.menu.file_popup_menu, popup.menu)
        
        // 添加分享选项
        popup.menu.add("分享")
        
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
                else -> {
                    if (menuItem.title == "分享") {
                        shareFile(fileItem)
                        true
                    } else {
                        false
                    }
                }
            }
        }
        popup.show()
    }

    private fun shareFile(fileItem: FileItem) {
        FileUtils.shareFile(requireContext(), fileItem.file)
    }

    private fun showCreateFolderDialog() {
        val input = TextInputEditText(requireContext())
        val inputLayout = TextInputLayout(requireContext()).apply {
            hint = "文件夹名称"
            addView(input)
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("新建文件夹")
            .setView(inputLayout)
            .setPositiveButton("创建") { _, _ ->
                val folderName = input.text.toString().trim()
                if (folderName.isNotEmpty()) {
                    createFolder(folderName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showRenameDialog(fileItem: FileItem) {
        val input = TextInputEditText(requireContext()).apply {
            setText(fileItem.name)
            selectAll()
        }
        val inputLayout = TextInputLayout(requireContext()).apply {
            hint = "文件名"
            addView(input)
        }
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("重命名")
            .setView(inputLayout)
            .setPositiveButton("确认") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty() && newName != fileItem.name) {
                    renameFile(fileItem, newName)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showDeleteConfirmDialog(fileItem: FileItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("删除确认")
            .setMessage("确定要删除 \"${fileItem.name}\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteFile(fileItem)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun createFolder(name: String) {
        lifecycleScope.launch {
            val success = fileManager.createDirectory(currentPath, name)
            if (success) {
                refreshCurrentDirectory()
                Toast.makeText(requireContext(), "文件夹创建成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "文件夹创建失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteFile(fileItem: FileItem) {
        lifecycleScope.launch {
            val success = fileManager.deleteFile(fileItem.path)
            if (success) {
                refreshCurrentDirectory()
                Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renameFile(fileItem: FileItem, newName: String) {
        lifecycleScope.launch {
            val success = fileManager.renameFile(fileItem.path, newName)
            if (success) {
                refreshCurrentDirectory()
                Toast.makeText(requireContext(), "重命名成功", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "重命名失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun onBackPressed(): Boolean {
        val parentPath = fileManager.getParentPath(currentPath)
        return if (parentPath != null && parentPath != currentPath) {
            navigateToDirectory(parentPath)
            true
        } else {
            false
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(initialPath: String? = null): FileBrowserFragment {
            val fragment = FileBrowserFragment()
            initialPath?.let {
                val bundle = Bundle().apply {
                    putString("initial_path", it)
                }
                fragment.arguments = bundle
            }
            return fragment
        }
    }
}