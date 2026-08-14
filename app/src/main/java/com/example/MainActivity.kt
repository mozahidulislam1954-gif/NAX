package com.example

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LiveAudioSession
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

class MainActivity : ComponentActivity() {
    private val viewModel: ZoyaViewModel by viewModels()

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val uiState by viewModel.uiState.collectAsState()
                val recordAudioPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black
                ) {
                    ZoyaScreen(
                        state = uiState,
                        onToggle = {
                            if (recordAudioPermission.status.isGranted) {
                                viewModel.toggleConnection()
                            } else {
                                recordAudioPermission.launchPermissionRequest()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ZoyaScreen(state: LiveAudioSession.State, onToggle: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF050505))
    ) {
        // Background blurs
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(300.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFDB2777).copy(alpha = 0.15f), Color.Transparent)
                    )
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = 48.dp, y = (-32).dp)
                .size(250.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF4F46E5).copy(alpha = 0.1f), Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SESSION ACTIVE",
                        color = Color(0xFFEC4899),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Row {
                        Text(
                            text = "Zoya ",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            text = "Ai",
                            color = Color(0xFF64748B),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }

                // Active dot
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                        .background(Color(0x0DFFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (state != LiveAudioSession.State.DISCONNECTED) Color(0xFFEC4899) else Color.DarkGray,
                                shape = CircleShape
                            )
                    )
                }
            }

            // Main Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.95f,
                    targetValue = 1.05f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1500, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAnimation"
                )

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(bottom = 48.dp)
                ) {
                    // Outer pulsing ring
                    Box(
                        modifier = Modifier
                            .size(192.dp)
                            .scale(if (state == LiveAudioSession.State.LISTENING || state == LiveAudioSession.State.SPEAKING) pulseScale else 1f)
                            .border(2.dp, Color(0xFFEC4899).copy(alpha = 0.2f), CircleShape)
                    )

                    // Inner circle
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                            .background(Color(0x66000000), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Visualizer bars
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.Bottom,
                            modifier = Modifier.height(48.dp)
                        ) {
                            val barHeights = listOf(16.dp, 40.dp, 24.dp, 48.dp, 32.dp, 16.dp)
                            val barColors = listOf(Color(0xFFF472B6), Color(0xFFEC4899), Color(0xFFDB2777), Color(0xFFF472B6), Color(0xFFEC4899), Color(0xFFDB2777))
                            
                            val speakAnim by infiniteTransition.animateFloat(
                                initialValue = 0.5f,
                                targetValue = 1.5f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(300, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "speakAnimation"
                            )

                            barHeights.forEachIndexed { index, height ->
                                val currentHeight = if (state == LiveAudioSession.State.SPEAKING) {
                                    val factor = if (index % 2 == 0) speakAnim else (2f - speakAnim)
                                    height * factor
                                } else height * 0.2f
                                
                                Box(
                                    modifier = Modifier
                                        .width(6.dp)
                                        .height(currentHeight)
                                        .background(barColors[index], CircleShape)
                                )
                            }
                        }
                    }
                }

                // Text
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = Color(0xE6FFFFFF))) {
                            append("\"Don't just stare, darling. ")
                        }
                        withStyle(SpanStyle(color = Color(0xFFF472B6), fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                            append("Speak up.")
                        }
                        withStyle(SpanStyle(color = Color(0xE6FFFFFF))) {
                            append("\"")
                        }
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Light,
                    lineHeight = 32.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 48.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                val statusText = when (state) {
                    LiveAudioSession.State.DISCONNECTED -> "SYSTEM OFFLINE"
                    LiveAudioSession.State.CONNECTING -> "CONNECTING..."
                    LiveAudioSession.State.LISTENING -> "ZOYA IS LISTENING"
                    LiveAudioSession.State.SPEAKING -> "ZOYA IS SPEAKING"
                }

                Text(
                    text = statusText,
                    color = Color(0x4DFFFFFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 2.sp
                )
            }

            // Footer
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                            .background(Color(0x0DFFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color(0x99FFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Main Toggle Button
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFDB2777), Color(0xFF4F46E5))
                                ),
                                shape = CircleShape
                            )
                            .padding(2.dp)
                            .background(Color(0xFF0A0A0A), CircleShape)
                            .clickable { onToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (state == LiveAudioSession.State.DISCONNECTED) Icons.Default.Mic else Icons.Default.Stop,
                                contentDescription = "Toggle Zoya",
                                tint = Color(0xFF050505),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }

                    // Right button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .border(1.dp, Color(0x1AFFFFFF), CircleShape)
                            .background(Color(0x0DFFFFFF), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0x99FFFFFF),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "END-TO-END ENCRYPTED VOICE",
                    color = Color(0x33FFFFFF),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 3.sp
                )
            }
        }
    }
}
