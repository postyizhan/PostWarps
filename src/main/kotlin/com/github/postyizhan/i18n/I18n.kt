package com.github.postyizhan.i18n

import com.github.postyizhan.PostWarps
import org.bukkit.ChatColor
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*

class I18n(private val plugin: PostWarps) {
    private val messages: MutableMap<String, Properties> = HashMap()
    private lateinit var currentLang: String
    
    init {
        // 加载语言文件
        loadLanguages()
    }
    
    private fun loadLanguages() {
        // 获取当前语言
        currentLang = plugin.language
        
        // 确保语言文件存在
        saveDefaultLanguageFiles()
        
        // 加载所有语言文件
        val langFolder = File(plugin.dataFolder, "lang")
        if (langFolder.exists() && langFolder.isDirectory) {
            langFolder.listFiles()?.forEach { file ->
                if (file.isFile && file.name.endsWith(".properties")) {
                    val langCode = file.name.replace(".properties", "")
                    val props = Properties()
                    try {
                        props.load(InputStreamReader(file.inputStream(), StandardCharsets.UTF_8))
                        messages[langCode] = props
                    } catch (e: Exception) {
                        plugin.logger.warning("Failed to load language file: ${file.name}")
                        e.printStackTrace()
                    }
                }
            }
        }
        
        // 确保至少有一个默认语言
        if (!messages.containsKey(currentLang)) {
            plugin.logger.warning("Language $currentLang not found, falling back to en_US")
            currentLang = "en_US"
        }
        
        if (plugin.debugMode) {
            plugin.logger.info("Loaded ${messages.size} language files")
        }
    }
    
    private fun saveDefaultLanguageFiles() {
        val langFolder = File(plugin.dataFolder, "lang")
        if (!langFolder.exists()) {
            langFolder.mkdirs()
        }
        
        // 保存默认语言文件
        saveResourceIfNotExists("lang/zh_CN.properties")
        saveResourceIfNotExists("lang/en_US.properties")
    }
    
    private fun saveResourceIfNotExists(resourcePath: String) {
        val resourceFile = File(plugin.dataFolder, resourcePath)
        if (!resourceFile.exists()) {
            plugin.saveResource(resourcePath, false)
        }
    }
    
    fun getMessage(key: String, vararg args: Any): String {
        val props = messages[currentLang] ?: messages["en_US"] ?: Properties()
        var message = props.getProperty(key)
        
        if (message == null) {
            // 回退到英语
            message = messages["en_US"]?.getProperty(key) ?: "Missing message: $key"
        }
        
        // 替换参数
        var result = message
        for (i in args.indices) {
            result = result.replace("{$i}", args[i].toString())
        }
        
        // 替换颜色代码
        return ChatColor.translateAlternateColorCodes('&', result)
    }
    
    fun getLanguage(): String {
        return currentLang
    }
    
    fun setLanguage(langCode: String) {
        if (messages.containsKey(langCode)) {
            currentLang = langCode
        } else {
            plugin.logger.warning("Language $langCode not found")
        }
    }
}
 