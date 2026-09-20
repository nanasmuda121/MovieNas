package com.movienas.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.movienas.R
import com.movienas.data.MovieBoxApi
import com.movienas.ui.adapter.MovieCardAdapter
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private lateinit var btnSearchBack: ImageView
    private lateinit var etSearchQuery: EditText
    private lateinit var btnSearchSubmit: ImageView
    private lateinit var searchProgressBar: ProgressBar
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var tvEmptySearch: TextView

    private var movieAdapter: MovieCardAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        initViews()
    }

    private fun initViews() {
        btnSearchBack = findViewById(R.id.btnSearchBack)
        etSearchQuery = findViewById(R.id.etSearchQuery)
        btnSearchSubmit = findViewById(R.id.btnSearchSubmit)
        searchProgressBar = findViewById(R.id.searchProgressBar)
        rvSearchResults = findViewById(R.id.rvSearchResults)
        tvEmptySearch = findViewById(R.id.tvEmptySearch)

        rvSearchResults.layoutManager = GridLayoutManager(this, 3)
        movieAdapter = MovieCardAdapter(emptyList()) { movie ->
            val intent = Intent(this, DetailActivity::class.java).apply {
                putExtra("EXTRA_DETAIL_PATH", movie.detailPath)
            }
            startActivity(intent)
        }
        rvSearchResults.adapter = movieAdapter

        btnSearchBack.setOnClickListener {
            finish()
        }

        btnSearchSubmit.setOnClickListener {
            performSearch()
        }

        etSearchQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    private fun performSearch() {
        val query = etSearchQuery.text.toString().trim()
        if (query.isEmpty()) return

        searchProgressBar.visibility = View.VISIBLE
        tvEmptySearch.visibility = View.GONE

        lifecycleScope.launch {
            val result = MovieBoxApi.search(query, page = 1, perPage = 30)
            searchProgressBar.visibility = View.GONE

            result.onSuccess { list ->
                if (list.isEmpty()) {
                    tvEmptySearch.text = "Tidak ada hasil untuk \"$query\""
                    tvEmptySearch.visibility = View.VISIBLE
                    movieAdapter?.updateData(emptyList())
                } else {
                    tvEmptySearch.visibility = View.GONE
                    movieAdapter?.updateData(list)
                }
            }.onFailure { err ->
                Toast.makeText(this@SearchActivity, "Gagal mencari: ${err.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
