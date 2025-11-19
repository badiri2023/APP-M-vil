package com.example.pong

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class PlayerListActivity : AppCompatActivity() {

    // Vistas del layout (asegúrate que los IDs coinciden con tu XML)
    private lateinit var playerRecyclerView: RecyclerView
    private lateinit var challengeContainer: LinearLayout
    private lateinit var playerDetailText: TextView
    private lateinit var challengeButton: Button

    private lateinit var playerAdapter: PlayerAdapter
    private var selectedPlayer: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Asegúrate de que el nombre aquí coincide con tu archivo XML del lobby
        setContentView(R.layout.vista2)

        // 1. Encontrar las vistas
        playerRecyclerView = findViewById(R.id.playerRecyclerView)
        challengeContainer = findViewById(R.id.challengeContainer)
        playerDetailText = findViewById(R.id.playerDetailText)
        challengeButton = findViewById(R.id.challengeButton)

        // 2. Configurar el Adaptador y el RecyclerView
        setupRecyclerView()

        // 3. Configurar el botón de "Challenge"
        challengeButton.setOnClickListener {
            selectedPlayer?.let {
                challengePlayer(it)
            }
        }

        // 4. Empezar a observar los datos del WebSocketManager
        observeWebSocket()
    }

    private fun setupRecyclerView() {
        // Necesitarás tener el archivo PlayerAdapter.kt
        playerAdapter = PlayerAdapter { playerName ->
            onPlayerClicked(playerName)
        }
        playerRecyclerView.adapter = playerAdapter
        playerRecyclerView.layoutManager = LinearLayoutManager(this)
    }

    // Muestra el panel derecho al clicar en un jugador
    private fun onPlayerClicked(playerName: String) {
        selectedPlayer = playerName
        playerDetailText.text = "Retar a: $playerName"
        challengeContainer.visibility = View.VISIBLE
    }

    // --- ¡AQUÍ ESTÁN LOS CAMBIOS! ---
    private fun observeWebSocket() {
        // Observar la LISTA DE JUGADORES
        WebSocketManager.playerList.observe(this) { players ->
            playerAdapter.submitList(players)

            // Ocultar el panel si el jugador seleccionado se desconecta
            if (selectedPlayer != null && !players.contains(selectedPlayer)) {
                challengeContainer.visibility = View.GONE
                selectedPlayer = null
            }
        }

        // Observar si ALGUIEN NOS RETA
        WebSocketManager.challengeReceived.observe(this) { event ->
            event?.let {
                showChallengeDialog(it.opponentName)

                // ¡CAMBIO! Le decimos al Manager que ya hemos usado el evento
                WebSocketManager.consumeChallengeReceivedEvent()
            }
        }

        // Observar si el reto fue RECHAZADO
        WebSocketManager.challengeDeclined.observe(this) { event ->
            event?.let {
                Toast.makeText(this, "${it.opponentName} rechazó tu reto.", Toast.LENGTH_SHORT).show()

                // ¡CAMBIO! Le decimos al Manager que ya hemos usado el evento
                WebSocketManager.consumeChallengeDeclinedEvent()
            }
        }

        // Observar si el JUEGO EMPIEZA
        WebSocketManager.gameStart.observe(this) { event ->
            event?.let {
                Toast.makeText(this, "¡Partida aceptada! Empezando...", Toast.LENGTH_LONG).show()
                goToGameScreen(it.opponentName, it.role)

                // ¡CAMBIO! Le decimos al Manager que ya hemos usado el evento
                WebSocketManager.consumeGameStartEvent()
            }
        }
    }
    // --- FIN DE LOS CAMBIOS ---


    // ENVÍA un reto a un oponente
    private fun challengePlayer(opponentName: String) {
        val challengeJson = JSONObject()
            .put("type", "challenge")
            .put("to", opponentName) // <-- Especifica a QUIÉN retas
        WebSocketManager.sendMessage(challengeJson.toString())
        Toast.makeText(this, "Reto enviado a $opponentName", Toast.LENGTH_SHORT).show()

        challengeContainer.visibility = View.GONE
        selectedPlayer = null
    }

    // MUESTRA el pop-up cuando te retan
    private fun showChallengeDialog(opponentName: String) {
        AlertDialog.Builder(this)
            .setTitle("¡Te han retado!")
            .setMessage("$opponentName quiere jugar contigo.")
            .setPositiveButton("Aceptar") { _, _ ->

                // Respondes al reto, especificando a QUIÉN respondes
                val responseJson = JSONObject()
                    .put("type", "challenge_response")
                    .put("to", opponentName)
                    .put("accepted", true)
                WebSocketManager.sendMessage(responseJson.toString())
            }
            .setNegativeButton("Rechazar") { _, _ ->
                val responseJson = JSONObject()
                    .put("type", "challenge_response")
                    .put("to", opponentName)
                    .put("accepted", false)
                WebSocketManager.sendMessage(responseJson.toString())
            }
            .setCancelable(false)
            .show()
    }

    // Inicia la GameActivity
    private fun goToGameScreen(opponentName: String, role: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("OPPONENT_NAME", opponentName)
        intent.putExtra("PLAYER_ROLE", role) // "p1" o "p2"
        startActivity(intent)
        finish() // Cierra el Lobby
    }
}