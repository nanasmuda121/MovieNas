package com.movienas.ui

import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.movienas.R
import com.movienas.data.CategorySection
import com.movienas.data.HomeData
import com.movienas.data.MovieBoxApi
import com.movienas.data.MovieItem
import com.movienas.ui.adapter.CategoryAdapter
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var mainProgressBar: ProgressBar
    private lateinit var mainScrollView: NestedScrollView
    private lateinit var rvCategories: RecyclerView
    private lateinit var btnSearchHeader: View
    private lateinit var tvBrandName: TextView

    // Filter Tabs
    private lateinit var tabAll: TextView
    private lateinit var tabMovies: TextView
    private lateinit var tabSeries: TextView
    private lateinit var tabDrama: TextView

    // Bottom Navigation
    private lateinit var navBtnHome: View
    private lateinit var navBtnMovies: View
    private lateinit var navBtnSeries: View
    private lateinit var navBtnDrama: View
    private lateinit var navBtnSearch: View

    // Hero Banner Views
    private lateinit var ivHeroBackdrop: ImageView
    private lateinit var tvHeroTrendingBadge: TextView
    private lateinit var tvHeroTypeBadge: TextView
    private lateinit var tvHeroRating: TextView
    private lateinit var tvHeroYear: TextView
    private lateinit var tvHeroTitle: TextView
    private lateinit var tvHeroDesc: TextView
    private lateinit var btnHeroPlay: View
    private lateinit var btnHeroDetail: View

    private var allCategories: List<CategorySection> = emptyList()
    private var categoryAdapter: CategoryAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupListeners()
        loadHomeFeed()
    }

    private fun initViews() {
        mainProgressBar = findViewById(R.id.mainProgressBar)
        mainScrollView = findViewById(R.id.mainScrollView)
        rvCategories = findViewById(R.id.rvCategories)
        btnSearchHeader = findViewById(R.id.btnSearchHeader)
        tvBrandName = findViewById(R.id.tvBrandName)

        // Set MovieNas gradient logo text matching web
        tvBrandName.text = Html.fromHtml("Movie<font color='#E50914'>Nas</font>", Html.FROM_HTML_MODE_LEGACY)

        // Filter Tabs
        tabAll = findViewById(R.id.tabAll)
        tabMovies = findViewById(R.id.tabMovies)
        tabSeries = findViewById(R.id.tabSeries)
        tabDrama = findViewById(R.id.tabDrama)

        // Bottom Navigation
        navBtnHome = findViewById(R.id.navBtnHome)
        navBtnMovies = findViewById(R.id.navBtnMovies)
        navBtnSeries = findViewById(R.id.navBtnSeries)
        navBtnDrama = findViewById(R.id.navBtnDrama)
        navBtnSearch = findViewById(R.id.navBtnSearch)

        // Hero Banner
        ivHeroBackdrop = findViewById(R.id.ivHeroBackdrop)
        tvHeroTrendingBadge = findViewById(R.id.tvHeroTrendingBadge)
        tvHeroTypeBadge = findViewById(R.id.tvHeroTypeBadge)
        tvHeroRating = findViewById(R.id.tvHeroRating)
        tvHeroYear = findViewById(R.id.tvHeroYear)
        tvHeroTitle = findViewById(R.id.tvHeroTitle)
        tvHeroDesc = findViewById(R.id.tvHeroDesc)
        btnHeroPlay = findViewById(R.id.btnHeroPlay)
        btnHeroDetail = findViewById(R.id.btnHeroDetail)

        rvCategories.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    private fun setupListeners() {
        btnSearchHeader.setOnClickListener {
            openSearch("")
        }

        // Filter tab clicks
        tabAll.setOnClickListener { filterFeed(0) }
        tabMovies.setOnClickListener { filterFeed(1) }
        tabSeries.setOnClickListener { filterFeed(2) }
        tabDrama.setOnClickListener { filterFeed(7) }

        // Bottom Nav Bar clicks
        navBtnHome.setOnClickListener {
            filterFeed(0)
            mainScrollView.smoothScrollTo(0, 0)
        }
        navBtnMovies.setOnClickListener { filterFeed(1) }
        navBtnSeries.setOnClickListener { filterFeed(2) }
        navBtnDrama.setOnClickListener { filterFeed(7) }
        navBtnSearch.setOnClickListener { openSearch("") }
    }

    private fun filterFeed(type: Int) {
        // Update tab pill UI
        resetTabs()
        when (type) {
            0 -> tabAll.setBackgroundResource(R.drawable.bg_pill_active)
            1 -> tabMovies.setBackgroundResource(R.drawable.bg_pill_active)
            2 -> tabSeries.setBackgroundResource(R.drawable.bg_pill_active)
            7 -> tabDrama.setBackgroundResource(R.drawable.bg_pill_active)
        }

        if (type == 0) {
            categoryAdapter = CategoryAdapter(allCategories) { selectedMovie ->
                openDetail(selectedMovie.detailPath)
            }
            rvCategories.adapter = categoryAdapter
        } else {
            val filtered = allCategories.mapNotNull { cat ->
                val matchingItems = cat.items.filter { it.subjectType == type }
                if (matchingItems.isNotEmpty()) {
                    cat.copy(items = matchingItems)
                } else null
            }
            categoryAdapter = CategoryAdapter(filtered) { selectedMovie ->
                openDetail(selectedMovie.detailPath)
            }
            rvCategories.adapter = categoryAdapter
        }
    }

    private fun resetTabs() {
        val inactive = R.drawable.bg_pill_inactive
        tabAll.setBackgroundResource(inactive)
        tabMovies.setBackgroundResource(inactive)
        tabSeries.setBackgroundResource(inactive)
        tabDrama.setBackgroundResource(inactive)
    }

    private fun loadHomeFeed() {
        mainProgressBar.visibility = View.VISIBLE
        mainScrollView.visibility = View.GONE

        lifecycleScope.launch {
            val result = MovieBoxApi.getHome("id")
            mainProgressBar.visibility = View.GONE

            result.onSuccess { homeData ->
                mainScrollView.visibility = View.VISIBLE
                allCategories = homeData.categories
                bindHero(homeData.heroItem)
                bindCategories(homeData.categories)
            }.onFailure { err ->
                Toast.makeText(this@MainActivity, "Gagal memuat: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun bindHero(hero: MovieItem?) {
        if (hero == null) return

        tvHeroTitle.text = hero.title
        tvHeroDesc.text = if (hero.description.isNotEmpty()) hero.description else "Film unggulan pilihan hari ini di MovieNas."

        val typeText = when (hero.subjectType) {
            2 -> "Series"
            7 -> "Short Drama"
            else -> "Movie"
        }
        tvHeroTypeBadge.text = typeText
        tvHeroRating.text = if (hero.imdbRating.isNotEmpty()) "★ ${hero.imdbRating}" else "★ 8.5"
        tvHeroYear.text = if (hero.year.isNotEmpty()) "• ${hero.year}" else ""

        val imgUrl = if (hero.stillsUrl.isNotEmpty()) hero.stillsUrl else hero.coverUrl
        Glide.with(this)
            .load(imgUrl)
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(ivHeroBackdrop)

        btnHeroPlay.setOnClickListener {
            // Direct play or open detail
            openDetail(hero.detailPath)
        }

        btnHeroDetail.setOnClickListener {
            openDetail(hero.detailPath)
        }
    }

    private fun bindCategories(categories: List<CategorySection>) {
        categoryAdapter = CategoryAdapter(categories) { selectedMovie ->
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

    private fun openSearch(query: String) {
        val intent = Intent(this, SearchActivity::class.java).apply {
            putExtra("EXTRA_QUERY", query)
        }
        startActivity(intent)
    }
}
