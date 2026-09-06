package com.tvmime.tv.ui.onboarding

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.tvmime.tv.viewmodel.TvMainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: TvMainViewModel,
    onLoginSuccess: () -> Unit
) {
    var qrBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
    val sessionCode = remember { (100000..999999).random().toString() }
    val vercelUrl = "https://tvmime.vercel.app/link?code=$sessionCode"

    LaunchedEffect(sessionCode) {
        // Generate QR on background thread to prevent UI stutter
        qrBitmap = withContext(Dispatchers.Default) {
            generateQrCode(vercelUrl, 400)
        }
        
        viewModel.startFirebaseSyncListener(sessionCode) {
            onLoginSuccess()
        }
        // In reality, here we would also start listening to Firebase for this sessionCode
    }

    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(0.8f).fillMaxHeight(0.8f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Option 1: QR Login
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Option 1: Phone Login", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                qrBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap,
                        contentDescription = "QR Code",
                        modifier = Modifier.size(200.dp).background(Color.White).padding(8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Scan with your camera", style = MaterialTheme.typography.bodyLarge)
                Text(text = "tvmime.vercel.app/link?code=$sessionCode", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }

            // Option 2: Email & Password (Mocked for UI Sprint)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Option 2: Account Sync", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                
                // Demo button simulating successful credential retrieval
                Surface(
                    onClick = { 
                        // Simulate successful login -> Trigger mock sync -> Navigate to Live TV
                        viewModel.triggerMockSync("https://iptv-org.github.io/iptv/countries/us.m3u")
                        onLoginSuccess()
                    },
                    modifier = Modifier.width(250.dp).height(50.dp),
                    colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "Simulate Login Success")
                    }
                }
            }
        }
    }
}

private fun generateQrCode(content: String, sizePx: Int): androidx.compose.ui.graphics.ImageBitmap {
    val hints = mapOf(EncodeHintType.MARGIN to 0)
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
    val width = matrix.width
    val height = matrix.height
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    for (x in 0 until width) {
        for (y in 0 until height) {
            bitmap.setPixel(x, y, if (matrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE)
        }
    }
    return bitmap.asImageBitmap()
}
