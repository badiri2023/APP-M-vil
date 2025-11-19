package com.example.pong

import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class GameActivity : AppCompatActivity() {

    // --- Vistas de Carga (Tu XML) ---
    private lateinit var loadingText: TextView
    private lateinit var loadingBar: ProgressBar
    private lateinit var playerStartText: TextView

    // --- Vistas de Juego (XML Nuevo) ---
    private lateinit var pongGameView: PongGameView
    private lateinit var scoreText: TextView
    // private lateinit var opponentNameText: TextView // <-- COMENTADA

    private var isGameViewLoaded = false
    private var myRole: String = "p1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.loadingscreen) // <-- 1. Carga la pantalla de carga

        // 2. Encontrar vistas de carga
        loadingText = findViewById(R.id.loadingText)
        loadingBar = findViewById(R.id.loadingBar)
        playerStartText = findViewById(R.id.playerStartText)

        // 3. Recoger datos del Lobby
        // val opponentName = intent.getStringExtra("OPPONENT_NAME") ?: "Oponente" // Ya no se usa
        myRole = intent.getStringExtra("PLAYER_ROLE") ?: "p1"

        // 4. Animar la barra de carga
        val progressAnimator = ObjectAnimator.ofInt(loadingBar, "progress", 0, 100)
        progressAnimator.duration = 6000
        progressAnimator.interpolator = LinearInterpolator()
        progressAnimator.start()

        // 5. Empezar a escuchar al servidor
        observeWebSocket()
    }

    private fun observeWebSocket() {

        // A. Actualizar texto de "Loading..."
        WebSocketManager.loadingText.observe(this) { text ->
            if (!isGameViewLoaded) {
                if (text.startsWith("Starts Player")) {
                    playerStartText.text = text
                    playerStartText.visibility = View.VISIBLE
                    loadingText.text = "¡Listos!"
                } else {
                    loadingText.text = text
                }
            }
        }

        // B. Actualizar texto de "3, 2, 1..."
        WebSocketManager.countdown.observe(this) { value ->
            if (!isGameViewLoaded) {
                loadingText.text = value
                playerStartText.visibility = View.GONE
            }
        }

        // C. ¡EMPIEZA EL JUEGO! (Llega el primer 'game_state')
        WebSocketManager.gameState.observe(this) { state ->
            if (!isGameViewLoaded) {
                isGameViewLoaded = true

                // 2. Carga tu layout de juego (asegúrate que se llama así)
                setContentView(R.layout.activity_game_play)

                // 3. Encontrar las vistas de Juego
                pongGameView = findViewById(R.id.pongGameView)
                scoreText = findViewById(R.id.scoreText)

                // --- ¡LÍNEAS COMENTADAS PARA EVITAR EL ERROR! ---
                // opponentNameText = findViewById(R.id.opponentNameText)
                // val opponentName = intent.getStringExtra("OPPONENT_NAME") ?: "Oponente"
                // opponentNameText.text = "vs $opponentName"
                // --- FIN ---

                pongGameView.setPlayerRole(myRole)
            }

            scoreText.text = "${state.score1} - ${state.score2}"
            pongGameView.updateState(state)
        }

        // D. FIN DEL JUEGO
        WebSocketManager.gameOver.observe(this) { message ->
            if (!isFinishing) {
                AlertDialog.Builder(this)
                    .setTitle("Partida Terminada")
                    .setMessage(message)
                    .setPositiveButton("Volver al Lobby") { _, _ ->
                        finish()
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        WebSocketManager.gameOver.removeObservers(this)
    }
}
