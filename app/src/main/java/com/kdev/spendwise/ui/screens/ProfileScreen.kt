package com.kdev.spendwise.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kdev.spendwise.ui.MainViewModel
import com.kdev.spendwise.ui.components.PremiumAlertDialog
import com.kdev.spendwise.util.BiometricUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Check if device actually supports biometrics to decide if we show the option
    val isBiometricHardwareAvailable = remember { BiometricUtils.isBiometricAvailable(context) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Profile", fontWeight = FontWeight.Bold)
                        Text(
                            "Manage personal info & settings",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DeleteForever,
                            contentDescription = "Delete Account",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 20.dp) // Reduced horizontal padding
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp), // Tighter spacing
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- 1. PROFILE HEADER ---
            // Reduced spacer and image size to fit better
            Spacer(Modifier.height(4.dp))
            Surface(
                modifier = Modifier.size(100.dp), // Reduced from 110dp
                shape = CircleShape,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Image(
                    painter = painterResource(id = viewModel.getCurrentAvatar()),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = viewModel.userName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = viewModel.userEmail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Spacer(Modifier.height(4.dp))

            // --- 2. PERSONAL DETAILS ---
            // Removed the separate "Personal Info" label to save space, the card implies it.
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Personal Details", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom=12.dp))
                    ProfileDetailRow(icon = Icons.Default.Cake, label = "Birthday", value = viewModel.userDob)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).alpha(0.1f))
                    ProfileDetailRow(icon = Icons.Default.PersonOutline, label = "Gender", value = viewModel.userGender)
                }
            }

            // --- 3. SETTINGS ---
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Text("App Settings", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top=8.dp, bottom=8.dp))

                    // DARK MODE TOGGLE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DarkMode, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text("Dark Mode", fontWeight = FontWeight.Medium)
                        }
                        Switch(
                            checked = viewModel.isDarkMode,
                            onCheckedChange = { viewModel.toggleTheme(it) },
                            modifier = Modifier.scale(0.8f) // Slightly smaller switch
                        )
                    }

                    if (isBiometricHardwareAvailable) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp).alpha(0.1f))

                        // BIOMETRIC TOGGLE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Fingerprint, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Biometric Security", fontWeight = FontWeight.Medium)
                                    Text("Unlock with fingerprint", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                                }
                            }
                            Switch(
                                checked = viewModel.isBiometricEnabled,
                                onCheckedChange = { viewModel.updateBiometric(it) },
                                modifier = Modifier.scale(0.8f)
                            )
                        }
                    }
                }
            }

            // --- 4. LOG OUT (Pushed to bottom) ---
            Spacer(Modifier.weight(1f))

            Button(
                onClick = { viewModel.logout() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                elevation = ButtonDefaults.buttonElevation(0.dp)
            ) {
                Icon(Icons.Default.Logout, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Log Out",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            // Bottom padding to ensure it doesn't touch the navigation bar
            Spacer(Modifier.height(24.dp))
        }

        if (showDeleteDialog) {
            PremiumAlertDialog(
                title = "Delete Account?",
                message = "This action is irreversible. All your transaction data and settings will be permanently erased.",
                confirmText = "Delete",
                dismissText = "Cancel",
                icon = Icons.Default.Warning,
                confirmButtonColor = MaterialTheme.colorScheme.error, // Red for high danger
                iconColor = MaterialTheme.colorScheme.error,
                onConfirm = {
                    showDeleteDialog = false
                    viewModel.deleteUserAccount(
                        onSuccess = { /* Handled by Auth State Observer in Main */ },
                        onError = { errorMsg ->
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                onDismiss = { showDeleteDialog = false }
            )
        }
    }
}

// Helper extension to scale switches slightly if needed
fun Modifier.scale(scale: Float) = this.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))

@Composable
fun ProfileDetailRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(0.5f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value.ifEmpty { "Not set" }, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}