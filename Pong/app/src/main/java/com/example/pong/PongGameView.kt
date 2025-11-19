package com.example.pong

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import org.json.JSONObject
import kotlin.math.max
import kotlin.math.min

// Vista personalizada que dibuja el juego y maneja el input
class PongGameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var gameState: GameState? = null
    private var myRole: String = "p1" // "p1" o "p2"

    // Constantes de dibujo
    private val paddleWidth = 20f
    private val paddleHeight = 150f
    private val ballSize = 20f

    private val paint = Paint().apply {
        color = Color.WHITE
    }

    // Función para que la Activity nos pase el estado
    fun updateState(newState: GameState) {
        this.gameState = newState
        invalidate() // Le dice a Android: "¡Redibújate!"
    }

    // Función para que la Activity nos diga si somos P1 o P2
    fun setPlayerRole(role: String) {
        this.myRole = role
    }

    // --- EL BUCLE DE DIBUJO ---
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // No dibujar nada si el estado es nulo
        val state = gameState ?: return

        // 1. Dibujar fondo (ya es negro por el XML, pero por si acaso)
        canvas.drawColor(Color.BLACK)

        // 3. Calcular posiciones en píxeles
        val h = height.toFloat()
        val w = width.toFloat()

        // PALA 1 (Izquierda)
        val p1_x = 50f
        val p1_y_center = (state.p1_y * h).toFloat()
        canvas.drawRect(
            p1_x,
            p1_y_center - (paddleHeight / 2),
            p1_x + paddleWidth,
            p1_y_center + (paddleHeight / 2),
            paint
        )

        // PALA 2 (Derecha)
        val p2_x = w - 50f - paddleWidth
        val p2_y_center = (state.p2_y * h).toFloat()
        canvas.drawRect(
            p2_x,
            p2_y_center - (paddleHeight / 2),
            p2_x + paddleWidth,
            p2_y_center + (paddleHeight / 2),
            paint
        )

        // PELOTA
        val ball_x_center = (state.ball_x * w).toFloat()
        val ball_y_center = (state.ball_y * h).toFloat()
        canvas.drawRect(
            ball_x_center - (ballSize / 2),
            ball_y_center - (ballSize / 2),
            ball_x_center + (ballSize / 2),
            ball_y_center + (ballSize / 2),
            paint
        )
    }

    // --- EL MANEJADOR DE INPUT ---
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {

                // 1. Coger la posición Y del dedo
                val y_pos_pixels = event.y

                // 2. Convertirla a un valor entre 0.0 y 1.0
                val y_pos_normalized = (y_pos_pixels / height.toFloat()).toDouble()

                // 3. Asegurarse de que está dentro de los límites
                val clamped_y = max(0.0, min(1.0, y_pos_normalized))

                // 4. Crear el JSON de movimiento
                val moveJson = JSONObject()
                    .put("type", "move")
                    .put("y_pos", clamped_y)

                // 5. Enviar al servidor
                WebSocketManager.sendMessage(moveJson.toString())

                return true // Hemos manejado el evento
            }
        }
        return false // No nos interesan otros eventos (ACTION_UP, etc.)
    }
}
