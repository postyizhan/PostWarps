package com.github.postyizhan.gui.core

import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

/**
 * 菜单渲染器接口
 * 负责将菜单配置和数据渲染为实际的GUI界面
 */
interface MenuRenderer {
    
    /**
     * 渲染菜单为Bukkit Inventory
     * @param context 菜单上下文
     * @return 渲染后的Inventory，如果渲染失败返回null
     */
    fun render(context: MenuContext): Inventory?
    
    /**
     * 检查是否支持渲染指定类型的菜单
     * @param menuType 菜单类型
     * @return 是否支持
     */
    fun supports(menuType: String): Boolean
}
