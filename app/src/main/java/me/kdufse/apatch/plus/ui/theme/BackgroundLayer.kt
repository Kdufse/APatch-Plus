package me.kdufse.apatch.plus.ui.theme

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.paint
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.kdufse.apatch.plus.APApplication

import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.ui.viewinterop.AndroidView
import com.ramcosta.composedestinations.generated.destinations.APModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.HomeScreenDestination
import com.ramcosta.composedestinations.generated.destinations.KPModuleScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SuperUserScreenDestination

/**
 * Background Layer Component
 * Priority: Video > Multi/Single Image > Default
 */
@Composable
fun BackgroundLayer(currentRoute: String? = null) {
    val context = LocalContext.current
    
    // Video Background Logic
    // Only show video if Custom Background is enabled AND Video Background is enabled
    if (BackgroundConfig.isCustomBackgroundEnabled && BackgroundConfig.isVideoBackgroundEnabled && !BackgroundConfig.videoBackgroundUri.isNullOrEmpty()) {
        var mediaPlayer by remember { mutableStateOf<android.media.MediaPlayer?>(null) }
        var videoView by remember { mutableStateOf<VideoView?>(null) }

        DisposableEffect(Unit) {
            onDispose {
                try {
                    videoView?.stopPlayback()
                    mediaPlayer?.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        key(BackgroundConfig.videoBackgroundUri) {
            AndroidView(
                factory = { ctx ->
                    object : VideoView(ctx) {
                        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
                            val width = getDefaultSize(0, widthMeasureSpec)
                            val height = getDefaultSize(0, heightMeasureSpec)
                            setMeasuredDimension(width, height)
                        }
                    }.apply {
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setVideoPath(BackgroundConfig.videoBackgroundUri)
                        setOnPreparedListener { mp ->
                            mp.isLooping = true
                            mp.setVideoScalingMode(android.media.MediaPlayer.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING)
                            mediaPlayer = mp
                            val vol = BackgroundConfig.videoVolume
                            mp.setVolume(vol, vol)
                            start()
                        }
                        setOnErrorListener { _, _, _ ->
                            true
                        }
                        videoView = this
                    }
                },
                update = {
                    mediaPlayer?.let { mp ->
                        val vol = BackgroundConfig.videoVolume
                        mp.setVolume(vol, vol)
                    }
                },
                onRelease = { view ->
                    try {
                        view.stopPlayback()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxSize().zIndex(-2f)
            )
        }
        
        // Dim overlay for video
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(-1f)
                .background(Color.Black.copy(alpha = BackgroundConfig.customBackgroundDim))
        )
        return
    }

    // Determine target URI for Image Background
    val rawTargetUri = if (BackgroundConfig.isMultiBackgroundEnabled) {
        when (currentRoute) {
            HomeScreenDestination.route -> BackgroundConfig.homeBackgroundUri
            KPModuleScreenDestination.route -> BackgroundConfig.kernelBackgroundUri
            SuperUserScreenDestination.route -> BackgroundConfig.superuserBackgroundUri
            APModuleScreenDestination.route -> BackgroundConfig.systemModuleBackgroundUri
            SettingScreenDestination.route -> BackgroundConfig.settingsBackgroundUri
            else -> BackgroundConfig.homeBackgroundUri
        }
    } else {
        BackgroundConfig.customBackgroundUri
    }

    // Resolve "background.png" to asset path
    val targetModel = if (rawTargetUri == "background.png") {
        "file:///android_asset/background.png"
    } else {
        rawTargetUri
    }

    // Default background (fallback)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(-2f)
            .background(MaterialTheme.colorScheme.background)
    )

    // Image Background Logic
    // Fix: Ensure we only show background if it is enabled AND uri is valid.
    // Also handle fallback for multi-background: if specific page bg is null, fallback to customBackgroundUri (global) if enabled?
    // User request: "If multi-background mode is enabled, disabling the switch should stop it."
    // Current logic: BackgroundConfig.isCustomBackgroundEnabled controls GLOBAL background.
    // Multi-background mode is an additional layer.
    // If isMultiBackgroundEnabled is FALSE, we use customBackgroundUri.
    // If isMultiBackgroundEnabled is TRUE, we use page specific URIs.
    // BUT, isCustomBackgroundEnabled acts as a master switch for "Show ANY background image".
    // If master switch is OFF, no image should be shown regardless of multi-mode.
    
    if (BackgroundConfig.isCustomBackgroundEnabled && targetModel != null && (targetModel !is String || targetModel.isNotEmpty())) {
        val painter = rememberAsyncImagePainter(
            model = targetModel,
            onError = { error ->
                android.util.Log.e("BackgroundLayer", "Failed to load background: ${error.result.throwable.message}")
            }
        )
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(-2f)
                .paint(painter = painter, contentScale = ContentScale.Crop)
        )
        
        // Dim overlay for image
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(-1f)
                .background(Color.Black.copy(alpha = BackgroundConfig.customBackgroundDim))
        )
    }
}

/**
 * 扩展函数，用于保存自定义背景
 */
fun Context.saveCustomBackground(uri: Uri?) {
    if (uri != null) {
        // 使用IO调度器在后台线程中处理
        kotlinx.coroutines.GlobalScope.launch(Dispatchers.IO) {
            BackgroundManager.saveAndApplyCustomBackground(this@saveCustomBackground, uri)
        }
    } else {
        BackgroundManager.clearCustomBackground(this)
    }
}