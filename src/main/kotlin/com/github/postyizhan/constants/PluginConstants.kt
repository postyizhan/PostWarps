package com.github.postyizhan.constants

/**
 * 插件常量定义 - 统一管理所有硬编码值
 * 提高代码的可配置性和可维护性
 */
object PluginConstants {
    
    /**
     * 网络相关常量
     */
    object Network {
        const val DEFAULT_CONNECT_TIMEOUT = 5000
        const val DEFAULT_READ_TIMEOUT = 10000
        const val USER_AGENT = "PostWarps UpdateChecker"
        const val GITHUB_API_ACCEPT_HEADER = "application/vnd.github.v3+json"
    }
    
    /**
     * 缓存相关常量
     */
    object Cache {
        const val DEFAULT_CLEANUP_INTERVAL_MINUTES = 5L
        const val DEFAULT_CACHE_EXPIRY_MINUTES = 30L
        const val MAX_CACHE_SIZE = 1000
    }
    
    /**
     * 菜单相关常量
     */
    object Menu {
        // 默认菜单列表
        val DEFAULT_MENUS = listOf("main", "create", "private_warps", "public_warps", "settings")

        // 菜单目录
        const val MENU_DIRECTORY = "menu"
        const val MENU_FILE_EXTENSION = ".yml"
    }
    
    /**
     * 数据库相关常量
     */
    object Database {
        // 连接池配置
        const val DEFAULT_POOL_SIZE = 10
        const val MIN_IDLE_CONNECTIONS = 2
        const val CONNECTION_TIMEOUT = 30000L
        const val IDLE_TIMEOUT = 600000L
        const val MAX_LIFETIME = 1800000L
        
        // 表名
        const val WARPS_TABLE = "warps"
        
        // 字段名
        const val FIELD_ID = "id"
        const val FIELD_NAME = "name"
        const val FIELD_OWNER = "owner"
        const val FIELD_OWNER_NAME = "owner_name"
        const val FIELD_WORLD_NAME = "world_name"
        const val FIELD_X = "x"
        const val FIELD_Y = "y"
        const val FIELD_Z = "z"
        const val FIELD_YAW = "yaw"
        const val FIELD_PITCH = "pitch"
        const val FIELD_IS_PUBLIC = "is_public"
        const val FIELD_DESCRIPTION = "description"
        const val FIELD_MATERIAL = "material"
        const val FIELD_CREATE_TIME = "create_time"
        
        // 默认值
        const val DEFAULT_MATERIAL = "ENDER_PEARL"
    }
    
    /**
     * 地标相关常量
     */
    object Warp {
        const val MAX_NAME_LENGTH = 32
        const val MAX_DESCRIPTION_LENGTH = 255
        const val MAX_OWNER_NAME_LENGTH = 16
        const val MAX_WORLD_NAME_LENGTH = 64
        const val MAX_MATERIAL_NAME_LENGTH = 64
    }
    
    /**
     * 权限相关常量
     */
    object Permissions {
        const val USE = "postwarps.use"
        const val CREATE = "postwarps.create"
        const val DELETE = "postwarps.delete"
        const val DELETE_OTHERS = "postwarps.delete.others"
        const val TELEPORT = "postwarps.teleport"
        const val LIST = "postwarps.list"
        const val INFO = "postwarps.info"
        const val PUBLIC = "postwarps.public"
        const val PRIVATE = "postwarps.private"
        const val MENU = "postwarps.menu"
        const val ADMIN = "postwarps.admin"
        const val VERSION = "postwarps.version"
    }
    
    /**
     * 命令相关常量
     */
    object Commands {
        const val ROOT_COMMAND = "postwarps"
        val ROOT_ALIASES = listOf("pw", "warp", "warps")
        const val LANGUAGE_COMMAND = "lang"
        val LANGUAGE_ALIASES = listOf("language")
    }
    
    /**
     * 配置相关常量
     */
    object Config {
        // 配置文件路径
        const val MAIN_CONFIG = "config.yml"
        const val GROUPS_CONFIG = "groups.yml"
        const val LANG_DIRECTORY = "lang"
        
        // 默认配置值
        const val DEFAULT_LANGUAGE = "zh_CN"
        const val DEFAULT_PREFIX = "&8[&3Post&bWarps&8] "
        const val DEFAULT_DEBUG = false
        
        // 配置键
        const val KEY_LANGUAGE = "language"
        const val KEY_PREFIX = "prefix"
        const val KEY_DEBUG = "debug"
        const val KEY_UPDATE_CHECKER_ENABLED = "update-checker.enabled"
        const val KEY_UPDATE_CHECKER_INTERVAL = "update-checker.check-interval-days"
        const val KEY_DATABASE_TYPE = "database.type"
        
        // 数据库配置键
        object Database {
            const val MYSQL_HOST = "database.mysql.host"
            const val MYSQL_PORT = "database.mysql.port"
            const val MYSQL_DATABASE = "database.mysql.database"
            const val MYSQL_USERNAME = "database.mysql.username"
            const val MYSQL_PASSWORD = "database.mysql.password"
            const val MYSQL_USE_SSL = "database.mysql.use-ssl"
            const val MYSQL_POOL_SIZE = "database.mysql.pool-size"
            
            const val SQLITE_FILE = "database.sqlite.file"
        }
    }
    
    /**
     * 消息相关常量
     */
    object Messages {
        // 系统消息键
        const val DATABASE_CONNECTED = "messages.database_connected"
        const val DATABASE_CLOSED = "messages.database_closed"
        const val PLUGIN_ENABLED = "messages.plugin_enabled"
        const val PLUGIN_DISABLED = "messages.plugin_disabled"
        
        // 更新检查消息键
        const val UPDATE_AVAILABLE = "system.updater.update_available"
        const val UPDATE_URL = "system.updater.update_url"
        const val UP_TO_DATE = "system.updater.up_to_date"
        
        // 帮助消息键
        const val HELP_HEADER = "help.header"
        const val HELP_FOOTER = "help.footer"
    }
    
    /**
     * 文件相关常量
     */
    object Files {
        const val DATABASE_SQLITE_DEFAULT = "database.db"
        const val BACKUP_DIRECTORY = "backups"
        const val LOGS_DIRECTORY = "logs"
    }
    
    /**
     * 版本相关常量
     */
    object Version {
        const val MIN_BUKKIT_VERSION = "1.13"
        const val CURRENT_API_VERSION = "1.13"
    }
    
    /**
     * 调试相关常量
     */
    object Debug {
        const val PREFIX = "[DEBUG]"
        const val MAX_LOG_LENGTH = 1000
    }
    
    /**
     * 玩家菜单历史相关常量
     */
    object PlayerMenu {
        const val MAX_HISTORY_SIZE = 10
        const val DEFAULT_MENU_NAME = "main"
    }
    
    /**
     * 经济相关常量
     */
    object Economy {
        const val DEFAULT_CURRENCY_SYMBOL = "$"
        const val DEFAULT_POINTS_SYMBOL = "点"
    }
    
    /**
     * 传送相关常量
     */
    object Teleport {
        const val DEFAULT_DELAY_SECONDS = 3
        const val MAX_DELAY_SECONDS = 60
        const val WARMUP_TASK_NAME = "teleport-warmup"
    }
}
