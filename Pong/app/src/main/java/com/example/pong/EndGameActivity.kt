package com.example.pong

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class EndGameActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_endgame)

        val titleText: TextView = findViewById(R.id.titleText)
        val detailText: TextView = findViewById(R.id.detailText)
        val lobbyButton: Button = findViewById(R.id.lobbyButton)

        // Recoger las dades de intent
        val winner = intent.getStringExtra("WINNER_NAME") ?: ""
        val reason = intent.getStringExtra("REASON") ?: ""
        val myName = intent.getStringExtra("MY_NICKNAME") ?: "Tu"
        val opponentName = intent.getStringExtra("OPPONENT_NAME") ?: "Oponente"

        // Mostrar quien ha ganado o perdido
        if (reason.isNotEmpty()) {
            titleText.text = "Game Over"
            detailText.text = reason
        } else if (winner.equals(myName, ignoreCase = true)) {
            titleText.text = "VICTORIA"
            detailText.text = "Has derrotado a $opponentName"
        } else {
            titleText.text = "DERROTA"
            detailText.text = "Derrotado por $winner"
        }
        // volvemos al lobby
        lobbyButton.setOnClickListener {
            val intent = Intent(this, PlayerListActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
        // Desactivar el boton volver
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
}
