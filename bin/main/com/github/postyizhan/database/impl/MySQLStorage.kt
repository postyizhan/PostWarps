package com.github.postyizhan.database.impl

import com.github.postyizhan.PostWarps
import com.github.postyizhan.constants.ConfigurableConstants
import com.github.postyizhan.constants.PluginConstants
import com.github.postyizhan.database.IStorage
import com.github.postyizhan.database.base.WarpDAO
import com.github.postyizhan.model.Warp
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.file.FileConfiguration
import java.sql.Connection
import java.sql.SQLException
import java.util.*

/**
 * MySQL数据库存储实现 - 重构后的版本
 * 继承WarpDAO基类，减少重复代码
 */
class MySQLStorage(plugin: PostWarps) : WarpDAO(plugin), IStorage {

    private var dataSource: HikariDataSource? = null
    private val config: FileConfiguration
        get() = plugin.getConfigManager().getConfig()

    /**
     * 初始化数据库
     */
    override fun init() {
        try {
            initializeConnectionPool()
            if (!createWarpTable()) {
                throw SQLException("创建地标表失败")
            }
            logInfo("MySQL数据库初始化成功")
        } catch (e: Exception) {
            logError("无法初始化MySQL数据库", e)
            throw e
        }
    }

    /**
     * 初始化连接池
     */
    private fun initializeConnectionPool() {
        val hikariConfig = HikariConfig().apply {
            val host = config.getString(PluginConstants.Config.Database.MYSQL_HOST, "localhost") ?: "localhost"
            val port = config.getInt(PluginConstants.Config.Database.MYSQL_PORT, 3306)
            val database = config.getString(PluginConstants.Config.Database.MYSQL_DATABASE, "postwarps") ?: "postwarps"
            val username = config.getString(PluginConstants.Config.Database.MYSQL_USERNAME, "root") ?: "root"
            val password = config.getString(PluginConstants.Config.Database.MYSQL_PASSWORD, "password") ?: "password"
            val useSSL = config.getBoolean(PluginConstants.Config.Database.MYSQL_USE_SSL, false)

            jdbcUrl = "jdbc:mysql://$host:$port/$database?useSSL=$useSSL&useUnicode=true&characterEncoding=utf8&serverTimezone=UTC"
            this.username = username
            this.password = password
            maximumPoolSize = ConfigurableConstants.Database.getPoolSize(plugin)
            minimumIdle = ConfigurableConstants.Database.getMinIdleConnections(plugin)
            connectionTimeout = ConfigurableConstants.Database.getConnectionTimeout(plugin)
            idleTimeout = ConfigurableConstants.Database.getIdleTimeout(plugin)
            maxLifetime = ConfigurableConstants.Database.getMaxLifetime(plugin)

            // 性能优化配置
            addDataSourceProperty("cachePrepStmts", "true")
            addDataSourceProperty("prepStmtCacheSize", "250")
            addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            addDataSourceProperty("useServerPrepStmts", "true")
            addDataSourceProperty("rewriteBatchedStatements", "true")
        }

        dataSource = HikariDataSource(hikariConfig)
        logDebug("MySQL连接池初始化成功")
    }

    /**
     * 创建地标表
     */
    override fun createWarpTable(): Boolean {
        val sql = """
            CREATE TABLE IF NOT EXISTS warps (
                id INT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(32) NOT NULL,
                owner VARCHAR(36) NOT NULL,
                owner_name VARCHAR(16) NOT NULL,
                world_name VARCHAR(64) NOT NULL,
                x DOUBLE NOT NULL,
                y DOUBLE NOT NULL,
                z DOUBLE NOT NULL,
                yaw FLOAT NOT NULL,
                pitch FLOAT NOT NULL,
                is_public BOOLEAN NOT NULL DEFAULT FALSE,
                description VARCHAR(255),
                material VARCHAR(64) DEFAULT 'ENDER_PEARL',
                skull_owner VARCHAR(16),
                skull_texture TEXT,
                create_time BIGINT NOT NULL,
                INDEX idx_warps_owner (owner),
                INDEX idx_warps_public (is_public),
                INDEX idx_warps_name_owner (name, owner),
                INDEX idx_warps_name_public (name, is_public)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
        """.trimIndent()

        val result = executeUpdate(sql) >= 0
        if (result) {
            logDebug("MySQL地标表创建/验证成功")
        } else {
            logError("MySQL地标表创建失败", RuntimeException("Table creation failed"))
        }
        return result
    }

