package com.tvmime.tv.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.IconButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Surface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import android.graphics.Bitmap
import com.tvmime.theme.DesignSystemTokens
import com.tvmime.tv.hardware.DeviceCapabilityDetector
import com.tvmime.tv.viewmodel.TvMainViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class OnboardingTab {
    DIRECT_LOGIN,
    DEMO_MODE
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: TvMainViewModel,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val capabilities = remember { DeviceCapabilityDetector.detect(context) }

    var selectedTab by remember { mutableStateOf(OnboardingTab.DIRECT_LOGIN) }

    // Direct Login State
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var loginStatusText by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }

    val bgMain = Color(DesignSystemTokens.Colors.Background)
    val cardBg = Color(DesignSystemTokens.Colors.Card)
    val crimson = Color(DesignSystemTokens.Colors.Crimson)
    val crimsonBright = Color(DesignSystemTokens.Colors.CrimsonBright)
    val textPrimary = Color(DesignSystemTokens.Colors.TextPrimary)
    val textSecondary = Color(DesignSystemTokens.Colors.TextSecondary)



    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgMain)
            .padding(36.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Banner & Device Capability Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(crimson),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "TV",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                    }
                    Column {
                        Text(
                            text = "TVMIME SETUP WIZARD",
                            color = textPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Connect your IPTV playlists to start streaming in 60 FPS",
                            color = textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }

                // Device Intelligence Badge
                Box(
                    modifier = Modifier
                        .background(Color(0xFF181822), RoundedCornerShape(10.dp))
                        .border(1.dp, Color(0xFF262634), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "DETECTED HARDWARE",
                            color = crimsonBright,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${capabilities.model} • ${capabilities.totalRamMb}MB RAM",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = capabilities.recommendedBufferProfile,
                            color = Color(0xFF10B981),
                            fontSize = 10.sp
                        )
                    }
                }
            }

            // Main Content Area (Split: Left Tabs, Right Card)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                // Left Navigation Tabs
                Column(
                    modifier = Modifier.width(260.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TabButton(
                        title = "Direct Login",
                        subtitle = "Sign in with TVMime email/pass",
                        icon = Icons.Default.AccountCircle,
                        isSelected = selectedTab == OnboardingTab.DIRECT_LOGIN,
                        onClick = { selectedTab = OnboardingTab.DIRECT_LOGIN }
                    )

                    TabButton(
                        title = "Demo Test Drive",
                        subtitle = "Try free public streams instantly",
                        icon = Icons.Default.PlayArrow,
                        isSelected = selectedTab == OnboardingTab.DEMO_MODE,
                        onClick = { selectedTab = OnboardingTab.DEMO_MODE }
                    )
                }

                // Right Detail Container
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(cardBg, RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF262634), RoundedCornerShape(16.dp))
                        .padding(28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (selectedTab) {

                        OnboardingTab.DIRECT_LOGIN -> {
                            Column(
                                modifier = Modifier.width(380.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "SIGN IN TO TVMIME ACCOUNT",
                                    color = crimson,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("Email Address", color = Color.Gray, fontSize = 12.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = crimson,
                                        unfocusedBorderColor = Color(0xFF383848)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Password", color = Color.Gray, fontSize = 12.sp) },
                                    singleLine = true,
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = "Toggle password visibility",
                                                tint = Color.Gray
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = crimson,
                                        unfocusedBorderColor = Color(0xFF383848)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (loginStatusText != null) {
                                    Text(
                                        text = loginStatusText ?: "",
                                        color = if (isLoggingIn) Color(0xFF38BDF8) else Color(0xFFEF4444),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )
                                }

                                Surface(
                                    onClick = {
                                        if (isLoggingIn) return@Surface
                                        if (email.isBlank() || password.isBlank()) {
                                            loginStatusText = "Please enter email and password."
                                            return@Surface
                                        }
                                        isLoggingIn = true
                                        loginStatusText = "Signing in and syncing playlists..."

                                        coroutineScope.launch {
                                            val result = viewModel.syncFromCloud(email, password)
                                            isLoggingIn = false
                                            if (result.isSuccess) {
                                                loginStatusText = "Success!"
                                                delay(500)
                                                onComplete()
                                            } else {
                                                loginStatusText = result.exceptionOrNull()?.message ?: "Authentication failed."
                                            }
                                        }
                                    },
                                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = crimson,
                                        focusedContainerColor = crimsonBright
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        if (isLoggingIn) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = Color.White,
                                                strokeWidth = 2.dp
                                            )
                                        } else {
                                            Text(
                                                text = "Sign In & Load Playlists",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        OnboardingTab.DEMO_MODE -> {
                            Column(
                                modifier = Modifier.width(420.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(Color(0xFF10B981).copy(alpha = 0.15f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayCircleOutline,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Text(
                                    text = "INSTANT DEMO TEST DRIVE",
                                    color = Color(0xFF10B981),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )

                                Text(
                                    text = "Load legal public demo IPTV channels (Big Buck Bunny, Sintel, Tears of Steel, NASA TV) to verify 60fps hardware acceleration with zero setup.",
                                    color = textSecondary,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp
                                )

                                Surface(
                                    onClick = {
                                        viewModel.addDemoPortal()
                                        onComplete()
                                    },
                                    shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                    colors = ClickableSurfaceDefaults.colors(
                                        containerColor = Color(0xFF10B981),
                                        focusedContainerColor = Color(0xFF059669)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Text(
                                            text = "Launch Demo Player Now",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "TVMime v1.1.0",
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp
                )
                Text(
                    text = "Use D-Pad remote to navigate options",
                    color = Color(0xFF6B7280),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TabButton(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isSelected) Color(0xFF1F1F2C) else Color(0xFF121217),
            focusedContainerColor = Color(0xFFE50914)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) Color(0xFFFF1E27) else Color.White,
                modifier = Modifier.size(22.dp)
            )
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF9CA3AF),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun rememberQrBitmap(content: String, sizePx: Int = 240): androidx.compose.ui.graphics.ImageBitmap? {
    return remember(content, sizePx) {
        if (content.isBlank()) return@remember null
        try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bmp.setPixel(x, y, if (matrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            bmp.asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }
}

