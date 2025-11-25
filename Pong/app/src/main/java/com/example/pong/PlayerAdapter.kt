package com.example.pong

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

//// ListAdapter que gestiona la lista de jugadores usando DiffUtil para animaciones automáticas
class PlayerAdapter(
    private val onPlayerClicked: (String) -> Unit
) : ListAdapter<String, PlayerAdapter.PlayerViewHolder>(PlayerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player, parent, false)
        return PlayerViewHolder(view, onPlayerClicked)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    //ViewHolder para cada fla
    class PlayerViewHolder(
        itemView: View,
        val onPlayerClicked: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.playerName)
        private var currentPlayerName: String? = null
        init {
            itemView.setOnClickListener {
                currentPlayerName?.let {
                    onPlayerClicked(it)
                }
            }
        }

        fun bind(name: String) {
            currentPlayerName = name
            nameTextView.text = name
        }
    }
}

// Clase para que ListAdapter sepa que ha cambiado
class PlayerDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
}
