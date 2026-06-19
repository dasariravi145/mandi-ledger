package com.dasariravi145.agrolynch.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dasariravi145.agrolynch.domain.model.BillTemplateType

@Composable
fun BillTemplateSelectorDialog(
    isPremium: Boolean,
    currentTemplate: BillTemplateType,
    onTemplateSelected: (BillTemplateType) -> Unit,
    onSaveAsDefault: (BillTemplateType) -> Unit = {},
    onUpgradeClick: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Bill Template", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                items(BillTemplateType.entries) { template ->
                    TemplateItem(
                        template = template,
                        isSelected = template == currentTemplate,
                        isLocked = template.isPremium && !isPremium,
                        isPremiumUser = isPremium,
                        onSetDefault = { onSaveAsDefault(template) },
                        onClick = {
                            if (template.isPremium && !isPremium) {
                                onUpgradeClick()
                            } else {
                                onTemplateSelected(template)
                            }
                        }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TemplateItem(
    template: BillTemplateType,
    isSelected: Boolean,
    isLocked: Boolean,
    isPremiumUser: Boolean,
    onSetDefault: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White
        ),
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, Color.LightGray)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(template.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (template.isPremium) {
                        Text("Premium Design", fontSize = 11.sp, color = if(isLocked) Color.Red else Color.Gray)
                    } else {
                        Text("Free / Default", fontSize = 11.sp, color = Color.Gray)
                    }
                }
                
                if (isLocked) {
                    Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color.Gray)
                } else if (isSelected) {
                    Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
                } else if (template.isPremium) {
                    Icon(Icons.Default.Star, contentDescription = "Premium", tint = Color(0xFFFFD700))
                }
            }
            
            if (isPremiumUser && !isLocked && !isSelected) {
                TextButton(
                    onClick = { onSetDefault() },
                    modifier = Modifier.align(Alignment.End),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Set as Default", fontSize = 11.sp)
                }
            }
        }
    }
}
