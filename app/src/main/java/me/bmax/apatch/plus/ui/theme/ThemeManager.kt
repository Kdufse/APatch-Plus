package me.bmax.apatch.ui.theme

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.bmax.apatch.APApplication
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object ThemeManager {
    private const val TAG = "ThemeManager"
    private const val THEME_CONFIG_FILENAME = "theme.json"
    private const val BACKGROUND_FILENAME = "background.jpg"
    private const val FONT_FILENAME = "font.ttf"
    private const val KEY_STR = "FolkPatchThemeSecretKey2025"

    private fun getSecretKey(): SecretKeySpec {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(KEY_STR.toByteArray())
        return SecretKeySpec(bytes, "AES")
    }

    data class ThemeConfig(
        val isBackgroundEnabled: Boolean,
        val backgroundOpacity: Float,
        val backgroundDim: Float,
        val isFontEnabled: Boolean,
        val customColor: String,
        val homeLayoutStyle: String,
        val nightModeEnabled: Boolean,
        val nightModeFollowSys: Boolean,
        // Grid Working Card Background
        val isGridWorkingCardBackgroundEnabled: Boolean = false,
        val gridWorkingCardBackgroundOpacity: Float = 1.0f,
        val gridWorkingCardBackgroundDim: Float = 0.3f
    )

    data class ThemeMetadata(
        val name: String,
        val type: String, // "phone" or "tablet"
        val version: String,
        val author: String,
        val description: String
    )

    suspend fun exportTheme(context: Context, uri: Uri, metadata: ThemeMetadata): Boolean {
        return withContext(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "theme_export")
            if (cacheDir.exists()) cacheDir.deleteRecursively()
            cacheDir.mkdirs()

            try {
                // 1. Collect Config
                val prefs = APApplication.sharedPreferences
                val config = ThemeConfig(
                    isBackgroundEnabled = BackgroundConfig.isCustomBackgroundEnabled,
                    backgroundOpacity = BackgroundConfig.customBackgroundOpacity,
                    backgroundDim = BackgroundConfig.customBackgroundDim,
                    isFontEnabled = FontConfig.isCustomFontEnabled,
                    customColor = prefs.getString("custom_color", "blue") ?: "blue",
                    homeLayoutStyle = prefs.getString("home_layout_style", "default") ?: "default",
                    nightModeEnabled = prefs.getBoolean("night_mode_enabled", false),
                    nightModeFollowSys = prefs.getBoolean("night_mode_follow_sys", true),
                    isGridWorkingCardBackgroundEnabled = BackgroundConfig.isGridWorkingCardBackgroundEnabled,
                    gridWorkingCardBackgroundOpacity = BackgroundConfig.gridWorkingCardBackgroundOpacity,
                    gridWorkingCardBackgroundDim = BackgroundConfig.gridWorkingCardBackgroundDim
                )

                // 2. Write Config JSON
                val json = JSONObject().apply {
                    put("isBackgroundEnabled", config.isBackgroundEnabled)
                    put("backgroundOpacity", config.backgroundOpacity.toDouble())
                    put("backgroundDim", config.backgroundDim.toDouble())
                    put("isFontEnabled", config.isFontEnabled)
                    put("customColor", config.customColor)
                    put("homeLayoutStyle", config.homeLayoutStyle)
                    put("nightModeEnabled", config.nightModeEnabled)
                    put("nightModeFollowSys", config.nightModeFollowSys)
                    
                    // Grid Working Card Background
                    put("isGridWorkingCardBackgroundEnabled", config.isGridWorkingCardBackgroundEnabled)
                    put("gridWorkingCardBackgroundOpacity", config.gridWorkingCardBackgroundOpacity.toDouble())
                    put("gridWorkingCardBackgroundDim", config.gridWorkingCardBackgroundDim.toDouble())

                    // Add metadata
                    put("meta_name", metadata.name)
                    put("meta_type", metadata.type)
                    put("meta_version", metadata.version)
                    put("meta_author", metadata.author)
                    put("meta_description", metadata.description)
                }
                File(cacheDir, THEME_CONFIG_FILENAME).writeText(json.toString())


                // 3. Copy Background if enabled
                if (config.isBackgroundEnabled) {
                    val extensions = listOf(".jpg", ".png", ".gif", ".webp")
                    for (ext in extensions) {
                        val bgFile = File(context.filesDir, "background$ext")
                        if (bgFile.exists()) {
                            bgFile.copyTo(File(cacheDir, "background$ext"))
                            break // Only one background file should exist
                        }
                    }
                }
                
                // Copy Grid Working Card Background if enabled
                if (config.isGridWorkingCardBackgroundEnabled) {
                    val extensions = listOf(".jpg", ".png", ".gif", ".webp")
                    for (ext in extensions) {
                        val bgFile = File(context.filesDir, "grid_working_card_background$ext")
                        if (bgFile.exists()) {
                            bgFile.copyTo(File(cacheDir, "grid_working_card_background$ext"))
                            break 
                        }
                    }
                }

                // 4. Copy Font if enabled
                if (config.isFontEnabled) {
                    val fontName = FontConfig.customFontFilename
                    if (fontName != null) {
                        val fontFile = File(context.filesDir, fontName)
                        if (fontFile.exists()) {
                            fontFile.copyTo(File(cacheDir, FONT_FILENAME))
                        }
                    }
                }

                // 5. Encrypt and Zip to Uri
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    // Init Cipher
                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
                    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

                    // Write IV first
                    os.write(iv)

                    CipherOutputStream(os, cipher).use { cos ->
                        ZipOutputStream(BufferedOutputStream(cos)).use { zos ->
                            cacheDir.listFiles()?.forEach { file ->
                                val entry = ZipEntry(file.name)
                                zos.putNextEntry(entry)
                                FileInputStream(file).use { fis ->
                                    fis.copyTo(zos)
                                }
                                zos.closeEntry()
                            }
                        }
                    }
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Export failed", e)
                false
            } finally {
                cacheDir.deleteRecursively()
            }
        }
    }

    suspend fun readThemeMetadata(context: Context, uri: Uri): ThemeMetadata? {
        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { `is` ->
                    // Read IV
                    val iv = ByteArray(16)
                    if (`is`.read(iv) != 16) return@withContext null

                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

                    CipherInputStream(`is`, cipher).use { cis ->
                        ZipInputStream(BufferedInputStream(cis)).use { zis ->
                            var entry: ZipEntry?
                            while (zis.nextEntry.also { entry = it } != null) {
                                if (entry!!.name == THEME_CONFIG_FILENAME) {
                                    // Read the JSON content
                                    val jsonStr = zis.bufferedReader().use { it.readText() }
                                    val json = JSONObject(jsonStr)
                                    return@withContext ThemeMetadata(
                                        name = json.optString("meta_name", ""),
                                        type = json.optString("meta_type", "phone"),
                                        version = json.optString("meta_version", ""),
                                        author = json.optString("meta_author", ""),
                                        description = json.optString("meta_description", "")
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read theme metadata", e)
            }
            null
        }
    }

    suspend fun importTheme(context: Context, uri: Uri): Boolean {
        return withContext(Dispatchers.IO) {
            val cacheDir = File(context.cacheDir, "theme_import")
            if (cacheDir.exists()) cacheDir.deleteRecursively()
            cacheDir.mkdirs()

            try {
                // 1. Decrypt and Unzip
                context.contentResolver.openInputStream(uri)?.use { `is` ->
                    // Read IV
                    val iv = ByteArray(16)
                    if (`is`.read(iv) != 16) throw Exception("Invalid theme file")

                    val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), IvParameterSpec(iv))

                    CipherInputStream(`is`, cipher).use { cis ->
                        ZipInputStream(BufferedInputStream(cis)).use { zis ->
                            var entry: ZipEntry?
                            while (zis.nextEntry.also { entry = it } != null) {
                                val file = File(cacheDir, entry!!.name)
                                // Prevent path traversal
                                if (!file.canonicalPath.startsWith(cacheDir.canonicalPath)) {
                                    continue
                                }
                                FileOutputStream(file).use { fos ->
                                    zis.copyTo(fos)
                                }
                            }
                        }
                    }
                }

                // 2. Read Config
                val configFile = File(cacheDir, THEME_CONFIG_FILENAME)
                if (!configFile.exists()) return@withContext false
                
                val json = JSONObject(configFile.readText())
                val isBackgroundEnabled = json.optBoolean("isBackgroundEnabled", false)
                val backgroundOpacity = json.optDouble("backgroundOpacity", 0.5).toFloat()
                val backgroundDim = json.optDouble("backgroundDim", 0.2).toFloat()
                val isFontEnabled = json.optBoolean("isFontEnabled", false)
                val customColor = json.optString("customColor", "blue")
                val homeLayoutStyle = json.optString("homeLayoutStyle", "default")
                val nightModeEnabled = json.optBoolean("nightModeEnabled", false)
                val nightModeFollowSys = json.optBoolean("nightModeFollowSys", true)
                
                // Grid Working Card Background
                val isGridWorkingCardBackgroundEnabled = json.optBoolean("isGridWorkingCardBackgroundEnabled", false)
                val gridWorkingCardBackgroundOpacity = json.optDouble("gridWorkingCardBackgroundOpacity", 1.0).toFloat()
                val gridWorkingCardBackgroundDim = json.optDouble("gridWorkingCardBackgroundDim", 0.3).toFloat()

                // 3. Apply Background
                BackgroundConfig.setCustomBackgroundOpacityValue(backgroundOpacity)
                BackgroundConfig.setCustomBackgroundDimValue(backgroundDim)
                BackgroundConfig.setCustomBackgroundEnabledState(isBackgroundEnabled)

                if (isBackgroundEnabled) {
                    val extensions = listOf(".jpg", ".png", ".gif", ".webp")
                    var bgFound = false
                    for (ext in extensions) {
                        val bgFile = File(cacheDir, "background$ext")
                        if (bgFile.exists()) {
                            // Clear old background files first
                            for (oldExt in extensions) {
                                val oldFile = File(context.filesDir, "background$oldExt")
                                if (oldFile.exists()) oldFile.delete()
                            }
                            
                            val destFile = File(context.filesDir, "background$ext")
                            bgFile.copyTo(destFile, overwrite = true)
                            // Update URI to point to local file with timestamp to force refresh
                             val fileUri = Uri.fromFile(destFile).buildUpon()
                                .appendQueryParameter("t", System.currentTimeMillis().toString())
                                .build()
                             BackgroundConfig.updateCustomBackgroundUri(fileUri.toString())
                             bgFound = true
                             break
                        }
                    }
                    if (!bgFound) {
                        // Fallback logic if needed, or disable background
                    }
                } else {
                     // Maybe clear if we want to enforce theme state exactly
                     // But user might want to keep files.
                     // The requirement implies importing the theme as is.
                }
                
                // Apply Grid Working Card Background
                BackgroundConfig.setGridWorkingCardBackgroundOpacityValue(gridWorkingCardBackgroundOpacity)
                BackgroundConfig.setGridWorkingCardBackgroundDimValue(gridWorkingCardBackgroundDim)
                BackgroundConfig.setGridWorkingCardBackgroundEnabledState(isGridWorkingCardBackgroundEnabled)
                
                if (isGridWorkingCardBackgroundEnabled) {
                    val extensions = listOf(".jpg", ".png", ".gif", ".webp")
                    for (ext in extensions) {
                        val bgFile = File(cacheDir, "grid_working_card_background$ext")
                        if (bgFile.exists()) {
                            // Clear old files
                            for (oldExt in extensions) {
                                val oldFile = File(context.filesDir, "grid_working_card_background$oldExt")
                                if (oldFile.exists()) oldFile.delete()
                            }
                            
                            val destFile = File(context.filesDir, "grid_working_card_background$ext")
                            bgFile.copyTo(destFile, overwrite = true)
                            // Update URI
                             val fileUri = Uri.fromFile(destFile).buildUpon()
                                .appendQueryParameter("t", System.currentTimeMillis().toString())
                                .build()
                             BackgroundConfig.updateGridWorkingCardBackgroundUri(fileUri.toString())
                             break
                        }
                    }
                }
                
                BackgroundConfig.save(context)

                // 4. Apply Font
                if (isFontEnabled) {
                     val fontFile = File(cacheDir, FONT_FILENAME)
                     if (fontFile.exists()) {
                         FontConfig.applyCustomFont(context, fontFile)
                     }
                } else {
                    FontConfig.clearFont(context)
                }
                
                // 5. Apply Color and Home Layout Style
                APApplication.sharedPreferences.edit()
                    .putString("custom_color", customColor)
                    .putString("home_layout_style", homeLayoutStyle)
                    .putBoolean("night_mode_enabled", nightModeEnabled)
                    .putBoolean("night_mode_follow_sys", nightModeFollowSys)
                    .apply()
                
                // 6. Refresh Theme
                refreshTheme.postValue(true)
                
                true
            } catch (e: Exception) {
                Log.e(TAG, "Import failed", e)
                false
            } finally {
                cacheDir.deleteRecursively()
            }
        }
    }
}
