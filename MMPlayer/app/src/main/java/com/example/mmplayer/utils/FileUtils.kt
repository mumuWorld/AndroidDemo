package com.example.mmplayer.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object FileUtils {
    
    fun shareFile(context: Context, file: File) {
        try {
            val uri: Uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                Uri.fromFile(file)
            }

            val mimeType = getMimeType(file)
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "分享文件: ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "分享文件")
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun shareMultipleFiles(context: Context, files: List<File>) {
        try {
            val uris = mutableListOf<Uri>()
            
            files.forEach { file ->
                val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } else {
                    Uri.fromFile(file)
                }
                uris.add(uri)
            }

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND_MULTIPLE
                type = "*/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "分享文件")
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            // 图片
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "webp" -> "image/webp"
            
            // 视频
            "mp4" -> "video/mp4"
            "avi" -> "video/x-msvideo"
            "mkv" -> "video/x-matroska"
            "mov" -> "video/quicktime"
            "wmv" -> "video/x-ms-wmv"
            "flv" -> "video/x-flv"
            "3gp" -> "video/3gpp"
            "webm" -> "video/webm"
            
            // 音频
            "mp3" -> "audio/mpeg"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            "m4a" -> "audio/mp4"
            
            // 文档
            "txt" -> "text/plain"
            "pdf" -> "application/pdf"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "ppt" -> "application/vnd.ms-powerpoint"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            
            // 压缩包
            "zip" -> "application/zip"
            "rar" -> "application/x-rar-compressed"
            "7z" -> "application/x-7z-compressed"
            
            // APK
            "apk" -> "application/vnd.android.package-archive"
            
            else -> "*/*"
        }
    }
}