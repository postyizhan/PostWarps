package com.github.postyizhan.command

import com.github.postyizhan.PostWarps
import com.github.postyizhan.command.commands.RootCommand
import com.github.postyizhan.command.LanguageCommand

/**
 * 新的命令管理器，使用模块化设计
 */
class CommandManager(private val plugin: PostWarps) {

    private val rootCommand = RootCommand(plugin)
    private val languageCommand = LanguageCommand(plugin)

    /**
     * 注册命令
     */
    fun registerCommands() {
        // 注册主命令 /pw
        val pluginCommand = plugin.getCommand("postwarps")
        pluginCommand?.setExecutor(rootCommand)
        pluginCommand?.tabCompleter = rootCommand

        // 注册语言命令 /lang
        val langCommand = plugin.getCommand("lang")
        langCommand?.setExecutor(languageCommand)
        langCommand?.tabCompleter = languageCommand

        if (plugin.isDebugEnabled()) {
            plugin.logger.info("[DEBUG] Registered new modular command system")
        }
    }
}
