package com.github.postyizhan.database

import com.github.postyizhan.PostWarps
import com.github.postyizhan.database.impl.MySQLStorage
import com.github.postyizhan.database.impl.SQLiteStorage
import com.github.postyizhan.model.Warp
import com.github.postyizhan.util.MessageUtil
import org.bukkit.configuration.file.FileConfiguration
import java.util.*

class DatabaseManager(private val plugin: PostWarps) {
    
    private lateinit var storage: IStorage
    private val config: FileConfiguration
        get() = plugin.getConfigManager().getConfig()
    

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
    

    fun close() {
        if (this::storage.isInitialized) {
            storage.close()
            plugin.server.consoleSender.sendMessage(MessageUtil.color(
                MessageUtil.getMessage("messages.database_closed")
            ))
        }
    }
    

    fun createWarp(warp: Warp): Boolean {
        return storage.createWarp(warp)
    }
    

    fun deleteWarp(id: Int): Boolean {
        return storage.deleteWarp(id)
    }
    

    fun deleteWarp(name: String, owner: UUID): Boolean {
        return storage.deleteWarp(name, owner)
    }
    

    fun getWarp(id: Int): Warp? {
        return storage.getWarp(id)
    }
    

    fun getWarp(name: String, owner: UUID): Warp? {
        return storage.getWarp(name, owner)
    }
    

    fun getPublicWarp(name: String): Warp? {
        return storage.getPublicWarp(name)
    }
    

    fun getAllWarps(): List<Warp> {
        return storage.getAllWarps()
    }


    fun getAllPublicWarps(): List<Warp> {
        return storage.getAllPublicWarps()
    }
    

    fun getPlayerWarps(owner: UUID): List<Warp> {
        return storage.getPlayerWarps(owner)
    }
    

    fun getPlayerPublicWarps(owner: UUID): List<Warp> {
        return storage.getPlayerPublicWarps(owner)
    }
    

    fun getPlayerPrivateWarps(owner: UUID): List<Warp> {
        return storage.getPlayerPrivateWarps(owner)
    }
    

    fun setWarpPublic(id: Int, isPublic: Boolean): Boolean {
        return storage.setWarpPublic(id, isPublic)
    }
    

    fun setWarpPublic(name: String, owner: UUID, isPublic: Boolean): Boolean {
        return storage.setWarpPublic(name, owner, isPublic)
    }
    

    fun updateWarpDescription(id: Int, description: String): Boolean {
        return storage.updateWarpDescription(id, description)
    }
    

    fun updateWarpDescription(name: String, owner: UUID, description: String): Boolean {
        return storage.updateWarpDescription(name, owner, description)
    }
    

    fun updateWarpLocation(id: Int, worldName: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float): Boolean {
        return storage.updateWarpLocation(id, worldName, x, y, z, yaw, pitch)
    }


    fun updateWarpMaterial(id: Int, material: String): Boolean {
        return storage.updateWarpMaterial(id, material)
    }


    fun updateWarpMaterial(name: String, owner: UUID, material: String): Boolean {
        return storage.updateWarpMaterial(name, owner, material)
    }


    fun updateWarpMaterial(id: Int, material: String, skullOwner: String?, skullTexture: String?): Boolean {
        return storage.updateWarpMaterial(id, material, skullOwner, skullTexture)
    }


    fun updateWarpMaterial(name: String, owner: UUID, material: String, skullOwner: String?, skullTexture: String?): Boolean {
        return storage.updateWarpMaterial(name, owner, material, skullOwner, skullTexture)
    }
}
