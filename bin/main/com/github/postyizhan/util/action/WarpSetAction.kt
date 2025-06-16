package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.entity.Player

/**
 * 设置地标数据动作处理器
 */
class WarpSetAction(plugin: PostWarps) : AbstractAction(plugin) {
    override fun execute(player: Player, actionValue: String) {
        val key = extractActionValue(actionValue, ActionType.WARP_SET.prefix)
        logDebug("Player ${player.name} setting warp data for key: $key")
        
        val currentMenu = plugin.getMenuManager().getOpenMenu(player) ?: return
        
        // 关闭当前菜单
        player.closeInventory()
        
        // 创建铁砧GUI
        AnvilGUI.Builder()
            .onClose { stateSnapshot ->
                // 重新打开之前的菜单
                plugin.server.scheduler.runTaskLater(plugin, Runnable {
                    plugin.getMenuManager().openMenu(stateSnapshot.player, currentMenu)
                }, 1L)
            }
            .onClick { slot, stateSnapshot ->
                if (slot == AnvilGUI.Slot.OUTPUT) {
                    // 设置数据
                    plugin.getMenuManager().setPlayerData(stateSnapshot.player, key, stateSnapshot.text)
                    
                    // 重新打开之前的菜单
                    plugin.server.scheduler.runTaskLater(plugin, Runnable {
                        plugin.getMenuManager().openMenu(stateSnapshot.player, currentMenu)
                    }, 1L)
                    
                    listOf(AnvilGUI.ResponseAction.close())
                } else {
                    emptyList()
                }
            }
            .text("输入${getDisplayName(key)}")
            .title("设置${getDisplayName(key)}")
            .plugin(plugin)
            .open(player)
    }
    
    /**
     * 获取字段显示名
     */
    private fun getDisplayName(key: String): String {
        return when (key) {
            "name" -> "地标名称"
            "desc" -> "地标描述"
            else -> key
        }
    }
}
