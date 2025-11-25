package com.example.pong

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.json.JSONObject

class PlayerListActivity : AppCompatActivity() {

    // Vistas del layout (Panel de Reto Saliente)
    private lateinit var playerRecyclerView: RecyclerView
    private lateinit var challengeContainer: LinearLayout
    private lateinit var playerDetailText: TextView
    private lateinit var challengeButton: Button

    // ¡NUEVO! Vistas del Panel de Reto Entrante
    private lateinit var incomingChallengeContainer: LinearLayout
    private lateinit var incomingChallengeText: TextView
    private lateinit var acceptButton: Button
    private lateinit var rejectButton: Button

    private lateinit var playerAdapter: PlayerAdapter
    private var selectedPlayer: String? = null
    private var incomingChallenger: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.vista2)

        // Vistas necesarias (Panel Saliente)
        playerRecyclerView = findViewById(R.id.playerRecyclerView)
        challengeContainer = findViewById(R.id.challengeContainer)
        playerDetailText = findViewById(R.id.playerDetailText)
        challengeButton = findViewById(R.id.challengeButton)

        // Encontrar vistas del Panel Entrante
        incomingChallengeContainer = findViewById(R.id.incomingChallengeContainer)
        incomingChallengeText = findViewById(R.id.incomingChallengeText)
        acceptButton = findViewById(R.id.acceptButton)
        rejectButton = findViewById(R.id.rejectButton)

        // Adaptador y el RecyclerView
        setupRecyclerView()

        // Botón challenge (Saliente)
        challengeButton.setOnClickListener {
            selectedPlayer?.let {
                challengePlayer(it)
            }
        }

        // ¡NUEVO! Listeners para los botones Aceptar/Rechazar (Entrante)
        acceptButton.setOnClickListener {
            incomingChallenger?.let {
                sendChallengeResponse(it, true) // Aceptar
                hideIncomingChallengePanel()
            }
        }

        rejectButton.setOnClickListener {
            incomingChallenger?.let {
                sendChallengeResponse(it, false) // Rechazar
                hideIncomingChallengePanel()
            }
        }

        // Inicio observacion del WebSocketManager
        observeWebSocket()
    }

    private fun setupRecyclerView() {
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

        hideIncomingChallengePanel()
        challengeContainer.visibility = View.VISIBLE
    }

    // Lista jugadores
    private fun observeWebSocket() {
        WebSocketManager.playerList.observe(this) { players ->
            playerAdapter.submitList(players)
            // Borra panel saliente si el jugador se desconecta
            if (selectedPlayer != null && !players.contains(selectedPlayer)) {
                challengeContainer.visibility = View.GONE
                selectedPlayer = null
            }
            // Borra el reto entrante si el retador se desconecta
            if (incomingChallenger != null && !players.contains(incomingChallenger)) {
                hideIncomingChallengePanel()
            }
        }

        // Retos entrantes
        WebSocketManager.challengeReceived.observe(this) { event ->
            event?.let {
                // Se muestra el panel
                showIncomingChallengePanel(it.opponentName)

                WebSocketManager.consumeChallengeReceivedEvent()
            }
        }

        // Rechazo reto
        WebSocketManager.challengeDeclined.observe(this) { event ->
            event?.let {
                Toast.makeText(this, "${it.opponentName} rechazó tu reto.", Toast.LENGTH_SHORT).show()
                WebSocketManager.consumeChallengeDeclinedEvent()
            }
        }

        // inicio de juego
        WebSocketManager.gameStart.observe(this) { event ->
            event?.let {
                Toast.makeText(this, "Partida aceptada... Empezando...", Toast.LENGTH_LONG).show()
                goToGameScreen(it.opponentName, it.role)
                WebSocketManager.consumeGameStartEvent()
            }
        }
    }

    // Retar a oponente
    private fun challengePlayer(opponentName: String) {
        val challengeJson = JSONObject()
            .put("type", "challenge")
            .put("to", opponentName)
        WebSocketManager.sendMessage(challengeJson.toString())
        Toast.makeText(this, "Reto enviado a $opponentName", Toast.LENGTH_SHORT).show()

        challengeContainer.visibility = View.GONE
        selectedPlayer = null
    }


    // Muestra el panel de reto entrante
    private fun showIncomingChallengePanel(opponentName: String) {
        incomingChallenger = opponentName
        incomingChallengeText.text = "¡$opponentName te ha retado!"
        // Oculta el panel de retar
        challengeContainer.visibility = View.GONE
        selectedPlayer = null
        // Muestra el nuevo panel de reto entrante
        incomingChallengeContainer.visibility = View.VISIBLE
    }

    // Oculta el panel de reto entrante
    private fun hideIncomingChallengePanel() {
        incomingChallengeContainer.visibility = View.GONE
        incomingChallenger = null
    }

    // Envía la respuesta
    private fun sendChallengeResponse(opponentName: String, accepted: Boolean) {
        val responseJson = JSONObject()
            .put("type", "challenge_response")
            .put("to", opponentName)
            .put("accepted", accepted)
        WebSocketManager.sendMessage(responseJson.toString())
    }

    // Inicia la GameActivity
    private fun goToGameScreen(opponentName: String, role: String) {
        val intent = Intent(this, GameActivity::class.java)
        intent.putExtra("OPPONENT_NAME", opponentName)
        intent.putExtra("PLAYER_ROLE", role)
        startActivity(intent)
        finish()
    }
}
