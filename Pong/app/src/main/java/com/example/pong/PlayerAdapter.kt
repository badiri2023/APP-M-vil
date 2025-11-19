package com.example.pong

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

// Adaptador para la lista de jugadores (String)
// Usa ListAdapter para eficiencia automática
class PlayerAdapter(
    private val onPlayerClicked: (String) -> Unit
) : ListAdapter<String, PlayerAdapter.PlayerViewHolder>(PlayerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayerViewHolder {
        // Asegúrate de que tu layout para la fila se llama 'player_list_item.xml'
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_player, parent, false)
        return PlayerViewHolder(view, onPlayerClicked)
    }

    override fun onBindViewHolder(holder: PlayerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    // El ViewHolder que "pinta" cada fila
    class PlayerViewHolder(
        itemView: View,
        val onPlayerClicked: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        // --- ¡ESTE ES EL CAMBIO! ---
        // Ahora busca 'R.id.playerName' para que coincida con tu XML.
        private val nameTextView: TextView = itemView.findViewById(R.id.playerName)
        // --- FIN DEL CAMBIO ---

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

// Clase para que ListAdapter sepa qué ha cambiado
class PlayerDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
    override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
        return oldItem == newItem
    }
}
