package com.github.postyizhan.gui.condition

import com.github.postyizhan.PostWarps

/**
 * 条件检查器工厂 - 负责创建和管理条件检查器
 * 提供扩展点，允许插件动态注册自定义条件检查器
 */
class ConditionCheckerFactory(private val plugin: PostWarps) {
    
    // 已注册的检查器类型
    private val registeredCheckerTypes = mutableSetOf<Class<out ConditionChecker>>()
    
    // 自定义检查器工厂函数
    private val customFactories = mutableMapOf<String, () -> ConditionChecker>()
    
    /**
     * 创建所有默认的条件检查器
     * @return 条件检查器列表
     */
    fun createDefaultCheckers(): List<ConditionChecker> {
        val checkers = mutableListOf<ConditionChecker>()
        
        // 创建内置检查器
        checkers.add(createPermissionChecker())
        checkers.add(createOpChecker())
        checkers.add(createDataChecker())
        checkers.add(createPlayerChecker())
        
        // 创建自定义检查器
        for (factory in customFactories.values) {
            try {
                checkers.add(factory())
            } catch (e: Exception) {
                plugin.logger.warning("创建自定义条件检查器失败: ${e.message}")
                if (plugin.isDebugEnabled()) {
                    e.printStackTrace()
                }
            }
        }
        
        logDebug("创建了 ${checkers.size} 个条件检查器")
        return checkers
    }
    
    /**
     * 创建权限检查器
     */
    private fun createPermissionChecker(): ConditionChecker {
        return PermissionConditionChecker().also {
            registeredCheckerTypes.add(it::class.java)
        }
    }
    
    /**
     * 创建OP检查器
     */
    private fun createOpChecker(): ConditionChecker {
        return OpConditionChecker().also {
            registeredCheckerTypes.add(it::class.java)
        }
    }
    
    /**
     * 创建数据检查器
     */
    private fun createDataChecker(): ConditionChecker {
        return DataConditionChecker().also {
            registeredCheckerTypes.add(it::class.java)
        }
    }
    
    /**
     * 创建玩家检查器
     */
    private fun createPlayerChecker(): ConditionChecker {
        return PlayerConditionChecker().also {
            registeredCheckerTypes.add(it::class.java)
        }
    }
    
    /**
     * 注册自定义条件检查器工厂
     * @param name 检查器名称
     * @param factory 工厂函数
     */
    fun registerCustomChecker(name: String, factory: () -> ConditionChecker) {
        customFactories[name] = factory
        logDebug("注册自定义条件检查器工厂: $name")
    }
    
    /**
     * 移除自定义条件检查器工厂
     * @param name 检查器名称
     */
    fun unregisterCustomChecker(name: String) {
        val removed = customFactories.remove(name)
        if (removed != null) {
            logDebug("移除自定义条件检查器工厂: $name")
        }
    }
    
    /**
     * 检查指定类型的检查器是否已注册
     * @param checkerClass 检查器类
     * @return 如果已注册则返回true
     */
    fun isCheckerRegistered(checkerClass: Class<out ConditionChecker>): Boolean {
        return registeredCheckerTypes.contains(checkerClass)
    }
    
    /**
     * 获取所有已注册的检查器类型
     * @return 检查器类型集合
     */
    fun getRegisteredCheckerTypes(): Set<Class<out ConditionChecker>> {
        return registeredCheckerTypes.toSet()
    }
    
    /**
     * 获取所有自定义检查器名称
     * @return 自定义检查器名称列表
     */
    fun getCustomCheckerNames(): List<String> {
        return customFactories.keys.toList()
    }
    
    /**
     * 创建指定类型的检查器实例
     * @param checkerClass 检查器类
     * @return 检查器实例，如果创建失败则返回null
     */
    fun createChecker(checkerClass: Class<out ConditionChecker>): ConditionChecker? {
        return try {
            when (checkerClass) {
                PermissionConditionChecker::class.java -> PermissionConditionChecker()
                OpConditionChecker::class.java -> OpConditionChecker()
                DataConditionChecker::class.java -> DataConditionChecker()
                PlayerConditionChecker::class.java -> PlayerConditionChecker()
                else -> {
                    // 尝试使用默认构造函数创建
                    checkerClass.getDeclaredConstructor().newInstance()
                }
            }
        } catch (e: Exception) {
            plugin.logger.warning("创建条件检查器失败: ${checkerClass.simpleName} - ${e.message}")
            if (plugin.isDebugEnabled()) {
                e.printStackTrace()
            }
            null
        }
    }
    
    /**
     * 验证检查器是否有效
     * @param checker 检查器实例
     * @return 如果有效则返回true
     */
    fun validateChecker(checker: ConditionChecker): Boolean {
        return try {
            // 检查是否有支持的前缀
            val prefixes = checker.getSupportedPrefixes()
            if (prefixes.isEmpty()) {
                logDebug("检查器 ${checker::class.simpleName} 没有支持的前缀")
                return false
            }
            
            // 检查前缀是否有效
            for (prefix in prefixes) {
                if (prefix.isBlank()) {
                    logDebug("检查器 ${checker::class.simpleName} 包含空白前缀")
                    return false
                }
            }
            
            true
        } catch (e: Exception) {
            plugin.logger.warning("验证条件检查器失败: ${checker::class.simpleName} - ${e.message}")
            false
        }
    }
    
    /**
     * 获取工厂统计信息
     * @return 工厂统计
     */
    fun getFactoryStats(): FactoryStats {
        return FactoryStats(
            registeredTypes = registeredCheckerTypes.size,
            customFactories = customFactories.size,
            totalCheckers = registeredCheckerTypes.size + customFactories.size
        )
    }
    
    /**
     * 清理所有注册信息
     */
    fun cleanup() {
        registeredCheckerTypes.clear()
        customFactories.clear()
        logDebug("清理条件检查器工厂")
    }
    
    /**
     * 记录调试信息
     */
    private fun logDebug(message: String) {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] ConditionCheckerFactory: $message")
        }
    }
    
    /**
     * 工厂统计数据类
     */
    data class FactoryStats(
        val registeredTypes: Int,
        val customFactories: Int,
        val totalCheckers: Int
    )
}
