package io.github.abc15018045126.sora.langs.textmate.registry

import io.github.abc15018045126.sora.langs.textmate.registry.model.ThemeModel
import org.eclipse.tm4e.core.registry.IThemeSource

class ThemeRegistry {

    private val allListener = mutableListOf<ThemeChangeListener>()
    private val allThemeModel = mutableListOf<ThemeModel>()

    var currentThemeModel: ThemeModel = ThemeModel.EMPTY
        private set

    @Throws(Exception::class)
    @JvmOverloads
    fun loadTheme(themeSource: IThemeSource, isCurrentTheme: Boolean = true) {
        loadTheme(ThemeModel(themeSource), isCurrentTheme)
    }

    @Throws(Exception::class)
    @JvmOverloads
    fun loadTheme(themeModel: ThemeModel, isCurrentTheme: Boolean = true) {
        synchronized(this) {
            if (!themeModel.isLoaded) {
                themeModel.load()
            }
            val theme = findThemeByThemeName(themeModel.name)
            if (theme != null) {
                setTheme(theme)
                return
            }
            allThemeModel.add(themeModel)
            if (isCurrentTheme) {
                setTheme(themeModel)
            }
        }
    }

    fun findThemeByFileName(name: String): ThemeModel? {
        return allThemeModel.find { it.name == name }
    }

    fun findThemeByThemeName(name: String): ThemeModel? {
        return allThemeModel.find { it.rawTheme?.name == name }
    }

    @Synchronized
    fun setTheme(name: String): Boolean {
        var targetModel = findThemeByFileName(name)
        if (targetModel != null) {
            setTheme(targetModel)
            return true
        }
        targetModel = findThemeByThemeName(name)
        if (targetModel != null) {
            setTheme(targetModel)
            return true
        }
        return false
    }

    fun setTheme(theme: ThemeModel) {
        currentThemeModel = theme
        if (!allThemeModel.contains(theme)) {
            allThemeModel.add(theme)
        }
        if (!theme.isLoaded) {
            try {
                theme.load()
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
        dispatchThemeChange(currentThemeModel)
    }

    private fun dispatchThemeChange(targetThemeModel: ThemeModel) {
        val listeners = synchronized(this) { allListener.toList() }
        for (listener in listeners) {
            listener.onChangeTheme(targetThemeModel)
        }
    }

    fun hasListener(themeChangeListener: ThemeChangeListener): Boolean {
        return synchronized(this) { allListener.contains(themeChangeListener) }
    }

    @Synchronized
    fun addListener(themeChangeListener: ThemeChangeListener) {
        allListener.add(themeChangeListener)
    }

    @Synchronized
    fun removeListener(themeChangeListener: ThemeChangeListener) {
        allListener.remove(themeChangeListener)
    }

    fun dispose() {
        synchronized(this) {
            allListener.clear()
        }
    }

    fun interface ThemeChangeListener {
        fun onChangeTheme(newTheme: ThemeModel)
    }

    companion object {
        @get:Synchronized
        private var internalInstance: ThemeRegistry? = null
            get() {
                if (field == null) {
                    field = ThemeRegistry()
                }
                return field
            }

        @JvmStatic
        fun getInstance(): ThemeRegistry = internalInstance!!
    }
}
