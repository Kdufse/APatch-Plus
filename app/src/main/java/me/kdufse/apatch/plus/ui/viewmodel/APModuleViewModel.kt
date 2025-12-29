package me.kdufse.apatch.plus.ui.viewmodel

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.kdufse.apatch.plus.apApp
import me.kdufse.apatch.plus.util.getRootShell
import me.kdufse.apatch.plus.util.listModules
import me.kdufse.apatch.plus.util.toggleModule
import org.json.JSONArray
import org.json.JSONObject
import java.text.Collator
import java.text.DecimalFormat
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import com.topjohnwu.superuser.io.SuFile

class APModuleViewModel : ViewModel() {
    companion object {
        private const val TAG = "ModuleViewModel"
        private var modules by mutableStateOf<List<ModuleInfo>>(emptyList())
    }

    class ModuleInfo(
        val id: String,
        val name: String,
        val author: String,
        val version: String,
        val versionCode: Int,
        val description: String,
        val enabled: Boolean,
        val update: Boolean,
        val remove: Boolean,
        val updateJson: String,
        val hasWebUi: Boolean,
        val hasActionScript: Boolean,
        val banner: String // 添加banner字段
    )

    data class ModuleUpdateInfo(
        val version: String,
        val versionCode: Int,
        val zipUrl: String,
        val changelog: String,
    )

    var isRefreshing by mutableStateOf(false)
        private set

    val moduleList by derivedStateOf {
        val comparator = compareBy(Collator.getInstance(Locale.getDefault()), ModuleInfo::id)
        modules.sortedWith(comparator).also {
            isRefreshing = false
        }
    }

    var isNeedRefresh by mutableStateOf(false)
        private set

    fun markNeedRefresh() {
        isNeedRefresh = true
    }

