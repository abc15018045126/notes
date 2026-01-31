package io.github.abc15018045126.sora.lang.diagnostic

/**
 * Class for describing a diagnostic region.
 *
 * @author abc15018045126
 */
class DiagnosticRegion @JvmOverloads constructor(
    /**
     * The start index of the diagnostic
     */
    @JvmField var startIndex: Int,
    /**
     * The end index of the diagnostic
     */
    @JvmField var endIndex: Int,
    /**
     * One diagnostic has only one severity specification
     *
     * @see SEVERITY_NONE
     * @see SEVERITY_TYPO
     * @see SEVERITY_WARNING
     * @see SEVERITY_ERROR
     */
    @JvmField var severity: Short,
    /**
     * Id specified by diagnostic provider
     */
    @JvmField var id: Long = 0,
    /**
     * The detail of the problem
     */
    @JvmField var detail: DiagnosticDetail? = null
) : Comparable<DiagnosticRegion> {

    override fun compareTo(other: DiagnosticRegion): Int {
        var cmp = startIndex.compareTo(other.startIndex)
        if (cmp == 0) {
            cmp = endIndex.compareTo(other.endIndex)
        }
        if (cmp == 0) {
            cmp = severity.compareTo(other.severity)
        }
        if (cmp == 0) {
            cmp = id.compareTo(other.id)
        }
        return cmp
    }

    companion object {
        const val SEVERITY_NONE: Short = 0
        const val SEVERITY_TYPO: Short = 1
        const val SEVERITY_WARNING: Short = 2
        const val SEVERITY_ERROR: Short = 3
    }
}
