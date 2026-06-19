package com.dasariravi145.agrolynch.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import coil.compose.rememberAsyncImagePainter
import com.dasariravi145.agrolynch.data.local.entity.InvoiceTemplatePositionEntity
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateEditorScreen(
    viewModel: TemplateEditorViewModel,
    templateImageUrl: String?,
    onBack: () -> Unit
) {
    val positions by viewModel.positions.collectAsState()
    var containerSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Template Positions") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefault() }) {
                        Icon(Icons.Default.Refresh, "Reset")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.LightGray)
        ) {
            // Template Image Container
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(Color.White)
                    .onGloballyPositioned { containerSize = it.size.toSize() }
            ) {
                if (templateImageUrl != null) {
                    Image(
                        painter = rememberAsyncImagePainter(templateImageUrl),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No Template Image Uploaded", color = Color.Gray)
                    }
                }

                // Draggable fields
                if (containerSize.width > 0) {
                    positions.forEach { position ->
                        DraggableField(
                            position = position,
                            containerSize = containerSize,
                            onPositionChanged = { updated ->
                                viewModel.updatePosition(updated)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DraggableField(
    position: InvoiceTemplatePositionEntity,
    containerSize: androidx.compose.ui.geometry.Size,
    onPositionChanged: (InvoiceTemplatePositionEntity) -> Unit
) {
    var offset by remember(position, containerSize) {
        mutableStateOf(
            if (position.xPercent > 0 || position.yPercent > 0) {
                Offset(
                    x = position.xPercent * containerSize.width / 100f,
                    y = position.yPercent * containerSize.height / 100f
                )
            } else {
                // Fallback to legacy x/y if any (assuming A4 size 595x842 for legacy)
                Offset(
                    x = if (position.x > 0) position.x * containerSize.width / 595f else 0f,
                    y = if (position.y > 0) position.y * containerSize.height / 842f else 0f
                )
            }
        )
    }

    Surface(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        onPositionChanged(
                            position.copy(
                                xPercent = offset.x / containerSize.width * 100f,
                                yPercent = offset.y / containerSize.height * 100f
                            )
                        )
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        offset = Offset(
                            x = (offset.x + dragAmount.x).coerceIn(0f, containerSize.width),
                            y = (offset.y + dragAmount.y).coerceIn(0f, containerSize.height)
                        )
                    }
                )
            }
            .border(1.dp, Color.Blue, RoundedCornerShape(2.dp))
            .background(Color.Blue.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(2.dp)
    ) {
        Text(
            text = position.fieldKey,
            modifier = Modifier.padding(2.dp),
            fontSize = 8.sp,
            color = Color.Blue
        )
    }
}
