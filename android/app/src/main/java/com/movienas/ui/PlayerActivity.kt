package com.movienas.ui

import android.graphics.Color
import android.os.Bundle
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
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.movienas.R
import com.movienas.data.MovieBoxApi
import com.movienas.data.StreamData
import com.movienas.data.VideoStream
import com.movienas.player.MoviePlayerManager
import kotlinx.coroutines.launch

@UnstableApi
class PlayerActivity : AppCompatActivity() {

    private lateinit var playerView: PlayerView
    private lateinit var playerProgressBar: ProgressBar
    private lateinit var tvPlayerTitle: TextView
    private lateinit var btnPlayerBack: ImageView
    private lateinit var qualityContainer: LinearLayout

    private var exoPlayer: ExoPlayer? = null
    private var streamData: StreamData? = null
    private var currentQuality: String = ""

    private val detailPath: String by lazy { intent.getStringExtra("EXTRA_DETAIL_PATH") ?: "" }
    private val subjectId: String by lazy { intent.getStringExtra("EXTRA_SUBJECT_ID") ?: "" }
    private val movieTitle: String by lazy { intent.getStringExtra("EXTRA_TITLE") ?: "Pemutar Video" }
    private val season: Int by lazy { intent.getIntExtra("EXTRA_SEASON", 0) }
    private val episode: Int by lazy { intent.getIntExtra("EXTRA_EPISODE", 0) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on during video playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Enable immersive fullscreen
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )

        setContentView(R.layout.activity_player)

        initViews()
        initPlayer()
        loadStreamData()
    }

    private fun initViews() {
        playerView = findViewById(R.id.playerView)
        playerProgressBar = findViewById(R.id.playerProgressBar)
        tvPlayerTitle = findViewById(R.id.tvPlayerTitle)
        btnPlayerBack = findViewById(R.id.btnPlayerBack)
        qualityContainer = findViewById(R.id.qualityContainer)

        val displayTitle = if (season > 0 || episode > 0) "$movieTitle (Ep $episode)" else movieTitle
        tvPlayerTitle.text = displayTitle

        btnPlayerBack.setOnClickListener {
            finish()
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
    }

    private fun loadStreamData() {
        playerProgressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            val result = MovieBoxApi.getStream(
                detailPath = detailPath,
                subjectId = subjectId,
                season = season,
                episode = episode,
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
                    setupQualityButtons(data.streams)
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
        val subtitleUrl = streamData?.subtitles?.firstOrNull()?.srtUrl

        MoviePlayerManager.playVideo(
            context = this,
            player = exoPlayer ?: return,
            videoUrl = stream.url, // URL langsung dari CDN Alibaba tanpa proxy!
            subtitleUrl = subtitleUrl,
            autoPlay = true
        )
    }

    private fun setupQualityButtons(streams: List<VideoStream>) {
        qualityContainer.removeAllViews()

        for (s in streams) {
            val btn = Button(this).apply {
                text = s.quality
                textSize = 12f
                val isSelected = s.quality == currentQuality

                if (isSelected) {
                    setBackgroundResource(R.drawable.bg_button_red)
                    setTextColor(Color.WHITE)
                } else {
                    setBackgroundResource(R.drawable.bg_badge)
                    setTextColor(Color.parseColor("#94A3B8"))
                }

                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    resources.getDimensionPixelSize(android.R.dimen.app_icon_size) - 20
                ).apply {
                    setMargins(0, 0, 16, 0)
                }
                layoutParams = params

                setOnClickListener {
                    currentQuality = s.quality
                    setupQualityButtons(streams) // Refresh button styles

                    MoviePlayerManager.switchResolution(
                        context = this@PlayerActivity,
                        player = exoPlayer ?: return@setOnClickListener,
                        newVideoUrl = s.url,
                        subtitleUrl = streamData?.subtitles?.firstOrNull()?.srtUrl
                    )
                    Toast.makeText(this@PlayerActivity, "Kualitas: ${s.quality}", Toast.LENGTH_SHORT).show()
                }
            }
            qualityContainer.addView(btn)
        }
    }

    override fun onStop() {
        super.onStop()
        exoPlayer?.pause()
    }

    override fun onDestroy() {
        super.onDestroy()
        exoPlayer?.release()
        exoPlayer = null
    }
}
