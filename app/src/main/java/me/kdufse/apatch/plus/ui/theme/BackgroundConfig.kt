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

    // Multi-Background Mode
    var isMultiBackgroundEnabled: Boolean by mutableStateOf(false)
        private set
    var homeBackgroundUri: String? by mutableStateOf(null)
        private set
    var kernelBackgroundUri: String? by mutableStateOf(null)
        private set
    var superuserBackgroundUri: String? by mutableStateOf(null)
        private set
    var systemModuleBackgroundUri: String? by mutableStateOf(null)
        private set
    var settingsBackgroundUri: String? by mutableStateOf(null)
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

    private const val KEY_MULTI_BACKGROUND_ENABLED = "multi_background_enabled"
    private const val KEY_HOME_BACKGROUND_URI = "home_background_uri"
    private const val KEY_KERNEL_BACKGROUND_URI = "kernel_background_uri"
    private const val KEY_SUPERUSER_BACKGROUND_URI = "superuser_background_uri"
    private const val KEY_SYSTEM_MODULE_BACKGROUND_URI = "system_module_background_uri"
    private const val KEY_SETTINGS_BACKGROUND_URI = "settings_background_uri"

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

    // Multi-Background Setters
    fun setMultiBackgroundEnabledState(enabled: Boolean) {
        isMultiBackgroundEnabled = enabled
    }

    fun updateHomeBackgroundUri(uri: String?) {
        homeBackgroundUri = uri
    }

    fun updateKernelBackgroundUri(uri: String?) {
        kernelBackgroundUri = uri
    }

    fun updateSuperuserBackgroundUri(uri: String?) {
        superuserBackgroundUri = uri
    }

    fun updateSystemModuleBackgroundUri(uri: String?) {
        systemModuleBackgroundUri = uri
    }

    fun updateSettingsBackgroundUri(uri: String?) {
        settingsBackgroundUri = uri
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

            putBoolean(KEY_MULTI_BACKGROUND_ENABLED, isMultiBackgroundEnabled)
            putString(KEY_HOME_BACKGROUND_URI, homeBackgroundUri)
            putString(KEY_KERNEL_BACKGROUND_URI, kernelBackgroundUri)
            putString(KEY_SUPERUSER_BACKGROUND_URI, superuserBackgroundUri)
            putString(KEY_SYSTEM_MODULE_BACKGROUND_URI, systemModuleBackgroundUri)
            putString(KEY_SETTINGS_BACKGROUND_URI, settingsBackgroundUri)
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

        val multiEnabled = prefs.getBoolean(KEY_MULTI_BACKGROUND_ENABLED, false)
        val homeUri = prefs.getString(KEY_HOME_BACKGROUND_URI, null)
        val kernelUri = prefs.getString(KEY_KERNEL_BACKGROUND_URI, null)
        val superuserUri = prefs.getString(KEY_SUPERUSER_BACKGROUND_URI, null)
        val systemModuleUri = prefs.getString(KEY_SYSTEM_MODULE_BACKGROUND_URI, null)
        val settingsUri = prefs.getString(KEY_SETTINGS_BACKGROUND_URI, null)
        
        Log.d(TAG, "加载背景配置: URI=$uri, enabled=$enabled, opacity=$opacity, dim=$dim")
        
        customBackgroundUri = uri
        isCustomBackgroundEnabled = enabled
        customBackgroundOpacity = opacity
        customBackgroundDim = dim
        
        gridWorkingCardBackgroundUri = gridUri
        isGridWorkingCardBackgroundEnabled = gridEnabled
        gridWorkingCardBackgroundOpacity = gridOpacity
        gridWorkingCardBackgroundDim = gridDim

        isMultiBackgroundEnabled = multiEnabled
        homeBackgroundUri = homeUri
        kernelBackgroundUri = kernelUri
        superuserBackgroundUri = superuserUri
        systemModuleBackgroundUri = systemModuleUri
        settingsBackgroundUri = settingsUri
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

        isMultiBackgroundEnabled = false
        homeBackgroundUri = null
        kernelBackgroundUri = null
        superuserBackgroundUri = null
        systemModuleBackgroundUri = null
        settingsBackgroundUri = null
    }
}

/**
 * 背景管理器
 */
object BackgroundManager {
    private const val TAG = "BackgroundManager"
    private const val BACKGROUND_FILENAME = "background.jpg"
    private const val GRID_WORKING_CARD_BACKGROUND_FILENAME = "grid_working_card_background.jpg"

