package com.ops.disguisedphone

/**
 * In-memory only (resets to false on process death, which is the safe
 * default -- notifications stay blocked unless Setup is actively open).
 */
object SetupForeground {
    @Volatile
    var inForeground: Boolean = false
}
