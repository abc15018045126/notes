
package io.github.abc15018045126.sora.widget.component;

/**
 * Builtin editor component.
 *
 * @author abc15018045126
 * @see EditorAutoCompletion
 * @see EditorTextActionWindow
 * @see Magnifier
 * @see EditorDiagnosticTooltipWindow
 */
public interface EditorBuiltinComponent {

    /**
     * Check whether this component is enabled
     */
    boolean isEnabled();

    /**
     * Enable/disable this builtin component
     */
    void setEnabled(boolean enabled);

}

