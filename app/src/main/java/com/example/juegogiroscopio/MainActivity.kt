package com.example.juegogiroscopio

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF121212)
                ) {
                    FreeMoveGameScreen()
                }
            }
        }
    }
}

@Composable
fun FreeMoveGameScreen() {
    val context = LocalContext.current

    // Posición del jugador en el plano 2D (X e Y)
    var playerX by remember { mutableFloatStateOf(500f) }
    var playerY by remember { mutableFloatStateOf(900f) }

    // Dimensiones de pantalla detectadas por el Canvas
    var screenWidth by remember { mutableFloatStateOf(1080f) }
    var screenHeight by remember { mutableFloatStateOf(1920f) }

    // Posición del objetivo a recolectar (Estrella)
    var targetX by remember { mutableFloatStateOf(200f) }
    var targetY by remember { mutableFloatStateOf(300f) }

    // Posición y velocidad de un enemigo que rebota por la pantalla
    var enemyX by remember { mutableFloatStateOf(500f) }
    var enemyY by remember { mutableFloatStateOf(500f) }
    var enemySpeedX by remember { mutableFloatStateOf(7f) }
    var enemySpeedY by remember { mutableFloatStateOf(9f) }

    var score by remember { mutableIntStateOf(0) }
    var gameOver by remember { mutableStateOf(false) }

    // Leer AMBOS ejes del acelerómetro (Inclinación horizontal y vertical)
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER && !gameOver) {
                    val tiltX = event.values[0] // Inclinación lateral
                    val tiltY = event.values[1] // Inclinación adelante/atrás

                    // Modifica X e Y simultáneamente para un movimiento libre en 2D
                    playerX -= tiltX * 2.5f
                    playerY += tiltY * 2.5f
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Bucle principal de juego independiente de la caída vertical
    LaunchedEffect(gameOver) {
        if (gameOver) {
            score = 0
            gameOver = false
            playerX = screenWidth / 2f
            playerY = screenHeight / 2f
        }

        while (!gameOver) {
            delay(16L) // ~60 FPS

            if (screenWidth > 0f) {
                // Mantener al jugador dentro de los límites de la pantalla
                playerX = playerX.coerceIn(50f, screenWidth - 50f)
                playerY = playerY.coerceIn(50f, screenHeight - 50f)

                // Mover el enemigo y hacer que rebote en las paredes
                enemyX += enemySpeedX
                enemyY += enemySpeedY

                if (enemyX <= 40f || enemyX >= screenWidth - 40f) enemySpeedX *= -1f
                if (enemyY <= 40f || enemyY >= screenHeight - 40f) enemySpeedY *= -1f

                // Comprobar si el jugador atrapa el objetivo (Estrella)
                val distanceToTarget = Offset(playerX - targetX, playerY - targetY).getDistance()
                if (distanceToTarget < 80f) {
                    score += 10
                    // Reubicar la estrella aleatoriamente
                    targetX = Random.nextFloat() * (screenWidth - 200f) + 100f
                    targetY = Random.nextFloat() * (screenHeight - 400f) + 200f
                }

                // Comprobar colisión con el enemigo móvil
                val distanceToEnemy = Offset(playerX - enemyX, playerY - enemyY).getDistance()
                if (distanceToEnemy < 70f) {
                    gameOver = true
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            screenWidth = size.width
            screenHeight = size.height

            // 1. Dibujar Objetivo (Moneda/Estrella amarilla)
            drawCircle(
                color = Color(0xFFFFD700),
                radius = 35f,
                center = Offset(targetX, targetY)
            )

            // 2. Dibujar Enemigo Móvil (Orbe rojo que rebota)
            drawCircle(
                color = Color(0xFFFF3D00),
                radius = 45f,
                center = Offset(enemyX, enemyY)
            )

            // 3. Dibujar Jugador (Esfera azul controlada por giroscopio 2D)
            drawCircle(
                color = Color(0xFF00E5FF),
                radius = 40f,
                center = Offset(playerX, playerY)
            )
        }

        // Marcador superior
        Text(
            text = "Puntos: $score",
            color = Color.White,
            fontSize = 28.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
        )

        // Pantalla de Game Over con opción de toque para reiniciar
        if (gameOver) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { gameOver = false },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "¡COLISIÓN!\nPuntuación total: $score\n\nToca para volver a jugar",
                    color = Color.Red,
                    fontSize = 26.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}