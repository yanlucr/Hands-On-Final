/*package com.example.batteryanimation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.tooling.preview.Preview
import com.example.batteryanimation.ui.theme.BatteryAnimationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BatteryAnimationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    BatteryAnimation(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}


@Composable
fun BatteryAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()

    val animatedAngle = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val animatedRadius = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f, // Expande e contrai suavemente o feixe
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val radius = (canvasWidth.coerceAtMost(canvasHeight)) / 2.5f
        val strokeWidth = 20f

        drawCircle(
            color = Color.Green.copy(alpha = 0.2f),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth
            )
        )

        drawArc(
            color = Color.Green,
            startAngle = animatedAngle.value,
            sweepAngle = 120f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = strokeWidth,
                cap = StrokeCap.Round
            ),
            size = size.copy(
                width = radius * 2,
                height = radius * 2
            ),
            topLeft = center.copy(
                x = center.x - radius,
                y = center.y - radius
            )
        )

        val borderWidth = 30f + animatedRadius.value
        drawRoundRect(
            color = Color.Green,
            topLeft = Offset(
                x = borderWidth / 2, // Corrigido para garantir que fique dentro do Canvas
                y = borderWidth / 2
            ),
            size = Size(
                width = canvasWidth - borderWidth, // Ajustado para considerar a borda
                height = canvasHeight - borderWidth
            ),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = borderWidth,
                cap = StrokeCap.Round
            ),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun BatteryAnimationPreview() {
    BatteryAnimationTheme {
        BatteryAnimation(modifier = Modifier.fillMaxSize())
    }
}

*/

package com.example.batteryanimation

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.batteryanimation.ui.BatteryLevelScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BatteryLevelScreen(context = this)
        }
    }
}

fun getBatteryLevel(context: Context): Int {
    val batteryStatus = context.registerReceiver(
        null,
        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
    )
    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    return ((level / scale.toFloat()) * 100).toInt()
}
