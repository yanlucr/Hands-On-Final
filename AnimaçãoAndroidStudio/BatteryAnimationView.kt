
package com.example.batteryanimation.ui

import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import com.example.batteryanimation.getBatteryLevel
import kotlinx.coroutines.delay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue


@Composable
fun BatteryLevelScreen(context: Context) {
    var batteryLevel by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            batteryLevel = getBatteryLevel(context)
            delay(1000)
        }
    }

    BatteryAnimationScreen(batteryLevel = batteryLevel)
}

@Composable
fun BatteryAnimationScreen(batteryLevel: Int) {
    val animatedBatteryLevel = animateFloatAsState(
        targetValue = batteryLevel.toFloat(),
        animationSpec = tween(durationMillis = 1000)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        // Dimensões da barra circular
        val strokeWidth = 30f
        val radius = size.minDimension / 3

        // Barra circular
        drawArc(
            color = Color.Green,
            startAngle = -90f,
            sweepAngle = (animatedBatteryLevel.value / 100) * 360f, // Preenchimento
            useCenter = false,
            style = Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            ),
            size = Size(radius * 2, radius * 2),
            topLeft = Offset(center.x - radius, center.y - radius)
        )

        // Texto central com a porcentagem da bateria
        drawContext.canvas.nativeCanvas.apply {
            drawText(
                "${batteryLevel}%",
                center.x,
                center.y + (radius / 4), // Ajuste do texto
                android.graphics.Paint().apply {
                    textSize = 60f
                    color = android.graphics.Color.GREEN
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }

        val borderWidth = strokeWidth / 2
        val totalPerimeter = (size.width * 2) + (size.height * 2)
        val animatedPerimeter = totalPerimeter * (animatedBatteryLevel.value / 100)

        var remainingLength = animatedPerimeter

        if (remainingLength > 0) {
            val lineLength = remainingLength.coerceAtMost(size.width)
            drawLine(
                color = Color.Green,
                start = Offset(borderWidth / 2, borderWidth / 2),
                end = Offset(borderWidth / 2 + lineLength, borderWidth / 2),
                strokeWidth = borderWidth,
                cap = StrokeCap.Round
            )
            remainingLength -= lineLength
        }

        // Right (lado direito)
        if (remainingLength > 0) {
            val lineLength = remainingLength.coerceAtMost(size.height)
            drawLine(
                color = Color.Green,
                start = Offset(size.width - borderWidth / 2, borderWidth / 2),
                end = Offset(size.width - borderWidth / 2, borderWidth / 2 + lineLength),
                strokeWidth = borderWidth,
                cap = StrokeCap.Round
            )
            remainingLength -= lineLength
        }

        if (remainingLength > 0) {
            val lineLength = remainingLength.coerceAtMost(size.width)
            drawLine(
                color = Color.Green,
                start = Offset(size.width - borderWidth / 2, size.height - borderWidth / 2),
                end = Offset(size.width - borderWidth / 2 - lineLength, size.height - borderWidth / 2),
                strokeWidth = borderWidth,
                cap = StrokeCap.Round
            )
            remainingLength -= lineLength
        }

        if (remainingLength > 0) {
            val lineLength = remainingLength.coerceAtMost(size.height)
            drawLine(
                color = Color.Green,
                start = Offset(borderWidth / 2, size.height - borderWidth / 2),
                end = Offset(borderWidth / 2, size.height - borderWidth / 2 - lineLength),
                strokeWidth = borderWidth,
                cap = StrokeCap.Round
            )
        }
    }
}







