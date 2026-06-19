package com.dasariravi145.agrolynch.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.domain.model.BillTemplateType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TemplateSelectionBottomSheet(
    onDismiss: () -> Unit,
    onTemplateSelected: (BillTemplateType) -> Unit,
    isPremium: Boolean,
    currentTemplateId: String
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp).fillMaxHeight(0.7f)) {
            Text("Choose Invoice Template", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(BillTemplateType.entries) { template ->
                    TemplateItem(
                        template = template,
                        isSelected = template.name == currentTemplateId,
                        isLocked = template.isPremium && !isPremium,
                        onClick = {
                            if (!template.isPremium || isPremium) {
                                onTemplateSelected(template)
                            }
                        }
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun TemplateItem(
    template: BillTemplateType,
    isSelected: Boolean,
    isLocked: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) {
            Box(Modifier.size(120.dp, 160.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFF3F4F6))) {
                // Placeholder for template thumbnail
                Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
                    Text(template.displayName.take(1), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                }
                
                if (isLocked) {
                    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Lock, null, tint = Color.White)
                    }
                }
                
                if (template.isPremium) {
                    Surface(color = Color(0xFFFFD700), shape = RoundedCornerShape(4.dp), modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                        Icon(Icons.Default.Star, null, Modifier.size(12.dp), tint = Color.White)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(template.displayName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}
