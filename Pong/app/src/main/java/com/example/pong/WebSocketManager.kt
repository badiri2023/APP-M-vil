package com.example.pong

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

/**
 * Singleton (object) que gestiona la conexión WebSocket para toda la app.
 */
object WebSocketManager {

    private var webSocketClient: WebSocketClient? = null
    var currentNickname: String? = null

    private val _connectionState = MutableLiveData<ConnectionState>()
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _playerList = MutableLiveData<List<String>>()
    val playerList: LiveData<List<String>> = _playerList

    private val _challengeReceived = MutableLiveData<ChallengeEvent?>()
    val challengeReceived: LiveData<ChallengeEvent?> = _challengeReceived

    private val _challengeDeclined = MutableLiveData<ChallengeEvent?>()
    val challengeDeclined: LiveData<ChallengeEvent?> = _challengeDeclined

    private val _gameStart = MutableLiveData<GameStartEvent?>()
    val gameStart: LiveData<GameStartEvent?> = _gameStart

    private val _countdown = MutableLiveData<String>()
    val countdown: LiveData<String> = _countdown

    private val _gameState = MutableLiveData<GameState>()
    val gameState: LiveData<GameState> = _gameState

    private val _gameOver = MutableLiveData<GameOverEvent?>()
    val gameOver: LiveData<GameOverEvent?> = _gameOver

    private val _loadingText = MutableLiveData<String>()
    val loadingText: LiveData<String> = _loadingText

    fun connect(serverUrl: String, nickname: String) {
        if (webSocketClient != null && webSocketClient!!.isOpen) {
            Log.w("WSManager", "Ya estaba conectado.")
            return
        }

        val uri = URI(serverUrl)
        currentNickname = nickname
        _connectionState.postValue(ConnectionState.Connecting)

        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.i("WSManager", "✅ Conectado al servidor, enviando Nickname...")
                webSocketClient?.send("NICKNAME:$nickname")
            }

            override fun onMessage(message: String?) {
                Log.i("WSManager", "📥 Mensaje recibido: $message")
                if (message == null) return

                when {
                    message == "ACCEPTED" -> {
                        _connectionState.postValue(ConnectionState.Connected(nickname))
                    }
                    message.startsWith("REJECTED:") -> {
                        val reason = message.substringAfter("REJECTED:")
                        _connectionState.postValue(ConnectionState.Error("Rechazado: $reason"))
                        webSocketClient?.close()
                    }
                    message.startsWith("{") -> {
                        processJsonMessage(message)
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.w("WSManager", "🔌 Conexión cerrada: $code, razón: $reason")
                _connectionState.postValue(ConnectionState.Disconnected)
                currentNickname = null
            }

            override fun onError(ex: Exception?) {
                Log.e("WSManager", "❌ Error de red: ${ex?.message}")
                _connectionState.postValue(ConnectionState.Error(ex?.message ?: "Error de conexión"))
            }
        }
        if (serverUrl.startsWith("wss://")) {
            try {
                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                })

                val sslContext = SSLContext.getInstance("TLS")
                sslContext.init(null, trustAllCerts, java.security.SecureRandom())

                webSocketClient?.setSocketFactory(sslContext.socketFactory)
                Log.i("WSManager", "Aplicado fix de SSL para WSS.")

            } catch (e: Exception) {
                Log.e("WSManager", "Error al aplicar el fix de SSL", e)
            }
        }
        webSocketClient?.connect()
    }

    private fun processJsonMessage(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            when (json.optString("type", "")) {

                // --- Lobby ---
                "clients" -> {
                    val list = json.optJSONArray("list") ?: JSONArray()
                    val names = mutableListOf<String>()
                    for (i in 0 until list.length()) {
                        val name = list.getString(i)

                        // Lògica de filtre de Desktop
                        val isMe = name.equals(currentNickname, ignoreCase = true)
                        val lowerName = name.lowercase()
                        val isRaspberryPi = lowerName.contains("pantalla") ||
                                lowerName.contains("raspberry") ||
                                lowerName.contains("pi") ||
                                lowerName.contains("matrix") ||
                                lowerName == "screen" ||
                                lowerName == "display"

                        if (!isMe && !isRaspberryPi) {
                            names.add(name)
                        }
                    }
                    _playerList.postValue(names)
                }
                "challenge_received" -> {
                    val fromPlayer = json.optString("from", "??")
                    _challengeReceived.postValue(ChallengeEvent(fromPlayer))
                }
                "challenge_declined" -> {
                    val fromPlayer = json.optString("from", "??")
                    _challengeDeclined.postValue(ChallengeEvent(fromPlayer))
                }

                // --- Transición al Juego ---
                "game_start" -> {
                    val opponent = json.optString("opponent")
                    val role = json.optString("role")
                    _gameStart.postValue(GameStartEvent(opponent, role))
                }

                // --- Mensajes para la pantalla de carga ---
                "choosing_starter" -> {
                    _loadingText.postValue("Eligiendo quién saca...")
                }
                "text" -> { // Para "Starts Player X"
                    _loadingText.postValue(json.optString("message", ""))
                }
                // --- Durante el Juego ---
                "countdown" -> {
                    _countdown.postValue(json.optString("value", "!"))
                }
                "game_state" -> {
                    _gameState.postValue(GameState(
                        p1_y = json.optDouble("p1_y", 0.5),
                        p2_y = json.optDouble("p2_y", 0.5),
                        ball_x = json.optDouble("ball_x", 0.5),
                        ball_y = json.optDouble("ball_y", 0.5),
                        score1 = json.optInt("score1", 0),
                        score2 = json.optInt("score2", 0)
                    ))
                }
                // Modificat per la pantalla de fi de joc
                "game_over" -> {
                    val winner = json.optString("winner", "")
                    val reason = json.optString("reason", "")
                    _gameOver.postValue(GameOverEvent(winner, reason))
                }
            }
        } catch (e: Exception) {
            Log.e("WSManager", "Error procesando JSON: ${e.message}")
        }
    }

    // Función para que las Activities envíen mensajes
    fun sendMessage(message: String) {
        if (webSocketClient != null && webSocketClient!!.isOpen) {
            Log.d("WSManager", "📤 Enviando: $message")
            webSocketClient?.send(message)
        } else {
            Log.e("WSManager", "No se puede enviar mensaje, no hay conexión.")
            _connectionState.postValue(ConnectionState.Error("Error: Sin conexión"))
        }
    }

    fun close() {
        webSocketClient?.close()
    }

    // --- Funciones para consumir eventos ---
    fun consumeGameOverEvent() {
        _gameOver.postValue(null)
    }
    fun consumeGameStartEvent() {
        _gameStart.postValue(null)
    }
    fun consumeChallengeReceivedEvent() {
        _challengeReceived.postValue(null)
    }
    fun consumeChallengeDeclinedEvent() {
        _challengeDeclined.postValue(null)
    }
}

// --- Clases selladas para manejar los estados de LiveData ---
/** Estados que MainActivity observará */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val nickname: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

/** Eventos que PlayerListActivity observará */
data class ChallengeEvent(val opponentName: String)
data class GameStartEvent(val opponentName: String, val role: String) // "p1" o "p2"

/** Estado que GameActivity observará */
data class GameState(
    val p1_y: Double,
    val p2_y: Double,
    val ball_x: Double,
    val ball_y: Double,
    val score1: Int,
    val score2: Int
)

/** Evento para la pantalla de fin de juego */
data class GameOverEvent(val winnerName: String, val reason: String)
