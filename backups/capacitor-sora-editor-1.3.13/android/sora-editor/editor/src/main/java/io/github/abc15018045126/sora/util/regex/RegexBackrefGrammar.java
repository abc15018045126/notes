
package io.github.abc15018045126.sora.util.regex;

public class RegexBackrefGrammar {

    public final static RegexBackrefGrammar DEFAULT = new RegexBackrefGrammar('$', '\\');

    public final char backrefStartChar;

    public final char escapeChar;

    public RegexBackrefGrammar(char backrefStartChar, char escapeChar) {
        this.backrefStartChar = backrefStartChar;
        this.escapeChar = escapeChar;
    }

}

