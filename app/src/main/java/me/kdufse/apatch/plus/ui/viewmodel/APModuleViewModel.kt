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
        val banner: String = "", // 添加 banner 字段
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
                        
                        // 调试：检查 banner 字段是否存在
                        val hasBannerField = obj.has("banner")
                        val bannerFromJson = obj.optString("banner", "")
                        
                        Log.i(TAG, "Module $id: hasBannerField=$hasBannerField, banner='$bannerFromJson'")
                        
                        // 如果 JSON 中有 banner 字段，使用它
                        // 否则手动检查常见的 banner 文件
                        val banner = if (bannerFromJson.isNotEmpty()) {
                            bannerFromJson
                        } else {
                            // 手动检查 banner 文件
                            detectBannerFile(id)
                        }
                        
                        Log.i(TAG, "Final banner for $id: '$banner'")
                        
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
                            banner  // 使用检测到的 banner
                        )
                    }.toList()
                isNeedRefresh = false
                
                // 打印所有模块的 banner 信息
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
    
    // 手动检测 banner 文件
    private fun detectBannerFile(moduleId: String): String {
        return try {
            // 常见的 banner 文件名
            val bannerFiles = listOf("banner.jpg", "banner.png", "banner.webp", "banner.jpeg")
            
            for (bannerFile in bannerFiles) {
                // 尝试不同的路径
                val paths = listOf(
                    "/data/adb/modules/$moduleId/$bannerFile",
                    "/data/adb/ap/modules/$moduleId/$bannerFile",
                    "/data/adb/modules/$moduleId/files/$bannerFile"
                )
                
                for (path in paths) {
                    val file = SuFile(path)
                    if (file.exists() && file.canRead()) {
                        Log.i(TAG, "Found banner for $moduleId: $path")
                        return bannerFile  // 返回文件名
                    }
                }
            }
            
            // 检查 module.prop 文件中的 banner 字段
            val propPaths = listOf(
                "/data/adb/modules/$moduleId/module.prop",
                "/data/adb/ap/modules/$moduleId/module.prop"
            )
            
            for (propPath in propPaths) {
                val propFile = SuFile(propPath)
                if (propFile.exists()) {
                    val lines = propFile.readText().lines()
                    for (line in lines) {
                        if (line.startsWith("banner=")) {
                            val bannerValue = line.substringAfter("banner=").trim()
                            if (bannerValue.isNotEmpty()) {
                                Log.i(TAG, "Found banner in module.prop for $moduleId: $bannerValue")
                                return bannerValue
                            }
                        }
                    }
                }
            }
            
            "" // 没有找到 banner
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting banner for $moduleId: $e")
            ""
        }
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