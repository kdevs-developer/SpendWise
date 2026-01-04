package com.kdev.spendwise.ui.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.kdev.spendwise.R
import com.kdev.spendwise.ui.MainViewModel

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onStartRegistration: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val premiumGradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF4F5B93), // Start color (Offset 0)
            Color(0xFFE0E4FF)  // End color (Offset 1)
        )
    )
    // Configure Google Sign-In
    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id)) // Ensure this ID is in strings.xml
            .requestEmail()
            .build()
    }

    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    // Call ViewModel to sign in with Firebase
                    viewModel.signInWithGoogle(idToken) { isNewUser ->
                        isLoading = false
                        if (isNewUser) {
                            onStartRegistration()
                        } else {
                            onLoginSuccess()
                        }
                    }
                } else {
                    isLoading = false
                    errorMessage = "Google Sign-In failed: No ID Token found."
                }
            } catch (e: ApiException) {
                isLoading = false
                Log.e("LoginScreen", "Google Sign-In failed", e)
                errorMessage = "Login failed. Please try again."
            }
        } else {
            isLoading = false
        }
    }

    // UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7FA)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            // --- UPDATED: APP LOGO (No Circle) ---
            Icon(
                imageVector = Icons.Default.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier
                    .size(120.dp)
                    .graphicsLayer(alpha = 0.99f) // Required for blending
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            drawRect(
                                brush = premiumGradient,
                                blendMode = BlendMode.SrcAtop
                            )
                        }
                    }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- UPDATED: WELCOME TEXT (Centered for Small Screens) ---
            Text(
                text = "Welcome to SpendWise",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1C29),
                textAlign = TextAlign.Center, // Ensures it looks good if it breaks to 2 lines
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Track your expenses and manage your budget with ease.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(48.dp))

            if (isLoading) {
                CircularProgressIndicator(color = Color(0xFF4F5B93))
            } else {

                // Helper Text
                Text(
                    text = "Sign in with",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Circular Google Login Button
                Button(
                    onClick = {
                        isLoading = true
                        errorMessage = null
                        launcher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier
                        .size(70.dp) // Circular Size
                        .shadow(8.dp, CircleShape),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp) // Reset padding for icon centering
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google), // Ensure you have a google logo drawable
                        contentDescription = "Google Login",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = errorMessage!!,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}