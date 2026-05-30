package com.dasariravi145.agrolynch.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.domain.model.ChartPoint
import com.dasariravi145.agrolynch.domain.model.PieChartData

@Composable
fun LineChart(
    data: List<ChartPoint>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) return

    val maxValue = data.maxOf { it.value }.takeIf { it > 0 } ?: 1f
    
    Column(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val width = size.width
            val height = size.height
            val spacing = width / (data.size - 1).coerceAtLeast(1)
            
            val path = Path()
            data.forEachIndexed { index, point ->
                val x = index * spacing
                val y = height - (point.value / maxValue * height)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3.dp.toPx())
            )
            
            // Draw points
            data.forEachIndexed { index, point ->
                val x = index * spacing
                val y = height - (point.value / maxValue * height)
                drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(x, y))
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { point ->
                Text(text = point.label, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun PieChart(
    data: List<PieChartData>,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return
    
    val total = data.sumOf { it.value.toDouble() }.toFloat()
    
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(200.dp)) {
            var startAngle = 0f
            data.forEach { item ->
                val sweepAngle = (item.value / total) * 360f
                drawArc(
                    color = Color(item.color),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    size = Size(size.width, size.height)
                )
                startAngle += sweepAngle
            }
        }
    }
}

@Composable
fun BarChart(
    data: List<ChartPoint>,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary
) {
    if (data.isEmpty()) return
    
    val maxValue = data.maxOf { it.value }.takeIf { it > 0 } ?: 1f
    
    Canvas(modifier = modifier.height(200.dp)) {
        val width = size.width
        val height = size.height
        val barWidth = width / (data.size * 2)
        
        data.forEachIndexed { index, point ->
            val left = index * (barWidth * 2) + barWidth / 2
            val top = height - (point.value / maxValue * height)
            drawRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, height - top)
            )
        }
    }
}
