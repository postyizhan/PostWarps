package com.github.postyizhan.gui.impl

import com.github.postyizhan.gui.core.MenuDataProvider
import com.github.postyizhan.gui.core.MenuContext
import com.github.postyizhan.gui.core.MenuData
import com.github.postyizhan.gui.util.MenuCache
import com.github.postyizhan.PostWarps
import org.bukkit.entity.Player
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 地标菜单数据提供器
 * 负责提供地标相关菜单的数据
 */
class WarpMenuDataProvider(
    private val plugin: PostWarps,
    private val cache: MenuCache
) : MenuDataProvider {
    
    override suspend fun getData(player: Player, menuName: String, context: MenuContext): MenuData {
        // 检查是否需要强制刷新
        val forceRefresh = context.playerData["force_refresh"] != null

        // 尝试从缓存获取（除非强制刷新）
        val cachedData = if (!forceRefresh) cache.getPlayerMenuData(player, menuName) else null
        if (cachedData != null) {
            return cachedData
        }

        // 清除强制刷新标记
        if (forceRefresh) {
            context.playerData.remove("force_refresh")
        }

        // 从数据库获取数据
        val data = when (menuName) {
            "private_warps" -> getPrivateWarpsData(player, context)
            "public_warps" -> getPublicWarpsData(player, context)
            else -> MenuData()
        }

        // 缓存数据（如果不是强制刷新的话）
        if (!forceRefresh) {
            cache.cachePlayerMenuData(player, menuName, data)
        }

        return data
    }
    
    override fun supports(menuName: String): Boolean {
        return menuName in listOf("private_warps", "public_warps")
    }
    
    /**
     * 获取私有地标数据
     */
    private suspend fun getPrivateWarpsData(player: Player, context: MenuContext): MenuData {
        return withContext(Dispatchers.IO) {
            try {
                val allWarps = plugin.getDatabaseManager().getPlayerWarps(player.uniqueId)

                // 应用搜索过滤器
                val searchFilter = context.playerData["search_filter"] as? String
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("DEBUG: Private warps search - Filter: '$searchFilter', Total warps: ${allWarps.size}")
                }

                val filteredWarps = if (searchFilter.isNullOrEmpty()) {
                    allWarps
                } else {
                    val filtered = allWarps.filter { warp ->
                        warp.name.contains(searchFilter, ignoreCase = true) ||
                        warp.description.contains(searchFilter, ignoreCase = true) ||
                        warp.worldName.contains(searchFilter, ignoreCase = true)
                    }
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("DEBUG: Private warps search - Filtered warps: ${filtered.size}")
                    }
                    filtered
                }

                val currentPage = context.getCurrentPage()
                val warpsPerPage = 21 // 3行 * 7列

                val totalPages = if (filteredWarps.isEmpty()) 1 else (filteredWarps.size - 1) / warpsPerPage + 1
                val validPage = currentPage.coerceIn(0, totalPages - 1)

                // 更新页码（如果需要）
                if (validPage != currentPage) {
                    context.setCurrentPage(validPage)
                }

                MenuData(
                    warps = filteredWarps,
                    totalPages = totalPages,
                    currentPage = validPage,
                    dynamicData = mapOf(
                        "warp_count" to filteredWarps.size,
                        "total_warp_count" to allWarps.size,
                        "has_warps" to filteredWarps.isNotEmpty(),
                        "is_searching" to !searchFilter.isNullOrEmpty(),
                        "search_keyword" to (context.playerData["search_display"] as? String ?: searchFilter ?: "")
                    )
                )
            } catch (e: Exception) {
                plugin.logger.warning("获取私有地标数据时发生错误: ${e.message}")
                MenuData()
            }
        }
    }
    
    /**
     * 获取公开地标数据
     */
    private suspend fun getPublicWarpsData(@Suppress("UNUSED_PARAMETER") player: Player, context: MenuContext): MenuData {
        return withContext(Dispatchers.IO) {
            try {
                val allWarps = plugin.getDatabaseManager().getAllPublicWarps()

                // 应用搜索过滤器
                val searchFilter = context.playerData["search_filter"] as? String
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("DEBUG: Public warps search - Filter: '$searchFilter', Total warps: ${allWarps.size}")
                }

                val filteredWarps = if (searchFilter.isNullOrEmpty()) {
                    allWarps
                } else {
                    val filtered = allWarps.filter { warp ->
                        warp.name.contains(searchFilter, ignoreCase = true) ||
                        warp.description.contains(searchFilter, ignoreCase = true) ||
                        warp.worldName.contains(searchFilter, ignoreCase = true) ||
                        warp.ownerName.contains(searchFilter, ignoreCase = true)
                    }
                    if (plugin.isDebugEnabled()) {
                        plugin.logger.info("DEBUG: Public warps search - Filtered warps: ${filtered.size}")
                    }
                    filtered
                }

                val currentPage = context.getCurrentPage()
                val warpsPerPage = 21 // 3行 * 7列

                val totalPages = if (filteredWarps.isEmpty()) 1 else (filteredWarps.size - 1) / warpsPerPage + 1
                val validPage = currentPage.coerceIn(0, totalPages - 1)

                // 更新页码（如果需要）
                if (validPage != currentPage) {
                    context.setCurrentPage(validPage)
                }

                MenuData(
                    warps = filteredWarps,
                    totalPages = totalPages,
                    currentPage = validPage,
                    dynamicData = mapOf(
                        "warp_count" to filteredWarps.size,
                        "total_warp_count" to allWarps.size,
                        "has_warps" to filteredWarps.isNotEmpty(),
                        "is_searching" to !searchFilter.isNullOrEmpty(),
                        "search_keyword" to (context.playerData["search_display"] as? String ?: searchFilter ?: "")
                    )
                )
            } catch (e: Exception) {
                plugin.logger.warning("获取公开地标数据时发生错误: ${e.message}")
                MenuData()
            }
        }
    }
}