    /**
     * 获取数据库连接
     */
    override fun getConnection(): Connection? {
        return try {
            dataSource?.connection
        } catch (e: SQLException) {
            logError("获取MySQL数据库连接失败", e)
            null
        }
    }

    /**
     * 关闭数据库连接池
     */
    override fun close() {
        try {
            if (dataSource?.isClosed == false) {
                dataSource?.close()
                logInfo("MySQL连接池已关闭")
            }
        } catch (e: Exception) {
            logError("关闭MySQL连接池失败", e)
        } finally {
            dataSource = null
        }
    }
    // 基本CRUD操作已在WarpDAO基类中实现，这里只需要实现特定于MySQL的方法

    // IStorage接口的实现方法，委托给基类
    override fun getAllPublicWarps(): List<Warp> = getPublicWarps()
    override fun getPlayerWarps(owner: UUID): List<Warp> = getWarpsByOwner(owner)
    override fun getPlayerPublicWarps(owner: UUID): List<Warp> = getWarpsByOwner(owner).filter { it.isPublic }
    override fun getPlayerPrivateWarps(owner: UUID): List<Warp> = getWarpsByOwner(owner).filter { !it.isPublic }
    override fun setWarpPublic(id: Int, isPublic: Boolean): Boolean = updateWarpVisibility(id, isPublic)
    override fun setWarpPublic(name: String, owner: UUID, isPublic: Boolean): Boolean {
        val warp = getWarp(name, owner) ?: return false
        return updateWarpVisibility(warp.id, isPublic)
    }
    override fun updateWarpDescription(name: String, owner: UUID, description: String): Boolean {
        val warp = getWarp(name, owner) ?: return false
        return updateWarpDescription(warp.id, description)
    }

    // MySQL特定的优化方法
    override fun updateWarpLocation(id: Int, worldName: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float): Boolean {
        val sql = "UPDATE warps SET world_name = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ? WHERE id = ?"
        val params = listOf(worldName, x, y, z, yaw, pitch, id)
        val result = executeUpdate(sql, params) > 0
        if (result) {
            logDebug("更新地标位置成功: ID=$id")
        } else {
            logWarning("更新地标位置失败: ID=$id")
        }
        return result
    }

    override fun updateWarpMaterial(id: Int, material: String): Boolean {
        val sql = "UPDATE warps SET material = ? WHERE id = ?"
        val result = executeUpdate(sql, listOf(material, id)) > 0
        if (result) {
            logDebug("更新地标材质成功: ID=$id, 材质=$material")
        } else {
            logWarning("更新地标材质失败: ID=$id")
        }
        return result
    }

    override fun updateWarpMaterial(name: String, owner: UUID, material: String): Boolean {
        val warp = getWarp(name, owner) ?: return false
        return updateWarpMaterial(warp.id, material)
    }

    override fun updateWarpMaterial(id: Int, material: String, skullOwner: String?, skullTexture: String?): Boolean {
        val sql = "UPDATE warps SET material = ?, skull_owner = ?, skull_texture = ? WHERE id = ?"
        val params = listOf(material, skullOwner ?: "", skullTexture ?: "", id)
        val result = executeUpdate(sql, params) > 0
        if (result) {
            logDebug("更新地标材质和头颅信息成功: ID=$id, 材质=$material")
        } else {
            logWarning("更新地标材质和头颅信息失败: ID=$id")
        }
        return result
    }

    override fun updateWarpMaterial(name: String, owner: UUID, material: String, skullOwner: String?, skullTexture: String?): Boolean {
        val warp = getWarp(name, owner) ?: return false
        return updateWarpMaterial(warp.id, material, skullOwner, skullTexture)
    }


}
 