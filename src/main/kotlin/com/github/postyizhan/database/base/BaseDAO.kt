package com.github.postyizhan.database.base

import com.github.postyizhan.PostWarps
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

abstract class BaseDAO(protected val plugin: PostWarps) {
    

    protected abstract fun getConnection(): Connection?
    

    protected fun <T> executeQuery(
        sql: String,
        params: List<Any> = emptyList(),
        mapper: (ResultSet) -> T
    ): List<T> {
        return executeWithConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                setParameters(statement, params)
                statement.executeQuery().use { resultSet ->
                    val results = mutableListOf<T>()
                    while (resultSet.next()) {
                        results.add(mapper(resultSet))
                    }
                    results
                }
            }
        } ?: emptyList()
    }
    

    protected fun <T> executeQuerySingle(
        sql: String,
        params: List<Any> = emptyList(),
        mapper: (ResultSet) -> T
    ): T? {
        return executeWithConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                setParameters(statement, params)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        mapper(resultSet)
                    } else {
                        null
                    }
                }
            }
        }
    }
    

    protected fun executeUpdate(sql: String, params: List<Any> = emptyList()): Int {
        return executeWithConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                setParameters(statement, params)
                statement.executeUpdate()
            }
        } ?: -1
    }
    

    protected fun executeInsertWithGeneratedKey(sql: String, params: List<Any> = emptyList()): Long? {
        return executeWithConnection { connection ->
            connection.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS).use { statement ->
                setParameters(statement, params)
                val affectedRows = statement.executeUpdate()
                
                if (affectedRows > 0) {
                    statement.generatedKeys.use { keys ->
                        if (keys.next()) {
                            keys.getLong(1)
                        } else {
                            null
                        }
                    }
                } else {
                    null
                }
            }
        }
    }
    

    protected fun executeBatch(sql: String, paramsList: List<List<Any>>): IntArray? {
        if (paramsList.isEmpty()) return intArrayOf()
        
        return executeWithConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                for (params in paramsList) {
                    setParameters(statement, params)
                    statement.addBatch()
                }
                statement.executeBatch()
            }
        }
    }
    

    protected fun <T> executeInTransaction(operation: (Connection) -> T): T? {
        return executeWithConnection { connection ->
            val originalAutoCommit = connection.autoCommit
            try {
                connection.autoCommit = false
                val result = operation(connection)
                connection.commit()
                result
            } catch (e: Exception) {
                try {
                    connection.rollback()
                    logDebug("事务回滚成功")
                } catch (rollbackException: SQLException) {
                    logError("事务回滚失败", rollbackException)
                }
                throw e
            } finally {
                try {
                    connection.autoCommit = originalAutoCommit
                } catch (e: SQLException) {
                    logError("恢复自动提交模式失败", e)
                }
            }
        }
    }
    

    private fun <T> executeWithConnection(operation: (Connection) -> T): T? {
        val connection = getConnection()
        if (connection == null) {
            logError("无法获取数据库连接", RuntimeException("Connection is null"))
            return null
        }
        
        return try {
            operation(connection)
        } catch (e: SQLException) {
            logError("数据库操作失败", e)
            null
        } catch (e: Exception) {
            logError("执行数据库操作时发生未知错误", e)
            null
        } finally {
            try {
                if (!connection.isClosed) {
                    connection.close()
                }
            } catch (e: SQLException) {
                logError("关闭数据库连接失败", e)
            }
        }
    }
    

    private fun setParameters(statement: PreparedStatement, params: List<Any>) {
        params.forEachIndexed { index, param ->
            statement.setObject(index + 1, param)
        }
    }
    

    protected fun tableExists(tableName: String): Boolean {
        return executeWithConnection { connection ->
            val metaData = connection.metaData
            metaData.getTables(null, null, tableName, arrayOf("TABLE")).use { resultSet ->
                resultSet.next()
            }
        } ?: false
    }
    
    protected fun logDebug(message: String) {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] ${this::class.simpleName}: $message")
        }
    }
    
    protected fun logError(message: String, exception: Exception) {
        plugin.logger.severe("${this::class.simpleName}: $message - ${exception.message}")
        if (plugin.isDebugEnabled()) {
            exception.printStackTrace()
        }
    }
    
    protected fun logWarning(message: String) {
        plugin.logger.warning("${this::class.simpleName}: $message")
    }
    
    protected fun logInfo(message: String) {
        plugin.logger.info("${this::class.simpleName}: $message")
    }
}
