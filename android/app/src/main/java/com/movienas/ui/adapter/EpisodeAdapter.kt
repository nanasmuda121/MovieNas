package com.movienas.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.movienas.R

class EpisodeAdapter(
    private val episodes: List<Int>,
    private var selectedEpisode: Int,
    private val onEpisodeClick: (Int) -> Unit
) : RecyclerView.Adapter<EpisodeAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvEpisodeNumber: TextView = view.findViewById(R.id.tvEpisodeNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_episode_badge, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val ep = episodes[position]
        holder.tvEpisodeNumber.text = "Ep $ep"

        val isSelected = ep == selectedEpisode
        if (isSelected) {
            holder.tvEpisodeNumber.setBackgroundResource(R.drawable.bg_button_red)
            holder.tvEpisodeNumber.setTextColor(Color.WHITE)
        } else {
            holder.tvEpisodeNumber.setBackgroundResource(R.drawable.bg_card_rounded)
            holder.tvEpisodeNumber.setTextColor(Color.parseColor("#94A3B8"))
        }

        holder.itemView.setOnClickListener {
            selectedEpisode = ep
            notifyDataSetChanged()
            onEpisodeClick(ep)
        }
    }

    override fun getItemCount(): Int = episodes.size

    fun setSelected(ep: Int) {
        selectedEpisode = ep
        notifyDataSetChanged()
    }
}
