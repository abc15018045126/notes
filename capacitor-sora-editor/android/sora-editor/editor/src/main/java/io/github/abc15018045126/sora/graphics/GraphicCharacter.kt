package io.github.abc15018045126.sora.graphics

import io.github.abc15018045126.sora.util.MyCharacter

object GraphicCharacter {

    @JvmStatic
    fun isCombiningCharacter(codePoint: Int): Boolean {
        return MyCharacter.isVariationSelector(codePoint) || MyCharacter.isFitzpatrick(codePoint)
                || MyCharacter.isZWJ(codePoint) || MyCharacter.isZWNJ(codePoint) ||
                MyCharacter.couldBeEmoji(codePoint)
                || (Character.charCount(codePoint) == 1 && Character.isSurrogate(codePoint.toChar()))
                || isASCIICombiningSymbol(codePoint)
    }

    @JvmStatic
    fun isASCIICombiningSymbol(codePoint: Int): Boolean {
        return codePoint == '.'.code || codePoint == '/'.code || codePoint == '!'.code || codePoint == '='.code ||
                codePoint == '<'.code || codePoint == '>'.code || codePoint == '-'.code
    }

    @JvmStatic
    fun couldBeEmojiPart(codePoint: Int): Boolean {
        return MyCharacter.isVariationSelector(codePoint) || MyCharacter.isFitzpatrick(codePoint)
                || MyCharacter.isZWJ(codePoint) || MyCharacter.isZWNJ(codePoint) ||
                MyCharacter.couldBeEmoji(codePoint)
    }
}
