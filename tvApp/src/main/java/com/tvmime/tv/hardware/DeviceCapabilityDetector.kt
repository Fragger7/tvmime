package com.tvmime.tv.hardware

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecList
import android.os.Build

data class DeviceCapabilities(
    val model: String,
    val androidVersion: String,
    val totalRamMb: Long,
    val isLowRamDevice: Boolean,
    val hasHevcHardwareDecoder: Boolean,
    val hasAv1HardwareDecoder: Boolean,
    val hasAvcHardwareDecoder: Boolean,
    val recommendedBufferProfile: String,
    val recommendedBufferReason: String,
    val recommendedDecoderMode: String
)

object DeviceCapabilityDetector {

    fun detect(context: Context): DeviceCapabilities {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager?.getMemoryInfo(memInfo)

        val totalRamMb = memInfo.totalMem / (1024 * 1024)
        val isLowRam = totalRamMb < 2200 // Devices with ~2GB RAM or less (e.g. Firestick Lite, Firestick 4K 1st gen)

        var hasHevc = false
        var hasAv1 = false
        var hasAvc = false

        try {
            val codecList = MediaCodecList(MediaCodecList.ALL_CODECS)
            for (codec in codecList.codecInfos) {
                if (codec.isEncoder) continue
                val name = codec.name.lowercase()
                val isHardware = !name.startsWith("omx.google.") && 
                                 !name.startsWith("c2.android.") && 
                                 !name.contains("sw")

                for (type in codec.supportedTypes) {
                    if (type.equals("video/hevc", ignoreCase = true) && isHardware) {
                        hasHevc = true
                    }
                    if (type.equals("video/av01", ignoreCase = true) && isHardware) {
                        hasAv1 = true
                    }
                    if (type.equals("video/avc", ignoreCase = true) && isHardware) {
                        hasAvc = true
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback heuristics based on Android API level
            hasHevc = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
            hasAvc = true
        }

        val (profile, reason) = if (isLowRam) {
            "Fast Zap (Low RAM Profile)" to "Device has ${totalRamMb}MB RAM. Using 1.5s initial buffer to prevent OS memory exhaustion."
        } else {
            "Balanced 4K Buffer" to "Device has ${totalRamMb / 1024}GB RAM. Using 5.0s deep buffer with hardware acceleration."
        }

        val decoderMode = if (hasHevc) {
            "Hardware Accelerated (HEVC/H.265 VPU)"
        } else {
            "Standard Hardware (AVC/H.264)"
        }

        return DeviceCapabilities(
            model = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            totalRamMb = totalRamMb,
            isLowRamDevice = isLowRam,
            hasHevcHardwareDecoder = hasHevc,
            hasAv1HardwareDecoder = hasAv1,
            hasAvcHardwareDecoder = hasAvc,
            recommendedBufferProfile = profile,
            recommendedBufferReason = reason,
            recommendedDecoderMode = decoderMode
        )
    }
}
