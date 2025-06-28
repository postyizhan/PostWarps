package com.github.postyizhan.database

import com.github.postyizhan.PostWarps
import com.github.postyizhan.database.impl.MySQLStorage
import com.github.postyizhan.database.impl.SQLiteStorage
import com.github.postyizhan.model.Warp
import com.github.postyizhan.util.MessageUtil
import org.bukkit.configuration.file.FileConfiguration
import java.util.*

/**
 * 数据库管理器，负责管理数据库连接和操作
 */
class DatabaseManager(private val plugin: PostWarps) {
    
    private lateinit var storage: IStorage
    private val config: FileConfiguration
        get() = plugin.getConfigManager().getConfig()
    
    /**
     * 初始化数据库
     */
    fun init() {
        val type = config.getString("database.type", "SQLite") ?: "SQLite"
        
        storage = when (type.lowercase()) {
            "mysql" -> MySQLStorage(plugin)
            else -> SQLiteStorage(plugin)
        }
        
        try {
            storage.init()
            plugin.server.consoleSender.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("messages.database_connected")
                    .replace("{type}", type)
            ))
        } catch (e: Exception) {
            plugin.logger.severe("Failed to initialize database: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 关闭数据库连接
     */
    fun close() {
        if (this::storage.isInitialized) {
            storage.close()
            plugin.server.consoleSender.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("messages.database_closed")
            ))
        }
    }
    
    /**
     * 创建地标
     */
    fun createWarp(warp: Warp): Boolean {
        return storage.createWarp(warp)
    }
    
    /**
     * 删除地标
     */
    fun deleteWarp(id: Int): Boolean {
        return storage.deleteWarp(id)
    }
    
    /**
     * 根据名称和所有者删除地标
     */
    fun deleteWarp(name: String, owner: UUID): Boolean {
        return storage.deleteWarp(name, owner)
    }
    
    /**
     * 获取地标
     */
    fun getWarp(id: Int): Warp? {
        return storage.getWarp(id)
    }
    
    /**
     * 根据名称和所有者获取地标
     */
    fun getWarp(name: String, owner: UUID): Warp? {
        return storage.getWarp(name, owner)
    }
    
    /**
     * 根据名称获取公开地标
     */
    fun getPublicWarp(name: String): Warp? {
        return storage.getPublicWarp(name)
    }
    
    /**
     * 获取所有地标
     */
    fun getAllWarps(): List<Warp> {
        return storage.getAllWarps()
    }

    /**
     * 获取所有公开地标
     */
    fun getAllPublicWarps(): List<Warp> {
        return storage.getAllPublicWarps()
    }
    
    /**
     * 获取指定玩家的所有地标
     */
    fun getPlayerWarps(owner: UUID): List<Warp> {
        return storage.getPlayerWarps(owner)
    }
    
    /**
     * 获取指定玩家的公开地标
     */
    fun getPlayerPublicWarps(owner: UUID): List<Warp> {
        return storage.getPlayerPublicWarps(owner)
    }
    
    /**
     * 获取指定玩家的私有地标
     */
    fun getPlayerPrivateWarps(owner: UUID): List<Warp> {
        return storage.getPlayerPrivateWarps(owner)
    }
    
    /**
     * 设置地标公开状态
     */
    fun setWarpPublic(id: Int, isPublic: Boolean): Boolean {
        return storage.setWarpPublic(id, isPublic)
    }
    
    /**
     * 根据名称和所有者设置地标公开状态
     */
    fun setWarpPublic(name: String, owner: UUID, isPublic: Boolean): Boolean {
        return storage.setWarpPublic(name, owner, isPublic)
    }
    
    /**
     * 更新地标描述
     */
    fun updateWarpDescription(id: Int, description: String): Boolean {
        return storage.updateWarpDescription(id, description)
    }
    
    /**
     * 根据名称和所有者更新地标描述
     */
    fun updateWarpDescription(name: String, owner: UUID, description: String): Boolean {
        return storage.updateWarpDescription(name, owner, description)
    }
    
    /**
     * 更新地标位置
     */
    fun updateWarpLocation(id: Int, worldName: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float): Boolean {
        return storage.updateWarpLocation(id, worldName, x, y, z, yaw, pitch)
    }

    /**
     * 更新地标显示材质
     */
    fun updateWarpMaterial(id: Int, material: String): Boolean {
        return storage.updateWarpMaterial(id, material)
    }

    /**
     * 根据名称和所有者更新地标显示材质
     */
    fun updateWarpMaterial(name: String, owner: UUID, material: String): Boolean {
        return storage.updateWarpMaterial(name, owner, material)
    }
}
