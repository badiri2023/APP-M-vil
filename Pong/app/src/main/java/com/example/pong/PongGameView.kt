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

class PongGameView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var gameState: GameState? = null
    private var myRole: String = "p1"

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
        invalidate()
    }

    // Función para que la Activity nos diga si somos P1 o P2
    fun setPlayerRole(role: String) {
        this.myRole = role
    }

    // Bucle de dibujo
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // No dibujar nada si el estado es nulo
        val state = gameState ?: return

        // Por si acaso dibujamos el fondo de negro
        canvas.drawColor(Color.BLACK)

        // Calcular posiciones en píxeles
        val h = height.toFloat()
        val w = width.toFloat()

        // --- LÍMITES DE DIBUJO ---
        val paddleHalfHeight = paddleHeight / 2
        val minY = paddleHalfHeight
        val maxY = h - paddleHalfHeight

        // PALA 1 (Izquierda)
        val p1_x = 50f
        val p1_y_center = (state.p1_y * h).toFloat().coerceIn(minY, maxY)

        canvas.drawRect(
            p1_x,
            p1_y_center - paddleHalfHeight, // Ahora el top nunca será < 0
            p1_x + paddleWidth,
            p1_y_center + paddleHalfHeight, // Ahora el bottom nunca será > h
            paint
        )

        // PALA 2 (Derecha)
        val p2_x = w - 50f - paddleWidth
        // Limitamos la posición del CENTRO
        val p2_y_center = (state.p2_y * h).toFloat().coerceIn(minY, maxY)

        canvas.drawRect(
            p2_x,
            p2_y_center - paddleHalfHeight,
            p2_x + paddleWidth,
            p2_y_center + paddleHalfHeight,
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

    // Manejar movimiento
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return false

        when (event.action) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                // Especificaicones de posicion
                val y_pos_pixels = event.y
                val y_pos_normalized = (y_pos_pixels / height.toFloat()).toDouble()
                val clamped_y = max(0.0, min(1.0, y_pos_normalized))
                val moveJson = JSONObject()
                    .put("type", "move")
                    .put("y_pos", clamped_y)
                // Enviar al servidor estadoa ctual pala del jugador
                WebSocketManager.sendMessage(moveJson.toString())
                return true
            }
        }
        return false
    }
}
