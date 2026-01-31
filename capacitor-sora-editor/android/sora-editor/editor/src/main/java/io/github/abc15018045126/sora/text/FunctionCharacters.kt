package io.github.abc15018045126.sora.text

/**
 * Utility for ASCII function characters
 *
 * @author abc15018045126
 */
object FunctionCharacters {

    private val names = arrayOf(
        "NUL", "SOH", "STX", "ETX", "EOT", "ENQ", "ACK",
        "BEL", "BS", "HT", "LF", "VT", "FF", "CR", "SO",
        "SI", "DLE", "DC1", "DC2", "DC3", "DC4", "NAK",
        "SYN", "ETB", "CAN", "EM", "SUB", "ESC", "FS",
        "GS", "RS", "US", "SP"
    )

    /**
     * Check if the letter is ASCII function character.
     */
    @JvmStatic
    fun isFunctionCharacter(letter: Char): Boolean {
        return letter.code < 32 || letter.code == 127
    }

    /**
     * Check if the letter is ASCII function character, '\t' excluded.
     */
    @JvmStatic
    fun isEditorFunctionChar(letter: Char): Boolean {
        return letter != '\t' && isFunctionCharacter(letter)
    }

    /**
     * Get the name of function character
     */
    @JvmStatic
    fun getNameForFunctionCharacter(letter: Char): String {
        if (letter.code < 32) {
            return names[letter.code]
        } else if (letter.code == 127) {
            return "DEL"
        }
        return "UNK"
    }
}
