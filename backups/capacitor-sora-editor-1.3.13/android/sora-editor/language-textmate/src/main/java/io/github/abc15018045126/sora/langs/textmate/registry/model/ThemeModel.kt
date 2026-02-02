package io.github.abc15018045126.sora.langs.textmate.registry.model

import io.github.abc15018045126.sora.langs.textmate.utils.StringUtils
import org.eclipse.tm4e.core.internal.theme.Theme
import org.eclipse.tm4e.core.internal.theme.raw.IRawTheme
import org.eclipse.tm4e.core.internal.theme.raw.RawThemeReader
import org.eclipse.tm4e.core.registry.IThemeSource

class ThemeModel {
    var themeSource: IThemeSource? = null
        private set
    var rawTheme: IRawTheme? = null
        private set
    var theme: Theme? = null
        private set
    var name: String
    var isDark = false

    constructor(themeSource: IThemeSource) {
        this.themeSource = themeSource
        this.name = StringUtils.getFileNameWithoutExtension(themeSource.filePath)
    }

    constructor(themeSource: IThemeSource, name: String) {
        this.themeSource = themeSource
        this.name = name
    }

    private constructor(name: String) {
        this.themeSource = null
        this.rawTheme = null
        this.name = name
        this.theme = Theme.createFromRawTheme(null, null)
    }

    @Throws(Exception::class)
    @JvmOverloads
    fun load(colorMap: List<String>? = null) {
        rawTheme = RawThemeReader.readTheme(themeSource)
        theme = Theme.createFromRawTheme(rawTheme, colorMap)
    }

    val isLoaded: Boolean
        get() = theme != null

    companion object {
        @JvmField
        val EMPTY = ThemeModel("EMPTY")
    }
}
