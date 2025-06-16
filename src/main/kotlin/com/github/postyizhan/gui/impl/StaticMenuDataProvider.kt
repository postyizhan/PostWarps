package com.github.postyizhan.gui.impl

import com.github.postyizhan.gui.core.MenuDataProvider
import com.github.postyizhan.gui.core.MenuContext
import com.github.postyizhan.gui.core.MenuData
import org.bukkit.entity.Player

/**
 * 静态菜单数据提供器
 * 为不需要动态数据的菜单提供基础数据
 */
class StaticMenuDataProvider : MenuDataProvider {
    
    override suspend fun getData(player: Player, menuName: String, context: MenuContext): MenuData {
        return when (menuName) {
            "main" -> getMainMenuData(player, context)
            "create" -> getCreateMenuData(player, context)
            "settings" -> getSettingsMenuData(player, context)
            else -> MenuData()
        }
    }
    
    override fun supports(menuName: String): Boolean {
        return menuName in listOf("main", "create", "settings")
    }
    
    /**
     * 获取主菜单数据
     */
    private fun getMainMenuData(@Suppress("UNUSED_PARAMETER") player: Player, context: MenuContext): MenuData {
        return MenuData(
            staticData = mapOf(
                "server_name" to "PostWarps",
                "version" to context.plugin.description.version
            )
        )
    }
    
    /**
     * 获取创建菜单数据
     */
    private fun getCreateMenuData(player: Player, @Suppress("UNUSED_PARAMETER") context: MenuContext): MenuData {
        return MenuData(
            staticData = mapOf(
                "location" to "${player.location.blockX}, ${player.location.blockY}, ${player.location.blockZ}",
                "world" to (player.world.name)
            )
        )
    }
    
    /**
     * 获取设置菜单数据
     */
    private fun getSettingsMenuData(player: Player, @Suppress("UNUSED_PARAMETER") context: MenuContext): MenuData {
        return MenuData(
            staticData = mapOf(
                "player_name" to player.name
            )
        )
    }
}
