package com.example.mmplayer.service

import android.content.Context
import android.os.Environment
import com.example.mmplayer.model.FileItem
import com.example.mmplayer.model.SortOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileManager {
    
    suspend fun getFiles(directoryPath: String, sortOption: SortOption = SortOption.NAME): List<FileItem> {
        return withContext(Dispatchers.IO) {
            try {
                val directory = File(directoryPath)
                if (!directory.exists() || !directory.isDirectory) {
                    return@withContext emptyList()
                }
                
                val files = directory.listFiles()?.map { FileItem(it) } ?: emptyList()
                
                sortFiles(files, sortOption)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    suspend fun getVideoFiles(directoryPath: String, sortOption: SortOption = SortOption.NAME): List<FileItem> {
        return withContext(Dispatchers.IO) {
            try {
                val directory = File(directoryPath)
                if (!directory.exists() || !directory.isDirectory) {
                    return@withContext emptyList()
                }
                
                val videoFiles = mutableListOf<FileItem>()
                collectVideoFiles(directory, videoFiles)
                
                sortFiles(videoFiles, sortOption)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    suspend fun getAudioFiles(directoryPath: String, sortOption: SortOption = SortOption.NAME): List<FileItem> {
        return withContext(Dispatchers.IO) {
            try {
                val directory = File(directoryPath)
                if (!directory.exists() || !directory.isDirectory) {
                    return@withContext emptyList()
                }
                
                val audioFiles = mutableListOf<FileItem>()
                collectAudioFiles(directory, audioFiles)
                
                sortFiles(audioFiles, sortOption)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    private fun collectVideoFiles(directory: File, videoFiles: MutableList<FileItem>) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                collectVideoFiles(file, videoFiles)
            } else {
                val fileItem = FileItem(file)
                if (fileItem.isVideo) {
                    videoFiles.add(fileItem)
                }
            }
        }
    }
    
    private fun collectAudioFiles(directory: File, audioFiles: MutableList<FileItem>) {
        directory.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                collectAudioFiles(file, audioFiles)
            } else {
                val fileItem = FileItem(file)
                if (fileItem.isAudio) {
                    audioFiles.add(fileItem)
                }
            }
        }
    }
    
    private fun sortFiles(files: List<FileItem>, sortOption: SortOption): List<FileItem> {
        return when (sortOption) {
            SortOption.NAME -> files.sortedWith { a, b ->
                if (a.isDirectory && !b.isDirectory) -1
                else if (!a.isDirectory && b.isDirectory) 1
                else a.name.compareTo(b.name, ignoreCase = true)
            }
            SortOption.SIZE -> files.sortedWith { a, b ->
                if (a.isDirectory && !b.isDirectory) -1
                else if (!a.isDirectory && b.isDirectory) 1
                else a.size.compareTo(b.size)
            }
            SortOption.DATE -> files.sortedWith { a, b ->
                if (a.isDirectory && !b.isDirectory) -1
                else if (!a.isDirectory && b.isDirectory) 1
                else b.lastModified.compareTo(a.lastModified)
            }
            SortOption.TYPE -> files.sortedWith { a, b ->
                if (a.isDirectory && !b.isDirectory) -1
                else if (!a.isDirectory && b.isDirectory) 1
                else a.extension.compareTo(b.extension, ignoreCase = true)
            }
        }
    }
    
    suspend fun createDirectory(parentPath: String, name: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val newDir = File(parentPath, name)
                newDir.mkdir()
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    suspend fun deleteFile(filePath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val file = File(filePath)
                if (file.isDirectory) {
                    file.deleteRecursively()
                } else {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    suspend fun renameFile(oldPath: String, newName: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val oldFile = File(oldPath)
                val newFile = File(oldFile.parent, newName)
                oldFile.renameTo(newFile)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    suspend fun copyFile(sourcePath: String, destinationPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(sourcePath)
                val destinationFile = File(destinationPath)
                
                if (sourceFile.isDirectory) {
                    sourceFile.copyRecursively(destinationFile)
                } else {
                    sourceFile.copyTo(destinationFile, overwrite = true)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    suspend fun moveFile(sourcePath: String, destinationPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val sourceFile = File(sourcePath)
                val destinationFile = File(destinationPath)
                
                if (copyFile(sourcePath, destinationPath)) {
                    deleteFile(sourcePath)
                } else {
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }
    
    fun getStorageRoots(): List<String> {
        val roots = mutableListOf<String>()
        
        // 内部存储
        val internalStorage = Environment.getExternalStorageDirectory().absolutePath
        roots.add(internalStorage)
        
        // 尝试获取外部存储（SD卡等）
        try {
            val externalDirs = File("/storage").listFiles()
            externalDirs?.forEach { dir ->
                if (dir.isDirectory && dir.canRead() && !dir.name.equals("emulated", ignoreCase = true)) {
                    roots.add(dir.absolutePath)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return roots
    }
    
    fun getParentPath(currentPath: String): String? {
        val file = File(currentPath)
        return file.parent
    }
}