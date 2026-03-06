package com.voicewolf.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.voicewolf.app.databinding.ItemInfoLogBinding

/**
 * Adapter for displaying game event logs
 */
class InfoLogAdapter(
    private val events: MutableList<GameEvent> = mutableListOf()
) : RecyclerView.Adapter<InfoLogAdapter.InfoLogViewHolder>() {

    inner class InfoLogViewHolder(private val binding: ItemInfoLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(event: GameEvent) {
            binding.infoLogText.text = event.description
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoLogViewHolder {
        val binding = ItemInfoLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return InfoLogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: InfoLogViewHolder, position: Int) {
        holder.bind(events[position])
    }

    override fun getItemCount() = events.size

    fun addEvent(event: GameEvent) {
        events.add(0, event) // Add to top
        notifyItemInserted(0)
    }

    fun clear() {
        val size = events.size
        events.clear()
        notifyItemRangeRemoved(0, size)
    }

    fun getEvents(): List<GameEvent> = events.toList()
}
