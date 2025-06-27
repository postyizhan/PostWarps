package com.github.postyizhan.gui.util

import com.github.postyizhan.PostWarps
import com.github.postyizhan.util.MessageUtil
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player

/**
 * 菜单国际化处理器
 * 负责处理菜单项的多语言支持
 */
class MenuI18nProcessor(private val plugin: PostWarps) {
    
    /**
     * 获取本地化的物品名称
     * @param itemConfig 物品配置
     * @param player 玩家
     * @return 本地化的名称，如果没有找到则返回默认名称
     */
    fun getLocalizedName(itemConfig: ConfigurationSection, player: Player): String? {
        // 获取玩家语言
        val language = getPlayerLanguage(player)
        
        // 尝试从i18n配置获取
        val i18nName = getI18nValue(itemConfig, "name", language)
        if (i18nName != null) {
            return i18nName
        }
        
        // 回退到默认名称
        return itemConfig.getString("name")
    }
    
    /**
     * 获取本地化的物品描述
     * @param itemConfig 物品配置
     * @param player 玩家
     * @return 本地化的描述列表，如果没有找到则返回默认描述
     */
    fun getLocalizedLore(itemConfig: ConfigurationSection, player: Player): List<String>? {
        // 获取玩家语言
        val language = getPlayerLanguage(player)
        
        // 尝试从i18n配置获取
        val i18nLore = getI18nValueList(itemConfig, "lore", language)
        if (i18nLore != null) {
            return i18nLore
        }
        
        // 回退到默认描述
        return itemConfig.getStringList("lore").takeIf { it.isNotEmpty() }
    }
    
    /**
     * 获取本地化的子图标名称
     * @param iconConfig 子图标配置
     * @param player 玩家
     * @return 本地化的名称，如果没有找到则返回默认名称
     */
    fun getLocalizedIconName(iconConfig: ConfigurationSection, player: Player): String? {
        // 获取玩家语言
        val language = getPlayerLanguage(player)
        
        // 尝试从i18n配置获取
        val i18nName = getI18nValue(iconConfig, "name", language)
        if (i18nName != null) {
            return i18nName
        }
        
        // 回退到默认名称
        return iconConfig.getString("name")
    }
    
    /**
     * 获取本地化的子图标描述
     * @param iconConfig 子图标配置
     * @param player 玩家
     * @return 本地化的描述列表，如果没有找到则返回默认描述
     */
    fun getLocalizedIconLore(iconConfig: ConfigurationSection, player: Player): List<String>? {
        // 获取玩家语言
        val language = getPlayerLanguage(player)
        
        // 尝试从i18n配置获取
        val i18nLore = getI18nValueList(iconConfig, "lore", language)
        if (i18nLore != null) {
            return i18nLore
        }
        
        // 回退到默认描述
        return iconConfig.getStringList("lore").takeIf { it.isNotEmpty() }
    }
    
    /**
     * 获取玩家的语言设置
     * @param player 玩家
     * @return 语言代码
     */
    private fun getPlayerLanguage(player: Player): String {
        // 使用统一的语言获取方法，支持手动设置的语言偏好
        return MessageUtil.getPlayerLanguage(player)
    }
    
    /**
     * 从i18n配置中获取字符串值
     * @param config 配置节点
     * @param key 键名
     * @param language 语言代码
     * @return 本地化的值，如果没有找到则返回null
     */
    private fun getI18nValue(config: ConfigurationSection, key: String, language: String): String? {
        val i18nSection = config.getConfigurationSection("i18n") ?: return null
        val languageSection = i18nSection.getConfigurationSection(language) ?: return null
        return languageSection.getString(key)
    }
    
    /**
     * 从i18n配置中获取字符串列表值
     * @param config 配置节点
     * @param key 键名
     * @param language 语言代码
     * @return 本地化的值列表，如果没有找到则返回null
     */
    private fun getI18nValueList(config: ConfigurationSection, key: String, language: String): List<String>? {
        val i18nSection = config.getConfigurationSection("i18n") ?: return null
        val languageSection = i18nSection.getConfigurationSection(language) ?: return null
        val list = languageSection.getStringList(key)
        return if (list.isNotEmpty()) list else null
    }
    
    /**
     * 检查配置是否包含i18n设置
     * @param config 配置节点
     * @return 是否包含i18n配置
     */
    fun hasI18nConfig(config: ConfigurationSection): Boolean {
        return config.contains("i18n")
    }
    
    /**
     * 获取本地化的菜单标题
     * @param menuConfig 菜单配置
     * @param player 玩家
     * @return 本地化的标题，如果没有找到则返回默认标题
     */
    fun getLocalizedTitle(menuConfig: ConfigurationSection, player: Player): String? {
        // 获取玩家语言
        val language = getPlayerLanguage(player)

        // 检查新的标题i18n格式 (title.zh_CN, title.en_US)
        val titleSection = menuConfig.getConfigurationSection("title")
        if (titleSection != null) {
            val localizedTitle = titleSection.getString(language)
            if (localizedTitle != null) {
                return localizedTitle
            }
            // 回退到默认语言
            val defaultTitle = titleSection.getString("zh_CN") ?: titleSection.getString("en_US")
            if (defaultTitle != null) {
                return defaultTitle
            }
        }

        // 尝试从旧的i18n配置获取
        val i18nTitle = getI18nValue(menuConfig, "title", language)
        if (i18nTitle != null) {
            return i18nTitle
        }

        // 回退到默认标题
        return menuConfig.getString("title")
    }

    /**
     * 获取支持的语言列表
     * @param config 配置节点
     * @return 支持的语言代码列表
     */
    fun getSupportedLanguages(config: ConfigurationSection): Set<String> {
        val i18nSection = config.getConfigurationSection("i18n") ?: return emptySet()
        return i18nSection.getKeys(false)
    }
}
