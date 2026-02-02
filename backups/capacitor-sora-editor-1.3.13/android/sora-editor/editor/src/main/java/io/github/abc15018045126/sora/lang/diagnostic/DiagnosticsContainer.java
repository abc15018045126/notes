
package io.github.abc15018045126.sora.lang.diagnostic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A thread-safe class for containing diagnostics
 *
 * @author abc15018045126
 */
public class DiagnosticsContainer {

    private final List<DiagnosticRegion> regions = new ArrayList<>();
    private final boolean shiftEnabled;

    /**
     * Create a new DiagnosticsContainer, with auto-shifting enabled
     */
    public DiagnosticsContainer() {
        this(true);
    }

    /**
     * Create a new DiagnosticsContainer
     *
     * @param shiftEnabled Whether shift the positions when text is modified
     */
    public DiagnosticsContainer(boolean shiftEnabled) {
        this.shiftEnabled = shiftEnabled;
    }

    /**
     * Add multiple diagnostics
     */
    public synchronized void addDiagnostics(Collection<DiagnosticRegion> regions) {
        this.regions.addAll(regions);
    }

    /**
     * Add single diagnostic item
     */
    public synchronized void addDiagnostic(DiagnosticRegion diagnostic) {
        regions.add(diagnostic);
    }

    /**
     * Query diagnostics that can be displayed either partly or fully in the given region
     *
     * @param result     Destination of result
     * @param startIndex Start index of query
     * @param endIndex   End index of query
     */
    public synchronized void queryInRegion(List<DiagnosticRegion> result, int startIndex, int endIndex) {
        for (var region : regions) {
            if (region.endIndex > startIndex && region.startIndex <= endIndex) {
                result.add(region);
            }
        }
    }

    public synchronized void shiftOnInsert(int insertStart, int insertEnd) {
        if (!shiftEnabled) {
            return;
        }
        var length = insertEnd - insertStart;
        for (var region : regions) {
            // Type 1, text is inserted inside a diagnostic
            if (region.startIndex <= insertStart && region.endIndex >= insertStart) {
                region.endIndex += length;
            }
            // Type 2, text is inserted before a diagnostic
            if (region.startIndex > insertStart) {

                region.startIndex += length;
                region.endIndex += length;
            }
        }
    }

    public synchronized void shiftOnDelete(int deleteStart, int deleteEnd) {
        if (!shiftEnabled) {
            return;
        }
        var length = deleteEnd - deleteStart;
        var garbage = new ArrayList<DiagnosticRegion>();
        for (var region : regions) {
            // Compute cross length
            var sharedStart = Math.max(deleteStart, region.startIndex);
            var sharedEnd = Math.min(deleteEnd, region.endIndex);
            if (sharedEnd <= sharedStart) {
                // No shared region
                if (region.startIndex >= deleteEnd) {
                    // Shift left
                    region.startIndex -= length;
                    region.endIndex -= length;
                }
            } else {
                // Has shared region
                var sharedLength = sharedEnd - sharedStart;
                region.endIndex -= sharedLength;
                if (region.startIndex > deleteStart) {
                    // Shift left
                    var shiftLeftCount = region.startIndex - deleteStart;
                    region.startIndex -= shiftLeftCount;
                    region.endIndex -= shiftLeftCount;
                }

                if (region.startIndex == region.endIndex) {
                    garbage.add(region);
                }
            }
        }
        regions.removeAll(garbage);
    }

    /**
     * Remove all items
     */
    public synchronized void reset() {
        regions.clear();
    }

}

