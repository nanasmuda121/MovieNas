package com.movienas.ui

import android.app.PictureInPictureParams
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.movienas.R
import com.movienas.data.MovieBoxApi
import com.movienas.data.StreamData
import com.movienas.data.SubtitleItem
import com.movienas.data.VideoStream
import com.movienas.player.MoviePlayerManager
import kotlinx.coroutines.launch

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var playerProgressBar: ProgressBar
    private lateinit var playerTopBar: LinearLayout
    private lateinit var playerBottomBar: LinearLayout

    private lateinit var tvPlayerTitle: TextView
    private lateinit var tvPlayerSubtitle: TextView
    private lateinit var btnPlayerBack: View
    private lateinit var btnPlayerResize: View
    private lateinit var btnPlayerPip: View

    // Controls
    private lateinit var layoutEpisodeNav: LinearLayout
    private lateinit var btnPrevEp: View
    private lateinit var btnNextEp: View
    private lateinit var tvEpisodeIndicator: TextView

    private lateinit var qualityContainer: LinearLayout
    private lateinit var subtitleContainer: LinearLayout
    private lateinit var speedContainer: LinearLayout

    private var exoPlayer: ExoPlayer? = null
    private var streamData: StreamData? = null
    private var currentQuality: String = ""
    private var currentSubtitleUrl: String? = null
    private var currentSpeed: Float = 1.0f
    private var currentResizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT

    private var currentSeason: Int = 1
    private var currentEpisode: Int = 1

    private val detailPath: String by lazy { intent.getStringExtra("EXTRA_DETAIL_PATH") ?: "" }
    private val subjectId: String by lazy { intent.getStringExtra("EXTRA_SUBJECT_ID") ?: "" }
    private val movieTitle: String by lazy { intent.getStringExtra("EXTRA_TITLE") ?: "Pemutar Video" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during video playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Enable immersive sticky fullscreen
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        setContentView(R.layout.activity_player)

        currentSeason = intent.getIntExtra("EXTRA_SEASON", 1)
        currentEpisode = intent.getIntExtra("EXTRA_EPISODE", 1)

        initViews()
        initPlayer()
        setupSpeedControls()
        loadStreamData()
    }

    private fun initViews() {
        playerView = findViewById(R.id.playerView)
        playerProgressBar = findViewById(R.id.playerProgressBar)
        playerTopBar = findViewById(R.id.playerTopBar)
        playerBottomBar = findViewById(R.id.playerBottomBar)

        tvPlayerTitle = findViewById(R.id.tvPlayerTitle)
        tvPlayerSubtitle = findViewById(R.id.tvPlayerSubtitle)
        btnPlayerBack = findViewById(R.id.btnPlayerBack)
        btnPlayerResize = findViewById(R.id.btnPlayerResize)
        btnPlayerPip = findViewById(R.id.btnPlayerPip)

        layoutEpisodeNav = findViewById(R.id.layoutEpisodeNav)
        btnPrevEp = findViewById(R.id.btnPrevEp)
        btnNextEp = findViewById(R.id.btnNextEp)
        tvEpisodeIndicator = findViewById(R.id.tvEpisodeIndicator)

        qualityContainer = findViewById(R.id.qualityContainer)
        subtitleContainer = findViewById(R.id.subtitleContainer)
        speedContainer = findViewById(R.id.speedContainer)

        updateTitleInfo()

        btnPlayerBack.setOnClickListener {
            finish()
        }

        // PiP Button Click
        btnPlayerPip.setOnClickListener {
            enterPictureInPicture()
        }

        // Aspect Ratio Resize Toggle
        btnPlayerResize.setOnClickListener {
            cycleResizeMode()
        }

        // Prev & Next Episode clicks
        btnPrevEp.setOnClickListener {
            if (currentEpisode > 1) {
                currentEpisode--
                updateTitleInfo()
                loadStreamData()
            }
        }

        btnNextEp.setOnClickListener {
            currentEpisode++
            updateTitleInfo()
            loadStreamData()
        }
    }

    private fun updateTitleInfo() {
        tvPlayerTitle.text = movieTitle
        if (currentEpisode > 0) {
            tvPlayerSubtitle.text = "Season $currentSeason • Episode $currentEpisode"
            tvEpisodeIndicator.text = "Episode $currentEpisode"
            layoutEpisodeNav.visibility = View.VISIBLE
            btnPrevEp.visibility = if (currentEpisode > 1) View.VISIBLE else View.INVISIBLE
        } else {
            tvPlayerSubtitle.text = "Film Layar Lebar • Subtitle Indonesia"
            layoutEpisodeNav.visibility = View.GONE
        }
    }

    private fun initPlayer() {
        exoPlayer = MoviePlayerManager.createPlayer(
            context = this,
            onStateChanged = { isLoading, _ ->
                playerProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            },
            onError = { error ->
                playerProgressBar.visibility = if (exoPlayer?.isPlaying == true) View.GONE else View.VISIBLE
                Toast.makeText(this, "Gagal memutar video: ${error.message}", Toast.LENGTH_LONG).show()
            }
        )
        playerView.player = exoPlayer
        playerView.resizeMode = currentResizeMode
    }

    private fun loadStreamData() {
        playerProgressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = MovieBoxApi.getStream(
                detailPath = detailPath,
                subjectId = subjectId,
                season = currentSeason,
                episode = currentEpisode,
                lang = "id"
            )
            playerProgressBar.visibility = View.GONE

            result.onSuccess { data ->
                streamData = data

                if (data.streams.isNotEmpty()) {
                    val defaultQuality = data.streams.find { it.resolution == 720 }
                        ?: data.streams.find { it.resolution == 1080 }
                        ?: data.streams.first()

                    currentQuality = defaultQuality.quality

                    // Default subtitle: Indonesian if available
                    val idSub = data.subtitles.find {
                        it.languageCode.equals("id", ignoreCase = true) ||
                                it.languageName.contains("Indonesia", ignoreCase = true)
                    }
                    currentSubtitleUrl = idSub?.srtUrl ?: data.subtitles.firstOrNull()?.srtUrl

                    setupQualityButtons(data.streams)
                    setupSubtitleButtons(data.subtitles)
                    playSelectedStream(defaultQuality)
                } else {
                    Toast.makeText(this@PlayerActivity, "Sumber video belum tersedia dari server", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { err ->
                Toast.makeText(this@PlayerActivity, "Error: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun playSelectedStream(stream: VideoStream) {
        MoviePlayerManager.playVideo(
            context = this,
            player = exoPlayer ?: return,
            videoUrl = stream.url,
            subtitleUrl = currentSubtitleUrl,
            autoPlay = true
        )
    }

    private fun setupQualityButtons(streams: List<VideoStream>) {
        qualityContainer.removeAllViews()

        for (s in streams) {
            val btn = Button(this).apply {
                val label = if (s.sizeFormatted.isNotEmpty()) "${s.quality} (${s.sizeFormatted})" else s.quality
                text = label
                textSize = 11f
                val isSelected = s.quality == currentQuality

                if (isSelected) {
                    setBackgroundResource(R.drawable.bg_pill_active)
                    setTextColor(Color.WHITE)
                } else {
                    setBackgroundResource(R.drawable.bg_pill_inactive)
                    setTextColor(Color.parseColor("#94A3B8"))
                }

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    resources.getDimensionPixelSize(android.R.dimen.app_icon_size) - 24
                ).apply {
                    setMargins(0, 0, 12, 0)
                }
                layoutParams = params

                setOnClickListener {
                    currentQuality = s.quality
                    setupQualityButtons(streams)

                    MoviePlayerManager.switchResolution(
                        context = this@PlayerActivity,
                        player = exoPlayer ?: return@setOnClickListener,
                        newVideoUrl = s.url,
                        subtitleUrl = currentSubtitleUrl
                    )
                    Toast.makeText(this@PlayerActivity, "Kualitas: ${s.quality}", Toast.LENGTH_SHORT).show()
                }
            }
            qualityContainer.addView(btn)
        }
    }

    private fun setupSubtitleButtons(subtitles: List<SubtitleItem>) {
        subtitleContainer.removeAllViews()

        // Button Mati / Off
        val offBtn = Button(this).apply {
            text = "Mati"
            textSize = 11f
            val isSelected = currentSubtitleUrl == null
            if (isSelected) {
                setBackgroundResource(R.drawable.bg_pill_active)
                setTextColor(Color.WHITE)
            } else {
                setBackgroundResource(R.drawable.bg_pill_inactive)
                setTextColor(Color.parseColor("#94A3B8"))
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                resources.getDimensionPixelSize(android.R.dimen.app_icon_size) - 24
            ).apply { setMargins(0, 0, 12, 0) }
            layoutParams = params

            setOnClickListener {
                currentSubtitleUrl = null
                setupSubtitleButtons(subtitles)
                val currentStream = streamData?.streams?.find { it.quality == currentQuality }
                    ?: streamData?.streams?.firstOrNull()
                if (currentStream != null) {
                    MoviePlayerManager.switchResolution(
                        context = this@PlayerActivity,
                        player = exoPlayer ?: return@setOnClickListener,
                        newVideoUrl = currentStream.url,
                        subtitleUrl = null
                    )
                }
                Toast.makeText(this@PlayerActivity, "Subtitle dinonaktifkan", Toast.LENGTH_SHORT).show()
            }
        }
        subtitleContainer.addView(offBtn)

        // Subtitle Language Buttons
        for (sub in subtitles) {
            val btn = Button(this).apply {
                text = sub.languageName
                textSize = 11f
                val isSelected = sub.srtUrl == currentSubtitleUrl

                if (isSelected) {
                    setBackgroundResource(R.drawable.bg_pill_active)
                    setTextColor(Color.WHITE)
                } else {
                    setBackgroundResource(R.drawable.bg_pill_inactive)
                    setTextColor(Color.parseColor("#94A3B8"))
                }

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    resources.getDimensionPixelSize(android.R.dimen.app_icon_size) - 24
                ).apply { setMargins(0, 0, 12, 0) }
                layoutParams = params

                setOnClickListener {
                    currentSubtitleUrl = sub.srtUrl
                    setupSubtitleButtons(subtitles)

                    val currentStream = streamData?.streams?.find { it.quality == currentQuality }
                        ?: streamData?.streams?.firstOrNull()
                    if (currentStream != null) {
                        MoviePlayerManager.switchResolution(
                            context = this@PlayerActivity,
                            player = exoPlayer ?: return@setOnClickListener,
                            newVideoUrl = currentStream.url,
                            subtitleUrl = sub.srtUrl
                        )
                    }
                    Toast.makeText(this@PlayerActivity, "Subtitle: ${sub.languageName}", Toast.LENGTH_SHORT).show()
                }
            }
            subtitleContainer.addView(btn)
        }
    }

    private fun setupSpeedControls() {
        speedContainer.removeAllViews()
        val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

        for (spd in speeds) {
            val btn = Button(this).apply {
                text = "${spd}x"
                textSize = 11f
                val isSelected = spd == currentSpeed

                if (isSelected) {
                    setBackgroundResource(R.drawable.bg_pill_active)
                    setTextColor(Color.WHITE)
                } else {
                    setBackgroundResource(R.drawable.bg_pill_inactive)
                    setTextColor(Color.parseColor("#94A3B8"))
                }

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    resources.getDimensionPixelSize(android.R.dimen.app_icon_size) - 24
                ).apply { setMargins(0, 0, 12, 0) }
                layoutParams = params

                setOnClickListener {
                    currentSpeed = spd
                    exoPlayer?.playbackParameters = PlaybackParameters(spd)
                    setupSpeedControls()
                    Toast.makeText(this@PlayerActivity, "Kecepatan: ${spd}x", Toast.LENGTH_SHORT).show()
                }
            }
            speedContainer.addView(btn)
        }
    }

    private fun cycleResizeMode() {
        val (nextMode, modeName) = when (currentResizeMode) {
            AspectRatioFrameLayout.RESIZE_MODE_FIT -> Pair(AspectRatioFrameLayout.RESIZE_MODE_ZOOM, "Zoom (Layar Penuh)")
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> Pair(AspectRatioFrameLayout.RESIZE_MODE_FILL, "Fill (Regang)")
            else -> Pair(AspectRatioFrameLayout.RESIZE_MODE_FIT, "Fit (Standar 16:9)")
        }
        currentResizeMode = nextMode
        playerView.resizeMode = nextMode
        Toast.makeText(this, "Rasio Layar: $modeName", Toast.LENGTH_SHORT).show()
    }

    private fun enterPictureInPicture() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val aspectRatio = Rational(16, 9)
            val pipParams = PictureInPictureParams.Builder()
                .setAspectRatio(aspectRatio)
                .build()
            enterPictureInPictureMode(pipParams)
        } else {
            Toast.makeText(this, "Picture-in-Picture butuh Android 8.0+", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            playerTopBar.visibility = View.GONE
            playerBottomBar.visibility = View.GONE
            playerView.useController = false
        } else {
            playerTopBar.visibility = View.VISIBLE
            playerBottomBar.visibility = View.VISIBLE
            playerView.useController = true
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && exoPlayer?.isPlaying == true) {
            enterPictureInPicture()
        }
    }

    override fun onStop() {
        super.onStop()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInPictureInPictureMode) {
            exoPlayer?.pause()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}
