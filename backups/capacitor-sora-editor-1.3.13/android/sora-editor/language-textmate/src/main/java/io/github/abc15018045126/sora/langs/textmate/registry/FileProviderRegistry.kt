package io.github.abc15018045126.sora.langs.textmate.registry

import io.github.abc15018045126.sora.langs.textmate.registry.provider.FileResolver
import java.io.InputStream

class FileProviderRegistry private constructor() {

    private val allFileResolvers = mutableListOf<FileResolver>()

    init {
        allFileResolvers.add(FileResolver.DEFAULT)
    }

    @Synchronized
    fun addFileProvider(fileResolver: FileResolver) {
        if (fileResolver != FileResolver.DEFAULT) {
            allFileResolvers.add(fileResolver)
        }
    }

    @Synchronized
    fun removeFileProvider(fileResolver: FileResolver) {
        if (fileResolver != FileResolver.DEFAULT) {
            allFileResolvers.remove(fileResolver)
        }
    }

    fun tryGetInputStream(path: String): InputStream? {
        for (provider in allFileResolvers) {
            val stream = provider.resolveStreamByPath(path)
            if (stream != null) {
                return stream
            }
        }
        return null
    }

    fun dispose() {
        for (provider in allFileResolvers) {
            provider.dispose()
        }
        allFileResolvers.clear()
    }

    companion object {
        private val _instance: FileProviderRegistry by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { 
            FileProviderRegistry() 
        }

        @JvmStatic
        fun getInstance(): FileProviderRegistry = _instance
    }
}
