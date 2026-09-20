package com.movienas.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.movienas.R
import com.movienas.data.MovieBoxApi
import com.movienas.data.MovieDetail
import com.movienas.ui.adapter.EpisodeAdapter
import kotlinx.coroutines.launch

class DetailActivity : AppCompatActivity() {

    private lateinit var detailProgressBar: ProgressBar
    private lateinit var detailScrollView: NestedScrollView
    private lateinit var btnBack: View
    private lateinit var ivDetailBackdrop: ImageView
    private lateinit var ivDetailPoster: ImageView
    private lateinit var tvDetailTitle: TextView
    private lateinit var tvDetailMeta: TextView
    private lateinit var tvDetailRating: TextView
    private lateinit var btnDetailPlay: View
    private lateinit var tvDetailPlayText: TextView
    private lateinit var tvDetailSynopsis: TextView

    // Episodes
    private lateinit var episodesSection: LinearLayout
    private lateinit var tvEpisodesTitle: TextView
    private lateinit var rvEpisodes: RecyclerView

    private var movieDetail: MovieDetail? = null
    private var selectedSeason: Int = 1
    private var selectedEpisode: Int = 1

    private val detailPath: String by lazy {
        intent.getStringExtra("EXTRA_DETAIL_PATH") ?: ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        initViews()
        loadDetailData()
    }

    private fun initViews() {
        detailProgressBar = findViewById(R.id.detailProgressBar)
        detailScrollView = findViewById(R.id.detailScrollView)
        btnBack = findViewById(R.id.btnBack)
        ivDetailBackdrop = findViewById(R.id.ivDetailBackdrop)
        ivDetailPoster = findViewById(R.id.ivDetailPoster)
        tvDetailTitle = findViewById(R.id.tvDetailTitle)
        tvDetailMeta = findViewById(R.id.tvDetailMeta)
        tvDetailRating = findViewById(R.id.tvDetailRating)
        btnDetailPlay = findViewById(R.id.btnDetailPlay)
        tvDetailPlayText = findViewById(R.id.tvDetailPlayText)
        tvDetailSynopsis = findViewById(R.id.tvDetailSynopsis)

        episodesSection = findViewById(R.id.episodesSection)
        tvEpisodesTitle = findViewById(R.id.tvEpisodesTitle)
        rvEpisodes = findViewById(R.id.rvEpisodes)

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadDetailData() {
        if (detailPath.isEmpty()) {
            Toast.makeText(this, "Path film tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        detailProgressBar.visibility = View.VISIBLE
        detailScrollView.visibility = View.GONE

        lifecycleScope.launch {
            val result = MovieBoxApi.getDetail(detailPath, "id")
            detailProgressBar.visibility = View.GONE

            result.onSuccess { detail ->
                movieDetail = detail
                detailScrollView.visibility = View.VISIBLE
                bindDetail(detail)
            }.onFailure { err ->
                Toast.makeText(this@DetailActivity, "Gagal memuat detail: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun bindDetail(detail: MovieDetail) {
        tvDetailTitle.text = detail.title

        val metaParts = mutableListOf<String>()
        if (detail.year.isNotEmpty()) metaParts.add(detail.year)
        if (detail.durationFormatted.isNotEmpty()) metaParts.add(detail.durationFormatted)
        if (detail.genre.isNotEmpty()) metaParts.add(detail.genre.take(2).joinToString(", "))
        tvDetailMeta.text = metaParts.joinToString(" • ")

        tvDetailRating.text = "★ ${detail.imdbRating}"
        tvDetailSynopsis.text = if (detail.description.isNotEmpty()) detail.description else "Tidak ada sinopsis tersedia."

        Glide.with(this)
            .load(detail.coverUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(ivDetailBackdrop)

        Glide.with(this)
            .load(detail.coverUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(ivDetailPoster)

        // Movie vs Series configuration
        if (detail.isEpisodic && detail.seasons.isNotEmpty()) {
            episodesSection.visibility = View.VISIBLE
            val firstSeason = detail.seasons[0]
            selectedSeason = firstSeason.seasonNumber
            selectedEpisode = firstSeason.allEpisodes.firstOrNull() ?: 1

            tvDetailPlayText.text = "▶ Tonton Episode $selectedEpisode"
            btnDetailPlay.setOnClickListener {
                startPlayer(selectedSeason, selectedEpisode)
            }

            rvEpisodes.layoutManager = GridLayoutManager(this, 5)
            val episodeAdapter = EpisodeAdapter(firstSeason.allEpisodes, selectedEpisode) { clickedEp ->
                selectedEpisode = clickedEp
                tvDetailPlayText.text = "▶ Tonton Episode $selectedEpisode"
                startPlayer(selectedSeason, selectedEpisode)
            }
            rvEpisodes.adapter = episodeAdapter
        } else {
            episodesSection.visibility = View.GONE
            tvDetailPlayText.text = "▶ Tonton Film Sekarang"
            btnDetailPlay.setOnClickListener {
                startPlayer(0, 0)
            }
        }
    }

    private fun startPlayer(season: Int, episode: Int) {
        val detail = movieDetail ?: return
        val episodesList = ArrayList<Int>()
        detail.seasons.firstOrNull()?.allEpisodes?.let { episodesList.addAll(it) }

        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("EXTRA_DETAIL_PATH", detail.detailPath)
            putExtra("EXTRA_SUBJECT_ID", detail.subjectId)
            putExtra("EXTRA_TITLE", detail.title)
            putExtra("EXTRA_COVER_URL", detail.coverUrl)
            putExtra("EXTRA_TYPE_LABEL", detail.typeLabel)
            putExtra("EXTRA_SEASON", season)
            putExtra("EXTRA_EPISODE", episode)
            putExtra("EXTRA_IS_EPISODIC", detail.isEpisodic)
            putIntegerArrayListExtra("EXTRA_EPISODES_LIST", episodesList)
        }
        startActivity(intent)
    }
}
