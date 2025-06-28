package com.github.postyizhan.gui.tracker

import com.github.postyizhan.PostWarps
import com.github.postyizhan.constants.ConfigurableConstants
import org.bukkit.entity.Player
import java.util.concurrent.ConcurrentHashMap

/**
 * 玩家菜单跟踪器 - 负责跟踪玩家当前打开的菜单和相关数据
 * 职责：管理玩家菜单状态、数据存储和清理
 */
class PlayerMenuTracker(private val plugin: PostWarps) {
    
    // 玩家当前打开的菜单
    private val playerCurrentMenus = ConcurrentHashMap<Player, String>()
    
    // 玩家菜单数据存储
    private val playerMenuData = ConcurrentHashMap<Player, MutableMap<String, Any>>()
    
    // 玩家菜单历史记录（用于返回上一级菜单）
    private val playerMenuHistory = ConcurrentHashMap<Player, MutableList<String>>()
    
    /**
     * 设置玩家当前打开的菜单
     * @param player 玩家
     * @param menuName 菜单名称
     */
    fun setCurrentMenu(player: Player, menuName: String) {
        val previousMenu = playerCurrentMenus.put(player, menuName)
        
        // 记录菜单历史
        if (previousMenu != null && previousMenu != menuName) {
            addToHistory(player, previousMenu)
        }
        
        logDebug("玩家 ${player.name} 打开菜单: $menuName")
    }
    
    /**
     * 获取玩家当前打开的菜单
     * @param player 玩家
     * @return 菜单名称，如果没有打开菜单则返回null
     */
    fun getCurrentMenu(player: Player): String? {
        return playerCurrentMenus[player]
    }
    
    /**
     * 检查玩家是否打开了指定菜单
     * @param player 玩家
     * @param menuName 菜单名称
     * @return 如果玩家打开了指定菜单则返回true
     */
    fun isMenuOpen(player: Player, menuName: String): Boolean {
        return playerCurrentMenus[player] == menuName
    }
    
    /**
     * 检查玩家是否打开了任何菜单
     * @param player 玩家
     * @return 如果玩家打开了菜单则返回true
     */
    fun hasOpenMenu(player: Player): Boolean {
        return playerCurrentMenus.containsKey(player)
    }
    
    /**
     * 关闭玩家的菜单
     * @param player 玩家
     * @return 被关闭的菜单名称，如果没有打开菜单则返回null
     */
    fun closeMenu(player: Player): String? {
        val closedMenu = playerCurrentMenus.remove(player)
        if (closedMenu != null) {
            logDebug("玩家 ${player.name} 关闭菜单: $closedMenu")
        }
        return closedMenu
    }
    
    /**
     * 获取玩家的菜单数据
     * @param player 玩家
     * @return 玩家的菜单数据映射
     */
    fun getPlayerData(player: Player): MutableMap<String, Any> {
        return playerMenuData.computeIfAbsent(player) { mutableMapOf() }
    }
    
    /**
     * 设置玩家的菜单数据
     * @param player 玩家
     * @param key 数据键
     * @param value 数据值
     */
    fun setPlayerData(player: Player, key: String, value: Any) {
        val data = getPlayerData(player)
        data[key] = value
        logDebug("设置玩家 ${player.name} 的数据: $key = $value")
    }
    
    /**
     * 获取玩家的特定数据
     * @param player 玩家
     * @param key 数据键
     * @return 数据值，如果不存在则返回null
     */
    fun getPlayerData(player: Player, key: String): Any? {
        return getPlayerData(player)[key]
    }
    
    /**
     * 移除玩家的特定数据
     * @param player 玩家
     * @param key 数据键
     * @return 被移除的数据值，如果不存在则返回null
     */
    fun removePlayerData(player: Player, key: String): Any? {
        val data = getPlayerData(player)
        val removed = data.remove(key)
        if (removed != null) {
            logDebug("移除玩家 ${player.name} 的数据: $key")
        }
        return removed
    }
    
    /**
     * 批量设置玩家数据
     * @param player 玩家
     * @param data 要设置的数据映射
     */
    fun setPlayerData(player: Player, data: Map<String, Any>) {
        val playerData = getPlayerData(player)
        playerData.putAll(data)
        logDebug("批量设置玩家 ${player.name} 的数据: ${data.keys}")
    }
    
