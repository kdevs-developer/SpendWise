package com.kdev.spendwise.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun PremiumAlertDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    icon: ImageVector,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmButtonColor: Color = MaterialTheme.colorScheme.primary,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.width(320.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- ICON HEADER ---
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(iconColor.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))

                // --- TEXT CONTENT ---
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(Modifier.height(12.dp))

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.2
                )

                Spacer(Modifier.height(32.dp))

                // --- ACTION BUTTONS ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel / Dismiss Button
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = dismissText,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Confirm Button
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = confirmButtonColor),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            text = confirmText,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}