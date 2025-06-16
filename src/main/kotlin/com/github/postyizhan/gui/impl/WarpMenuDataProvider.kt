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
        // 尝试从缓存获取
        val cachedData = cache.getPlayerMenuData(player, menuName)
        if (cachedData != null) {
            return cachedData
        }
        
        // 从数据库获取数据
        val data = when (menuName) {
            "private_warps" -> getPrivateWarpsData(player, context)
            "public_warps" -> getPublicWarpsData(player, context)
            else -> MenuData()
        }
        
        // 缓存数据
        cache.cachePlayerMenuData(player, menuName, data)
        
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
                val warps = plugin.getDatabaseManager().getPlayerWarps(player.uniqueId)
                val currentPage = context.getCurrentPage()
                val warpsPerPage = 21 // 3行 * 7列
                
                val totalPages = if (warps.isEmpty()) 1 else (warps.size - 1) / warpsPerPage + 1
                val validPage = currentPage.coerceIn(0, totalPages - 1)
                
                // 更新页码（如果需要）
                if (validPage != currentPage) {
                    context.setCurrentPage(validPage)
                }
                
                MenuData(
                    warps = warps,
                    totalPages = totalPages,
                    currentPage = validPage,
                    dynamicData = mapOf(
                        "warp_count" to warps.size,
                        "has_warps" to warps.isNotEmpty()
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
                val warps = plugin.getDatabaseManager().getAllPublicWarps()
                val currentPage = context.getCurrentPage()
                val warpsPerPage = 21 // 3行 * 7列
                
                val totalPages = if (warps.isEmpty()) 1 else (warps.size - 1) / warpsPerPage + 1
                val validPage = currentPage.coerceIn(0, totalPages - 1)
                
                // 更新页码（如果需要）
                if (validPage != currentPage) {
                    context.setCurrentPage(validPage)
                }
                
                MenuData(
                    warps = warps,
                    totalPages = totalPages,
                    currentPage = validPage,
                    dynamicData = mapOf(
                        "warp_count" to warps.size,
                        "has_warps" to warps.isNotEmpty()
                    )
                )
            } catch (e: Exception) {
                plugin.logger.warning("获取公开地标数据时发生错误: ${e.message}")
                MenuData()
            }
        }
    }
}
