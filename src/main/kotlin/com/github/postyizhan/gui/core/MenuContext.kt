package com.github.postyizhan.gui.core

import org.bukkit.entity.Player
import org.bukkit.configuration.file.YamlConfiguration
import com.github.postyizhan.PostWarps
import com.github.postyizhan.gui.util.MenuI18nProcessor

/**
 * 菜单上下文
 * 包含菜单渲染所需的所有信息
 */
data class MenuContext(
    val plugin: PostWarps,
    val player: Player,
    val menuName: String,
    val config: YamlConfiguration,
    val playerData: MutableMap<String, Any>,
    val menuData: MenuData? = null
) {

    // 国际化处理器
    private val i18nProcessor = MenuI18nProcessor(plugin)
    
    /**
     * 获取本地化的菜单标题
     */
    fun getTitle(): String {
        return i18nProcessor.getLocalizedTitle(config, player)
            ?: config.getString("title", "&8【 &b菜单 &8】")
            ?: "&8【 &b菜单 &8】"
    }
    
    /**
     * 获取菜单布局
     */
    fun getLayout(): List<String> {
        return config.getStringList("layout")
    }
    
    /**
     * 获取物品配置
     */
    fun getItemsConfig(): org.bukkit.configuration.ConfigurationSection? {
        return config.getConfigurationSection("items")
    }
    
    /**
     * 获取当前页码
     */
    fun getCurrentPage(): Int {
        return playerData["page"] as? Int ?: 0
    }
    
    /**
     * 设置当前页码
     */
    fun setCurrentPage(page: Int) {
        playerData["page"] = page
    }
    
    /**
     * 获取玩家数据中的值
     */
    fun <T> getPlayerData(key: String): T? {
        @Suppress("UNCHECKED_CAST")
        return playerData[key] as? T
    }
    
    /**
     * 设置玩家数据
     */
    fun setPlayerData(key: String, value: Any) {
        playerData[key] = value
    }
}
