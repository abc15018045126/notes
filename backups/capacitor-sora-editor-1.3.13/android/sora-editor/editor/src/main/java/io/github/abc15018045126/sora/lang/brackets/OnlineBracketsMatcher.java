
package io.github.abc15018045126.sora.lang.brackets;

import androidx.annotation.NonNull;

import io.github.abc15018045126.sora.text.Content;

/**
 * Compute paired bracket when queried
 *
 * @author abc15018045126
 */
public class OnlineBracketsMatcher implements BracketsProvider {

    private final char[] pairs;
    private final int limit;

    /**
     * @param pairs Pairs. For example: {'(', ')', '{', '}'}
     * @param limit Max length to search
     */
    public OnlineBracketsMatcher(char[] pairs, int limit) {
        if ((pairs.length & 1) != 0) {
            throw new IllegalArgumentException("pairs must have even length");
        }
        this.pairs = pairs;
        this.limit = limit;
    }

    private int findIndex(char ch) {
        for (int i = 0; i < pairs.length; i++) {
            if (ch == pairs[i]) {
                return i;
            }
        }
        return -1;
    }

    private PairedBracket tryComputePaired(Content text, int index) {
        char a = text.charAt(index);
        int symbolIndex = findIndex(a);
        if (symbolIndex != -1) {
            char b = pairs[symbolIndex ^ 1];
            int stack = 0;
            if ((symbolIndex & 1) == 0) {
                // Find forward
                for (int i = index + 1; i < text.length() && i - index < limit; i++) {
                    char ch = text.charAt(i);
                    if (ch == b) {
                        if (stack <= 0) {
                            return new PairedBracket(index, i);
                        } else {
                            stack--;
                        }
                    } else if (ch == a) {
                        stack++;
                    }
                }
            } else {
                // Find backward
                for (int i = index - 1; i >= 0 && index - i < limit; i--) {
                    char ch = text.charAt(i);
                    if (ch == b) {
                        if (stack <= 0) {
                            return new PairedBracket(i, index);
                        } else {
                            stack--;
                        }
                    } else if (ch == a) {
                        stack++;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public PairedBracket getPairedBracketAt(@NonNull Content text, int index) {
        PairedBracket pairedBracket = null;
        if (index > 0) {
            pairedBracket = tryComputePaired(text, index - 1);
        }
        if (pairedBracket == null && index < text.length()) {
            pairedBracket = tryComputePaired(text, index);
        }
        return pairedBracket;
    }
}

