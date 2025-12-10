package me.kdufse.apatch.plus.ui.theme

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * 背景配置管理类
 */
object BackgroundConfig {
    var customBackgroundUri: String? by mutableStateOf(null)
        private set
    var isCustomBackgroundEnabled: Boolean by mutableStateOf(false)
        private set
    var customBackgroundOpacity: Float by mutableStateOf(0.5f)
        private set
    var customBackgroundDim: Float by mutableStateOf(0.2f)
        private set

    // Grid Layout Working Card Background
    var gridWorkingCardBackgroundUri: String? by mutableStateOf(null)
        private set
    var isGridWorkingCardBackgroundEnabled: Boolean by mutableStateOf(false)
        private set
    var gridWorkingCardBackgroundOpacity: Float by mutableStateOf(1.0f)
        private set
    var gridWorkingCardBackgroundDim: Float by mutableStateOf(0.3f)
        private set
    
    private const val PREFS_NAME = "background_settings"
    private const val KEY_CUSTOM_BACKGROUND_URI = "custom_background_uri"
    private const val KEY_CUSTOM_BACKGROUND_ENABLED = "custom_background_enabled"
    private const val KEY_CUSTOM_BACKGROUND_OPACITY = "custom_background_opacity"
    private const val KEY_CUSTOM_BACKGROUND_DIM = "custom_background_dim"
    
    private const val KEY_GRID_WORKING_CARD_BACKGROUND_URI = "grid_working_card_background_uri"
    private const val KEY_GRID_WORKING_CARD_BACKGROUND_ENABLED = "grid_working_card_background_enabled"
    private const val KEY_GRID_WORKING_CARD_BACKGROUND_OPACITY = "grid_working_card_background_opacity"
    private const val KEY_GRID_WORKING_CARD_BACKGROUND_DIM = "grid_working_card_background_dim"

    private const val TAG = "BackgroundConfig"
    
    /**
     * 更新自定义背景URI
     */
    fun updateCustomBackgroundUri(uri: String?) {
        customBackgroundUri = uri
        isCustomBackgroundEnabled = uri != null
    }

    /**
     * 更新Grid布局工作中卡片背景URI
     */
    fun updateGridWorkingCardBackgroundUri(uri: String?) {
        gridWorkingCardBackgroundUri = uri
        isGridWorkingCardBackgroundEnabled = uri != null
    }
    
    /**
     * 启用/禁用自定义背景
     */
    fun setCustomBackgroundEnabledState(enabled: Boolean) {
        isCustomBackgroundEnabled = enabled
    }

    /**
     * 启用/禁用Grid布局工作中卡片背景
     */
    fun setGridWorkingCardBackgroundEnabledState(enabled: Boolean) {
        isGridWorkingCardBackgroundEnabled = enabled
    }

    /**
     * 设置Grid布局工作中卡片背景不透明度
     */
    fun setGridWorkingCardBackgroundOpacityValue(opacity: Float) {
        gridWorkingCardBackgroundOpacity = opacity
    }

    /**
     * 设置Grid布局工作中卡片背景暗度
     */
    fun setGridWorkingCardBackgroundDimValue(dim: Float) {
        gridWorkingCardBackgroundDim = dim
    }

    /**
     * 设置自定义背景不透明度
     */
    fun setCustomBackgroundOpacityValue(opacity: Float) {
        customBackgroundOpacity = opacity
    }

    /**
     * 设置自定义背景暗度
     */
    fun setCustomBackgroundDimValue(dim: Float) {
        customBackgroundDim = dim
    }
    
