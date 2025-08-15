package com.example.mmplayer.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.PopupMenu
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.mmplayer.R
import com.example.mmplayer.adapter.PlaylistAdapter
import com.example.mmplayer.databinding.ActivityAudioPlayerBinding
import com.example.mmplayer.model.FileItem
import com.example.mmplayer.service.FileManager
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AudioPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAudioPlayerBinding
    private var exoPlayer: ExoPlayer? = null
    private lateinit var playlistAdapter: PlaylistAdapter
    private val fileManager = FileManager()
    
    private var currentAudioPath: String? = null
    private var currentAudioTitle: String? = null
    private var playbackSpeed = 1.0f
    private var playlist = mutableListOf<FileItem>()
    private var currentPlaylistIndex = 0
    private var isShuffleEnabled = false
    private var repeatMode = RepeatMode.OFF
    
    private val progressUpdateHandler = Handler(Looper.getMainLooper())
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            progressUpdateHandler.postDelayed(this, 1000)
        }
    }

    enum class RepeatMode {
        OFF, ONE, ALL
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAudioPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        initializePlayer()
        setupClickListeners()
        setupPlaylist()

        val audioPath = intent.getStringExtra("audio_path")
        val audioTitle = intent.getStringExtra("audio_title")
        
        if (audioPath != null) {
            currentAudioPath = audioPath
            currentAudioTitle = audioTitle
            loadAudio(audioPath, audioTitle)
            loadPlaylistFromDirectory(File(audioPath).parent ?: "")
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun initializePlayer() {
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> startProgressUpdates()
                        Player.STATE_ENDED -> onAudioEnded()
                    }
                }
                
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayPauseButton(isPlaying)
                }
            })
        }
    }

    private fun setupClickListeners() {
        binding.ivPlayPause.setOnClickListener { togglePlayPause() }
        binding.ivPrevious.setOnClickListener { playPrevious() }
        binding.ivNext.setOnClickListener { playNext() }
        binding.ivShuffle.setOnClickListener { toggleShuffle() }
        binding.ivRepeat.setOnClickListener { toggleRepeat() }
        binding.ivPlaylist.setOnClickListener { togglePlaylist() }
        binding.tvSpeed.setOnClickListener { showSpeedDialog() }
        binding.ivMore.setOnClickListener { showMoreOptionsMenu(it) }
        
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    exoPlayer?.seekTo((progress * 1000).toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupPlaylist() {
        playlistAdapter = PlaylistAdapter { fileItem ->
            val index = playlist.indexOfFirst { it.path == fileItem.path }
            if (index != -1) {
                currentPlaylistIndex = index
                loadAudio(fileItem.path, fileItem.name)
                hidePlaylist()
            }
        }
        
        binding.rvAudioPlaylist.apply {
            layoutManager = LinearLayoutManager(this@AudioPlayerActivity)
            adapter = playlistAdapter
        }
    }

    private fun loadAudio(audioPath: String, title: String?) {
        currentAudioPath = audioPath
        currentAudioTitle = title
        
        val file = File(audioPath)
        binding.tvSongTitle.text = title ?: file.nameWithoutExtension
        binding.tvArtist.text = "未知艺术家" // TODO: 从音频文件元数据获取
        
        // 加载专辑封面（如果有的话）
        loadAlbumArt(audioPath)
        
        val dataSourceFactory = DefaultDataSourceFactory(this, "AudioPlayer")
        val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(audioPath.toUri()))
        
        exoPlayer?.apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
        
        // 更新播放列表中的当前播放项
        playlistAdapter.setCurrentPlaying(audioPath)
    }

    private fun loadAlbumArt(audioPath: String) {
        // 尝试从音频文件夹中找到专辑封面图片
        val audioFile = File(audioPath)
        val parentDir = audioFile.parentFile
        
        val coverFiles = listOf("cover.jpg", "cover.png", "album.jpg", "album.png", "folder.jpg")
        var coverFound = false
        
        parentDir?.listFiles()?.forEach { file ->
            if (coverFiles.any { it.equals(file.name, ignoreCase = true) }) {
                Glide.with(this)
                    .load(file)
                    .placeholder(R.drawable.ic_launcher_foreground)
                    .into(binding.ivAlbumArt)
                coverFound = true
                return@forEach
            }
        }
        
        if (!coverFound) {
            // 使用默认图片
            binding.ivAlbumArt.setImageResource(R.drawable.ic_launcher_foreground)
        }
    }

    private fun loadPlaylistFromDirectory(directoryPath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val audioFiles = fileManager.getAudioFiles(directoryPath)
                playlist.clear()
                playlist.addAll(audioFiles)
                
                currentAudioPath?.let { currentPath ->
                    currentPlaylistIndex = playlist.indexOfFirst { it.path == currentPath }
                }
                
                withContext(Dispatchers.Main) {
                    playlistAdapter.submitList(playlist.toList())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun togglePlayPause() {
        exoPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    private fun playPrevious() {
        if (playlist.isNotEmpty()) {
            val nextIndex = if (isShuffleEnabled) {
                playlist.indices.random()
            } else {
                if (currentPlaylistIndex > 0) currentPlaylistIndex - 1 else playlist.size - 1
            }
            
            currentPlaylistIndex = nextIndex
            val previousAudio = playlist[currentPlaylistIndex]
            loadAudio(previousAudio.path, previousAudio.name)
        }
    }

    private fun playNext() {
        if (playlist.isNotEmpty()) {
            val nextIndex = if (isShuffleEnabled) {
                playlist.indices.random()
            } else {
                if (currentPlaylistIndex < playlist.size - 1) currentPlaylistIndex + 1 else 0
            }
            
            currentPlaylistIndex = nextIndex
            val nextAudio = playlist[currentPlaylistIndex]
            loadAudio(nextAudio.path, nextAudio.name)
        }
    }

    private fun onAudioEnded() {
        when (repeatMode) {
            RepeatMode.ONE -> {
                exoPlayer?.seekTo(0)
                exoPlayer?.play()
            }
            RepeatMode.ALL -> playNext()
            RepeatMode.OFF -> {
                if (currentPlaylistIndex < playlist.size - 1) {
                    playNext()
                }
            }
        }
    }

    private fun toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled
        binding.ivShuffle.alpha = if (isShuffleEnabled) 1.0f else 0.7f
    }

    private fun toggleRepeat() {
        repeatMode = when (repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        
        val alpha = if (repeatMode == RepeatMode.OFF) 0.7f else 1.0f
        binding.ivRepeat.alpha = alpha
        
        // TODO: 更新重复按钮图标
    }

    private fun togglePlaylist() {
        binding.rvAudioPlaylist.visibility = if (binding.rvAudioPlaylist.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun hidePlaylist() {
        binding.rvAudioPlaylist.visibility = View.GONE
    }

    private fun showSpeedDialog() {
        val speeds = arrayOf("0.5x", "0.75x", "1.0x", "1.25x", "1.5x", "2.0x")
        val speedValues = arrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
        
        PopupMenu(this, binding.tvSpeed).apply {
            speeds.forEachIndexed { index, speed ->
                menu.add(speed).setOnMenuItemClickListener {
                    playbackSpeed = speedValues[index]
                    setPlaybackSpeed(playbackSpeed)
                    true
                }
            }
            show()
        }
    }

    private fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackParameters(PlaybackParameters(speed))
        binding.tvSpeed.text = "${speed}x"
    }

    private fun showMoreOptionsMenu(view: View) {
        PopupMenu(this, view).apply {
            menuInflater.inflate(R.menu.audio_player_menu, menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_info -> {
                        showAudioInfo()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun showAudioInfo() {
        // TODO: 显示音频信息对话框
    }

    private fun updatePlayPauseButton(isPlaying: Boolean) {
        val iconRes = if (isPlaying) {
            R.drawable.ic_launcher_foreground // pause icon
        } else {
            R.drawable.ic_launcher_foreground // play icon
        }
        binding.ivPlayPause.setImageResource(iconRes)
    }

    private fun startProgressUpdates() {
        progressUpdateHandler.post(progressUpdateRunnable)
    }

    private fun stopProgressUpdates() {
        progressUpdateHandler.removeCallbacks(progressUpdateRunnable)
    }

    private fun updateProgress() {
        exoPlayer?.let { player ->
            val currentPosition = player.currentPosition / 1000
            val duration = player.duration / 1000
            
            binding.tvCurrentTime.text = formatTime(currentPosition)
            binding.tvTotalTime.text = formatTime(duration)
            
            if (duration > 0) {
                binding.seekBar.progress = (currentPosition * 100 / duration).toInt()
            }
        }
    }

    private fun formatTime(seconds: Long): String {
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%02d:%02d", minutes, remainingSeconds)
    }

    override fun onBackPressed() {
        when {
            binding.rvAudioPlaylist.visibility == View.VISIBLE -> hidePlaylist()
            else -> super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        stopProgressUpdates()
    }

    override fun onResume() {
        super.onResume()
        if (exoPlayer?.playWhenReady == true) {
            startProgressUpdates()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
        stopProgressUpdates()
    }
}