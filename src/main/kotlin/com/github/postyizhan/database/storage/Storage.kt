package com.github.postyizhan.database.storage

import com.github.postyizhan.model.Warp
import java.util.*

interface Storage {
    // 初始化数据库
    fun init()
    
    // 关闭数据库连接
    fun close()
    
    // 创建地标
    fun createWarp(warp: Warp): Boolean
    
    // 删除地标
    fun deleteWarp(name: String, ownerUUID: UUID): Boolean
    
    // 获取特定地标
    fun getWarp(name: String): Warp?
    
    // 获取玩家的所有地标
    fun getWarpsByOwner(ownerUUID: UUID): List<Warp>
    
    // 获取所有地标
    fun getAllWarps(): List<Warp>
    
    // 增加地标访问次数
    fun incrementVisits(warpId: Int): Boolean
    
    // 获取玩家地标数量
    fun getWarpCount(ownerUUID: UUID): Int
    
    // 检查地标是否存在
    fun warpExists(name: String): Boolean
}
