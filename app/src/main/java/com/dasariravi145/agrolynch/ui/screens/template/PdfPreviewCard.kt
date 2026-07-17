package com.dasariravi145.agrolynch.ui.screens.template

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun PdfPreviewCard(file: File, modifier: Modifier = Modifier) {
    var bitmaps by remember(file) { mutableStateOf<List<Bitmap>>(emptyList()) }

    LaunchedEffect(file) {
        if (file.exists()) {
            try {
                val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(fileDescriptor)
                val pageCount = renderer.pageCount
                val list = mutableListOf<Bitmap>()
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)
                    // Increase scale for better quality
                    val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    list.add(bitmap)
                    page.close()
                }
                bitmaps = list
                renderer.close()
                fileDescriptor.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 600.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        if (bitmaps.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(bitmaps) { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "PDF Page",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat()),
                        contentScale = ContentScale.Fit
                    )
                }
            }
        }
    }
}