    /**
     * 保存配置到SharedPreferences
     */
    fun save(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString(KEY_CUSTOM_BACKGROUND_URI, customBackgroundUri)
            putBoolean(KEY_CUSTOM_BACKGROUND_ENABLED, isCustomBackgroundEnabled)
            putFloat(KEY_CUSTOM_BACKGROUND_OPACITY, customBackgroundOpacity)
            putFloat(KEY_CUSTOM_BACKGROUND_DIM, customBackgroundDim)
            
            putString(KEY_GRID_WORKING_CARD_BACKGROUND_URI, gridWorkingCardBackgroundUri)
            putBoolean(KEY_GRID_WORKING_CARD_BACKGROUND_ENABLED, isGridWorkingCardBackgroundEnabled)
            putFloat(KEY_GRID_WORKING_CARD_BACKGROUND_OPACITY, gridWorkingCardBackgroundOpacity)
            putFloat(KEY_GRID_WORKING_CARD_BACKGROUND_DIM, gridWorkingCardBackgroundDim)
            apply()
        }
    }
    
    /**
     * 从SharedPreferences加载配置
     */
    fun load(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val uri = prefs.getString(KEY_CUSTOM_BACKGROUND_URI, null)
        val enabled = prefs.getBoolean(KEY_CUSTOM_BACKGROUND_ENABLED, false)
        val opacity = prefs.getFloat(KEY_CUSTOM_BACKGROUND_OPACITY, 0.5f)
        val dim = prefs.getFloat(KEY_CUSTOM_BACKGROUND_DIM, 0.2f)
        
        val gridUri = prefs.getString(KEY_GRID_WORKING_CARD_BACKGROUND_URI, null)
        val gridEnabled = prefs.getBoolean(KEY_GRID_WORKING_CARD_BACKGROUND_ENABLED, false)
        val gridOpacity = prefs.getFloat(KEY_GRID_WORKING_CARD_BACKGROUND_OPACITY, 1.0f)
        val gridDim = prefs.getFloat(KEY_GRID_WORKING_CARD_BACKGROUND_DIM, 0.3f)
        
        Log.d(TAG, "加载背景配置: URI=$uri, enabled=$enabled, opacity=$opacity, dim=$dim")
        
        customBackgroundUri = uri
        isCustomBackgroundEnabled = enabled
        customBackgroundOpacity = opacity
        customBackgroundDim = dim
        
        gridWorkingCardBackgroundUri = gridUri
        isGridWorkingCardBackgroundEnabled = gridEnabled
        gridWorkingCardBackgroundOpacity = gridOpacity
        gridWorkingCardBackgroundDim = gridDim
    }
    
    /**
     * 重置配置
     */
    fun reset() {
        customBackgroundUri = null
        isCustomBackgroundEnabled = false
        customBackgroundOpacity = 0.5f
        customBackgroundDim = 0.2f
        
        gridWorkingCardBackgroundUri = null
        isGridWorkingCardBackgroundEnabled = false
        gridWorkingCardBackgroundOpacity = 1.0f
        gridWorkingCardBackgroundDim = 0.3f
    }
}

/**
 * 背景管理器
 */
object BackgroundManager {
    private const val TAG = "BackgroundManager"
    private const val BACKGROUND_FILENAME = "background.jpg"
    private const val GRID_WORKING_CARD_BACKGROUND_FILENAME = "grid_working_card_background.jpg"

    /**
     * 获取文件扩展名
     */
    private fun getFileExtension(context: Context, uri: Uri): String {
        return try {
            val mimeType = context.contentResolver.getType(uri)
            when {
                mimeType?.contains("gif", true) == true -> ".gif"
                mimeType?.contains("png", true) == true -> ".png"
                mimeType?.contains("webp", true) == true -> ".webp"
                else -> ".jpg"
            }
        } catch (e: Exception) {
            ".jpg"
        }
    }

    /**
     * 获取背景文件
     */
    private fun getBackgroundFile(context: Context, extension: String = ".jpg"): File {
        return File(context.filesDir, "background$extension")
    }

    private fun getGridWorkingCardBackgroundFile(context: Context, extension: String = ".jpg"): File {
        return File(context.filesDir, "grid_working_card_background$extension")
    }

    /**
     * 清理旧的背景文件
     */
    private fun clearOldFiles(context: Context, baseName: String) {
        val extensions = listOf(".jpg", ".png", ".gif", ".webp")
        extensions.forEach { ext ->
            val file = File(context.filesDir, "$baseName$ext")
            if (file.exists()) {
                file.delete()
            }
        }
    }
    
