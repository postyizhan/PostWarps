package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.entity.Player

/**
 * 清除地标搜索动作处理器
 * 清除当前的搜索过滤器
 */
class WarpSearchClearAction(plugin: PostWarps) : AbstractAction(plugin) {
    
    override fun execute(player: Player, actionValue: String) {
        logDebug("Player ${player.name} clearing warp search")
        
        val currentMenu = plugin.getMenuManager().getOpenMenu(player) ?: return
        
        // 清除搜索过滤器
        plugin.getMenuManager().setPlayerData(player, "search_filter", "")
        plugin.getMenuManager().setPlayerData(player, "search_display", "")
        plugin.getMenuManager().setPlayerData(player, "page", 0)

        // 清除菜单缓存以强制重新加载数据
        clearMenuCache(player, currentMenu)

        player.sendMessage(MessageUtil.color(
            MessageUtil.getMessage("search.cleared", player)
        ))
        
        // 重新打开菜单以显示清除搜索后的结果
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            plugin.getMenuManager().openMenu(player, currentMenu)
        }, 1L)
    }

    /**
     * 清除菜单缓存以强制重新加载数据
     */
    private fun clearMenuCache(player: Player, menuName: String) {
        try {
            // 通过反射访问MenuManager的cache字段
            val menuManager = plugin.getMenuManager()
            val cacheField = menuManager.javaClass.getDeclaredField("cache")
            cacheField.isAccessible = true
            val cache = cacheField.get(menuManager)

            // 调用clearPlayerCache方法
            val clearMethod = cache.javaClass.getMethod("clearPlayerCache", org.bukkit.entity.Player::class.java)
            clearMethod.invoke(cache, player)

            logDebug("Cleared menu cache for player ${player.name}")
        } catch (e: Exception) {
            logDebug("Failed to clear menu cache: ${e.message}")
            // 如果反射失败，尝试其他方式
            plugin.getMenuManager().setPlayerData(player, "force_refresh", System.currentTimeMillis())
        }
    }
}
