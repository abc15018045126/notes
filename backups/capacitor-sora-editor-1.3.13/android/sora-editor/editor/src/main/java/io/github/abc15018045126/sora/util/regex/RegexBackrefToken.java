
package io.github.abc15018045126.sora.util.regex;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.regex.Matcher;

public class RegexBackrefToken {

    private final boolean isRef;

    private final String text;

    private final int group;

    public RegexBackrefToken(boolean isRef, String text, int group) {
        this.isRef = isRef;
        this.text = text;
        this.group = group;
    }

    public String getReplacementText(@NonNull Matcher matcher) {
        if (isReference()) {
            return matcher.group(getGroup());
        }
        return text;
    }

    public boolean isReference() {
        return isRef;
    }

    public String getText() {
        return text;
    }

    public int getGroup() {
        return group;
    }

}

