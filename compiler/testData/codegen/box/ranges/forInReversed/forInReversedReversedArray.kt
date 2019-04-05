// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME
import kotlin.test.*

fun box(): String {
    val arr = intArrayOf(1, 2, 3, 4)
    var sum = 0
    for (i in arr.reversed().reversed()) {
        sum = sum * 10 + i
    }
    assertEquals(1234, sum)

    return "OK"
}