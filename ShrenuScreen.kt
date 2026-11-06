package com.yourname.shrenu

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ShrenuScreen(viewModel: ShrenuViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val transcript by viewModel.transcript.collectAsState()
    val context = LocalContext.current

    // Permission handling
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasMicPermission = isGranted }
    )

    // UI Colors based on state
    val backgroundColor = Color(0xFF121212)
    val accentColor by animateColorAsState(
        targetValue = when (uiState) {
            ShrenuState.IDLE -> Color(0xFF6200EE)
            ShrenuState.LISTENING -> Color(0xFF03DAC5)
            ShrenuState.THINKING -> Color(0xFFFFB300)
            ShrenuState.SPEAKING -> Color(0xFFE91E63)
            ShrenuState.ERROR -> Color.Red
        }, label = "AccentColor"
    )

    // Breathing animation for the microphone
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (uiState == ShrenuState.LISTENING || uiState == ShrenuState.SPEAKING) 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "PulseAnimation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header
        Text(
            text = "Shrenu",
            color = Color.White,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp)
        )

        // Text Transcript
        Text(
            text = transcript,
            color = Color.LightGray,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 32.dp)
        )

        // Microphone Button
        FloatingActionButton(
            onClick = {
                if (hasMicPermission) {
                    if (uiState == ShrenuState.IDLE || uiState == ShrenuState.SPEAKING) {
                        viewModel.startListening()
                    }
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            containerColor = accentColor,
            shape = CircleShape,
            modifier = Modifier
                .size(96.dp)
                .scale(scale)
                .padding(bottom = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = "Talk to Shrenu",
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }
}