    fun disableAllModules() {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true
            modules.forEach { 
                if (it.enabled) {
                    toggleModule(it.id, false)
                }
            }
            fetchModuleList()
        }
    }

    fun fetchModuleList() {
        viewModelScope.launch(Dispatchers.IO) {
            isRefreshing = true

            val oldModuleList = modules

            val start = SystemClock.elapsedRealtime()

            kotlin.runCatching {

                val result = listModules()
                Log.i(TAG, "=== RAW MODULE DATA ===")
                Log.i(TAG, "Full result: $result")

                val array = JSONArray(result)
                modules = (0 until array.length())
                    .asSequence()
                    .map { array.getJSONObject(it) }
                    .map { obj ->
                        val id = obj.getString("id")
                        
                        // 从module.prop文件中读取banner变量
                        val banner = getModuleBanner(id)
                        
                        ModuleInfo(
                            id,
                            obj.optString("name"),
                            obj.optString("author", "Unknown"),
                            obj.optString("version", "Unknown"),
                            obj.optInt("versionCode", 0),
                            obj.optString("description"),
                            obj.getBoolean("enabled"),
                            obj.getBoolean("update"),
                            obj.getBoolean("remove"),
                            obj.optString("updateJson"),
                            obj.optBoolean("web"),
                            obj.optBoolean("action"),
                            banner  // 添加banner字段
                        )
                    }.toList()
                isNeedRefresh = false
                
                // 记录所有模块的banner信息
                Log.i(TAG, "=== MODULES WITH BANNER ===")
                modules.forEach { module ->
                    Log.i(TAG, "Module: ${module.id}, Banner: '${module.banner}'")
                }
            }.onFailure { e ->
                Log.e(TAG, "fetchModuleList: ", e)
                isRefreshing = false
            }

            // when both old and new is kotlin.collections.EmptyList
            // moduleList update will don't trigger
            if (oldModuleList === modules) {
                isRefreshing = false
            }

            Log.i(TAG, "load cost: ${SystemClock.elapsedRealtime() - start}, modules: $modules")
        }
    }
    
    // 从module.prop文件中读取banner变量
    private fun getModuleBanner(moduleId: String): String {
        return try {
            // 可能的module.prop文件路径
            val propPaths = listOf(
                "/data/adb/modules/$moduleId/module.prop",
                "/data/adb/ap/modules/$moduleId/module.prop"
            )
            
            for (propPath in propPaths) {
                val propFile = SuFile(propPath)
                if (propFile.exists() && propFile.canRead()) {
                    Log.i(TAG, "Reading module.prop from: $propPath")
                    
                    val lines = propFile.readText().lines()
                    for (line in lines) {
                        // 查找banner变量
                        if (line.startsWith("banner=")) {
                            val bannerValue = line.substringAfter("banner=").trim()
                            if (bannerValue.isNotEmpty()) {
                                Log.i(TAG, "Found banner for module $moduleId: $bannerValue")
                                return bannerValue
                            }
                        }
                    }
                }
            }
            
            // 如果没有在module.prop中找到banner，检查常见的banner文件
            detectBannerFile(moduleId)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading banner for module $moduleId: $e")
            ""
        }
    }
    
    // 检测常见的banner文件（兼容性）
    private fun detectBannerFile(moduleId: String): String {
        try {
            // 常见的banner文件名
            val bannerFiles = listOf("banner.jpg", "banner.png", "banner.webp", "banner.jpeg")
            
            // 可能的模块路径
            val modulePaths = listOf(
                "/data/adb/modules/$moduleId",
                "/data/adb/ap/modules/$moduleId"
            )
            
            for (modulePath in modulePaths) {
                val moduleDir = SuFile(modulePath)
                if (moduleDir.exists() && moduleDir.isDirectory) {
                    for (bannerFile in bannerFiles) {
                        val bannerPath = "$modulePath/$bannerFile"
                        val banner = SuFile(bannerPath)
                        if (banner.exists() && banner.canRead()) {
                            Log.i(TAG, "Found banner file for module $moduleId: $bannerFile")
                            return bannerFile
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting banner file for $moduleId: $e")
        }
        
        return "" // 没有找到banner
    }

    private fun sanitizeVersionString(version: String): String {
        return version.replace(Regex("[^a-zA-Z0-9.\\-_]"), "_")
    }

    fun checkUpdate(m: ModuleInfo): Triple<String, String, String> {
        val empty = Triple("", "", "")
        if (m.updateJson.isEmpty() || m.remove || m.update || !m.enabled) {
            return empty
        }
        // download updateJson
        val result = kotlin.runCatching {
            val url = m.updateJson
            Log.i(TAG, "checkUpdate url: $url")
            val response = apApp.okhttpClient
                .newCall(
                    okhttp3.Request.Builder()
                        .url(url)
                        .build()
                ).execute()
            Log.d(TAG, "checkUpdate code: ${response.code}")
            if (response.isSuccessful) {
                response.body?.string() ?: ""
            } else {
                ""
            }
        }.getOrDefault("")
        Log.i(TAG, "checkUpdate result: $result")

        if (result.isEmpty()) {
            return empty
        }

        val updateJson = kotlin.runCatching {
            JSONObject(result)
        }.getOrNull() ?: return empty

        val version = sanitizeVersionString(updateJson.optString("version", ""))
        val versionCode = updateJson.optInt("versionCode", 0)
        val zipUrl = updateJson.optString("zipUrl", "")
        val changelog = updateJson.optString("changelog", "")
        if (versionCode <= m.versionCode || zipUrl.isEmpty()) {
            return empty
        }

        return Triple(zipUrl, version, changelog)
    }

    fun getModuleSize(moduleId: String): String {
        val bytes = runCatching {
            val command = "/data/adb/ap/bin/busybox du -sb /data/adb/modules/$moduleId"
            val result = getRootShell().newJob().add(command).to(ArrayList(), null).exec()

            if (result.isSuccess && result.out.isNotEmpty()) {
                val sizeStr = result.out.firstOrNull()?.split("\t")?.firstOrNull()
                sizeStr?.toLongOrNull() ?: 0L
            } else {
                0L
            }
        }.getOrDefault(0L)

        return formatFileSize(bytes)
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 KB"

    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)

    return DecimalFormat("#,##0.#").format(
        bytes / 1024.0.pow(digitGroups.toDouble())
    ) + " " + units[digitGroups]
}