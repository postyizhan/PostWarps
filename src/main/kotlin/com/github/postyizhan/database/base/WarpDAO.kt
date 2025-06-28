package com.github.postyizhan.database.base

import com.github.postyizhan.PostWarps
import com.github.postyizhan.constants.PluginConstants
import com.github.postyizhan.model.Warp
import java.sql.ResultSet
import java.util.*

/**
 * Warp数据访问对象基类 - 提供Warp相关的通用数据库操作
 * 统一Warp实体的CRUD操作和查询逻辑
 */
abstract class WarpDAO(plugin: PostWarps) : BaseDAO(plugin) {
    
    /**
     * 创建地标表的抽象方法
     * 子类需要实现具体的表创建逻辑（MySQL和SQLite的语法可能不同）
     */
    protected abstract fun createWarpTable(): Boolean
    
    /**
     * 创建地标
     * @param warp 地标对象
     * @return 如果创建成功则返回true
     */
    fun createWarp(warp: Warp): Boolean {
        val sql = """
            INSERT INTO warps (name, owner, owner_name, world_name, x, y, z, yaw, pitch, is_public, description, material, create_time)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        
        val params = listOf(
            warp.name,
            warp.owner.toString(),
            warp.ownerName,
            warp.worldName,
            warp.x,
            warp.y,
            warp.z,
            warp.yaw,
            warp.pitch,
            warp.isPublic,
            warp.description,
            warp.displayMaterial,
            warp.createTime
        )
        
        val result = executeUpdate(sql, params) > 0
        if (result) {
            logDebug("Warp created successfully: ${warp.name} (owner: ${warp.ownerName})")
        } else {
            logWarning("Failed to create warp: ${warp.name}")
        }
        return result
    }
    
    /**
     * 根据ID删除地标
     * @param id 地标ID
     * @return 如果删除成功则返回true
     */
    fun deleteWarp(id: Int): Boolean {
        val sql = "DELETE FROM warps WHERE id = ?"
        val result = executeUpdate(sql, listOf(id)) > 0
        if (result) {
            logDebug("Warp deleted successfully: ID=$id")
        } else {
            logWarning("Failed to delete warp: ID=$id")
        }
        return result
    }
    
    /**
     * 根据名称和所有者删除地标
     * @param name 地标名称
     * @param owner 所有者UUID
     * @return 如果删除成功则返回true
     */
    fun deleteWarp(name: String, owner: UUID): Boolean {
        val sql = "DELETE FROM warps WHERE name = ? AND owner = ?"
        val result = executeUpdate(sql, listOf(name, owner.toString())) > 0
        if (result) {
            logDebug("Warp deleted successfully: $name (owner: $owner)")
        } else {
            logWarning("Failed to delete warp: $name (owner: $owner)")
        }
        return result
    }
    
    /**
     * 根据ID获取地标
     * @param id 地标ID
     * @return 地标对象，如果不存在则返回null
     */
    fun getWarp(id: Int): Warp? {
        val sql = "SELECT * FROM warps WHERE id = ?"
        return executeQuerySingle(sql, listOf(id)) { resultSet ->
            mapResultSetToWarp(resultSet)
        }
    }
    
    /**
     * 根据名称和所有者获取地标
     * @param name 地标名称
     * @param owner 所有者UUID
     * @return 地标对象，如果不存在则返回null
     */
    fun getWarp(name: String, owner: UUID): Warp? {
        val sql = "SELECT * FROM warps WHERE name = ? AND owner = ?"
        return executeQuerySingle(sql, listOf(name, owner.toString())) { resultSet ->
            mapResultSetToWarp(resultSet)
        }
    }
    
    /**
     * 根据名称获取公开地标
     * @param name 地标名称
     * @return 地标对象，如果不存在则返回null
     */
    fun getPublicWarp(name: String): Warp? {
        val sql = "SELECT * FROM warps WHERE name = ? AND is_public = true"
        return executeQuerySingle(sql, listOf(name)) { resultSet ->
            mapResultSetToWarp(resultSet)
        }
    }
    
    /**
     * 获取所有地标
     * @return 地标列表
     */
    fun getAllWarps(): List<Warp> {
        val sql = "SELECT * FROM warps ORDER BY create_time DESC"
        return executeQuery(sql) { resultSet ->
            mapResultSetToWarp(resultSet)
        }
    }
    
    /**
     * 获取公开地标列表
     * @return 公开地标列表
     */
    fun getPublicWarps(): List<Warp> {
        val sql = "SELECT * FROM warps WHERE is_public = true ORDER BY create_time DESC"
        return executeQuery(sql) { resultSet ->
            mapResultSetToWarp(resultSet)
        }
    }
    
    /**
     * 获取指定所有者的地标列表
     * @param owner 所有者UUID
     * @return 地标列表
     */
    fun getWarpsByOwner(owner: UUID): List<Warp> {
        val sql = "SELECT * FROM warps WHERE owner = ? ORDER BY create_time DESC"
        return executeQuery(sql, listOf(owner.toString())) { resultSet ->
            mapResultSetToWarp(resultSet)
        }
    }
    
    /**
     * 搜索地标
     * @param keyword 关键词
     * @param includePrivate 是否包含私有地标
     * @param owner 如果包含私有地标，指定所有者UUID
     * @return 匹配的地标列表
     */
    fun searchWarps(keyword: String, includePrivate: Boolean = false, owner: UUID? = null): List<Warp> {
        val sql = if (includePrivate && owner != null) {
            "SELECT * FROM warps WHERE (name LIKE ? OR description LIKE ?) AND (is_public = true OR owner = ?) ORDER BY create_time DESC"
        } else {
            "SELECT * FROM warps WHERE (name LIKE ? OR description LIKE ?) AND is_public = true ORDER BY create_time DESC"
        }
        
        val searchPattern = "%$keyword%"
        val params = if (includePrivate && owner != null) {
            listOf(searchPattern, searchPattern, owner.toString())
        } else {
            listOf(searchPattern, searchPattern)
        }
        
        return executeQuery(sql, params) { resultSet ->
            mapResultSetToWarp(resultSet)
        }
    }
    
    /**
     * 更新地标的公开状态
     * @param id 地标ID
     * @param isPublic 是否公开
     * @return 如果更新成功则返回true
     */
    fun updateWarpVisibility(id: Int, isPublic: Boolean): Boolean {
        val sql = "UPDATE warps SET is_public = ? WHERE id = ?"
        val result = executeUpdate(sql, listOf(isPublic, id)) > 0
        if (result) {
            logDebug("Warp visibility updated successfully: ID=$id, public=$isPublic")
        } else {
            logWarning("Failed to update warp visibility: ID=$id")
        }
        return result
    }
    
    /**
     * 更新地标描述
     * @param id 地标ID
     * @param description 新描述
     * @return 如果更新成功则返回true
     */
    fun updateWarpDescription(id: Int, description: String): Boolean {
        val sql = "UPDATE warps SET description = ? WHERE id = ?"
        val result = executeUpdate(sql, listOf(description, id)) > 0
        if (result) {
            logDebug("Warp description updated successfully: ID=$id")
        } else {
            logWarning("Failed to update warp description: ID=$id")
        }
        return result
    }
    
    /**
     * 获取地标总数
     * @return 地标总数
     */
    fun getWarpCount(): Int {
        val sql = "SELECT COUNT(*) FROM warps"
        return executeQuerySingle(sql) { resultSet ->
            resultSet.getInt(1)
        } ?: 0
    }
    
    /**
     * 获取公开地标总数
     * @return 公开地标总数
     */
    fun getPublicWarpCount(): Int {
        val sql = "SELECT COUNT(*) FROM warps WHERE is_public = true"
        return executeQuerySingle(sql) { resultSet ->
            resultSet.getInt(1)
        } ?: 0
    }
    
    /**
     * 获取指定所有者的地标总数
     * @param owner 所有者UUID
     * @return 地标总数
     */
    fun getWarpCountByOwner(owner: UUID): Int {
        val sql = "SELECT COUNT(*) FROM warps WHERE owner = ?"
        return executeQuerySingle(sql, listOf(owner.toString())) { resultSet ->
            resultSet.getInt(1)
        } ?: 0
    }
    
    /**
     * 将ResultSet映射为Warp对象
     * @param resultSet 查询结果集
     * @return Warp对象
     */
    protected fun mapResultSetToWarp(resultSet: ResultSet): Warp {
        return Warp(
            id = resultSet.getInt(PluginConstants.Database.FIELD_ID),
            name = resultSet.getString(PluginConstants.Database.FIELD_NAME),
            owner = UUID.fromString(resultSet.getString(PluginConstants.Database.FIELD_OWNER)),
            ownerName = resultSet.getString(PluginConstants.Database.FIELD_OWNER_NAME),
            worldName = resultSet.getString(PluginConstants.Database.FIELD_WORLD_NAME),
            x = resultSet.getDouble(PluginConstants.Database.FIELD_X),
            y = resultSet.getDouble(PluginConstants.Database.FIELD_Y),
            z = resultSet.getDouble(PluginConstants.Database.FIELD_Z),
            yaw = resultSet.getFloat(PluginConstants.Database.FIELD_YAW),
            pitch = resultSet.getFloat(PluginConstants.Database.FIELD_PITCH),
            isPublic = resultSet.getBoolean(PluginConstants.Database.FIELD_IS_PUBLIC),
            description = resultSet.getString(PluginConstants.Database.FIELD_DESCRIPTION) ?: "",
            displayMaterial = resultSet.getString(PluginConstants.Database.FIELD_MATERIAL) ?: PluginConstants.Database.DEFAULT_MATERIAL,
            createTime = resultSet.getLong(PluginConstants.Database.FIELD_CREATE_TIME)
        )
    }
}
