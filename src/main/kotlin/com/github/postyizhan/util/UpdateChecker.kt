package com.github.postyizhan.util

import com.github.postyizhan.PostWarps
import com.github.postyizhan.constants.ConfigurableConstants
import com.github.postyizhan.constants.PluginConstants
import com.github.postyizhan.util.json.SimpleJsonParser
import org.bukkit.Bukkit
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

/**
 * 更新检查器 - 重构后的版本
 * 改进了资源管理、错误处理和JSON解析，使用常量管理配置
 */
class UpdateChecker(private val plugin: PostWarps, repository: String? = null) {

    private val currentVersion = plugin.description.version
    private var latestRelease: SimpleJsonParser.GitHubRelease? = null
    private val repository = repository ?: ConfigurableConstants.UpdateChecker.getRepository(plugin)
    private val apiUrl = "https://api.github.com/repos/${this.repository}/releases/latest"

    /**
     * 检查更新
     * @param callback 回调函数，参数为 (是否有更新, 最新版本)
     */
    fun checkForUpdates(callback: (Boolean, String) -> Unit) {
        // 在异步线程中执行
        Bukkit.getScheduler().runTaskAsynchronously(plugin, Runnable {
            val result = fetchLatestRelease()

            // 在主线程中执行回调
            Bukkit.getScheduler().runTask(plugin, Runnable {
                when (result) {
                    is UpdateCheckResult.Success -> {
                        latestRelease = result.release
                        val hasUpdate = compareVersions(currentVersion, result.release.cleanVersion) < 0
                        callback(hasUpdate, result.release.cleanVersion)
                        logDebug("更新检查成功: 当前版本=$currentVersion, 最新版本=${result.release.cleanVersion}, 有更新=$hasUpdate")
                    }
                    is UpdateCheckResult.Error -> {
                        logError("更新检查失败", result.exception)
                        callback(false, currentVersion)
                    }
                }
            })
        })
    }

    /**
     * 获取最新发布信息
     * @return 更新检查结果
     */
    private fun fetchLatestRelease(): UpdateCheckResult {
        return try {
            val jsonContent = downloadReleaseInfo()
            val release = SimpleJsonParser.parseGitHubRelease(jsonContent)

            if (release != null) {
                UpdateCheckResult.Success(release)
            } else {
                UpdateCheckResult.Error(IllegalStateException("无法解析GitHub API响应"))
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e)
        }
    }

    /**
     * 下载发布信息
     * @return JSON响应内容
     * @throws IOException 网络错误
     */
    private fun downloadReleaseInfo(): String {
        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection

        return try {
            // 配置连接
            connection.connectTimeout = ConfigurableConstants.Network.getConnectTimeout(plugin)
            connection.readTimeout = ConfigurableConstants.Network.getReadTimeout(plugin)
            connection.setRequestProperty("Accept", PluginConstants.Network.GITHUB_API_ACCEPT_HEADER)
            connection.setRequestProperty("User-Agent", ConfigurableConstants.Network.getUserAgent(plugin))
            connection.requestMethod = "GET"

            // 检查响应码
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw IOException("HTTP错误: $responseCode ${connection.responseMessage}")
            }

            // 使用use扩展函数自动管理资源
            connection.inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        } catch (e: SocketTimeoutException) {
            throw IOException("连接超时: ${e.message}", e)
        } catch (e: UnknownHostException) {
            throw IOException("无法连接到GitHub: ${e.message}", e)
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 比较版本号 - 改进的版本比较算法
     * @param v1 版本1
     * @param v2 版本2
     * @return 如果v1 < v2返回负数，v1 > v2返回正数，v1 = v2返回0
     */
    private fun compareVersions(v1: String, v2: String): Int {
        if (v1 == v2) return 0

        val parts1 = normalizeVersion(v1)
        val parts2 = normalizeVersion(v2)
        val maxLength = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLength) {
            val part1 = if (i < parts1.size) parts1[i] else 0
            val part2 = if (i < parts2.size) parts2[i] else 0

            when {
                part1 < part2 -> return -1
                part1 > part2 -> return 1
            }
        }

        return 0
    }

    /**
     * 标准化版本号
     * @param version 原始版本号
     * @return 标准化后的版本号部分列表
     */
    private fun normalizeVersion(version: String): List<Int> {
        return version
            .removePrefix("v")
            .split(".", "-", "_")
            .take(3) // 只取前3个部分 (major.minor.patch)
            .mapNotNull { part ->
                // 提取数字部分
                val numberPart = part.takeWhile { it.isDigit() }
                numberPart.toIntOrNull()
            }
    }

    /**
     * 获取最新发布信息
     * @return 最新发布信息，如果未检查或检查失败则返回null
     */
    fun getLatestRelease(): SimpleJsonParser.GitHubRelease? {
        return latestRelease
    }

    /**
     * 获取当前版本
     * @return 当前插件版本
     */
    fun getCurrentVersion(): String {
        return currentVersion
    }

    /**
     * 记录调试信息
     */
    private fun logDebug(message: String) {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] UpdateChecker: $message")
        }
    }

    /**
     * 记录错误信息
     */
    private fun logError(message: String, exception: Exception) {
        plugin.logger.warning("$message: ${exception.message}")
        if (plugin.isDebugEnabled()) {
            exception.printStackTrace()
        }
    }

    /**
     * 更新检查结果密封类
     */
    private sealed class UpdateCheckResult {
        data class Success(val release: SimpleJsonParser.GitHubRelease) : UpdateCheckResult()
        data class Error(val exception: Exception) : UpdateCheckResult()
    }
}
