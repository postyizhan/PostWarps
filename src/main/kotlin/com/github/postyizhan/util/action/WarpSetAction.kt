package com.github.postyizhan.util.action

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import net.wesjd.anvilgui.AnvilGUI
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

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
        
        // 创建输入物品
        val inputItem = createInputItem()

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
                    // 获取输入文本并去掉最前面的空格
                    val inputText = stateSnapshot.text.removePrefix(" ")

                    // 设置数据
                    plugin.getMenuManager().setPlayerData(stateSnapshot.player, key, inputText)
                    
                    // 如果是描述，则保存到数据库
                    if (key == "desc") {
                        // 获取当前选中的地标ID
                        val playerData = plugin.getMenuManager().getPlayerData(stateSnapshot.player)
                        val warpId = playerData["warp_id"] as? Int
                        
                        if (warpId != null) {
                            // 从数据库中获取地标
                            val warp = plugin.getDatabaseManager().getWarp(warpId)
                            
                            if (warp != null) {
                                // 检查是否是自己的地标
                                if (warp.owner == stateSnapshot.player.uniqueId || stateSnapshot.player.hasPermission("postwarps.admin")) {
                                    // 更新地标描述
                                    val success = plugin.getDatabaseManager().updateWarpDescription(warpId, stateSnapshot.text)
                                    
                                    if (success) {
                                        // 重新从数据库获取更新后的地标
                                        val updatedWarp = plugin.getDatabaseManager().getWarp(warpId)
                                        if (updatedWarp != null) {
                                            // 更新playerData中的描述值
                                            (playerData as? MutableMap<String, Any>)?.put("desc", updatedWarp.description)
                                        }
                                        logDebug("Updated warp ID: $warpId description to: ${stateSnapshot.text}")
                                    } else {
                                        logDebug("Failed to update warp ID: $warpId description")
                                    }
                                } else {
                                    logDebug("Player ${stateSnapshot.player.name} does not have permission to edit warp ID: $warpId")
                                }
                            } else {
                                logDebug("Warp with ID $warpId not found")
                            }
                        } else {
                            logDebug("No warp_id found in player data, only setting in memory")
                        }
                    }
                    
                    // 重新打开之前的菜单
                    plugin.server.scheduler.runTaskLater(plugin, Runnable {
                        plugin.getMenuManager().openMenu(stateSnapshot.player, currentMenu)
                    }, 1L)
                    
                    listOf(AnvilGUI.ResponseAction.close())
                } else {
                    emptyList()
                }
            }
            .itemLeft(inputItem)
            .title(getTitlePrompt(player, key))
            .plugin(plugin)
            .open(player)
    }
    
    /**
     * 创建输入物品
     */
    private fun createInputItem(): ItemStack {
        val item = ItemStack(Material.PAPER)
        val meta = item.itemMeta

        // 设置物品显示名称为一个空格
        meta?.setDisplayName(" ")

        item.itemMeta = meta
        return item
    }

    /**
     * 获取标题提示文本（国际化）
     */
    private fun getTitlePrompt(player: Player, key: String): String {
        return MessageUtil.getMessage("anvil.title.$key", player)
    }
}
