
package io.github.abc15018045126.sora.lang.completion;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;

import io.github.abc15018045126.sora.text.TextUtils;

/**
 * Utility class to provide some useful matching functions in generating completion.
 *
 * @author abc15018045126
 */
public class MatchHelper {

    /**
     * Color for matched text highlighting
     */
    public int highlightColor = 0xff3f51b5;
    /**
     * Case in-sensitive
     */
    public boolean ignoreCase = false;
    /**
     * Match case of first letter if ignoreCase=true
     * <p>
     * for {@link #startsWith(CharSequence, CharSequence)} only
     */
    public boolean matchFirstCase = false;

    public Spanned startsWith(CharSequence name, CharSequence pattern) {
        return startsWith(name, pattern, matchFirstCase, ignoreCase);
    }

    public Spanned startsWith(CharSequence name, CharSequence pattern, boolean matchFirstCase, boolean ignoreCase) {
        if (name.length() >= pattern.length()) {
            final var len = pattern.length();
            var matches = true;
            for (int i = 0; i < len; i++) {
                char a = name.charAt(i);
                char b = pattern.charAt(i);
                if (!(a == b || ((ignoreCase && (i != 0 || !matchFirstCase)) && Character.toLowerCase(a) == Character.toLowerCase(b)))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                var spanned = new SpannableString(name);
                spanned.setSpan(new ForegroundColorSpan(highlightColor), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                return spanned;
            }
        }
        return null;
    }

    public Spanned contains(CharSequence name, CharSequence pattern) {
        return contains(name, pattern, ignoreCase);
    }

    public Spanned contains(CharSequence name, CharSequence pattern, boolean ignoreCase) {
        int index = TextUtils.indexOf(name, pattern, ignoreCase, 0);
        if (index != -1) {
            var spanned = new SpannableString(name);
            spanned.setSpan(new ForegroundColorSpan(highlightColor), index, index + pattern.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return spanned;
        }
        return null;
    }

    /**
     * Common sub-sequence
     */
    public Spanned commonSub(CharSequence name, CharSequence pattern) {
        return commonSub(name, pattern, ignoreCase);
    }

    /**
     * Common sub-sequence
     */
    public Spanned commonSub(CharSequence name, CharSequence pattern, boolean ignoreCase) {
        if (name.length() >= pattern.length()) {
            SpannableString spanned = null;
            var len = pattern.length();
            int j = 0;
            for (int i = 0; i < len; i++) {
                char p = pattern.charAt(i);
                var matched = false;
                for (; j < name.length() && !matched; j++) {
                    char s = name.charAt(j);
                    if (s == j || (ignoreCase && Character.toLowerCase(s) == Character.toLowerCase(p))) {
                        matched = true;
                        if (spanned == null) {
                            spanned = new SpannableString(name);
                        }
                        spanned.setSpan(new ForegroundColorSpan(highlightColor), j, j + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
                if (!matched) {
                    return null;
                }
            }
            return spanned;
        }
        return null;
    }

}