    /**
     * 添加菜单到历史记录
     * @param player 玩家
     * @param menuName 菜单名称
     */
    private fun addToHistory(player: Player, menuName: String) {
        if (!ConfigurableConstants.PlayerMenu.isHistoryEnabled(plugin)) {
            return
        }

        val history = playerMenuHistory.computeIfAbsent(player) { mutableListOf() }

        // 避免重复添加相同菜单
        if (history.isEmpty() || history.last() != menuName) {
            history.add(menuName)

            // 限制历史记录长度
            val maxHistorySize = ConfigurableConstants.PlayerMenu.getMaxHistorySize(plugin)
            if (history.size > maxHistorySize) {
                history.removeAt(0)
            }
        }
    }
    
    /**
     * 获取玩家的上一个菜单
     * @param player 玩家
     * @return 上一个菜单名称，如果没有历史记录则返回null
     */
    fun getPreviousMenu(player: Player): String? {
        val history = playerMenuHistory[player]
        return if (history != null && history.isNotEmpty()) {
            history.removeAt(history.size - 1)
        } else {
            null
        }
    }
    
    /**
     * 清除玩家的菜单历史
     * @param player 玩家
     */
    fun clearHistory(player: Player) {
        playerMenuHistory.remove(player)
        logDebug("清除玩家 ${player.name} 的菜单历史")
    }
    
    /**
     * 获取玩家的菜单历史
     * @param player 玩家
     * @return 菜单历史列表的副本
     */
    fun getHistory(player: Player): List<String> {
        return playerMenuHistory[player]?.toList() ?: emptyList()
    }
    
    /**
     * 处理玩家退出事件
     * @param player 玩家
     */
    fun handlePlayerQuit(player: Player) {
        val removedMenu = playerCurrentMenus.remove(player)
        val removedData = playerMenuData.remove(player)
        val removedHistory = playerMenuHistory.remove(player)
        
        logDebug("玩家 ${player.name} 退出，清理数据: 菜单=$removedMenu, 数据=${removedData?.size ?: 0}项, 历史=${removedHistory?.size ?: 0}项")
    }
    
    /**
     * 关闭所有玩家的菜单
     */
    fun closeAllMenus() {
        val players = playerCurrentMenus.keys.toList()
        for (player in players) {
            if (player.isOnline) {
                player.closeInventory()
            }
        }
        
        val menuCount = playerCurrentMenus.size
        val dataCount = playerMenuData.size
        val historyCount = playerMenuHistory.size
        
        playerCurrentMenus.clear()
        playerMenuData.clear()
        playerMenuHistory.clear()
        
        logDebug("关闭所有菜单: $menuCount 个菜单, $dataCount 个数据, $historyCount 个历史")
    }
    
    /**
     * 获取当前在线玩家的菜单统计
     * @return 菜单统计信息
     */
    fun getMenuStats(): MenuStats {
        val onlinePlayersWithMenus = playerCurrentMenus.keys.count { it.isOnline }
        val totalDataEntries = playerMenuData.values.sumOf { it.size }
        val totalHistoryEntries = playerMenuHistory.values.sumOf { it.size }
        
        return MenuStats(
            playersWithOpenMenus = onlinePlayersWithMenus,
            totalDataEntries = totalDataEntries,
            totalHistoryEntries = totalHistoryEntries,
            menuDistribution = getMenuDistribution()
        )
    }
    
    /**
     * 获取菜单分布统计
     * @return 菜单名称到玩家数量的映射
     */
    private fun getMenuDistribution(): Map<String, Int> {
        return playerCurrentMenus.values
            .groupingBy { it }
            .eachCount()
    }
    
    /**
     * 记录调试信息
     * @param message 调试消息
     */
    private fun logDebug(message: String) {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] PlayerMenuTracker: $message")
        }
    }
    

    
    /**
     * 菜单统计数据类
     */
    data class MenuStats(
        val playersWithOpenMenus: Int,
        val totalDataEntries: Int,
        val totalHistoryEntries: Int,
        val menuDistribution: Map<String, Int>
    )
}
