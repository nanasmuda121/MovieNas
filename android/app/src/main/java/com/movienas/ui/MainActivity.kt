package com.movienas.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
import com.movienas.data.DownloadedVideo
import com.movienas.data.HomeData
import com.movienas.data.MovieBoxApi
import com.movienas.data.MovieItem
import com.movienas.data.OfflineDownloadManager
import com.movienas.ui.adapter.CategoryAdapter
import com.movienas.ui.adapter.OfflineVideoAdapter
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
    private lateinit var navBtnOffline: View
    private lateinit var navBtnSearch: View

    private lateinit var ivNavHome: ImageView
    private lateinit var tvNavHome: TextView
    private lateinit var ivNavMovies: ImageView
    private lateinit var tvNavMovies: TextView
    private lateinit var ivNavSeries: ImageView
    private lateinit var tvNavSeries: TextView
    private lateinit var ivNavDrama: ImageView
    private lateinit var tvNavDrama: TextView
    private lateinit var ivNavOffline: ImageView
    private lateinit var tvNavOffline: TextView
    private lateinit var ivNavSearch: ImageView
    private lateinit var tvNavSearch: TextView

    // Offline Tab Views
    private lateinit var layoutOffline: View
    private lateinit var btnOfflineRefresh: ImageView
    private lateinit var btnStorageMovies: TextView
    private lateinit var btnStorageDownload: TextView
    private lateinit var tvActiveStoragePath: TextView
    private lateinit var layoutStoragePermissionWarning: View
    private lateinit var btnGrantStoragePermission: TextView
    private lateinit var rvOfflineVideos: RecyclerView
    private lateinit var layoutOfflineEmpty: View
    private var offlineAdapter: OfflineVideoAdapter? = null

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
        maybeShowSupportDeveloperPopup()
    }

    override fun onResume() {
        super.onResume()
        if (::layoutOffline.isInitialized && layoutOffline.visibility == View.VISIBLE) {
            loadOfflineVideos()
        }
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
        navBtnOffline = findViewById(R.id.navBtnOffline)
        navBtnSearch = findViewById(R.id.navBtnSearch)

        ivNavHome = findViewById(R.id.ivNavHome)
        tvNavHome = findViewById(R.id.tvNavHome)
        ivNavMovies = findViewById(R.id.ivNavMovies)
        tvNavMovies = findViewById(R.id.tvNavMovies)
        ivNavSeries = findViewById(R.id.ivNavSeries)
        tvNavSeries = findViewById(R.id.tvNavSeries)
        ivNavDrama = findViewById(R.id.ivNavDrama)
        tvNavDrama = findViewById(R.id.tvNavDrama)
        ivNavOffline = findViewById(R.id.ivNavOffline)
        tvNavOffline = findViewById(R.id.tvNavOffline)
        ivNavSearch = findViewById(R.id.ivNavSearch)
        tvNavSearch = findViewById(R.id.tvNavSearch)

        // Offline Tab Views
        layoutOffline = findViewById(R.id.layoutOffline)
        btnOfflineRefresh = findViewById(R.id.btnOfflineRefresh)
        btnStorageMovies = findViewById(R.id.btnStorageMovies)
        btnStorageDownload = findViewById(R.id.btnStorageDownload)
        tvActiveStoragePath = findViewById(R.id.tvActiveStoragePath)
        layoutStoragePermissionWarning = findViewById(R.id.layoutStoragePermissionWarning)
        btnGrantStoragePermission = findViewById(R.id.btnGrantStoragePermission)
        rvOfflineVideos = findViewById(R.id.rvOfflineVideos)
        layoutOfflineEmpty = findViewById(R.id.layoutOfflineEmpty)

        rvOfflineVideos.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

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
            selectNavTab(0)
            hideOfflineTab()
            filterFeed(0)
            mainScrollView.smoothScrollTo(0, 0)
        }
        navBtnMovies.setOnClickListener {
            selectNavTab(1)
            hideOfflineTab()
            filterFeed(1)
        }
        navBtnSeries.setOnClickListener {
            selectNavTab(2)
            hideOfflineTab()
            filterFeed(2)
        }
        navBtnDrama.setOnClickListener {
            selectNavTab(3)
            hideOfflineTab()
            filterFeed(7)
        }
        navBtnOffline.setOnClickListener {
            selectNavTab(4)
            showOfflineTab()
        }
        navBtnSearch.setOnClickListener {
            selectNavTab(5)
            openSearch("")
        }

        // Offline storage selector clicks
        btnStorageMovies.setOnClickListener {
            OfflineDownloadManager.setPreferredStorageFolder(this, OfflineDownloadManager.PATH_MOVIES)
            updateStorageSelectorUi()
            loadOfflineVideos()
        }

        btnStorageDownload.setOnClickListener {
            OfflineDownloadManager.setPreferredStorageFolder(this, OfflineDownloadManager.PATH_DOWNLOAD)
            updateStorageSelectorUi()
            loadOfflineVideos()
        }

        btnOfflineRefresh.setOnClickListener {
            loadOfflineVideos()
            Toast.makeText(this, "Daftar video offline diperbarui", Toast.LENGTH_SHORT).show()
        }

        btnGrantStoragePermission.setOnClickListener {
            OfflineDownloadManager.requestStoragePermission(this)
        }
    }

    private fun selectNavTab(index: Int) {
        val redColor = ContextCompat.getColor(this, R.color.brand_red)
        val slateColor = ContextCompat.getColor(this, R.color.text_slate)

        val icons = listOf(ivNavHome, ivNavMovies, ivNavSeries, ivNavDrama, ivNavOffline, ivNavSearch)
        val texts = listOf(tvNavHome, tvNavMovies, tvNavSeries, tvNavDrama, tvNavOffline, tvNavSearch)

        for (i in icons.indices) {
            val isSelected = (i == index)
            val c = if (isSelected) redColor else slateColor
            icons[i].setColorFilter(c)
            texts[i].setTextColor(c)
        }
    }

    private fun showOfflineTab() {
        mainScrollView.visibility = View.GONE
        layoutOffline.visibility = View.VISIBLE
        updateStorageSelectorUi()
        loadOfflineVideos()
    }

    private fun hideOfflineTab() {
        layoutOffline.visibility = View.GONE
        mainScrollView.visibility = View.VISIBLE
    }

    private fun updateStorageSelectorUi() {
        val currentPath = OfflineDownloadManager.getPreferredStorageFolder(this)
        val isDownload = currentPath.contains("Download", ignoreCase = true)

        val activeBg = R.drawable.bg_pill_active
        val inactiveBg = R.drawable.bg_pill_inactive
        val white = Color.WHITE
        val slate = ContextCompat.getColor(this, R.color.text_slate)

        if (isDownload) {
            btnStorageDownload.setBackgroundResource(activeBg)
            btnStorageDownload.setTextColor(white)
            btnStorageMovies.setBackgroundResource(inactiveBg)
            btnStorageMovies.setTextColor(slate)
            tvActiveStoragePath.text = "Path: ${OfflineDownloadManager.PATH_DOWNLOAD}"
        } else {
            btnStorageMovies.setBackgroundResource(activeBg)
            btnStorageMovies.setTextColor(white)
            btnStorageDownload.setBackgroundResource(inactiveBg)
            btnStorageDownload.setTextColor(slate)
            tvActiveStoragePath.text = "Path: ${OfflineDownloadManager.PATH_MOVIES}"
        }
    }

    private fun loadOfflineVideos() {
        val hasPermission = OfflineDownloadManager.hasStoragePermission(this)
        layoutStoragePermissionWarning.visibility = if (hasPermission) View.GONE else View.VISIBLE

        val videos = OfflineDownloadManager.getDownloadedVideos()

        if (videos.isEmpty()) {
            layoutOfflineEmpty.visibility = View.VISIBLE
            rvOfflineVideos.visibility = View.GONE
        } else {
            layoutOfflineEmpty.visibility = View.GONE
            rvOfflineVideos.visibility = View.VISIBLE

            if (offlineAdapter == null) {
                offlineAdapter = OfflineVideoAdapter(
                    videos = videos,
                    onPlayClick = { video -> playOfflineVideo(video) },
                    onDeleteClick = { video -> confirmDeleteVideo(video) }
                )
                rvOfflineVideos.adapter = offlineAdapter
            } else {
                offlineAdapter?.updateList(videos)
            }
        }
    }

    private fun playOfflineVideo(video: DownloadedVideo) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("EXTRA_IS_OFFLINE", true)
            putExtra("EXTRA_FILE_PATH", video.path)
            putExtra("EXTRA_TITLE", video.title)
            putExtra("EXTRA_TYPE_LABEL", "Offline Video")
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun confirmDeleteVideo(video: DownloadedVideo) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Video Offline?")
            .setMessage("Apakah kamu yakin ingin menghapus '${video.title}' dari penyimpanan perangkat?")
            .setPositiveButton("Hapus") { dialog, _ ->
                val deleted = OfflineDownloadManager.deleteDownloadedVideo(video)
                if (deleted) {
                    Toast.makeText(this, "Video berhasil dihapus", Toast.LENGTH_SHORT).show()
                    loadOfflineVideos()
                } else {
                    Toast.makeText(this, "Gagal menghapus video atau berkas tidak ada", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (OfflineDownloadManager.hasStoragePermission(this)) {
                Toast.makeText(this, "Izin penyimpanan diberikan!", Toast.LENGTH_SHORT).show()
                layoutStoragePermissionWarning.visibility = View.GONE
                loadOfflineVideos()
            } else {
                Toast.makeText(this, "Izin penyimpanan belum diberikan", Toast.LENGTH_SHORT).show()
            }
        }
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
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun openSearch(query: String) {
        val intent = Intent(this, SearchActivity::class.java).apply {
            putExtra("EXTRA_QUERY", query)
        }
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    // ─── Support Developer Popup ───────────────────────────────────────────────

    private fun maybeShowSupportDeveloperPopup() {
        val prefs = getSharedPreferences("movienas_prefs", MODE_PRIVATE)
        val dontShow = prefs.getBoolean("support_dont_show", false)
        if (dontShow) return

        val launchCount = prefs.getInt("launch_count", 0) + 1
        prefs.edit().putInt("launch_count", launchCount).apply()

        // Show popup on 1st launch and every 5th launch
        if (launchCount != 1 && launchCount % 5 != 0) return

        showSupportDeveloperPopup()
    }

    private fun showSupportDeveloperPopup() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_support_developer, null)

        val dialog = AlertDialog.Builder(this, R.style.Theme_MovieNas_Dialog)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            attributes?.windowAnimations = R.style.DialogSlideAnimation
        }

        val btnJoin = dialogView.findViewById<LinearLayout>(R.id.btnJoinWhatsApp)
        val btnLater = dialogView.findViewById<TextView>(R.id.btnMaybeLater)
        val checkDont = dialogView.findViewById<CheckBox>(R.id.checkDontShowAgain)

        btnJoin.setOnClickListener {
            if (checkDont.isChecked) {
                getSharedPreferences("movienas_prefs", MODE_PRIVATE)
                    .edit().putBoolean("support_dont_show", true).apply()
            }
            val url = "https://whatsapp.com/channel/0029VbCsS2r2phHIV3O0nO1a"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            dialog.dismiss()
        }

        btnLater.setOnClickListener {
            if (checkDont.isChecked) {
                getSharedPreferences("movienas_prefs", MODE_PRIVATE)
                    .edit().putBoolean("support_dont_show", true).apply()
            }
            dialog.dismiss()
        }

        dialog.show()
    }
}
