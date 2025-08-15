package com.example.mmplayer.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mmplayer.model.FileItem
import com.example.mmplayer.model.SortOption
import com.example.mmplayer.model.ViewMode
import com.example.mmplayer.service.FileManager
import kotlinx.coroutines.launch
import java.util.*

class FileBrowserViewModel : ViewModel() {

    private val fileManager = FileManager()
    
    private val _files = MutableLiveData<List<FileItem>>()
    val files: LiveData<List<FileItem>> = _files
    
    private val _currentPath = MutableLiveData<String>()
    val currentPath: LiveData<String> = _currentPath
    
    private val _viewMode = MutableLiveData(ViewMode.LIST)
    val viewMode: LiveData<ViewMode> = _viewMode
    
    private val _sortOption = MutableLiveData(SortOption.NAME)
    val sortOption: LiveData<SortOption> = _sortOption
    
    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading
    
    private val navigationStack = Stack<String>()
    private var currentFilter: ((FileItem) -> Boolean)? = null
    private var allFiles = listOf<FileItem>()

    fun navigateToDirectory(path: String) {
        viewModelScope.launch {
            _loading.value = true
            
            try {
                val files = fileManager.getFiles(path, _sortOption.value ?: SortOption.NAME)
                allFiles = files
                applyCurrentFilter()
                
                // Update navigation
                _currentPath.value = path
                if (navigationStack.isEmpty() || navigationStack.peek() != path) {
                    navigationStack.push(path)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun navigateBack(): Boolean {
        return if (navigationStack.size > 1) {
            navigationStack.pop()
            val previousPath = navigationStack.peek()
            navigateToDirectory(previousPath)
            true
        } else {
            false
        }
    }

    fun canNavigateBack(): Boolean {
        return navigationStack.size > 1
    }

    fun refreshCurrentDirectory() {
        _currentPath.value?.let { path ->
            navigateToDirectory(path)
        }
    }

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            ViewMode.LIST -> ViewMode.GRID
            ViewMode.GRID -> ViewMode.LIST
            else -> ViewMode.LIST
        }
    }

    fun setSortOption(sortOption: SortOption) {
        _sortOption.value = sortOption
        refreshCurrentDirectory()
    }

    fun setFilter(filter: ((FileItem) -> Boolean)?) {
        currentFilter = filter
        applyCurrentFilter()
    }

    private fun applyCurrentFilter() {
        val filteredFiles = if (currentFilter != null) {
            allFiles.filter { currentFilter!!.invoke(it) }
        } else {
            allFiles
        }
        _files.value = filteredFiles
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            _currentPath.value?.let { currentPath ->
                val success = fileManager.createDirectory(currentPath, name)
                if (success) {
                    refreshCurrentDirectory()
                }
            }
        }
    }

    fun deleteFile(fileItem: FileItem) {
        viewModelScope.launch {
            val success = fileManager.deleteFile(fileItem.path)
            if (success) {
                refreshCurrentDirectory()
            }
        }
    }

    fun renameFile(fileItem: FileItem, newName: String) {
        viewModelScope.launch {
            val success = fileManager.renameFile(fileItem.path, newName)
            if (success) {
                refreshCurrentDirectory()
            }
        }
    }

    fun copyFile(fileItem: FileItem, destinationPath: String) {
        viewModelScope.launch {
            val success = fileManager.copyFile(fileItem.path, destinationPath)
            if (success) {
                refreshCurrentDirectory()
            }
        }
    }

    fun moveFile(fileItem: FileItem, destinationPath: String) {
        viewModelScope.launch {
            val success = fileManager.moveFile(fileItem.path, destinationPath)
            if (success) {
                refreshCurrentDirectory()
            }
        }
    }

    fun getVideoFiles(directoryPath: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val videoFiles = fileManager.getVideoFiles(directoryPath, _sortOption.value ?: SortOption.NAME)
                _files.value = videoFiles
                allFiles = videoFiles
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }

    fun getAudioFiles(directoryPath: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val audioFiles = fileManager.getAudioFiles(directoryPath, _sortOption.value ?: SortOption.NAME)
                _files.value = audioFiles
                allFiles = audioFiles
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _loading.value = false
            }
        }
    }
}