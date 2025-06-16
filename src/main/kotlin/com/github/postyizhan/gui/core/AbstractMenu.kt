package com.github.postyizhan.gui.core

import com.github.postyizhan.PostWarps
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory

/**
 * 抽象菜单基类
 * 提供菜单的基础功能和模板方法
 */
abstract class AbstractMenu(
    protected val plugin: PostWarps,
    val name: String,
    protected val config: YamlConfiguration
) {
    
    /**
     * 创建菜单库存
     * 模板方法，定义菜单创建的标准流程
     */
    fun createInventory(player: Player, playerData: MutableMap<String, Any>): Inventory? {
        try {
            // 1. 创建菜单上下文
            val context = createContext(player, playerData)
            
            // 2. 获取数据提供器
            val dataProvider = getDataProvider()
            
            // 3. 获取菜单数据（如果需要）
            val menuData = if (dataProvider != null) {
                // 暂时使用同步方式获取数据，后续可以优化为异步
                try {
                    kotlinx.coroutines.runBlocking {
                        dataProvider.getData(player, name, context)
                    }
                } catch (e: Exception) {
                    plugin.logger.warning("获取菜单数据时发生错误: ${e.message}")
                    null
                }
            } else null
            
            // 4. 更新上下文
            val updatedContext = context.copy(menuData = menuData)
            
            // 5. 获取渲染器并渲染
            val renderer = getRenderer()
            return renderer.render(updatedContext)
            
        } catch (e: Exception) {
            plugin.logger.warning("创建菜单 $name 时发生错误: ${e.message}")
            if (plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
            return null
        }
    }
    
    /**
     * 处理点击事件
     */
    abstract fun handleClick(player: Player, slot: Int, playerData: MutableMap<String, Any>): List<String>
    
    /**
     * 创建菜单上下文
     */
    protected open fun createContext(player: Player, playerData: MutableMap<String, Any>): MenuContext {
        return MenuContext(plugin, player, name, config, playerData)
    }
    
    /**
     * 获取数据提供器
     * 子类可以重写此方法提供特定的数据提供器
     */
    protected open fun getDataProvider(): MenuDataProvider? = null
    
    /**
     * 获取渲染器
     * 子类可以重写此方法提供特定的渲染器
     */
    protected abstract fun getRenderer(): MenuRenderer
}
