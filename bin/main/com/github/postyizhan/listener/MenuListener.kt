package com.github.postyizhan.listener

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.action.ActionFactory
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.ClickType
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerQuitEvent

/**
 * 菜单监听器，处理菜单点击和关闭事件
 */
class MenuListener(private val plugin: PostWarps) : Listener {
    
    // 动作工厂
    private val actionFactory = ActionFactory(plugin)
    
    /**
     * 处理菜单点击事件
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onInventoryClick(event: InventoryClickEvent) {
        val player = event.whoClicked as? Player ?: return
        
        // 检查是否是菜单
        val openMenu = plugin.getMenuManager().getOpenMenu(player) ?: return
        
        // 始终取消事件，防止物品被移动
        event.isCancelled = true
        
        // 获取点击的槽位
        val slot = event.rawSlot
        if (slot < 0 || slot >= event.inventory.size) {
            return
        }
        
        // 获取菜单
        val menu = plugin.getMenuManager().getMenu(openMenu) ?: return
        
        // 获取玩家数据
        val playerData = plugin.getMenuManager().getPlayerData(player)
        
        // 记录调试信息
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] Player ${player.name} clicked menu: $openMenu, slot: $slot, click type: ${event.click}")
        }
        
        // 处理点击
        val isShiftClick = event.click == ClickType.SHIFT_LEFT || event.click == ClickType.SHIFT_RIGHT
        val isLeftClick = event.click == ClickType.LEFT || event.click == ClickType.SHIFT_LEFT
        
        // 设置是否按住Shift键
        (playerData as? MutableMap<String, Any>)?.put("is_shift_click", isShiftClick)
        (playerData as? MutableMap<String, Any>)?.put("is_left_click", isLeftClick)
        
        // 处理点击，获取动作
        val actions = menu.handleClick(player, slot, playerData)
        
        // 执行动作
        for (action in actions) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("[DEBUG] Executing action: $action for player ${player.name}")
            }
            actionFactory.executeAction(player, action)
        }
    }
    
    /**
     * 处理菜单关闭事件
     */
    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val player = event.player as? Player ?: return
        
        // 检查是否是菜单
        val openMenu = plugin.getMenuManager().getOpenMenu(player) ?: return
        
        // 移除菜单
        plugin.getMenuManager().closeMenu(player)
    }
    
    /**
     * 处理玩家退出事件
     */
    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        plugin.getMenuManager().handleQuit(event.player)
    }
}
