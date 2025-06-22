package com.github.postyizhan.integration

import com.github.postyizhan.PostWarps
import org.bukkit.entity.Player
import java.lang.reflect.Method
import java.util.*

/**
 * PlayerPoints集成管理器
 * 提供点券系统的集成支持
 * 使用反射调用，避免直接依赖PlayerPoints
 */
class PlayerPointsManager(private val plugin: PostWarps) {

    private var playerPointsPlugin: Any? = null
    private var apiInstance: Any? = null
    private var isPlayerPointsEnabled = false

    // 反射方法缓存
    private var lookMethod: Method? = null
    private var giveMethod: Method? = null
    private var takeMethod: Method? = null
    
    /**
     * 初始化PlayerPoints集成
     */
    fun initialize(): Boolean {
        if (!plugin.server.pluginManager.isPluginEnabled("PlayerPoints")) {
            plugin.logger.info("PlayerPoints plugin not found, points economy will be disabled")
            return false
        }

        try {
            playerPointsPlugin = plugin.server.pluginManager.getPlugin("PlayerPoints")
            if (playerPointsPlugin != null) {
                // 尝试获取API实例
                val getApiMethod = playerPointsPlugin!!.javaClass.getMethod("getAPI")
                apiInstance = getApiMethod.invoke(playerPointsPlugin)

                if (apiInstance != null) {
                    // 缓存常用方法
                    val apiClass = apiInstance!!.javaClass
                    lookMethod = apiClass.getMethod("look", UUID::class.java)
                    giveMethod = apiClass.getMethod("give", UUID::class.java, Int::class.java)
                    takeMethod = apiClass.getMethod("take", UUID::class.java, Int::class.java)

                    isPlayerPointsEnabled = true
                    plugin.logger.info("Connected to PlayerPoints economy system")
                    return true
                } else {
                    plugin.logger.warning("Failed to get PlayerPoints API instance")
                }
            } else {
                plugin.logger.warning("Failed to get PlayerPoints plugin instance")
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to initialize PlayerPoints integration: ${e.message}")
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("DEBUG: PlayerPoints integration error details: ${e.stackTraceToString()}")
            }
        }

        return false
    }
    
    /**
     * 检查PlayerPoints是否可用
     */
    fun isAvailable(): Boolean = isPlayerPointsEnabled && apiInstance != null

    /**
     * 获取玩家点券余额
     */
    fun getBalance(player: Player): Int {
        if (!isAvailable()) return 0

        return try {
            lookMethod?.invoke(apiInstance, player.uniqueId) as? Int ?: 0
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("DEBUG: Failed to get PlayerPoints balance for ${player.name}: ${e.message}")
            }
            0
        }
    }
    
    /**
     * 检查玩家是否有足够的点券
     */
    fun hasEnough(player: Player, amount: Int): Boolean {
        if (!isAvailable() || amount <= 0) return true
        return getBalance(player) >= amount
    }
    
    /**
     * 从玩家账户扣除点券
     */
    fun withdraw(player: Player, amount: Int): Boolean {
        if (!isAvailable() || amount <= 0) return true

        return try {
            val success = takeMethod?.invoke(apiInstance, player.uniqueId, amount) as? Boolean ?: false
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("DEBUG: PlayerPoints withdraw - Player: ${player.name}, Amount: $amount, Success: $success")
            }
            success
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("DEBUG: PlayerPoints withdraw failed - Player: ${player.name}, Amount: $amount, Error: ${e.message}")
            }
            false
        }
    }
    
    /**
     * 向玩家账户存入点券
     */
    fun deposit(player: Player, amount: Int): Boolean {
        if (!isAvailable() || amount <= 0) return true

        return try {
            val success = giveMethod?.invoke(apiInstance, player.uniqueId, amount) as? Boolean ?: false
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("DEBUG: PlayerPoints deposit - Player: ${player.name}, Amount: $amount, Success: $success")
            }
            success
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("DEBUG: PlayerPoints deposit failed - Player: ${player.name}, Amount: $amount, Error: ${e.message}")
            }
            false
        }
    }
    
    /**
     * 格式化点券数量显示
     */
    fun format(amount: Int): String {
        return amount.toString()
    }
    
    /**
     * 获取点券单位名称
     */
    fun getCurrencyName(): String {
        return "点券"
    }
    
    /**
     * 获取点券单位名称（复数形式，中文无区别）
     */
    fun getCurrencyNamePlural(): String {
        return "点券"
    }
    
    /**
     * 关闭PlayerPoints集成
     */
    fun shutdown() {
        playerPointsPlugin = null
        apiInstance = null
        lookMethod = null
        giveMethod = null
        takeMethod = null
        isPlayerPointsEnabled = false
        plugin.logger.info("PlayerPoints integration has been shutdown")
    }
}
