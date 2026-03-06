package com.voicewolf.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.voicewolf.app.databinding.ItemPlayerBinding

/**
 * Adapter for displaying player positions in circular layout
 */
class PlayerAdapter(
    private val players: List<Player>,
    private val onPlayerClick: (Player) -> Unit
) : RecyclerView.Adapter<PlayerAdapter.PlayerViewHolder>() {

    private var selectedPlayerId: Int? = null

    inner class PlayerViewHolder(private val binding: ItemPlayerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(player: Player, position: Int) {
            binding.playerNumber.text = player.id.toString()
            
            // Update status indicator
            val statusDrawable = when {
                !player.isAlive -> R.drawable.status_indicator_dead
                selectedPlayerId == player.id -> R.drawable.status_indicator_selected
                else -> R.drawable.status_indicator_alive
            }
            binding.statusIndicator.setBackgroundResource(statusDrawable)

            // Set click listener
            binding.root.setOnClickListener {
                onPlayerClick(player)
            }

            // Long click for selection
            binding.root.setOnLongClickListener {
                selectedPlayerId = player.id
                notifyDataSetChanged()
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val binding = ItemPlayerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PlayerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(players[position], position)
    }

    override fun getItemCount() = players.size

    fun updatePlayerStatus(playerId: Int, isAlive: Boolean) {
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedPlayerId = null
        notifyDataSetChanged()
    }
}
