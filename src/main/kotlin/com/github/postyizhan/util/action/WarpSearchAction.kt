package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.entity.Player

/**
 * 地标搜索动作处理器
 * 使用铁砧GUI让玩家输入搜索关键词
 */
class WarpSearchAction(plugin: PostWarps) : AbstractAction(plugin) {
    
    override fun execute(player: Player, actionValue: String) {
        logDebug("Player ${player.name} opening warp search")

        val currentMenu = plugin.getMenuManager().getOpenMenu(player) ?: return

        // 检查是否是清除搜索的操作（通过actionValue参数传递）
        if (actionValue.contains("clear")) {
            // 清除搜索过滤器
            plugin.getMenuManager().setPlayerData(player, "search_filter", "")
            plugin.getMenuManager().setPlayerData(player, "page", 0)

            player.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("search.cleared")
            ))

            // 重新打开菜单
            plugin.server.scheduler.runTaskLater(plugin, Runnable {
                plugin.getMenuManager().openMenu(player, currentMenu)
            }, 1L)
            return
        }

        // 关闭当前菜单
        player.closeInventory()

        // 获取当前搜索关键词作为默认文本
        val currentSearch = plugin.getMenuManager().getPlayerData(player)["search_filter"] as? String ?: ""

        // 创建铁砧GUI用于搜索
        AnvilGUI.Builder()
            .onClose { stateSnapshot ->
                // 重新打开之前的菜单
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    plugin.getMenuManager().openMenu(stateSnapshot.player, currentMenu)
                }, 1L)
            }
            .onClick { slot, stateSnapshot ->
                if (slot == AnvilGUI.Slot.OUTPUT) {
                    val searchText = stateSnapshot.text.trim()

                    if (searchText.isEmpty()) {
                        // 如果搜索内容为空，清除搜索过滤器
                        plugin.getMenuManager().setPlayerData(stateSnapshot.player, "search_filter", "")
                        plugin.getMenuManager().setPlayerData(stateSnapshot.player, "search_display", "")
                        logDebug("Player ${stateSnapshot.player.name} cleared search filter")
                    } else {
                        // 设置搜索过滤器
                        plugin.getMenuManager().setPlayerData(stateSnapshot.player, "search_filter", searchText)
                        plugin.getMenuManager().setPlayerData(stateSnapshot.player, "search_display", searchText)
                        logDebug("Player ${stateSnapshot.player.name} set search filter to: $searchText")
                    }

                    // 重置到第一页
                    plugin.getMenuManager().setPlayerData(stateSnapshot.player, "page", 0)

                    // 清除菜单缓存以强制重新加载数据
                    clearMenuCache(stateSnapshot.player, currentMenu)

                    // 发送搜索结果消息
                    if (searchText.isEmpty()) {
                        stateSnapshot.player.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("search.cleared", stateSnapshot.player)
                        ))
                    } else {
                        stateSnapshot.player.sendMessage(MessageUtil.color(
                            MessageUtil.getMessage("search.set", stateSnapshot.player)
                                .replace("{keyword}", searchText)
                        ))
                    }

                    // 重新打开菜单以显示搜索结果
                    plugin.server.scheduler.runTaskLater(plugin, Runnable {
                        plugin.getMenuManager().openMenu(stateSnapshot.player, currentMenu)
                    }, 1L)

                    listOf(AnvilGUI.ResponseAction.close())
                } else {
                    emptyList()
                }
            }
            .text(if (currentSearch.isEmpty()) "输入搜索关键词" else currentSearch)
            .title("搜索地标")
            .plugin(plugin)
            .open(player)
    }

    /**
     * 清除菜单缓存以强制重新加载数据
     */
    private fun clearMenuCache(player: Player, @Suppress("UNUSED_PARAMETER") menuName: String) {
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
            // 可以通过重新设置一个特殊标记来强制刷新
            plugin.getMenuManager().setPlayerData(player, "force_refresh", System.currentTimeMillis())
        }
    }
}