    /**
     * 保存并应用自定义背景
     */
    suspend fun saveAndApplyCustomBackground(context: Context, uri: Uri): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val extension = getFileExtension(context, uri)
                // 清理旧文件
                clearOldFiles(context, "background")
                
                val savedUri = saveImageToInternalStorage(context, uri, getBackgroundFile(context, extension))
                if (savedUri != null) {
                    Log.d(TAG, "图片保存成功，URI: $savedUri")
                    BackgroundConfig.updateCustomBackgroundUri(savedUri.toString())
                    BackgroundConfig.save(context)
                    Log.d(TAG, "背景配置保存成功，URI: ${BackgroundConfig.customBackgroundUri}, 启用状态: ${BackgroundConfig.isCustomBackgroundEnabled}")
                    true
                } else {
                    Log.e(TAG, "图片保存失败")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存自定义背景失败: ${e.message}", e)
            false
        }
    }

    /**
     * 保存并应用Grid布局工作中卡片背景
     */
    suspend fun saveAndApplyGridWorkingCardBackground(context: Context, uri: Uri): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val extension = getFileExtension(context, uri)
                // 清理旧文件
                clearOldFiles(context, "grid_working_card_background")
                
                val savedUri = saveImageToInternalStorage(context, uri, getGridWorkingCardBackgroundFile(context, extension))
                if (savedUri != null) {
                    Log.d(TAG, "Grid卡片图片保存成功，URI: $savedUri")
                    BackgroundConfig.updateGridWorkingCardBackgroundUri(savedUri.toString())
                    BackgroundConfig.save(context)
                    true
                } else {
                    Log.e(TAG, "Grid卡片图片保存失败")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存Grid卡片自定义背景失败: ${e.message}", e)
            false
        }
    }
    
    /**
     * 清除自定义背景
     */
    fun clearCustomBackground(context: Context) {
        try {
            // 删除背景文件
            clearOldFiles(context, "background")
            
            // 重置配置（只重置全局背景相关）
            BackgroundConfig.updateCustomBackgroundUri(null)
            BackgroundConfig.setCustomBackgroundEnabledState(false)
            BackgroundConfig.save(context)
        } catch (e: Exception) {
            Log.e(TAG, "清除自定义背景失败: ${e.message}", e)
        }
    }

    /**
     * 清除Grid布局工作中卡片背景
     */
    fun clearGridWorkingCardBackground(context: Context) {
        try {
            // 删除背景文件
            clearOldFiles(context, "grid_working_card_background")
            
            // 重置配置
            BackgroundConfig.updateGridWorkingCardBackgroundUri(null)
            BackgroundConfig.setGridWorkingCardBackgroundEnabledState(false)
            BackgroundConfig.setGridWorkingCardBackgroundOpacityValue(1.0f)
            BackgroundConfig.setGridWorkingCardBackgroundDimValue(0.3f)
            BackgroundConfig.save(context)
        } catch (e: Exception) {
            Log.e(TAG, "清除Grid卡片自定义背景失败: ${e.message}", e)
        }
    }
    
    /**
     * 加载自定义背景
     */
    fun loadCustomBackground(context: Context) {
        BackgroundConfig.load(context)
    }
    
    /**
     * 保存图片到内部存储
     */
    private suspend fun saveImageToInternalStorage(context: Context, uri: Uri, targetFile: File): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
                // val file = getBackgroundFile(context) // Use targetFile instead

                FileOutputStream(targetFile).use { outputStream ->
                    val buffer = ByteArray(8 * 1024)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                }
                inputStream.close()

                // 返回带时间戳的URI，确保Compose重组
                val fileUri = Uri.fromFile(targetFile).buildUpon()
                    .appendQueryParameter("t", System.currentTimeMillis().toString())
                    .build()
                    
                Log.d(TAG, "图片保存成功，文件URI: $fileUri")
                fileUri
            } catch (e: Exception) {
                Log.e(TAG, "保存图片到内部存储失败: ${e.message}", e)
                null
            }
        }
    }
}