package io.github.abc15018045126.sora.widget.snippet.variable

import java.util.Random
import java.util.UUID

class RandomBasedSnippetVariableResolver : ISnippetVariableResolver {

    override fun getResolvableNames(): Array<String> {
        return arrayOf(
            "RANDOM", "RANDOM_HEX", "UUID"
        )
    }

    override fun resolve(name: String): String {
        return when (name) {
            "RANDOM" -> Random().nextInt().toString()
            "RANDOM_HEX" -> Random().nextInt().toString(16)
            "UUID" -> UUID.randomUUID().toString()
            else -> throw IllegalArgumentException("Unsupported variable name:$name")
        }
    }
}
