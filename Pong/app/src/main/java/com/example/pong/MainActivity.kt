package com.example.pong

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
// Importamos el Manager y la clase de Estado
import com.example.pong.WebSocketManager
import com.example.pong.ConnectionState

class MainActivity : AppCompatActivity() {

    // Variables para las vistas
    private lateinit var nicknameEditText: EditText
    private lateinit var serverUrlEditText: EditText
    private lateinit var connectButton: Button
    private lateinit var errorMessageTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Encontrar las vistas usando los IDs de tu XML
        nicknameEditText = findViewById(R.id.playerName)
        serverUrlEditText = findViewById(R.id.serverUrl)
        connectButton = findViewById(R.id.connectButton)
        errorMessageTextView = findViewById(R.id.errorMessage)

        // Empezar a "observar" el estado de la conexión
        observeConnectionState()

        // Configurar el click del botón
        connectButton.setOnClickListener {
            val nickname = nicknameEditText.text.toString().trim()
            val url = serverUrlEditText.text.toString().trim()

            // Validar que los campos no estén vacíos
            if (nickname.isEmpty() || url.isEmpty()) {
                errorMessageTextView.text = "Introduce URL y nickname"
                errorMessageTextView.visibility = View.VISIBLE
                return@setOnClickListener
            }

            // Asegurarse de que la URL tiene el formato correcto (wss://)
            val finalUrl = if (url.startsWith("ws://") || url.startsWith("wss://")) url else "wss://$url"

            // Pedirle al Manager que conecte
            WebSocketManager.connect(finalUrl, nickname)
        }
    }

    /**
     * Esta función "observa" el LiveData del WebSocketManager.
     * Reacciona solo a los cambios de estado relevantes.
     */
    private fun observeConnectionState() {
        WebSocketManager.connectionState.observe(this) { state ->
            when (state) {

                is ConnectionState.Connecting -> {
                    // Estamos conectando: ocultar error, desactivar botón
                    errorMessageTextView.visibility = View.GONE
                    connectButton.isEnabled = false
                }

                is ConnectionState.Connected -> {
                    // ¡ÉXITO! Navegamos a la siguiente pantalla
                    connectButton.isEnabled = false // Mantener desactivado
                    errorMessageTextView.visibility = View.GONE

                    val intent = Intent(this@MainActivity, PlayerListActivity::class.java)
                    startActivity(intent)
                    finish() // Cerrar esta Activity
                }

                is ConnectionState.Error -> {
                    // ¡ERROR! Mostrar el mensaje específico ("El nombre ya existe" o "Error de conexión")
                    errorMessageTextView.text = state.message
                    errorMessageTextView.visibility = View.VISIBLE
                    connectButton.isEnabled = true // Reactivar botón para reintentar
                }

                is ConnectionState.Disconnected -> {
                    // Se desconectó limpiamente (o se cerró la conexión)
                    // Simplemente reactivamos el botón
                    connectButton.isEnabled = true
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // No cerramos la conexión aquí. El Manager la mantiene viva.
    }
}
