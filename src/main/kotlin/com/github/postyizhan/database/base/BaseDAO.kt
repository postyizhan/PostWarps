package com.github.postyizhan.database.base

import com.github.postyizhan.PostWarps
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException

/**
 * 数据库访问对象基类 - 提供通用的数据库操作模式
 * 统一错误处理、资源管理和日志记录
 */
abstract class BaseDAO(protected val plugin: PostWarps) {
    
    /**
     * 获取数据库连接
     * 子类需要实现此方法来提供具体的连接获取逻辑
     */
    protected abstract fun getConnection(): Connection?
    
    /**
     * 执行查询操作
     * @param sql SQL查询语句
     * @param params 查询参数
     * @param mapper 结果映射函数
     * @return 查询结果列表
     */
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
    
    /**
     * 执行单个查询操作
     * @param sql SQL查询语句
     * @param params 查询参数
     * @param mapper 结果映射函数
     * @return 查询结果，如果没有结果则返回null
     */
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
    
    /**
     * 执行更新操作（INSERT, UPDATE, DELETE）
     * @param sql SQL更新语句
     * @param params 更新参数
     * @return 受影响的行数，如果操作失败则返回-1
     */
    protected fun executeUpdate(sql: String, params: List<Any> = emptyList()): Int {
        return executeWithConnection { connection ->
            connection.prepareStatement(sql).use { statement ->
                setParameters(statement, params)
                statement.executeUpdate()
            }
        } ?: -1
    }
    
    /**
     * 执行插入操作并返回生成的主键
     * @param sql SQL插入语句
     * @param params 插入参数
     * @return 生成的主键，如果操作失败则返回null
     */
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
    
    /**
     * 执行批量更新操作
     * @param sql SQL更新语句
     * @param paramsList 参数列表的列表
     * @return 每个操作受影响的行数数组，如果操作失败则返回null
     */
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
    
    /**
     * 在事务中执行操作
     * @param operation 要执行的操作
     * @return 操作结果
     */
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
    
    /**
     * 使用连接执行操作的通用模板方法
     * @param operation 要执行的操作
     * @return 操作结果，如果发生错误则返回null
     */
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
    
    /**
     * 设置PreparedStatement的参数
     * @param statement PreparedStatement对象
     * @param params 参数列表
     */
    private fun setParameters(statement: PreparedStatement, params: List<Any>) {
        params.forEachIndexed { index, param ->
            statement.setObject(index + 1, param)
        }
    }
    
    /**
     * 检查表是否存在
     * @param tableName 表名
     * @return 如果表存在则返回true
     */
    protected fun tableExists(tableName: String): Boolean {
        return executeWithConnection { connection ->
            val metaData = connection.metaData
            metaData.getTables(null, null, tableName, arrayOf("TABLE")).use { resultSet ->
                resultSet.next()
            }
        } ?: false
    }
    
    /**
     * 记录调试信息
     */
    protected fun logDebug(message: String) {
        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] ${this::class.simpleName}: $message")
        }
    }
    
    /**
     * 记录错误信息
     */
    protected fun logError(message: String, exception: Exception) {
        plugin.logger.severe("${this::class.simpleName}: $message - ${exception.message}")
        if (plugin.isDebugEnabled()) {
            exception.printStackTrace()
        }
    }
    
    /**
     * 记录警告信息
     */
    protected fun logWarning(message: String) {
        plugin.logger.warning("${this::class.simpleName}: $message")
    }
    
    /**
     * 记录信息
     */
    protected fun logInfo(message: String) {
        plugin.logger.info("${this::class.simpleName}: $message")
    }
}
