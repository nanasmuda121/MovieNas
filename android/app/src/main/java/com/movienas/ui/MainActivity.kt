package com.movienas.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.movienas.R
import com.movienas.data.HomeData
import com.movienas.data.MovieBoxApi
import com.movienas.data.MovieItem
import com.movienas.ui.adapter.CategoryAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var mainProgressBar: ProgressBar
    private lateinit var mainScrollView: NestedScrollView
    private lateinit var rvCategories: RecyclerView
    private lateinit var btnSearch: ImageView

    // Hero Banner
    private lateinit var ivHeroBackdrop: ImageView
    private lateinit var tvHeroTitle: TextView
    private lateinit var tvHeroDesc: TextView
    private lateinit var btnHeroPlay: Button

    private var categoryAdapter: CategoryAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        loadHomeFeed()
    }

    private fun initViews() {
        mainProgressBar = findViewById(R.id.mainProgressBar)
        mainScrollView = findViewById(R.id.mainScrollView)
        rvCategories = findViewById(R.id.rvCategories)
        btnSearch = findViewById(R.id.btnSearch)

        ivHeroBackdrop = findViewById(R.id.ivHeroBackdrop)
        tvHeroTitle = findViewById(R.id.tvHeroTitle)
        tvHeroDesc = findViewById(R.id.tvHeroDesc)
        btnHeroPlay = findViewById(R.id.btnHeroPlay)

        rvCategories.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        btnSearch.setOnClickListener {
            startActivity(Intent(this, SearchActivity::class.java))
        }
    }

    private fun loadHomeFeed() {
        mainProgressBar.visibility = View.VISIBLE
        mainScrollView.visibility = View.GONE

        lifecycleScope.launch {
            val result = MovieBoxApi.getHome("id")
            mainProgressBar.visibility = View.GONE

            result.onSuccess { homeData ->
                mainScrollView.visibility = View.VISIBLE
                bindHero(homeData.heroItem)
                bindCategories(homeData)
            }.onFailure { err ->
                Toast.makeText(this@MainActivity, "Gagal memuat: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun bindHero(hero: MovieItem?) {
        if (hero == null) return

        tvHeroTitle.text = hero.title
        tvHeroDesc.text = if (hero.description.isNotEmpty()) hero.description else "Film unggulan MovieNas hari ini."

        val imgUrl = if (hero.stillsUrl.isNotEmpty()) hero.stillsUrl else hero.coverUrl
        Glide.with(this)
            .load(imgUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(ivHeroBackdrop)

        btnHeroPlay.setOnClickListener {
            openDetail(hero.detailPath)
        }
    }

    private fun bindCategories(homeData: HomeData) {
        categoryAdapter = CategoryAdapter(homeData.categories) { selectedMovie ->
            openDetail(selectedMovie.detailPath)
        }
        rvCategories.adapter = categoryAdapter
    }

    private fun openDetail(detailPath: String) {
        val intent = Intent(this, DetailActivity::class.java).apply {
            putExtra("EXTRA_DETAIL_PATH", detailPath)
        }
        startActivity(intent)
    }
}
