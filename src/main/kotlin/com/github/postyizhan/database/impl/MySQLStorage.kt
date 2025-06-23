package com.github.postyizhan.database.impl

import com.github.postyizhan.PostWarps
import com.github.postyizhan.database.IStorage
import com.github.postyizhan.model.Warp
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.file.FileConfiguration
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

/**
 * MySQL数据库存储实现
 */
class MySQLStorage(private val plugin: PostWarps) : IStorage {
    
    private var dataSource: HikariDataSource? = null
    private val config: FileConfiguration
        get() = plugin.getConfigManager().getConfig()
    
    /**
     * 初始化数据库
     */
    override fun init() {
        try {
            // 配置HikariCP连接池
            val hikariConfig = HikariConfig()
            
            val host = config.getString("database.mysql.host", "localhost") ?: "localhost"
            val port = config.getInt("database.mysql.port", 3306)
            val database = config.getString("database.mysql.database", "postwarps") ?: "postwarps"
            val username = config.getString("database.mysql.username", "root") ?: "root"
            val password = config.getString("database.mysql.password", "password") ?: "password"
            val useSSL = config.getBoolean("database.mysql.use-ssl", false)
            val poolSize = config.getInt("database.mysql.pool-size", 10)
            
            hikariConfig.jdbcUrl = "jdbc:mysql://$host:$port/$database?useSSL=$useSSL&useUnicode=true&characterEncoding=utf8"
            hikariConfig.username = username
            hikariConfig.password = password
            hikariConfig.maximumPoolSize = poolSize
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            
            // 创建数据源
            dataSource = HikariDataSource(hikariConfig)
            
            // 创建表
            createTables()
        } catch (e: Exception) {
            plugin.logger.severe("无法初始化MySQL数据库: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * 创建表
     */
    private fun createTables() {
        val connection = getConnection() ?: return
        
        try {
            val statement = connection.createStatement()
            
            // 创建地标表
            statement.executeUpdate(
                """
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
                    is_public BOOLEAN NOT NULL,
                    description VARCHAR(255),
                    create_time BIGINT NOT NULL,
                    INDEX idx_warps_owner (owner),
                    INDEX idx_warps_public (is_public),
                    INDEX idx_warps_name_owner (name, owner)
                )
                """.trimIndent()
            )
            
            statement.close()
        } catch (e: SQLException) {
            plugin.logger.severe("创建数据库表时出错: ${e.message}")
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
    }
    
    /**
     * 获取数据库连接
     */
    private fun getConnection(): Connection? {
        return try {
            dataSource?.connection
        } catch (e: SQLException) {
            plugin.logger.severe("获取数据库连接时出错: ${e.message}")
            null
        }
    }
    
    /**
     * 关闭数据库连接池
     */
    override fun close() {
        if (dataSource?.isClosed == false) {
            dataSource?.close()
        }
    }
    
    /**
     * 创建地标
     */
    override fun createWarp(warp: Warp): Boolean {
        val connection = getConnection() ?: return false
        
        try {
            val sql = """
                INSERT INTO warps (name, owner, owner_name, world_name, x, y, z, yaw, pitch, is_public, description, create_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            
            val preparedStatement = connection.prepareStatement(sql)
            
            preparedStatement.setString(1, warp.name)
            preparedStatement.setString(2, warp.owner.toString())
            preparedStatement.setString(3, warp.ownerName)
            preparedStatement.setString(4, warp.worldName)
            preparedStatement.setDouble(5, warp.x)
            preparedStatement.setDouble(6, warp.y)
            preparedStatement.setDouble(7, warp.z)
            preparedStatement.setFloat(8, warp.yaw)
            preparedStatement.setFloat(9, warp.pitch)
            preparedStatement.setBoolean(10, warp.isPublic)
            preparedStatement.setString(11, warp.description)
            preparedStatement.setLong(12, warp.createTime)
            
            val result = preparedStatement.executeUpdate()
            preparedStatement.close()
            
            return result > 0
        } catch (e: SQLException) {
            plugin.logger.severe("创建地标时出错: ${e.message}")
            return false
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
    }
    
    /**
     * 根据ID删除地标
     */
    override fun deleteWarp(id: Int): Boolean {
        val connection = getConnection() ?: return false
        
        try {
            val sql = "DELETE FROM warps WHERE id = ?"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setInt(1, id)
            val result = preparedStatement.executeUpdate()
            preparedStatement.close()
            
            return result > 0
        } catch (e: SQLException) {
            plugin.logger.severe("删除地标时出错: ${e.message}")
            return false
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
    }
    
    /**
     * 根据名称和所有者删除地标
     */
    override fun deleteWarp(name: String, owner: UUID): Boolean {
        val connection = getConnection() ?: return false
        
        try {
            val sql = "DELETE FROM warps WHERE name = ? AND owner = ?"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setString(1, name)
            preparedStatement.setString(2, owner.toString())
            val result = preparedStatement.executeUpdate()
            preparedStatement.close()
            
            return result > 0
        } catch (e: SQLException) {
            plugin.logger.severe("删除地标时出错: ${e.message}")
            return false
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
    }
    
    /**
     * 根据ID获取地标
     */
    override fun getWarp(id: Int): Warp? {
        val connection = getConnection() ?: return null
        
        try {
            val sql = "SELECT * FROM warps WHERE id = ?"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setInt(1, id)
            val resultSet = preparedStatement.executeQuery()
            
            val warp = resultSet.takeIf { it.next() }?.let { createWarpFromResultSet(it) }
            
            resultSet.close()
            preparedStatement.close()
            
            return warp
        } catch (e: SQLException) {
            plugin.logger.severe("获取地标时出错: ${e.message}")
            return null
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
    }
    
    /**
     * 根据名称和所有者获取地标
     */
    override fun getWarp(name: String, owner: UUID): Warp? {
        val connection = getConnection() ?: return null
        
        try {
            val sql = "SELECT * FROM warps WHERE name = ? AND owner = ?"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setString(1, name)
            preparedStatement.setString(2, owner.toString())
            val resultSet = preparedStatement.executeQuery()
            
            val warp = resultSet.takeIf { it.next() }?.let { createWarpFromResultSet(it) }
            
            resultSet.close()
            preparedStatement.close()
            
            return warp
        } catch (e: SQLException) {
            plugin.logger.severe("获取地标时出错: ${e.message}")
            return null
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
    }
    
    /**
     * 根据名称获取公开地标
     */
    override fun getPublicWarp(name: String): Warp? {
        val connection = getConnection() ?: return null
        
        try {
            val sql = "SELECT * FROM warps WHERE name = ? AND is_public = true"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setString(1, name)
            val resultSet = preparedStatement.executeQuery()
            
            val warp = resultSet.takeIf { it.next() }?.let { createWarpFromResultSet(it) }
            
            resultSet.close()
            preparedStatement.close()
            
            return warp
        } catch (e: SQLException) {
            plugin.logger.severe("获取公开地标时出错: ${e.message}")
            return null
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
    }
    
    /**
     * 获取所有地标
     */
    override fun getAllWarps(): List<Warp> {
        val warps = mutableListOf<Warp>()
        val connection = getConnection() ?: return warps

        try {
            val sql = "SELECT * FROM warps ORDER BY name ASC"
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(sql)

            while (resultSet.next()) {
                val warp = createWarpFromResultSet(resultSet)
                warps.add(warp)
            }

            resultSet.close()
            statement.close()
        } catch (e: SQLException) {
            plugin.logger.severe("Failed to get all warps: ${e.message}")
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("Failed to close database connection: ${e.message}")
            }
        }

        return warps
    }

    /**
     * 获取所有公开地标
     */
    override fun getAllPublicWarps(): List<Warp> {
        val warps = mutableListOf<Warp>()
        val connection = getConnection() ?: return warps
        
        try {
            val sql = "SELECT * FROM warps WHERE is_public = true ORDER BY name ASC"
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(sql)
            
            while (resultSet.next()) {
                val warp = createWarpFromResultSet(resultSet)
                warps.add(warp)
            }
            
            resultSet.close()
            statement.close()
        } catch (e: SQLException) {
            plugin.logger.severe("获取所有公开地标时出错: ${e.message}")
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
        
        return warps
    }
    
    /**
     * 获取指定玩家的所有地标
     */
    override fun getPlayerWarps(owner: UUID): List<Warp> {
        val warps = mutableListOf<Warp>()
        val connection = getConnection() ?: return warps
        
        try {
            val sql = "SELECT * FROM warps WHERE owner = ? ORDER BY name ASC"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setString(1, owner.toString())
            val resultSet = preparedStatement.executeQuery()
            
            while (resultSet.next()) {
                val warp = createWarpFromResultSet(resultSet)
                warps.add(warp)
            }
            
            resultSet.close()
            preparedStatement.close()
        } catch (e: SQLException) {
            plugin.logger.severe("获取玩家地标时出错: ${e.message}")
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
        
        return warps
    }
    
    /**
     * 获取指定玩家的公开地标
     */
    override fun getPlayerPublicWarps(owner: UUID): List<Warp> {
        val warps = mutableListOf<Warp>()
        val connection = getConnection() ?: return warps
        
        try {
            val sql = "SELECT * FROM warps WHERE owner = ? AND is_public = true ORDER BY name ASC"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setString(1, owner.toString())
            val resultSet = preparedStatement.executeQuery()
            
            while (resultSet.next()) {
                val warp = createWarpFromResultSet(resultSet)
                warps.add(warp)
            }
            
            resultSet.close()
            preparedStatement.close()
        } catch (e: SQLException) {
            plugin.logger.severe("获取玩家公开地标时出错: ${e.message}")
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
        
        return warps
    }
    
    /**
     * 获取指定玩家的私有地标
     */
    override fun getPlayerPrivateWarps(owner: UUID): List<Warp> {
        val warps = mutableListOf<Warp>()
        val connection = getConnection() ?: return warps
        
        try {
            val sql = "SELECT * FROM warps WHERE owner = ? AND is_public = false ORDER BY name ASC"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setString(1, owner.toString())
            val resultSet = preparedStatement.executeQuery()
            
            while (resultSet.next()) {
                val warp = createWarpFromResultSet(resultSet)
                warps.add(warp)
            }
            
            resultSet.close()
            preparedStatement.close()
        } catch (e: SQLException) {
            plugin.logger.severe("获取玩家私有地标时出错: ${e.message}")
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
        
        return warps
    }
    
    /**
     * 设置地标公开状态
     */
    override fun setWarpPublic(id: Int, isPublic: Boolean): Boolean {
        val connection = getConnection() ?: return false
        
        try {
            val sql = "UPDATE warps SET is_public = ? WHERE id = ?"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setBoolean(1, isPublic)
            preparedStatement.setInt(2, id)
            val result = preparedStatement.executeUpdate()
            preparedStatement.close()
            
            return result > 0
        } catch (e: SQLException) {
            plugin.logger.severe("设置地标公开状态时出错: ${e.message}")
            return false
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
    }
    
    /**
     * 根据名称和所有者设置地标公开状态
     */
    override fun setWarpPublic(name: String, owner: UUID, isPublic: Boolean): Boolean {
        val connection = getConnection() ?: return false
        
        try {
            val sql = "UPDATE warps SET is_public = ? WHERE name = ? AND owner = ?"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setBoolean(1, isPublic)
            preparedStatement.setString(2, name)
            preparedStatement.setString(3, owner.toString())
            val result = preparedStatement.executeUpdate()
            preparedStatement.close()
            
            return result > 0
        } catch (e: SQLException) {
            plugin.logger.severe("设置地标公开状态时出错: ${e.message}")
            return false
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
    }
    
    /**
     * 更新地标描述
     */
    override fun updateWarpDescription(id: Int, description: String): Boolean {
        val connection = getConnection() ?: return false
        
        try {
            val sql = "UPDATE warps SET description = ? WHERE id = ?"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setString(1, description)
            preparedStatement.setInt(2, id)
            val result = preparedStatement.executeUpdate()
            preparedStatement.close()
            
            return result > 0
        } catch (e: SQLException) {
            plugin.logger.severe("更新地标描述时出错: ${e.message}")
            return false
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
    }
    
    /**
     * 根据名称和所有者更新地标描述
     */
    override fun updateWarpDescription(name: String, owner: UUID, description: String): Boolean {
        val connection = getConnection() ?: return false
        
        try {
            val sql = "UPDATE warps SET description = ? WHERE name = ? AND owner = ?"
            val preparedStatement = connection.prepareStatement(sql)
            preparedStatement.setString(1, description)
            preparedStatement.setString(2, name)
            preparedStatement.setString(3, owner.toString())
            val result = preparedStatement.executeUpdate()
            preparedStatement.close()
            
            return result > 0
        } catch (e: SQLException) {
            plugin.logger.severe("更新地标描述时出错: ${e.message}")
            return false
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
    }
    
    /**
     * 更新地标位置
     */
    override fun updateWarpLocation(id: Int, worldName: String, x: Double, y: Double, z: Double, yaw: Float, pitch: Float): Boolean {
        val connection = getConnection() ?: return false
        
        try {
            val sql = """
                UPDATE warps 
                SET world_name = ?, x = ?, y = ?, z = ?, yaw = ?, pitch = ?
                WHERE id = ?
            """.trimIndent()
            
            val preparedStatement = connection.prepareStatement(sql)
            
            preparedStatement.setString(1, worldName)
            preparedStatement.setDouble(2, x)
            preparedStatement.setDouble(3, y)
            preparedStatement.setDouble(4, z)
            preparedStatement.setFloat(5, yaw)
            preparedStatement.setFloat(6, pitch)
            preparedStatement.setInt(7, id)
            
            val result = preparedStatement.executeUpdate()
            preparedStatement.close()
            
            return result > 0
        } catch (e: SQLException) {
            plugin.logger.severe("更新地标位置时出错: ${e.message}")
            return false
        } finally {
            try {
                connection.close()
            } catch (e: SQLException) {
                plugin.logger.warning("关闭数据库连接时出错: ${e.message}")
            }
        }
    }
    
    /**
     * 从结果集创建地标对象
     */
    private fun createWarpFromResultSet(rs: ResultSet): Warp {
        return Warp(
            id = rs.getInt("id"),
            name = rs.getString("name"),
            owner = UUID.fromString(rs.getString("owner")),
            ownerName = rs.getString("owner_name"),
            worldName = rs.getString("world_name"),
            x = rs.getDouble("x"),
            y = rs.getDouble("y"),
            z = rs.getDouble("z"),
            yaw = rs.getFloat("yaw"),
            pitch = rs.getFloat("pitch"),
            isPublic = rs.getBoolean("is_public"),
            description = rs.getString("description") ?: "",
            createTime = rs.getLong("create_time")
        )
    }
}
 