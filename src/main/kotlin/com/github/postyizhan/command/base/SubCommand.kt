package com.github.postyizhan.command.base

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

/**
 * 子命令接口
 */
interface SubCommand {
    /**
     * 获取命令名称
     */
    fun getName(): String
    
    /**
     * 获取命令权限
     */
    fun getPermission(): String
    
    /**
     * 获取命令描述
     */
    fun getDescription(): String
    
    /**
     * 是否只允许玩家执行
     */
    fun isPlayerOnly(): Boolean = true
    
    /**
     * 执行命令
     * @param sender 命令发送者
     * @param args 命令参数
     * @return 是否成功执行
     */
    fun execute(sender: CommandSender, args: Array<String>): Boolean
    
    /**
     * 提供Tab补全
     * @param sender 命令发送者
     * @param args 当前参数
     * @return 补全列表
     */
    fun tabComplete(sender: CommandSender, args: Array<String>): List<String> = emptyList()
    
    /**
     * 获取子命令列表（用于多级命令）
     */
    fun getSubCommands(): Map<String, SubCommand> = emptyMap()
}
