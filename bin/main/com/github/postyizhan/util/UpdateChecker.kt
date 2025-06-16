package com.github.postyizhan.util

import com.github.postyizhan.PostWarps
import org.bukkit.Bukkit
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import java.util.regex.Pattern

/**
 * 更新检查器，负责检查插件是否有更新
 */
class UpdateChecker(private val plugin: PostWarps, private val repository: String) {
    
    private val currentVersion = plugin.description.version
    private var latestVersion: String? = null
    private val apiUrl = "https://api.github.com/repos/$repository/releases/latest"
    
    /**
     * 检查更新
     * @param callback 回调函数，参数为 (是否有更新, 最新版本)
     */
    fun checkForUpdates(callback: (Boolean, String) -> Unit) {
        // 在异步线程中执行
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            try {
                // 获取最新版本信息
                val connection = URL(apiUrl).openConnection()
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
                connection.setRequestProperty("User-Agent", "PostWarps UpdateChecker")
                
                val reader = BufferedReader(InputStreamReader(connection.getInputStream()))
                val content = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    content.append(line)
                }
                reader.close()
                
                // 解析版本号
                val jsonContent = content.toString()
                val tagPattern = Pattern.compile("\"tag_name\"\\s*:\\s*\"(.*?)\"")
                val matcher = tagPattern.matcher(jsonContent)
                
                if (matcher.find()) {
                    latestVersion = matcher.group(1).replace("v", "")
                    
                    // 在主线程中执行回调
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        val hasUpdate = compareVersions(currentVersion, latestVersion!!) < 0
                        callback(hasUpdate, latestVersion!!)
                    })
                } else {
                    // 无法解析版本号
                    Bukkit.getScheduler().runTask(plugin, Runnable {
                        callback(false, currentVersion)
                    })
                }
            } catch (e: Exception) {
                // 发生异常
                plugin.logger.warning("An error occurred while checking for updates: ${e.message}")
                Bukkit.getScheduler().runTask(plugin, Runnable {
                    callback(false, currentVersion)
                })
            }
        })
    }
    
    /**
     * 比较两个版本号
     * @return 0: 相等, 1: v1 > v2, -1: v1 < v2
     */
    private fun compareVersions(v1: String, v2: String): Int {
        val v1Parts = v1.replace("SNAPSHOT", "").split(".")
        val v2Parts = v2.replace("SNAPSHOT", "").split(".")
        
        val length = maxOf(v1Parts.size, v2Parts.size)
        
        for (i in 0 until length) {
            val part1 = if (i < v1Parts.size) v1Parts[i].toIntOrNull() ?: 0 else 0
            val part2 = if (i < v2Parts.size) v2Parts[i].toIntOrNull() ?: 0 else 0
            
            if (part1 < part2) return -1
            if (part1 > part2) return 1
        }
        
        return 0
    }
}
