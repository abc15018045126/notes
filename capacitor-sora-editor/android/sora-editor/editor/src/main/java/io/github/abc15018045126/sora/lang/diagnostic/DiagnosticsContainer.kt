package io.github.abc15018045126.sora.lang.diagnostic

/**
 * A thread-safe class for containing diagnostics
 *
 * @author abc15018045126
 */
class DiagnosticsContainer @JvmOverloads constructor(
    private val shiftEnabled: Boolean = true
) {

    private val regions = mutableListOf<DiagnosticRegion>()

    /**
     * Add multiple diagnostics
     */
    @Synchronized
    fun addDiagnostics(regions: Collection<DiagnosticRegion>) {
        this.regions.addAll(regions)
    }

    /**
     * Add single diagnostic item
     */
    @Synchronized
    fun addDiagnostic(diagnostic: DiagnosticRegion) {
        regions.add(diagnostic)
    }

    /**
     * Query diagnostics that can be displayed either partly or fully in the given region
     *
     * @param result     Destination of result
     * @param startIndex Start index of query
     * @param endIndex   End index of query
     */
    @Synchronized
    fun queryInRegion(result: MutableList<DiagnosticRegion>, startIndex: Int, endIndex: Int) {
        for (region in regions) {
            if (region.endIndex > startIndex && region.startIndex <= endIndex) {
                result.add(region)
            }
        }
    }

    @Synchronized
    fun shiftOnInsert(insertStart: Int, insertEnd: Int) {
        if (!shiftEnabled) {
            return
        }
        val length = insertEnd - insertStart
        for (region in regions) {
            // Type 1, text is inserted inside a diagnostic
            if (region.startIndex <= insertStart && region.endIndex >= insertStart) {
                region.endIndex += length
            }
            // Type 2, text is inserted before a diagnostic
            if (region.startIndex > insertStart) {
                region.startIndex += length
                region.endIndex += length
            }
        }
    }

    @Synchronized
    fun shiftOnDelete(deleteStart: Int, deleteEnd: Int) {
        if (!shiftEnabled) {
            return
        }
        val length = deleteEnd - deleteStart
        val garbage = mutableListOf<DiagnosticRegion>()
        for (region in regions) {
            // Compute cross length
            val sharedStart = maxOf(deleteStart, region.startIndex)
            val sharedEnd = minOf(deleteEnd, region.endIndex)
            if (sharedEnd <= sharedStart) {
                // No shared region
                if (region.startIndex >= deleteEnd) {
                    // Shift left
                    region.startIndex -= length
                    region.endIndex -= length
                }
            } else {
                // Has shared region
                val sharedLength = sharedEnd - sharedStart
                region.endIndex -= sharedLength
                if (region.startIndex > deleteStart) {
                    // Shift left
                    val shiftLeftCount = region.startIndex - deleteStart
                    region.startIndex -= shiftLeftCount
                    region.endIndex -= shiftLeftCount
                }

                if (region.startIndex == region.endIndex) {
                    garbage.add(region)
                }
            }
        }
        regions.removeAll(garbage)
    }

    /**
     * Remove all items
     */
    @Synchronized
    fun reset() {
        regions.clear()
    }
}
