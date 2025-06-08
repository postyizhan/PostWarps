package com.github.postyizhan.database.storage

import com.github.postyizhan.PostWarps
import com.github.postyizhan.model.Warp
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.util.*

class MySQLStorage(private val plugin: PostWarps) : Storage {
    private var dataSource: HikariDataSource? = null
    
    override fun init() {
        try {
            // 配置HikariCP
            val config = HikariConfig()
            config.jdbcUrl = "jdbc:mysql://${plugin.mysqlHost}:${plugin.mysqlPort}/${plugin.mysqlDatabase}?useSSL=false&autoReconnect=true"
            config.username = plugin.mysqlUsername
            config.password = plugin.mysqlPassword
            config.maximumPoolSize = 10
            config.connectionTimeout = 30000
            config.idleTimeout = 600000
            config.maxLifetime = 1800000
            
            // 创建数据源
            dataSource = HikariDataSource(config)
            
            // 创建表
            val connection = dataSource!!.connection
            val statement = connection.createStatement()
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS warps (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    name VARCHAR(255) NOT NULL,
                    owner_uuid VARCHAR(36) NOT NULL,
                    world_name VARCHAR(255) NOT NULL,
                    x DOUBLE NOT NULL,
                    y DOUBLE NOT NULL,
                    z DOUBLE NOT NULL,
                    yaw FLOAT NOT NULL,
                    pitch FLOAT NOT NULL,
                    description TEXT,
                    server VARCHAR(255),
                    created_at BIGINT NOT NULL,
                    visits INT DEFAULT 0
                )
                """
            )
            
            // 创建索引
            statement.execute("CREATE INDEX IF NOT EXISTS idx_warps_name ON warps (name)")
            statement.execute("CREATE INDEX IF NOT EXISTS idx_warps_owner ON warps (owner_uuid)")
            
            statement.close()
            connection.close()
            
            if (plugin.debugMode) {
                plugin.logger.info("MySQL connection established")
            }
        } catch (e: Exception) {
            plugin.logger.severe("Failed to initialize MySQL database: ${e.message}")
            e.printStackTrace()
        }
    }
    
    override fun close() {
        try {
            dataSource?.close()
        } catch (e: SQLException) {
            plugin.logger.warning("Error closing MySQL connection: ${e.message}")
        }
    }
    
    override fun createWarp(warp: Warp): Boolean {
        var connection: Connection? = null
        
        try {
            connection = dataSource!!.connection
            val sql = """
                INSERT INTO warps (name, owner_uuid, world_name, x, y, z, yaw, pitch, description, server, created_at, visits)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
            
            val statement = connection.prepareStatement(sql)
            statement.setString(1, warp.name.lowercase())
            statement.setString(2, warp.ownerUUID.toString())
            statement.setString(3, warp.worldName)
            statement.setDouble(4, warp.x)
            statement.setDouble(5, warp.y)
            statement.setDouble(6, warp.z)
            statement.setFloat(7, warp.yaw)
            statement.setFloat(8, warp.pitch)
            statement.setString(9, warp.description)
            statement.setString(10, warp.server)
            statement.setLong(11, warp.createdAt)
            statement.setInt(12, warp.visits)
            
            val result = statement.executeUpdate() > 0
            statement.close()
            
            if (plugin.debugMode) {
                plugin.logger.info("Created warp: ${warp.name}")
            }
            
            return result
        } catch (e: SQLException) {
            plugin.logger.severe("Error creating warp: ${e.message}")
            e.printStackTrace()
            return false
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                plugin.logger.warning("Error closing connection: ${e.message}")
            }
        }
    }
    
    override fun deleteWarp(name: String, ownerUUID: UUID): Boolean {
        var connection: Connection? = null
        
        try {
            connection = dataSource!!.connection
            val sql = "DELETE FROM warps WHERE LOWER(name) = LOWER(?) AND owner_uuid = ?"
            
            val statement = connection.prepareStatement(sql)
            statement.setString(1, name)
            statement.setString(2, ownerUUID.toString())
            
            val result = statement.executeUpdate() > 0
            statement.close()
            
            if (plugin.debugMode) {
                plugin.logger.info("Deleted warp: $name")
            }
            
            return result
        } catch (e: SQLException) {
            plugin.logger.severe("Error deleting warp: ${e.message}")
            e.printStackTrace()
            return false
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                plugin.logger.warning("Error closing connection: ${e.message}")
            }
        }
    }
    
    override fun getWarp(name: String): Warp? {
        var connection: Connection? = null
        
        try {
            connection = dataSource!!.connection
            val sql = "SELECT * FROM warps WHERE LOWER(name) = LOWER(?)"
            
            val statement = connection.prepareStatement(sql)
            statement.setString(1, name)
            
            val resultSet = statement.executeQuery()
            
            val warp = if (resultSet.next()) {
                resultSetToWarp(resultSet)
            } else {
                null
            }
            
            resultSet.close()
            statement.close()
            
            return warp
        } catch (e: SQLException) {
            plugin.logger.severe("Error getting warp: ${e.message}")
            e.printStackTrace()
            return null
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                plugin.logger.warning("Error closing connection: ${e.message}")
            }
        }
    }
    
    override fun getWarpsByOwner(ownerUUID: UUID): List<Warp> {
        val warps = mutableListOf<Warp>()
        var connection: Connection? = null
        
        try {
            connection = dataSource!!.connection
            val sql = "SELECT * FROM warps WHERE owner_uuid = ? ORDER BY name ASC"
            
            val statement = connection.prepareStatement(sql)
            statement.setString(1, ownerUUID.toString())
            
            val resultSet = statement.executeQuery()
            
            while (resultSet.next()) {
                warps.add(resultSetToWarp(resultSet))
            }
            
            resultSet.close()
            statement.close()
            
            return warps
        } catch (e: SQLException) {
            plugin.logger.severe("Error getting warps by owner: ${e.message}")
            e.printStackTrace()
            return emptyList()
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                plugin.logger.warning("Error closing connection: ${e.message}")
            }
        }
    }
    
    override fun getAllWarps(): List<Warp> {
        val warps = mutableListOf<Warp>()
        var connection: Connection? = null
        
        try {
            connection = dataSource!!.connection
            val sql = "SELECT * FROM warps ORDER BY name ASC"
            
            val statement = connection.createStatement()
            val resultSet = statement.executeQuery(sql)
            
            while (resultSet.next()) {
                warps.add(resultSetToWarp(resultSet))
            }
            
            resultSet.close()
            statement.close()
            
            return warps
        } catch (e: SQLException) {
            plugin.logger.severe("Error getting all warps: ${e.message}")
            e.printStackTrace()
            return emptyList()
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                plugin.logger.warning("Error closing connection: ${e.message}")
            }
        }
    }
    
    override fun incrementVisits(warpId: Int): Boolean {
        var connection: Connection? = null
        
        try {
            connection = dataSource!!.connection
            val sql = "UPDATE warps SET visits = visits + 1 WHERE id = ?"
            
            val statement = connection.prepareStatement(sql)
            statement.setInt(1, warpId)
            
            val result = statement.executeUpdate() > 0
            statement.close()
            
            return result
        } catch (e: SQLException) {
            plugin.logger.severe("Error incrementing visits: ${e.message}")
            e.printStackTrace()
            return false
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                plugin.logger.warning("Error closing connection: ${e.message}")
            }
        }
    }
    
    override fun getWarpCount(ownerUUID: UUID): Int {
        var connection: Connection? = null
        
        try {
            connection = dataSource!!.connection
            val sql = "SELECT COUNT(*) FROM warps WHERE owner_uuid = ?"
            
            val statement = connection.prepareStatement(sql)
            statement.setString(1, ownerUUID.toString())
            
            val resultSet = statement.executeQuery()
            
            val count = if (resultSet.next()) {
                resultSet.getInt(1)
            } else {
                0
            }
            
            resultSet.close()
            statement.close()
            
            return count
        } catch (e: SQLException) {
            plugin.logger.severe("Error getting warp count: ${e.message}")
            e.printStackTrace()
            return 0
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                plugin.logger.warning("Error closing connection: ${e.message}")
            }
        }
    }
    
    override fun warpExists(name: String): Boolean {
        var connection: Connection? = null
        
        try {
            connection = dataSource!!.connection
            val sql = "SELECT COUNT(*) FROM warps WHERE LOWER(name) = LOWER(?)"
            
            val statement = connection.prepareStatement(sql)
            statement.setString(1, name)
            
            val resultSet = statement.executeQuery()
            
            val exists = if (resultSet.next()) {
                resultSet.getInt(1) > 0
            } else {
                false
            }
            
            resultSet.close()
            statement.close()
            
            return exists
        } catch (e: SQLException) {
            plugin.logger.severe("Error checking if warp exists: ${e.message}")
            e.printStackTrace()
            return false
        } finally {
            try {
                connection?.close()
            } catch (e: SQLException) {
                plugin.logger.warning("Error closing connection: ${e.message}")
            }
        }
    }
    
    private fun resultSetToWarp(resultSet: ResultSet): Warp {
        return Warp(
            id = resultSet.getInt("id"),
            name = resultSet.getString("name"),
            ownerUUID = UUID.fromString(resultSet.getString("owner_uuid")),
            worldName = resultSet.getString("world_name"),
            x = resultSet.getDouble("x"),
            y = resultSet.getDouble("y"),
            z = resultSet.getDouble("z"),
            yaw = resultSet.getFloat("yaw"),
            pitch = resultSet.getFloat("pitch"),
            description = resultSet.getString("description") ?: "",
            server = resultSet.getString("server") ?: "",
            createdAt = resultSet.getLong("created_at"),
            visits = resultSet.getInt("visits")
        )
    }
}
