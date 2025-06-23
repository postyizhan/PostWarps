package com.github.postyizhan.integration

import com.github.postyizhan.PostWarps

/**
 * PlaceholderAPI集成管理器
 * 管理PlaceholderAPI扩展的注册和注销
 */
class PlaceholderAPIManager(private val plugin: PostWarps) {
    
    private var expansion: PlaceholderAPIExpansion? = null
    private var isPlaceholderAPIEnabled = false
    
    /**
     * 初始化PlaceholderAPI集成
     */
    fun initialize(): Boolean {
        if (!plugin.server.pluginManager.isPluginEnabled("PlaceholderAPI")) {
            plugin.logger.info("PlaceholderAPI plugin not found, placeholder support will be disabled")
            return false
        }
        
        try {
            // 检查PlaceholderAPI类是否存在
            Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion")
            
            // 创建并注册扩展
            expansion = PlaceholderAPIExpansion(plugin)
            val registered = expansion!!.register()
            
            if (registered) {
                isPlaceholderAPIEnabled = true
                plugin.logger.info("PlaceholderAPI integration enabled successfully")
                plugin.logger.info("Available placeholders: %postwarps_<placeholder>%")
                return true
            } else {
                plugin.logger.warning("Failed to register PlaceholderAPI expansion")
            }
        } catch (e: ClassNotFoundException) {
            plugin.logger.warning("PlaceholderAPI classes not found, placeholder support disabled")
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("DEBUG: PlaceholderAPI class not found: ${e.message}")
            }
        } catch (e: Exception) {
            plugin.logger.warning("Failed to initialize PlaceholderAPI integration: ${e.message}")
            if (plugin.isDebugEnabled()) {
                plugin.logger.info("DEBUG: PlaceholderAPI integration error details: ${e.stackTraceToString()}")
            }
        }
        
        return false
    }
    
    /**
     * 检查PlaceholderAPI是否可用
     */
    fun isAvailable(): Boolean = isPlaceholderAPIEnabled && expansion != null
    
    /**
     * 获取所有可用的占位符列表
     */
    fun getAvailablePlaceholders(): List<String> {
        return listOf(
            // 地标数量相关
            "%postwarps_total_warps%",
            "%postwarps_public_warps%",
            "%postwarps_private_warps%",
            
            // 经济相关
            "%postwarps_vault_balance%",
            "%postwarps_points_balance%",
            
            // 权限组相关
            "%postwarps_group%",
            
            // 费用相关
            "%postwarps_create_cost_vault%",
            "%postwarps_create_cost_points%",
            "%postwarps_teleport_cost_vault%",
            "%postwarps_teleport_cost_points%",
            
            // 地标检查相关
            "%postwarps_has_warp_<name>%",
            "%postwarps_warp_public_<name>%",
            
            // 服务器统计相关
            "%postwarps_server_total_warps%",
            "%postwarps_server_public_warps%",
            
            // 经济系统状态
            "%postwarps_vault_enabled%",
            "%postwarps_points_enabled%",
            "%postwarps_player_vault_enabled%",
            "%postwarps_player_points_enabled%",
            
            // 最近地标
            "%postwarps_latest_warp%",
            "%postwarps_latest_public_warp%"
        )
    }
    
    /**
     * 获取占位符使用说明
     */
    fun getPlaceholderHelp(): List<String> {
        return listOf(
            "=== PostWarps PlaceholderAPI 占位符 ===",
            "",
            "地标数量:",
            "  %postwarps_total_warps% - 玩家总地标数量",
            "  %postwarps_public_warps% - 玩家公开地标数量", 
            "  %postwarps_private_warps% - 玩家私有地标数量",
            "",
            "经济信息:",
            "  %postwarps_vault_balance% - 玩家Vault余额",
            "  %postwarps_points_balance% - 玩家点券余额",
            "",
            "权限组信息:",
            "  %postwarps_group% - 玩家当前权限组",
            "",
            "费用信息:",
            "  %postwarps_create_cost_vault% - 创建地标Vault费用",
            "  %postwarps_create_cost_points% - 创建地标点券费用",
            "  %postwarps_teleport_cost_vault% - 传送Vault费用",
            "  %postwarps_teleport_cost_points% - 传送点券费用",
            "",
            "地标检查:",
            "  %postwarps_has_warp_<name>% - 是否拥有指定地标",
            "  %postwarps_warp_public_<name>% - 指定地标是否公开",
            "",
            "服务器统计:",
            "  %postwarps_server_total_warps% - 服务器总地标数量",
            "  %postwarps_server_public_warps% - 服务器公开地标数量",
            "",
            "系统状态:",
            "  %postwarps_vault_enabled% - Vault系统是否启用",
            "  %postwarps_points_enabled% - 点券系统是否启用",
            "  %postwarps_player_vault_enabled% - 玩家是否启用Vault",
            "  %postwarps_player_points_enabled% - 玩家是否启用点券",
            "",
            "最近地标:",
            "  %postwarps_latest_warp% - 最近创建的地标",
            "  %postwarps_latest_public_warp% - 最近创建的公开地标",
            "",
            "使用示例:",
            "  你有 %postwarps_total_warps% 个地标",
            "  你的余额: %postwarps_vault_balance%",
            "  创建费用: %postwarps_create_cost_vault%"
        )
    }
    
    /**
     * 关闭PlaceholderAPI集成
     */
    fun shutdown() {
        if (expansion != null) {
            try {
                expansion!!.unregister()
                plugin.logger.info("PlaceholderAPI expansion unregistered")
            } catch (e: Exception) {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.info("DEBUG: Error unregistering PlaceholderAPI expansion: ${e.message}")
                }
            }
        }
        
        expansion = null
        isPlaceholderAPIEnabled = false
        plugin.logger.info("PlaceholderAPI integration has been shutdown")
    }
}
