package com.github.postyizhan.util

import com.github.postyizhan.PostWarps
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

object MessageUtil {

    private lateinit var plugin: PostWarps
    private lateinit var messages: YamlConfiguration
    private lateinit var prefix: String


    private val languageMessages = ConcurrentHashMap<String, YamlConfiguration>()


    private val playerLanguagePreferences = ConcurrentHashMap<String, String>()
    

    fun init(plugin: PostWarps) {
        this.plugin = plugin
        loadMessages()
    }
    

    private fun loadMessages() {
        val language = plugin.getConfigManager().getConfig().getString("language", "zh_CN")
        val langFile = File(plugin.dataFolder, "lang/$language.yml")
        

        if (!langFile.exists()) {
            plugin.saveResource("lang/$language.yml", false)
        }
        

        messages = try {
            YamlConfiguration.loadConfiguration(langFile)
        } catch (e: Exception) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Failed to load language file from disk, using built-in resource: ${e.message}")
            }
            val resource = plugin.getResource("lang/$language.yml") ?: plugin.getResource("lang/zh_CN.yml")
            if (resource != null) {
                YamlConfiguration.loadConfiguration(InputStreamReader(resource))
            } else {
                YamlConfiguration()
            }
        }
        

        prefix = messages.getString("prefix", "&8[&3Post&bWarps&8] ")
    }
    

    fun getMessage(path: String): String {
        var message = messages.getString(path)
        if (message == null) {
            if (plugin.isDebugEnabled()) {
                plugin.logger.warning("Message path not found: $path")
            }
            message = "&cMessage not found: $path"
        }
        return message.replace("{prefix}", prefix)
    }


    fun getMessage(path: String, player: Player): String {
        val language = getPlayerLanguage(player)
        val langMessages = getLanguageMessages(language)

        var message = langMessages.getString(path)
        if (message == null) {

            message = messages.getString(path)
            if (message == null) {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.warning("Message path not found: $path")
                }
                message = "&cMessage not found: $path"
            }
        }

        val langPrefix = langMessages.getString("prefix") ?: prefix
        return message.replace("{prefix}", langPrefix)
    }
    

    fun color(message: String): String {
        return ChatColor.translateAlternateColorCodes('&', message)
    }


    fun getPlayerLanguage(player: Player): String {

        val preference = playerLanguagePreferences[player.uniqueId.toString()]
        if (preference != null && preference != "auto") {
            return preference
        }


        val clientLanguage = try {
            player.locale
        } catch (e: Exception) {
            null
        }


        val supportedLanguage = when (clientLanguage) {
            "zh_cn", "zh_CN" -> "zh_CN"
            "en_us", "en_US" -> "en_US"
            else -> null
        }

        if (supportedLanguage != null) {
            return supportedLanguage
        }


        return plugin.getConfigManager().getConfig().getString("language", "zh_CN") ?: "zh_CN"
    }


    fun setPlayerLanguage(player: Player, language: String) {
        if (isLanguageSupported(language)) {
            playerLanguagePreferences[player.uniqueId.toString()] = language
            plugin.logger.info("Player ${player.name} language preference set to: $language")
        } else {
            plugin.logger.warning("Unsupported language: $language")
        }
    }


    fun clearPlayerLanguage(player: Player) {
        playerLanguagePreferences.remove(player.uniqueId.toString())
        plugin.logger.info("Player ${player.name} language preference cleared")
    }


    fun isLanguageSupported(language: String): Boolean {
        return language in listOf("zh_CN", "en_US")
    }


    fun getSupportedLanguages(): List<String> {
        return listOf("zh_CN", "en_US")
    }


    private fun getLanguageMessages(language: String): YamlConfiguration {
        return languageMessages.getOrPut(language) {
            loadLanguageMessages(language)
        }
    }


    private fun loadLanguageMessages(language: String): YamlConfiguration {
        val langFile = File(plugin.dataFolder, "lang/$language.yml")

        if (langFile.exists()) {
            try {
                val config = YamlConfiguration.loadConfiguration(langFile)
                plugin.logger.info("Successfully loaded language file: $language.yml from data folder")
                return config
            } catch (e: Exception) {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.warning("Failed to load language file $language.yml: ${e.message}")
                }
            }
        } else {

            try {
                plugin.getResource("lang/$language.yml")?.use { inputStream ->
                    InputStreamReader(inputStream, "UTF-8").use { reader ->
                        val config = YamlConfiguration.loadConfiguration(reader)
                        plugin.logger.info("Successfully loaded language file: $language.yml from resources")
                        return config
                    }
                }
            } catch (e: Exception) {
                if (plugin.isDebugEnabled()) {
                    plugin.logger.warning("Failed to load language file $language.yml from resources: ${e.message}")
                }
            }
        }


        if (plugin.isDebugEnabled()) {
            plugin.logger.warning("Language file $language.yml not found, using default language")
        }
        return messages
    }


    fun reloadLanguages() {
        languageMessages.clear()
        plugin.logger.info("Language cache cleared, language files will be reloaded")
    }
    

    fun sendMessage(sender: CommandSender, message: String) {
        sender.sendMessage(color(message))
    }
    

    fun process(message: String, vararg args: Pair<String, String>): String {
        var result = message
        args.forEach { (key, value) ->
            result = result.replace("{$key}", value)
        }
        return result
    }
}