    // Multi-Background Filenames
    private const val HOME_BACKGROUND_FILENAME = "background_home"
    private const val KERNEL_BACKGROUND_FILENAME = "background_kernel"
    private const val SUPERUSER_BACKGROUND_FILENAME = "background_superuser"
    private const val SYSTEM_MODULE_BACKGROUND_FILENAME = "background_system_module"
    private const val SETTINGS_BACKGROUND_FILENAME = "background_settings"

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
     * Clear generic background
     */
    private fun clearGenericBackground(
        context: Context,
        filenameBase: String,
        updateConfigAction: (String?) -> Unit
    ) {
        try {
            clearOldFiles(context, filenameBase)
            updateConfigAction(null)
            BackgroundConfig.save(context)
        } catch (e: Exception) {
            Log.e(TAG, "清除 $filenameBase 失败: ${e.message}", e)
        }
    }

    /**
     * Save and apply generic background
     */
    private suspend fun saveAndApplyGenericBackground(
        context: Context,
        uri: Uri,
        filenameBase: String,
        updateConfigAction: (String) -> Unit
    ): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val extension = getFileExtension(context, uri)
                clearOldFiles(context, filenameBase)
                
                val targetFile = File(context.filesDir, "$filenameBase$extension")
                val savedUri = saveImageToInternalStorage(context, uri, targetFile)
                
                if (savedUri != null) {
                    Log.d(TAG, "$filenameBase 图片保存成功，URI: $savedUri")
                    updateConfigAction(savedUri.toString())
                    BackgroundConfig.save(context)
                    true
                } else {
                    Log.e(TAG, "$filenameBase 图片保存失败")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "保存 $filenameBase 失败: ${e.message}", e)
            false
        }
    }

    // Home Background
    suspend fun saveAndApplyHomeBackground(context: Context, uri: Uri) = 
        saveAndApplyGenericBackground(context, uri, HOME_BACKGROUND_FILENAME) { BackgroundConfig.updateHomeBackgroundUri(it) }
    
    fun clearHomeBackground(context: Context) = 
        clearGenericBackground(context, HOME_BACKGROUND_FILENAME) { BackgroundConfig.updateHomeBackgroundUri(it) }

    // Kernel Background
    suspend fun saveAndApplyKernelBackground(context: Context, uri: Uri) = 
        saveAndApplyGenericBackground(context, uri, KERNEL_BACKGROUND_FILENAME) { BackgroundConfig.updateKernelBackgroundUri(it) }
    
    fun clearKernelBackground(context: Context) = 
        clearGenericBackground(context, KERNEL_BACKGROUND_FILENAME) { BackgroundConfig.updateKernelBackgroundUri(it) }

    // Superuser Background
    suspend fun saveAndApplySuperuserBackground(context: Context, uri: Uri) = 
        saveAndApplyGenericBackground(context, uri, SUPERUSER_BACKGROUND_FILENAME) { BackgroundConfig.updateSuperuserBackgroundUri(it) }
    
    fun clearSuperuserBackground(context: Context) = 
        clearGenericBackground(context, SUPERUSER_BACKGROUND_FILENAME) { BackgroundConfig.updateSuperuserBackgroundUri(it) }

    // System Module Background
    suspend fun saveAndApplySystemModuleBackground(context: Context, uri: Uri) = 
        saveAndApplyGenericBackground(context, uri, SYSTEM_MODULE_BACKGROUND_FILENAME) { BackgroundConfig.updateSystemModuleBackgroundUri(it) }
    
    fun clearSystemModuleBackground(context: Context) = 
        clearGenericBackground(context, SYSTEM_MODULE_BACKGROUND_FILENAME) { BackgroundConfig.updateSystemModuleBackgroundUri(it) }

    // Settings Background
    suspend fun saveAndApplySettingsBackground(context: Context, uri: Uri) = 
        saveAndApplyGenericBackground(context, uri, SETTINGS_BACKGROUND_FILENAME) { BackgroundConfig.updateSettingsBackgroundUri(it) }
    
    fun clearSettingsBackground(context: Context) = 
        clearGenericBackground(context, SETTINGS_BACKGROUND_FILENAME) { BackgroundConfig.updateSettingsBackgroundUri(it) }
    
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