
package io.github.abc15018045126.sora.util.regex;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.regex.Matcher;

public class RegexBackrefHelper {

    public static String computeReplacement(@NonNull Matcher matcher, @NonNull RegexBackrefGrammar grammar, @NonNull String replacementPattern) {
        var parser = new RegexBackrefParser(grammar);
        var tokens = parser.parse(replacementPattern, matcher.groupCount());
        return computeReplacement(matcher, tokens);
    }

    public static String computeReplacement(@NonNull Matcher matcher, @NonNull List<RegexBackrefToken> tokens) {
        var sb = new StringBuilder();
        for (var token : tokens) {
            if (token.isReference()) {
                String text = matcher.group(token.getGroup());
                sb.append(text == null ? "" : text);
            } else {
                sb.append(token.getText());
            }
        }
        return sb.toString();
    }

}

