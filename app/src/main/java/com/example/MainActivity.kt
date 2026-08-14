package com.example

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LiveAudioSession
import com.example.ui.theme.MyApplicationTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

fun Modifier.neumorphicCircle(
    backgroundColor: Color = Color(0xFFE0E5EC),
    isPressed: Boolean = false,
    shadowRadius: Dp = 10.dp,
    offsetX: Dp = 6.dp,
    offsetY: Dp = 6.dp
) = this.drawBehind {
    val darkShadow = Color(0xFFA3B1C6)
    val lightShadow = Color(0xFFFFFFFF)

    val radius = size.minDimension / 2
    val paint = Paint().apply { color = backgroundColor }
    val frameworkPaint = paint.asFrameworkPaint()

    drawIntoCanvas { canvas ->
        frameworkPaint.setShadowLayer(
            shadowRadius.toPx(),
            if (isPressed) offsetX.toPx() / 2 else offsetX.toPx(),
            if (isPressed) offsetY.toPx() / 2 else offsetY.toPx(),
            darkShadow.toArgb()
        )
        canvas.drawCircle(center, radius, paint)

        frameworkPaint.setShadowLayer(
            shadowRadius.toPx(),
            if (isPressed) -offsetX.toPx() / 2 else -offsetX.toPx(),
            if (isPressed) -offsetY.toPx() / 2 else -offsetY.toPx(),
            lightShadow.toArgb()
        )
        canvas.drawCircle(center, radius, paint)
    }
}

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
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    viewModel.appLaunchEvent.collect { appName ->
                        openApp(context, appName)
                    }
                }

                LaunchedEffect(Unit) {
                    viewModel.errorEvent.collect { errorMsg ->
                        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFE0E5EC)
                ) {
                    MeMaxScreen(
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

fun openApp(context: Context, appName: String) {
    val intent = when (appName.lowercase()) {
        "youtube" -> context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
        "maps", "map" -> context.packageManager.getLaunchIntentForPackage("com.google.android.apps.maps")
        "browser", "chrome" -> context.packageManager.getLaunchIntentForPackage("com.android.chrome") ?: Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://google.com"))
        "camera" -> Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
        "calculator" -> {
             Intent().apply {
                 action = Intent.ACTION_MAIN
                 addCategory(Intent.CATEGORY_APP_CALCULATOR)
             }
        }
        "gmail", "mail" -> context.packageManager.getLaunchIntentForPackage("com.google.android.gm")
        "photos", "gallery" -> context.packageManager.getLaunchIntentForPackage("com.google.android.apps.photos") ?: Intent(Intent.ACTION_VIEW).apply { type = "image/*" }
        else -> null
    }

    try {
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "Sorry, I can't find the $appName app on your device.", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to open $appName", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MeMaxScreen(state: LiveAudioSession.State, onToggle: () -> Unit) {
    val neumorphicBgColor = Color(0xFFE0E5EC)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(neumorphicBgColor)
    ) {
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
                            text = "MeMax ",
                            color = Color(0xFF333333),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            text = "Ai",
                            color = Color(0xFF888888),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }

                // Active dot in a neumorphic container
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .neumorphicCircle(backgroundColor = neumorphicBgColor, isPressed = true, shadowRadius = 4.dp, offsetX = 3.dp, offsetY = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (state != LiveAudioSession.State.DISCONNECTED && state != LiveAudioSession.State.ERROR) Color(0xFFEC4899) else Color(0xFFA3B1C6),
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
                            .size(220.dp)
                            .scale(if (state == LiveAudioSession.State.LISTENING || state == LiveAudioSession.State.SPEAKING) pulseScale else 1f)
                            .neumorphicCircle(backgroundColor = neumorphicBgColor, isPressed = false, shadowRadius = 16.dp, offsetX = 10.dp, offsetY = 10.dp)
                    )

                    // Inner image circle
                    val rotationAnim by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(if (state == LiveAudioSession.State.SPEAKING) 4000 else 12000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "rotationAnimation"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .neumorphicCircle(backgroundColor = neumorphicBgColor, isPressed = true, shadowRadius = 8.dp, offsetX = 6.dp, offsetY = 6.dp)
                            .padding(8.dp), // Inner padding so it doesn't overlap the neomorphic lip
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_abstract_radial_1786699480054),
                            contentDescription = "Abstract geometric core",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .rotate(if (state != LiveAudioSession.State.DISCONNECTED && state != LiveAudioSession.State.ERROR) rotationAnim else 0f)
                                .scale(if (state == LiveAudioSession.State.SPEAKING) pulseScale else 1f)
                        )
                    }
                }

                val statusText = when (state) {
                    LiveAudioSession.State.DISCONNECTED -> "SYSTEM OFFLINE"
                    LiveAudioSession.State.ERROR -> "SYSTEM ERROR"
                    LiveAudioSession.State.CONNECTING -> "CONNECTING..."
                    LiveAudioSession.State.LISTENING -> "MEMAX IS LISTENING"
                    LiveAudioSession.State.SPEAKING -> "MEMAX IS SPEAKING"
                }

                Text(
                    text = statusText,
                    color = Color(0xFF888888),
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
                    val leftInteractionSource = remember { MutableInteractionSource() }
                    val leftPressed by leftInteractionSource.collectIsPressedAsState()
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .neumorphicCircle(backgroundColor = neumorphicBgColor, isPressed = leftPressed, shadowRadius = 6.dp, offsetX = 4.dp, offsetY = 4.dp)
                            .clickable(interactionSource = leftInteractionSource, indication = null) { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Main Toggle Button
                    val mainInteractionSource = remember { MutableInteractionSource() }
                    val mainPressed by mainInteractionSource.collectIsPressedAsState()
                    val isInactive = state == LiveAudioSession.State.DISCONNECTED || state == LiveAudioSession.State.ERROR
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .neumorphicCircle(backgroundColor = neumorphicBgColor, isPressed = mainPressed || !isInactive, shadowRadius = 10.dp, offsetX = 8.dp, offsetY = 8.dp)
                            .clickable(interactionSource = mainInteractionSource, indication = null) { onToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isInactive) Icons.Default.Mic else Icons.Default.Stop,
                            contentDescription = "Toggle MeMax",
                            tint = if (isInactive) Color(0xFF666666) else Color(0xFFEC4899),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Right button
                    val rightInteractionSource = remember { MutableInteractionSource() }
                    val rightPressed by rightInteractionSource.collectIsPressedAsState()
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .neumorphicCircle(backgroundColor = neumorphicBgColor, isPressed = rightPressed, shadowRadius = 6.dp, offsetX = 4.dp, offsetY = 4.dp)
                            .clickable(interactionSource = rightInteractionSource, indication = null) { },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "END-TO-END ENCRYPTED VOICE",
                    color = Color(0xFFA3B1C6),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                
                val uriHandler = LocalUriHandler.current
                Text(
                    text = "POWERED BY HERMES AGENT",
                    color = Color(0xFFEC4899),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable {
                            uriHandler.openUri("https://github.com/nousresearch/hermes-agent?hl=en-IN")
                        }
                        .padding(8.dp)
                )

                Text(
                    text = "POWERED BY OPENHUMAN",
                    color = Color(0xFF0EA5E9), // A nice blue to contrast with the pink
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clickable {
                            uriHandler.openUri("https://github.com/tinyhumansai/openhuman?utm_source=sp_auto_dm&utm_referrer=sp_auto_dm&fbclid=PAT01DUASRLZxleHRuA2FlbQIxMABzcnRjBmFwcF9pZA81NjcwNjczNDMzNTI0MjcAAaekros4Pt6vaatzGL39KW4K--THjLVf39EV9PmoHCCAXD1bkVT0eAlvfxDB2Q_aem_BU4Vvow_xcK5_gnw9ndbgg")
                        }
                        .padding(8.dp)
                )
            }
        }
    }
}
