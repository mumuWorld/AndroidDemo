package com.example.mmplayer.model

import java.io.File
import java.util.Date

data class FileItem(
    val file: File,
    val name: String = file.name,
    val path: String = file.absolutePath,
    val isDirectory: Boolean = file.isDirectory,
    val size: Long = if (file.isDirectory) 0 else file.length(),
    val lastModified: Date = Date(file.lastModified()),
    val extension: String = if (file.isDirectory) "" else file.extension.lowercase(),
    val mimeType: String = getMimeType(file)
) {
    
    val isVideo: Boolean
        get() = mimeType.startsWith("video/") || 
                extension in listOf("mp4", "avi", "mkv", "mov", "wmv", "flv", "3gp", "webm", "m4v", "ts")
    
    val isAudio: Boolean
        get() = mimeType.startsWith("audio/") || 
                extension in listOf("mp3", "wav", "aac", "flac", "ogg", "m4a", "wma", "amr")
    
    val isImage: Boolean
        get() = mimeType.startsWith("image/") || 
                extension in listOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg")
    
    val isDocument: Boolean
        get() = extension in listOf("txt", "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx")
    
    val isArchive: Boolean
        get() = extension in listOf("zip", "rar", "7z", "tar", "gz")
    
    val displaySize: String
        get() = if (isDirectory) {
            ""
        } else {
            formatFileSize(size)
        }
    
    companion object {
        private fun getMimeType(file: File): String {
            return when (file.extension.lowercase()) {
                // Video
                "mp4" -> "video/mp4"
                "avi" -> "video/x-msvideo"
                "mkv" -> "video/x-matroska"
                "mov" -> "video/quicktime"
                "wmv" -> "video/x-ms-wmv"
                "flv" -> "video/x-flv"
                "3gp" -> "video/3gpp"
                "webm" -> "video/webm"
                "m4v" -> "video/x-m4v"
                "ts" -> "video/mp2t"
                
                // Audio
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "aac" -> "audio/aac"
                "flac" -> "audio/flac"
                "ogg" -> "audio/ogg"
                "m4a" -> "audio/mp4"
                "wma" -> "audio/x-ms-wma"
                "amr" -> "audio/amr"
                
                // Image
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "bmp" -> "image/bmp"
                "webp" -> "image/webp"
                "svg" -> "image/svg+xml"
                
                // Document
                "txt" -> "text/plain"
                "pdf" -> "application/pdf"
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "xls" -> "application/vnd.ms-excel"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "ppt" -> "application/vnd.ms-powerpoint"
                "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
                
                // Archive
                "zip" -> "application/zip"
                "rar" -> "application/x-rar-compressed"
                "7z" -> "application/x-7z-compressed"
                "tar" -> "application/x-tar"
                "gz" -> "application/gzip"
                
                else -> "application/octet-stream"
            }
        }
        
        private fun formatFileSize(bytes: Long): String {
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            var size = bytes.toDouble()
            var unitIndex = 0
            
            while (size >= 1024 && unitIndex < units.size - 1) {
                size /= 1024
                unitIndex++
            }
            
            return String.format("%.1f %s", size, units[unitIndex])
        }
    }
}