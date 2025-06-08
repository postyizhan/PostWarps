package com.github.postyizhan.database

import com.github.postyizhan.PostWarps
import com.github.postyizhan.database.storage.MySQLStorage
import com.github.postyizhan.database.storage.SQLiteStorage
import com.github.postyizhan.database.storage.Storage
import com.github.postyizhan.model.Warp
import java.util.*

class DatabaseManager(private val plugin: PostWarps) {
    private lateinit var storage: Storage
    
    fun init() {
        // 根据配置选择存储方式
        storage = if (plugin.useMySQL) {
            if (plugin.debugMode) {
                plugin.logger.info("Using MySQL storage")
            }
            MySQLStorage(plugin)
        } else {
            if (plugin.debugMode) {
                plugin.logger.info("Using SQLite storage")
            }
            SQLiteStorage(plugin)
        }
        
        // 初始化数据库
        storage.init()
    }
    
    fun close() {
        if (::storage.isInitialized) {
            storage.close()
        }
    }
    
    // 地标操作方法
    
    fun createWarp(warp: Warp): Boolean {
        return storage.createWarp(warp)
    }
    
    fun deleteWarp(name: String, ownerUUID: UUID): Boolean {
        return storage.deleteWarp(name, ownerUUID)
    }
    
    fun getWarp(name: String): Warp? {
        return storage.getWarp(name)
    }
    
    fun getWarpsByOwner(ownerUUID: UUID): List<Warp> {
        return storage.getWarpsByOwner(ownerUUID)
    }
    
    fun getAllWarps(): List<Warp> {
        return storage.getAllWarps()
    }
    
    fun incrementVisits(warpId: Int): Boolean {
        return storage.incrementVisits(warpId)
    }
    
    fun getWarpCount(ownerUUID: UUID): Int {
        return storage.getWarpCount(ownerUUID)
    }
    
    fun warpExists(name: String): Boolean {
        return storage.warpExists(name)
    }
}
