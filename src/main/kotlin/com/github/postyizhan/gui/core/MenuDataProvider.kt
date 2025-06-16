package com.github.postyizhan.gui.core

import org.bukkit.entity.Player
import com.github.postyizhan.model.Warp

/**
 * 菜单数据提供器接口
 * 负责为菜单提供所需的数据
 */
interface MenuDataProvider {
    
    /**
     * 获取菜单数据
     * @param player 玩家
     * @param menuName 菜单名称
     * @param context 菜单上下文
     * @return 菜单数据
     */
    suspend fun getData(player: Player, menuName: String, context: MenuContext): MenuData
    
    /**
     * 检查是否支持指定菜单的数据提供
     * @param menuName 菜单名称
     * @return 是否支持
     */
    fun supports(menuName: String): Boolean
}

/**
 * 菜单数据封装类
 */
data class MenuData(
    val staticData: Map<String, Any> = emptyMap(),
    val dynamicData: Map<String, Any> = emptyMap(),
    val warps: List<Warp> = emptyList(),
    val totalPages: Int = 1,
    val currentPage: Int = 0
)
