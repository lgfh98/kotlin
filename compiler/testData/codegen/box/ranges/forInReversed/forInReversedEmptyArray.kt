// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val arr = intArrayOf()
    for (i in arr.reversed()) {
        throw AssertionError("Loop should not be executed")
    }

    return "OK"
}