package com.example.mmplayer.ui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.*
import android.widget.PopupMenu
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mmplayer.R
import com.example.mmplayer.adapter.PlaylistAdapter
import com.example.mmplayer.databinding.ActivityVideoPlayerBinding
import com.example.mmplayer.model.FileItem
import com.example.mmplayer.service.FileManager
import com.example.mmplayer.utils.VideoGestureDetector
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVideoPlayerBinding
    private var exoPlayer: ExoPlayer? = null
    private lateinit var gestureDetector: GestureDetector
    private lateinit var audioManager: AudioManager
    private lateinit var playlistAdapter: PlaylistAdapter
    private val fileManager = FileManager()
    
    private var isControlsVisible = true
    private var isFullscreen = false
    private var currentVideoPath: String? = null
    private var currentVideoTitle: String? = null
    private var playbackSpeed = 1.0f
    private var playlist = mutableListOf<FileItem>()
    private var currentPlaylistIndex = 0
    
    private val hideControlsHandler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }
    
    private val progressUpdateHandler = Handler(Looper.getMainLooper())
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            updateProgress()
            progressUpdateHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSystemUI()
        initializeComponents()
        setupClickListeners()
        setupGestureDetector()
        setupPlaylist()

        val videoPath = intent.getStringExtra("video_path")
        val videoTitle = intent.getStringExtra("video_title")
        
        if (videoPath != null) {
            currentVideoPath = videoPath
            currentVideoTitle = videoTitle
            loadVideo(videoPath, videoTitle)
            loadPlaylistFromDirectory(File(videoPath).parent ?: "")
        }
    }

    private fun setupSystemUI() {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    private fun initializeComponents() {
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            hideLoadingIndicator()
                            startProgressUpdates()
                        }
                        Player.STATE_BUFFERING -> showLoadingIndicator()
                        Player.STATE_ENDED -> onVideoEnded()
                    }
                }
                
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    updatePlayPauseButtons(isPlaying)
                    if (isPlaying) {
                        scheduleControlsHide()
                    } else {
                        cancelControlsHide()
                    }
                }
            })
        }
        
        binding.playerView.player = exoPlayer
    }

    private fun setupClickListeners() {
        binding.ivBack.setOnClickListener { 
            if (isFullscreen) {
                exitFullscreen()
            } else {
                finish()
            }
        }
        
        binding.ivMore.setOnClickListener { showMoreOptionsMenu(it) }
        
        binding.ivPlayPause.setOnClickListener { togglePlayPause() }
        binding.ivCenterPlayPause.setOnClickListener { togglePlayPause() }
        
        binding.ivPrevious.setOnClickListener { playPrevious() }
        binding.ivNext.setOnClickListener { playNext() }
        
        binding.ivFullscreen.setOnClickListener { toggleFullscreen() }
        
        binding.tvSpeed.setOnClickListener { showSpeedDialog() }
        
        binding.ivPlaylist.setOnClickListener { togglePlaylist() }
        
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    exoPlayer?.seekTo((progress * 1000).toLong())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // 点击播放器区域切换控制栏显示
        binding.playerView.setOnClickListener {
            if (isControlsVisible) {
                hideControls()
            } else {
                showControls()
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, VideoGestureDetector(
            onSingleTap = {
                if (isControlsVisible) hideControls() else showControls()
            },
            onDoubleTap = { togglePlayPause() },
            onLongPress = { 
                setPlaybackSpeed(2.0f)
                showSpeedIndicator("2.0x")
            },
            onLongPressUp = {
                setPlaybackSpeed(playbackSpeed)
                hideSpeedIndicator()
            },
            onHorizontalScroll = { distanceX ->
                val seekTime = (distanceX * 0.1f).toInt()
                seekVideo(seekTime)
            },
            onVerticalScrollLeft = { distanceY ->
                adjustBrightness(distanceY)
            },
            onVerticalScrollRight = { distanceY ->
                adjustVolume(distanceY)
            }
        ))
        
        binding.playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun setupPlaylist() {
        playlistAdapter = PlaylistAdapter { fileItem ->
            val index = playlist.indexOfFirst { it.path == fileItem.path }
            if (index != -1) {
                currentPlaylistIndex = index
                loadVideo(fileItem.path, fileItem.name)
                hidePlaylist()
            }
        }
        
        binding.rvPlaylist.apply {
            layoutManager = LinearLayoutManager(this@VideoPlayerActivity)
            adapter = playlistAdapter
        }
    }

    private fun loadVideo(videoPath: String, title: String?) {
        currentVideoPath = videoPath
        currentVideoTitle = title
        
        binding.tvTitle.text = title ?: File(videoPath).name
        
        val dataSourceFactory = DefaultDataSourceFactory(this, "ExoPlayerDemo")
        val mediaSource: MediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(videoPath.toUri()))
        
        exoPlayer?.apply {
            setMediaSource(mediaSource)
            prepare()
            playWhenReady = true
        }
    }

    private fun loadPlaylistFromDirectory(directoryPath: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val videoFiles = fileManager.getVideoFiles(directoryPath)
                playlist.clear()
                playlist.addAll(videoFiles)
                
                currentVideoPath?.let { currentPath ->
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
        if (playlist.isNotEmpty() && currentPlaylistIndex > 0) {
            currentPlaylistIndex--
            val previousVideo = playlist[currentPlaylistIndex]
            loadVideo(previousVideo.path, previousVideo.name)
        }
    }

    private fun playNext() {
        if (playlist.isNotEmpty() && currentPlaylistIndex < playlist.size - 1) {
            currentPlaylistIndex++
            val nextVideo = playlist[currentPlaylistIndex]
            loadVideo(nextVideo.path, nextVideo.name)
        }
    }

    private fun onVideoEnded() {
        // 自动播放下一个视频
        if (currentPlaylistIndex < playlist.size - 1) {
            playNext()
        }
    }

    private fun toggleFullscreen() {
        if (isFullscreen) {
            exitFullscreen()
        } else {
            enterFullscreen()
        }
    }

    private fun enterFullscreen() {
        isFullscreen = true
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        )
    }

    private fun exitFullscreen() {
        isFullscreen = false
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
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

    private fun updatePlayPauseButtons(isPlaying: Boolean) {
        val iconRes = if (isPlaying) {
            R.drawable.ic_launcher_foreground // pause icon
        } else {
            R.drawable.ic_launcher_foreground // play icon
        }
        
        binding.ivPlayPause.setImageResource(iconRes)
        binding.ivCenterPlayPause.setImageResource(iconRes)
    }

    private fun setPlaybackSpeed(speed: Float) {
        exoPlayer?.setPlaybackParameters(PlaybackParameters(speed))
        if (speed == playbackSpeed) {
            binding.tvSpeed.text = "${speed}x"
        }
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

    private fun showMoreOptionsMenu(view: View) {
        PopupMenu(this, view).apply {
            menuInflater.inflate(R.menu.video_player_menu, menu)
            setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_rotate -> {
                        toggleFullscreen()
                        true
                    }
                    R.id.action_info -> {
                        showVideoInfo()
                        true
                    }
                    else -> false
                }
            }
            show()
        }
    }

    private fun showVideoInfo() {
        // TODO: 显示视频信息对话框
    }

    private fun togglePlaylist() {
        binding.rvPlaylist.visibility = if (binding.rvPlaylist.visibility == View.VISIBLE) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    private fun hidePlaylist() {
        binding.rvPlaylist.visibility = View.GONE
    }

    private fun seekVideo(seconds: Int) {
        exoPlayer?.let { player ->
            val newPosition = player.currentPosition + (seconds * 1000)
            val clampedPosition = newPosition.coerceIn(0, player.duration)
            player.seekTo(clampedPosition)
            
            showSeekIndicator(seconds)
        }
    }

    private fun adjustBrightness(distanceY: Float) {
        try {
            val window = window
            val layoutParams = window.attributes
            val currentBrightness = layoutParams.screenBrightness
            val delta = distanceY / window.decorView.height
            val newBrightness = (currentBrightness - delta).coerceIn(0.0f, 1.0f)
            
            layoutParams.screenBrightness = newBrightness
            window.attributes = layoutParams
            
            showBrightnessIndicator((newBrightness * 100).toInt())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun adjustVolume(distanceY: Float) {
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val delta = (distanceY / binding.playerView.height * maxVolume).toInt()
        val newVolume = (currentVolume - delta).coerceIn(0, maxVolume)
        
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        
        showVolumeIndicator((newVolume * 100 / maxVolume))
    }

    private fun showBrightnessIndicator(brightness: Int) {
        binding.llBrightnessIndicator.visibility = View.VISIBLE
        binding.tvBrightness.text = "$brightness%"
        
        // 2秒后隐藏
        Handler(Looper.getMainLooper()).postDelayed({
            binding.llBrightnessIndicator.visibility = View.GONE
        }, 2000)
    }

    private fun showVolumeIndicator(volume: Int) {
        binding.llVolumeIndicator.visibility = View.VISIBLE
        binding.tvVolume.text = "$volume%"
        
        // 2秒后隐藏
        Handler(Looper.getMainLooper()).postDelayed({
            binding.llVolumeIndicator.visibility = View.GONE
        }, 2000)
    }

    private fun showSeekIndicator(seconds: Int) {
        binding.llSeekIndicator.visibility = View.VISIBLE
        binding.tvSeekTime.text = if (seconds > 0) "+${seconds}s" else "${seconds}s"
        
        // 1秒后隐藏
        Handler(Looper.getMainLooper()).postDelayed({
            binding.llSeekIndicator.visibility = View.GONE
        }, 1000)
    }

    private fun showSpeedIndicator(speed: String) {
        // TODO: 显示倍速指示器
    }

    private fun hideSpeedIndicator() {
        // TODO: 隐藏倍速指示器
    }

    private fun showLoadingIndicator() {
        // TODO: 显示加载指示器
    }

    private fun hideLoadingIndicator() {
        // TODO: 隐藏加载指示器
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        
        when (newConfig.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                // 横屏时的处理
            }
            Configuration.ORIENTATION_PORTRAIT -> {
                // 竖屏时的处理
            }
        }
    }

    override fun onBackPressed() {
        when {
            binding.rvPlaylist.visibility == View.VISIBLE -> hidePlaylist()
            isFullscreen -> exitFullscreen()
            else -> super.onBackPressed()
        }
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
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
        cancelControlsHide()
    }
}