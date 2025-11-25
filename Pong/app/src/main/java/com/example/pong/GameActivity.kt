package com.example.pong

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class GameActivity : AppCompatActivity() {

    // Vistas de Carga
    private lateinit var loadingText: TextView
    private lateinit var loadingBar: ProgressBar
    private lateinit var playerStartText: TextView

    // Vistas de Juego
    private lateinit var pongGameView: PongGameView
    private lateinit var scoreText: TextView

    //Declarar las vistas para los nombres
    private lateinit var player1NameText: TextView
    private lateinit var player2NameText: TextView

    // Variables de Estado
    private var isGameViewLoaded = false
    private var myRole: String = "p1"
    private var opponentName: String = "Oponente"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // pantalla de carga
        setContentView(R.layout.loadingscreen)
        loadingText = findViewById(R.id.loadingText)
        loadingBar = findViewById(R.id.loadingBar)
        playerStartText = findViewById(R.id.playerStartText)

        //Recoger datos del Lobby
        opponentName = intent.getStringExtra("OPPONENT_NAME") ?: "Oponente"
        myRole = intent.getStringExtra("PLAYER_ROLE") ?: "p1"

        // Barra de carga progresiva
        val progressAnimator = ObjectAnimator.ofInt(loadingBar, "progress", 0, 100)
        progressAnimator.duration = 6000
        progressAnimator.interpolator = LinearInterpolator()
        progressAnimator.start()

        observeWebSocket()

        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun observeWebSocket() {

        //Actualizar texto  "Loading..."
        WebSocketManager.loadingText.observe(this) { text ->
            if (!isGameViewLoaded) {
                playerStartText.text = text
                playerStartText.visibility = View.VISIBLE
                loadingText.text = "¡Listos!"
            }
        }

        //Cuenta regresiva
        WebSocketManager.countdown.observe(this) { value ->
            if (!isGameViewLoaded) {
                loadingText.text = value // Mostramos "3", "2", "1"
                playerStartText.visibility = View.GONE
            }
        }

        // Inicio juego
        WebSocketManager.gameState.observe(this) { state ->

            if (!isGameViewLoaded) {
                isGameViewLoaded = true

                // Layout del juego
                setContentView(R.layout.activity_game_play)

                //Encontrar las vistas de Juego
                pongGameView = findViewById(R.id.pongGameView)
                scoreText = findViewById(R.id.scoreText)

                // Encontrar las vistas de los nombres ---
                player1NameText = findViewById(R.id.player1NameText)
                player2NameText = findViewById(R.id.player2NameText)

                //Asignar los nombres laterales
                val myNickname = WebSocketManager.currentNickname ?: "Tú"
                if (myRole == "p1") {
                    player1NameText.text = myNickname
                    player2NameText.text = opponentName
                } else {
                    player1NameText.text = opponentName
                    player2NameText.text = myNickname
                }

                // Configurar la vista
                pongGameView.setPlayerRole(myRole)
            }

            // --- Esto se ejecuta CADA VEZ que llega un 'game_state' ---
            // (Los nombres ya están puestos, solo actualizamos puntuación y canvas)
            scoreText.text = "${state.score1} - ${state.score2}"
            pongGameView.updateState(state)
        }

        // End Game
        WebSocketManager.gameOver.observe(this) { event ->
            event?.let {
                if (!isFinishing) {
                    goToEndGameScreen(it.winnerName, it.reason)
                    WebSocketManager.consumeGameOverEvent()
                    finish()
                }
            }
        }

        // Desconexion (por si acaso)
        WebSocketManager.connectionState.observe(this) { state ->
            if (state is ConnectionState.Disconnected || state is ConnectionState.Error) {
                // si es el servidor quien corta conexion
                if (isGameViewLoaded && !isFinishing) {
                    goToEndGameScreen("", "Se ha perdido la conexión con el servidor")
                }
            }
        }
    }

    /**
     * Navega a la pantalla de fin de partida, pasando los datos necesarios.
     */
    private fun goToEndGameScreen(winner: String, reason: String) {
        val intent = Intent(this, EndGameActivity::class.java)

        intent.putExtra("WINNER_NAME", winner)
        intent.putExtra("REASON", reason)
        val myNickname = WebSocketManager.currentNickname ?: "Tú"
        intent.putExtra("MY_NICKNAME", myNickname)
        intent.putExtra("OPPONENT_NAME", opponentName)

        startActivity(intent)
        finish()
    }
}
