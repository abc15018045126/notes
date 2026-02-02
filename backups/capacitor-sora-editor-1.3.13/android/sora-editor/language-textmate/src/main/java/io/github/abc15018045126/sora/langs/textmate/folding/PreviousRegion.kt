package io.github.abc15018045126.sora.langs.textmate.folding

class PreviousRegion(// indent or -2 if a marker
    @JvmField var indent: Int, // end line number for the region above
    @JvmField var endAbove: Int, // start line of the region. Only used for marker regions.
    @JvmField var line: Int
)
