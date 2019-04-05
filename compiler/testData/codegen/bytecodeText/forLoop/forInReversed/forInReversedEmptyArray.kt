// IGNORE_BACKEND: JVM
// Non-IR backend does not optimize for-loop over reversed arrays.
import kotlin.test.*

fun box(): String {
    val arr = intArrayOf()
    for (i in arr.reversed()) {
        throw AssertionError("Loop should not be executed")
    }

    return "OK"
}

// 0 reversed
// 0 iterator
