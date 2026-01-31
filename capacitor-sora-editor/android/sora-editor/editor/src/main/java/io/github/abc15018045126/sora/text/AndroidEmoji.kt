/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.abc15018045126.sora.text

import android.icu.lang.UCharacter
import android.icu.lang.UProperty
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Taken from {@link android.text.Emoji}
 */
object AndroidEmoji {

    const val COMBINING_ENCLOSING_KEYCAP: Int = 0x20E3

    const val ZERO_WIDTH_JOINER: Int = 0x200D

    const val VARIATION_SELECTOR_16: Int = 0xFE0F

    const val CANCEL_TAG: Int = 0xE007F

    /**
     * Returns true if the given code point is regional indicator symbol.
     */
    @JvmStatic
    fun isRegionalIndicatorSymbol(codePoint: Int): Boolean {
        return codePoint in 0x1F1E6..0x1F1FF
    }

    /**
     * Returns true if the given code point is emoji modifier.
     */
    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.P)
    fun isEmojiModifier(codePoint: Int): Boolean {
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI_MODIFIER)
    }

    /**
     * Returns true if the given code point is emoji modifier base.
     *
     * @param c codepoint to check
     * @return true if is emoji modifier base
     */
    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.P)
    fun isEmojiModifierBase(c: Int): Boolean {
        // These two characters were removed from Emoji_Modifier_Base in Emoji 4.0, but we need to
        // keep them as emoji modifier bases since there are fonts and user-generated text out there
        // that treats these as potential emoji bases.
        if (c == 0x1F91D || c == 0x1F93C) {
            return true
        }
        // If Android's copy of ICU is behind, check for new codepoints here.
        // Consult log for implementation pattern.
        return UCharacter.hasBinaryProperty(c, UProperty.EMOJI_MODIFIER_BASE)
    }

    /**
     * Returns true if the character has Emoji property.
     */
    @JvmStatic
    @RequiresApi(api = Build.VERSION_CODES.P)
    fun isEmoji(codePoint: Int): Boolean {
        return UCharacter.hasBinaryProperty(codePoint, UProperty.EMOJI)
    }

    // Returns true if the character can be a base character of COMBINING ENCLOSING KEYCAP.
    @JvmStatic
    fun isKeycapBase(codePoint: Int): Boolean {
        return (codePoint in '0'.code..'9'.code) || codePoint == '#'.code || codePoint == '*'.code
    }

    /**
     * Returns true if the character can be a part of tag_spec in emoji tag sequence.
     * <p>
     * Note that 0xE007F (CANCEL TAG) is not included.
     */
    @JvmStatic
    fun isTagSpecChar(codePoint: Int): Boolean {
        return codePoint in 0xE0020..0xE007E
    }
}
